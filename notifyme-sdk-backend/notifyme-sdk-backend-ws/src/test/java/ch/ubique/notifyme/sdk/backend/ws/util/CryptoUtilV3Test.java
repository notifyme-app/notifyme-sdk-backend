package ch.ubique.notifyme.sdk.backend.ws.util;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;

public class CryptoUtilV3Test {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  String healthAuthoritySkHex = "36b3b80a1cd2cc98d84b4ed2c109b74e7026f00c0d40a0b12a936b1814aa5e39";
  String healthAuthorityPkHex = "e4d2e06641730ce7c9986b1e7e91bf41bb3b8cc1d76d249fa99d0d8925e87a5c";
  String useruploadMpkHex =
      "956e6fa1345547e8e060c8962ddd38863bf2c85406ed03b204bc340fb5db01296a960d00be240caa08db001664f4f7028a9dbbb33aea172bffd58b4a644f1ecb3b7bbed378a8a7c9756ac8b4b47346d8dbf37a62377703b7fc8da3bb22a21415";
  String useruploadMskHex = "ce23ca6a3fd0d1307d3d0b2578784750b3f0e20b64e0c24e4cafb35561a0af35";
  CryptoWrapper cryptoWrapper =
      new CryptoWrapper(
          healthAuthoritySkHex, healthAuthorityPkHex, useruploadMskHex, useruploadMpkHex);

  private TestVectors testVectors;

  @Before
  public void init() throws FileNotFoundException {
    testVectors =
        new Gson()
            .fromJson(
                new InputStreamReader(
                    new FileInputStream("src/test/resources/crowd_notifier_test_vectors.json")),
                TestVectors.class);
  }

  @Test
  public void testFlow() {
    cryptoWrapper.getCryptoUtilV3().testFlow();
  }

  @Test
  public void testKeyGen() throws IOException {
    cryptoWrapper.getCryptoUtilV3().genKeys();
  }

  @Test
  public void testCreateTraceV3ForUserUpload() {
    // TODO: Implement
  }

  @Test
  public void testGetNoncesAndNotificationKey() {
    for (TestVectors.HKDFTest hkdfTest : testVectors.hkdfTestVector) {
      CryptoUtil.NoncesAndNotificationKey noncesAndNotificationKey =
          cryptoWrapper.getCryptoUtilV3().getNoncesAndNotificationKey(hkdfTest.qrCodePayload);

      assertArrayEquals(hkdfTest.noncePreId, noncesAndNotificationKey.noncePreId);
      assertArrayEquals(hkdfTest.nonceTimekey, noncesAndNotificationKey.nonceTimekey);
      assertArrayEquals(hkdfTest.notificationKey, noncesAndNotificationKey.notificationKey);
    }
  }

  public static class TestVectors {
    public ArrayList<IdentityTest> identityTestVector;
    public ArrayList<HKDFTest> hkdfTestVector;

    public static class HKDFTest {
      public byte[] qrCodePayload;
      public byte[] noncePreId;
      public byte[] nonceTimekey;
      public byte[] notificationKey;
    }

    public static class IdentityTest {
      public byte[] qrCodePayload;
      public int startOfInterval;
      public byte[] identity;
    }
  }
}
