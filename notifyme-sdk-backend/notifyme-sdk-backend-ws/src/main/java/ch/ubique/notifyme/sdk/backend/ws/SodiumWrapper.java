/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws;

import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.utils.LibraryLoadingException;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import ch.ubique.notifyme.sdk.backend.model.QRTraceOuterClass.QRTrace;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SodiumWrapper {

    private static final Logger logger = LoggerFactory.getLogger(SodiumWrapper.class);

    public final byte[] sk;
    public final byte[] pk;
    private final SodiumJava sodium;

    public SodiumJava getSodium() {
        return this.sodium;
    }

    public SodiumWrapper(String skHex, String pkHex) {
        try {
            this.sk = Hex.decodeHex(skHex);
        } catch (DecoderException e) {
            logger.error("unable to parse sk hexstring", e);
            throw new RuntimeException(e);
        }
        try {
            this.pk = Hex.decodeHex(pkHex);
        } catch (DecoderException e) {
            logger.error("unable to parse pk hexstring", e);
            throw new RuntimeException(e);
        }
        // Do custom loading for the libsodium lib, as it does not work out of the box
        // with spring boot bundled jars. To get a path to the full file, we copy
        // libsodium to a tmpfile and give that absolute path to lazysodium
        try {
            InputStream in =
                    getClass()
                            .getClassLoader()
                            .getResourceAsStream("libsodium/" + getSodiumPathInResources());
            File libTmpFile = File.createTempFile("libsodium", null);
            Files.copy(in, libTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            in.close();
            this.sodium = new SodiumJava(libTmpFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("unable to load libsodium", e);
            throw new RuntimeException(e);
        }
    }

    public byte[] decryptQrTrace(byte[] ctx) throws InvalidProtocolBufferException {
        byte[] msg = new byte[ctx.length - Box.SEALBYTES];
        sodium.crypto_box_seal_open(msg, ctx, ctx.length, pk, sk);
        return msg;
    }

    public byte[] deriveSecretKeyFromQRTrace(QRTrace qrTrace) {
        byte[] newPk = new byte[Box.PUBLICKEYBYTES];
        byte[] newSk = new byte[Box.SECRETKEYBYTES];
        byte[] innerHash = new byte[32];
        byte[] outerHash = new byte[32];
        
        byte[] contentBytes = qrTrace.getContent().toByteArray();
        byte[] innerHashIn = ArrayUtils.addAll(contentBytes, qrTrace.getR1().toByteArray());
        
        sodium.crypto_hash_sha256(innerHash, innerHashIn, innerHashIn.length);

        byte[] outerHashIn = ArrayUtils.addAll(innerHash, qrTrace.getR2().toByteArray());
        
        sodium.crypto_hash_sha256(outerHash, outerHashIn, outerHashIn.length);

        sodium.crypto_box_seed_keypair(newPk, newSk, outerHash);
        return newSk;
    }

    public byte[] createNonceForMessageEncytion() {
        byte[] nonce = new byte[Box.NONCEBYTES];
        sodium.randombytes_buf(nonce, nonce.length);
        return nonce;
    }

    public byte[] encryptMessage(byte[] secretKey, byte[] nonce, String message) {
        byte[] messageBytes = message.getBytes();
        byte[] encrytpedMessage = new byte[messageBytes.length + Box.MACBYTES];
        sodium.randombytes_buf(nonce, nonce.length);
        sodium.crypto_secretbox_easy(
                encrytpedMessage, messageBytes, messageBytes.length, nonce, secretKey);
        return encrytpedMessage;
    }

    /**
     * Returns the absolute path to sodium library inside JAR (beginning with '/'), e.g.
     * /linux/libsodium.so.
     */
    private static String getSodiumPathInResources() {
        boolean is64Bit = Native.POINTER_SIZE == 8;
        if (Platform.isWindows()) {
            if (is64Bit) {
                return getPath("windows64", "libsodium.dll");
            } else {
                return getPath("windows", "libsodium.dll");
            }
        }
        if (Platform.isARM()) {
            return getPath("armv6", "libsodium.so");
        }
        if (Platform.isLinux()) {
            if (is64Bit) {
                return getPath("linux64", "libsodium.so");
            } else {
                return getPath("linux", "libsodium.so");
            }
        }
        if (Platform.isMac()) {
            return getPath("mac", "libsodium.dylib");
        }

        String message =
                String.format(
                        "Unsupported platform: %s/%s",
                        System.getProperty("os.name"), System.getProperty("os.arch"));
        throw new LibraryLoadingException(message);
    }

    private static String getPath(String folder, String name) {
        String separator = "/";
        return folder + separator + name;
    }
}
