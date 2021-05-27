/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.util;

import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.PwHash;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibDependencyTest {

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
        boolean success =
                pwHash.cryptoPwHashStr(
                        outputHash, pw, pw.length, PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);

        // ... or you can use the super-powered lazy functions.
        // For example, this is equivalent to the above.
        PwHash.Lazy pwHashLazy = (PwHash.Lazy) lazySodium;
        String hash =
                pwHashLazy.cryptoPwHashStr(
                        "A cool password", PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);
    }
}
