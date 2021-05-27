package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataServiceV3;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.FakeRequestFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.IntervalThresholdFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.OverlappingIntervalsFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.UploadInsertionFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.TokenHelper;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import mockit.Mock;
import mockit.MockUp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test-config"})
public class InsertManagerTest {

    InsertManager insertManager;

    @Autowired SwissCovidDataServiceV3 swissCovidDataServiceV3;
    @Autowired CryptoWrapper cryptoWrapper;

    @Autowired TransactionManager transactionManager;

    @Value("${traceKey.bucketSizeInMs}")
    Long bucketSizeinMs;

    private TokenHelper tokenHelper;

    @Before
    public void setUp() throws Exception {
        tokenHelper = new TokenHelper();
        insertManager = new InsertManager(cryptoWrapper, swissCovidDataServiceV3);
    }

    @Test
    @Transactional
    public void testInsertEmptyList() throws Exception {
        final var now = Instant.now();
        assertTrue(swissCovidDataServiceV3.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).isEmpty());
        assertThrows(
                InsertException.class,
                () ->
                        insertWith(
                                new ArrayList<>(),
                                new ArrayList<>(),
                                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId())));
        assertTrue(swissCovidDataServiceV3.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).isEmpty());
    }

    @Test
    @Transactional
    public void testInsertInvalidVenueInfo() throws Exception {
        final var now = Instant.now();
        UploadInsertionFilter removeAll =
                new UploadInsertionFilter() {
                    @Override
                    public List<UploadVenueInfo> filter(
                            LocalDateTime now,
                            List<UploadVenueInfo> uploadVenueInfoList,
                            Object principal)
                            throws InsertException {
                        return new ArrayList<>();
                    }
                };
        final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>();
        uploadVenueInfoList.add(createUploadVenueInfo(now, now.plus(1, ChronoUnit.HOURS), false));
        insertWith(
                Collections.singletonList(removeAll),
                uploadVenueInfoList,
                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId()));
        assertTrue(swissCovidDataServiceV3.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).isEmpty());
    }

    @Test
    @Transactional
    public void testInsertValid() throws Exception {
        final var now = Instant.now();
        UploadInsertionFilter removeNone =
                new UploadInsertionFilter() {
                    @Override
                    public List<UploadVenueInfo> filter(
                            LocalDateTime now,
                            List<UploadVenueInfo> uploadVenueInfoList,
                            Object principal)
                            throws InsertException {
                        return uploadVenueInfoList;
                    }
                };
        final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>();
        uploadVenueInfoList.add(
                createUploadVenueInfo(
                        now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), false));
        insertWith(
                Collections.singletonList(removeNone),
                uploadVenueInfoList,
                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId()));
        Clock clock = Clock.fixed(now.plus(1, ChronoUnit.DAYS), ZoneOffset.UTC);
        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
        final var traceKeys = swissCovidDataServiceV3.findTraceKeys(now.minus(1, ChronoUnit.DAYS));
        assertEquals(1, traceKeys.size());
    }

    @Test
    @Transactional
    public void testAddRequestFilters() throws Exception {
        final var now = Instant.now();
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var fakeUpload =
                createUploadVenueInfo(
                        now.minus(6, ChronoUnit.DAYS), now.minus(6, ChronoUnit.DAYS), true);
        venueInfoList.add(fakeUpload);
        final var negativeIntervalUpload =
                createUploadVenueInfo(
                        now.minus(3, ChronoUnit.DAYS), now.minus(4, ChronoUnit.DAYS), false);
        venueInfoList.add(negativeIntervalUpload);
        final var overlapIntervalUpload1 =
                createUploadVenueInfo(
                        now.minus(60, ChronoUnit.HOURS), now.minus(2, ChronoUnit.DAYS), false);
        final var overlapIntervalUpload2 =
                createUploadVenueInfo(
                        now.minus(54, ChronoUnit.HOURS), now.minus(36, ChronoUnit.HOURS), false);
        venueInfoList.add(overlapIntervalUpload1);
        venueInfoList.add(overlapIntervalUpload2);
        final var validUpload =
                createUploadVenueInfo(
                        now.minus(24, ChronoUnit.HOURS), now.minus(23, ChronoUnit.HOURS), false);
        venueInfoList.add(validUpload);
        insertWith(
                Arrays.asList(
                        new FakeRequestFilter(),
                        new IntervalThresholdFilter(),
                        new OverlappingIntervalsFilter()),
                venueInfoList,
                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId()));
        Clock clock = Clock.fixed(now.plus(1, ChronoUnit.DAYS), ZoneOffset.UTC);
        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
        assertEquals(
                1, swissCovidDataServiceV3.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).size());
    }

    private void insertWith(
            List<UploadInsertionFilter> insertionFilterList,
            List<UploadVenueInfo> uploadVenueInfoList,
            LocalDateTime now)
            throws Exception {

        for (var insertionFilter : insertionFilterList) {
            if (insertionFilter != null) {
                insertManager.addFilter(insertionFilter);
            }
        }
        final var expiry = Instant.now().plus(5, ChronoUnit.MINUTES);
        final var token =
                tokenHelper.createToken(
                        "2021-04-29",
                        "0",
                        "notifyMe",
                        "userupload",
                        Date.from(expiry),
                        true,
                        Instant.now());
        insertManager.insertIntoDatabase(uploadVenueInfoList, token, now);
    }

    private UploadVenueInfo createUploadVenueInfo(Instant start, Instant end, boolean fake) {
        final var crypto = cryptoWrapper.getCryptoUtil();
        final var noncesAndNotificationKey =
                crypto.getNoncesAndNotificationKey(crypto.createNonce(256));
        byte[] preid =
                crypto.cryptoHashSHA256(
                        crypto.concatenate(
                                "CN-PREID".getBytes(StandardCharsets.US_ASCII),
                                "payload".getBytes(StandardCharsets.US_ASCII),
                                noncesAndNotificationKey.noncePreId));
        byte[] timekey =
                crypto.cryptoHashSHA256(
                        crypto.concatenate(
                                "CN-TIMEKEY".getBytes(StandardCharsets.US_ASCII),
                                crypto.longToBytes(3600L),
                                crypto.longToBytes(start.getEpochSecond()),
                                noncesAndNotificationKey.nonceTimekey));
        return UploadVenueInfo.newBuilder()
                .setPreId(ByteString.copyFrom(preid))
                .setTimeKey(ByteString.copyFrom(timekey))
                .setIntervalStartMs(start.toEpochMilli())
                .setIntervalEndMs(end.toEpochMilli())
                .setNotificationKey(ByteString.copyFrom(noncesAndNotificationKey.notificationKey))
                .setFake(fake)
                .build();
    }
}
