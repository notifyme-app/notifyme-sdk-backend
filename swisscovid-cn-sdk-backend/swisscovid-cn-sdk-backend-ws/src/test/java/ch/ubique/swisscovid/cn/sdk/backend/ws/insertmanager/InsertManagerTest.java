package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ch.ubique.swisscovid.cn.sdk.backend.data.InteractionDurationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UserUploadPayload;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.FakeRequestFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.IntervalThresholdFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters.UploadInsertionFilter;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.TokenHelper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.VenueInfoHelper;
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

    @Autowired SwissCovidDataService swissCovidDataService;
    @Autowired InteractionDurationDataService interactionDurationDataService;
    @Autowired CryptoWrapper cryptoWrapper;

    @Autowired TransactionManager transactionManager;

    @Value("${traceKey.bucketSizeInMs}")
    Long bucketSizeinMs;

    private TokenHelper tokenHelper;
    private VenueInfoHelper venueInfoHelper;

    @Before
    public void setUp() throws Exception {
        tokenHelper = new TokenHelper();
        venueInfoHelper = new VenueInfoHelper(cryptoWrapper);
        insertManager =
                new InsertManager(
                        cryptoWrapper, swissCovidDataService, interactionDurationDataService);
    }

    @Test
    @Transactional
    public void testInsertEmptyList() throws Exception {
        final var now = Instant.now();
        assertTrue(swissCovidDataService.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).isEmpty());
        assertThrows(
                InsertException.class,
                () ->
                        insertWith(
                                new ArrayList<>(),
                                new ArrayList<>(),
                                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId())));
        assertTrue(swissCovidDataService.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).isEmpty());
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
        final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>(
            createUploadVenueInfo(now, now.plus(1, ChronoUnit.HOURS), false));
        insertWith(
                Collections.singletonList(removeAll),
                uploadVenueInfoList,
                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId()));
        assertTrue(swissCovidDataService.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).isEmpty());
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
        final List<UploadVenueInfo> uploadVenueInfoList = new ArrayList<>(createUploadVenueInfo(
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
        final var traceKeys = swissCovidDataService.findTraceKeys(now.minus(1, ChronoUnit.DAYS));
        assertEquals(uploadVenueInfoList.size(), traceKeys.size());
    }

    @Test
    @Transactional
    public void testAddRequestFilters() throws Exception {
        final var now = Instant.now();
        final var fakeUpload =
                createUploadVenueInfo(
                        now.minus(6, ChronoUnit.DAYS), now.minus(6, ChronoUnit.DAYS), true);
        final var venueInfoList = new ArrayList<UploadVenueInfo>(fakeUpload);
        final var negativeIntervalUpload =
                createUploadVenueInfo(
                        now.minus(3, ChronoUnit.DAYS), now.minus(4, ChronoUnit.DAYS), false);
        venueInfoList.addAll(negativeIntervalUpload);
        final var validUpload =
                createUploadVenueInfo(
                        now.minus(24, ChronoUnit.HOURS), now.minus(23, ChronoUnit.HOURS), false);
        venueInfoList.addAll(validUpload);
        insertWith(
                Arrays.asList(new FakeRequestFilter(), new IntervalThresholdFilter()),
                venueInfoList,
                LocalDateTime.ofInstant(now, TimeZone.getDefault().toZoneId()));
        Clock clock = Clock.fixed(now.plus(1, ChronoUnit.DAYS), ZoneOffset.UTC);
        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
        assertEquals(validUpload.size(), swissCovidDataService.findTraceKeys(now.minus(1, ChronoUnit.DAYS)).size());
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
        final var userUploadPayload =
                UserUploadPayload.newBuilder()
                        .addAllVenueInfos(uploadVenueInfoList)
                        .setVersion(4)
                        .setUserInteractionDurationMs(0)
                        .build();
        insertManager.insertIntoDatabase(userUploadPayload, token, now);
    }

    private List<UploadVenueInfo> createUploadVenueInfo(Instant start, Instant end, boolean fake) {
        return venueInfoHelper.getVenueInfo(
                LocalDateTime.ofInstant(start, ZoneOffset.UTC),
                LocalDateTime.ofInstant(end, ZoneOffset.UTC),
                fake,
                null);
    }
}
