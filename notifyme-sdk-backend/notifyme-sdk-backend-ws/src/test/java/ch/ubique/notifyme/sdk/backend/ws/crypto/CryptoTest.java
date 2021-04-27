/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.crypto;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import com.goterl.lazycode.lazysodium.utils.KeyPair;
import org.junit.Test;

import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.PwHash;

import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoTest {

    String healthAuthoritySkHex = "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39";
    String healthAuthorityPkHex = "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c";
    String useruploadMpkHex = "4EA4588A04CCE9854EEFF50942EBB7D7DF6646A8F47124E9E035C2165C5BCFD52A0CBAC04ABD3B0BD1C955662D974F15EF118419249759B41245F46DFDBAAA0CAD074101A767F5566714E8A3CF2DC6D810D628FBA582706811C01869DD2C808B";
    String useruploadMskHex = "764F7BCC026EE4C2129B3FF488280FE96B62951FA9B9C34AC2E4B84D5B33121F";

    CryptoWrapper cryptoWrapper = new CryptoWrapper(healthAuthoritySkHex, healthAuthorityPkHex, useruploadMskHex, useruploadMpkHex);


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testDependency() throws SodiumException {
        SodiumJava sodium = new SodiumJava();
        LazySodiumJava lazySodium = new LazySodiumJava(sodium, StandardCharsets.UTF_8);
        // Now you can cast and use the enhanced native
        // Libsodium functions
        byte[] pw = lazySodium.bytes("A cool password");
        byte[] outputHash = new byte[PwHash.STR_BYTES];
        PwHash.Native pwHash = (PwHash.Native) lazySodium;
        boolean success = pwHash.cryptoPwHashStr(outputHash, pw, pw.length, PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);

        // ... or you can use the super-powered lazy functions.
        // For example, this is equivalent to the above.
        PwHash.Lazy pwHashLazy = (PwHash.Lazy) lazySodium;
        String hash = pwHashLazy.cryptoPwHashStr("A cool password", PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);
    }
    
    private byte[] fromBase64(String base64) throws UnsupportedEncodingException {
        return Base64.getUrlDecoder().decode(base64.getBytes("UTF-8"));
    }

    @Test
    public void testGenKeyPair() {
        final KeyPair kp = cryptoWrapper.keyGen();
    logger.info(
            String.format("Generated keypair as hex:\n {\n\t%s,\n\t %s\n}",
                    kp.getPublicKey().getAsHexString(), kp.getSecretKey().getAsHexString()));
    logger.info(
            String.format("Generated keypair as base64:\n {\n\t%s,\n\t %s\n}",
                    new String(Base64.getEncoder().encode(kp.getPublicKey().getAsBytes())), new String(Base64.getEncoder().encode(kp.getSecretKey().getAsBytes()))));
    logger.info(
            String.format("Generated keypair as bytes:\n {\n\t%s,\n\t %s\n}",
                    Arrays.toString(kp.getPublicKey().getAsBytes()), Arrays.toString(kp.getSecretKey().getAsBytes())));
    }
}
