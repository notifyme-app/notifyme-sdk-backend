package ch.ubique.notifyme.sdk.backend.ws.util;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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
    for (TestVectors.UploadTest uploadTest : testVectors.uploadTestVector) {
      final var uploadVenueInfo =
          UserUploadPayloadOuterClass.UploadVenueInfo.newBuilder()
              .setPreId(ByteString.copyFrom(uploadTest.preId))
              .setTimeKey(ByteString.copyFrom(uploadTest.timeKey))
              .setIntervalStartMs(uploadTest.intervalStartMs)
              .setIntervalEndMs(uploadTest.intervalEndMs)
              .setNotificationKey(ByteString.copyFrom(uploadTest.notificationKey))
              .setFake(uploadTest.fake)
              .build();
      final var userUpload =
          UserUploadPayloadOuterClass.UserUploadPayload.newBuilder()
              .setVersion(3)
              .addVenueInfos(uploadVenueInfo)
              .build();
      final var traceKeys = cryptoWrapper.getCryptoUtilV3().createTraceV3ForUserUpload(userUpload);
      assertNotNull(traceKeys);
      assertEquals(1, traceKeys.size());
    }
  }

  @Test
  public void testGetNoncesAndNotificationKey() {
    for (TestVectors.UploadTest uploadTest : testVectors.uploadTestVector) {
      CryptoUtil.NoncesAndNotificationKey noncesAndNotificationKey =
          cryptoWrapper.getCryptoUtilV3().getNoncesAndNotificationKey(uploadTest.qrCodePayload);

      assertArrayEquals(uploadTest.preId, noncesAndNotificationKey.noncePreId);
      assertArrayEquals(uploadTest.timeKey, noncesAndNotificationKey.nonceTimekey);
      assertArrayEquals(uploadTest.notificationKey, noncesAndNotificationKey.notificationKey);
    }
  }

  public static class TestVectors {
    public ArrayList<IdentityTest> identityTestVector;
    public ArrayList<HKDFTest> hkdfTestVector;
    public ArrayList<UploadTest> uploadTestVector;

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

    public static class UploadTest {
      public byte[] qrCodePayload;
      public byte[] preId;
      public byte[] timeKey;
      public long intervalStartMs;
      public long intervalEndMs;
      public byte[] notificationKey;
      public boolean fake;
    }
  }
}
