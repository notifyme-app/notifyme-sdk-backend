package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoUtil.NoncesAndNotificationKey;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.CryptoWrapper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.TokenHelper;
import ch.ubique.swisscovid.cn.sdk.backend.ws.util.VenueInfoHelper;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"dev", "test-config", "jwt"})
@TestPropertySource(properties = {"ws.app.jwt.publickey=classpath://generated_public_test.pem"})
public abstract class UploadInsertionFilterTest {
    protected static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("YYYY-MM-dd");
    protected TokenHelper tokenHelper;
    protected VenueInfoHelper venueInfoHelper;
    @Autowired CryptoWrapper cryptoWrapper;
    @Autowired JwtDecoder jwtDecoder;

    abstract List<List<UploadVenueInfo>> getValidVenueInfo();

    abstract List<List<UploadVenueInfo>> getInvalidVenueInfo();

    abstract UploadInsertionFilter insertionFilter();

    public Jwt getToken(LocalDateTime now) throws Exception {
        final var onset = now.minusDays(5).truncatedTo(ChronoUnit.DAYS).format(DATE_FORMATTER);
        final var expiry = now.plusMinutes(5).toInstant(ZoneOffset.UTC);
        return jwtDecoder.decode(
                tokenHelper.createToken(
                        onset,
                        "0",
                        "notifyMe",
                        "userupload",
                        Date.from(expiry),
                        true,
                        now.toInstant(ZoneOffset.UTC)));
    }

    @Before
    public void setUp() throws Exception {
        tokenHelper = new TokenHelper();
        venueInfoHelper = new VenueInfoHelper(cryptoWrapper);
    }

    @Test
    public void testFilterValid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        final List<List<UploadVenueInfo>> testcaseList = getValidVenueInfo();
        final var token = getToken(now);
        for (List<UploadVenueInfo> uploadVenueInfoList : testcaseList) {
            assertEquals(
                    uploadVenueInfoList.size(),
                    insertionFilter().filter(now, uploadVenueInfoList, token).size());
        }
    }

    @Test
    public void testFilterInvalid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        final List<List<UploadVenueInfo>> testcaseList = getInvalidVenueInfo();
        final var token = getToken(now);
        for (List<UploadVenueInfo> uploadVenueInfoList : testcaseList) {
            assertTrue(insertionFilter().filter(now, uploadVenueInfoList, token).isEmpty());
        }
    }

    public List<UploadVenueInfo> getVenueInfo(LocalDateTime start, LocalDateTime end) {
        return venueInfoHelper.getVenueInfo(start, end, false, null);
    }

    public List<UploadVenueInfo> getVenueInfo(boolean fake) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);
        return venueInfoHelper.getVenueInfo(start, end, fake, null);
    }
}
