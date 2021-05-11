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

import ch.ubique.notifyme.sdk.backend.ws.util.CryptoWrapper;
import com.goterl.lazycode.lazysodium.LazySodiumJava;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.exceptions.SodiumException;
import com.goterl.lazycode.lazysodium.interfaces.PwHash;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoTest {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  String healthAuthoritySkHex = "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39";
  String healthAuthorityPkHex = "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c";
  String useruploadMpkHex =
      "956e6fa1345547e8e060c8962ddd38863bf2c85406ed03b204bc340fb5db01296a960d00be240caa08db001664f4f7028a9dbbb33aea172bffd58b4a644f1ecb3b7bbed378a8a7c9756ac8b4b47346d8dbf37a62377703b7fc8da3bb22a21415";
  String useruploadMskHex = "ce23ca6a3fd0d1307d3d0b2578784750b3f0e20b64e0c24e4cafb35561a0af35";
  CryptoWrapper cryptoWrapper =
      new CryptoWrapper(
          healthAuthoritySkHex, healthAuthorityPkHex, useruploadMskHex, useruploadMpkHex);

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
        pwHash.cryptoPwHashStr(outputHash, pw, pw.length, PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);

    // ... or you can use the super-powered lazy functions.
    // For example, this is equivalent to the above.
    PwHash.Lazy pwHashLazy = (PwHash.Lazy) lazySodium;
    String hash =
        pwHashLazy.cryptoPwHashStr("A cool password", PwHash.OPSLIMIT_MIN, PwHash.MEMLIMIT_MIN);
  }

  @Test
  public void testFlow() {
    cryptoWrapper.getCryptoUtilV3().testFlow();
  }

  @Test
  public void testKeyGen() throws IOException {
    cryptoWrapper.getCryptoUtilV3().genKeys();
  }
}
