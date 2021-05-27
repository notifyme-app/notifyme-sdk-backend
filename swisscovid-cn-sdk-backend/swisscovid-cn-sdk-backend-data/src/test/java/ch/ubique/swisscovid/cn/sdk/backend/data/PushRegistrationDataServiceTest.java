package ch.ubique.swisscovid.cn.sdk.backend.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushType;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PushRegistrationDataServiceTest extends BaseDataServiceTest {

    @Autowired PushRegistrationDataService pushRegistrationDataService;

    @Test
    @Transactional
    public void upsertPushRegistration() {
        final var registration = createPushRegistration(PushType.AND);
        pushRegistrationDataService.upsertPushRegistration(registration);
        final var registrations =
                pushRegistrationDataService.getPushRegistrationByType(PushType.AND);
        assertNotNull(registrations);
        assertEquals(1, registrations.size());
        assertEquals(registration.getPushToken(), registrations.get(0).getPushToken());
    }

    @Test
    @Transactional
    public void getPushRegistrationByType() {
        final var registration = createPushRegistration(PushType.AND);
        assertTrue(pushRegistrationDataService.getPushRegistrationByType(PushType.IOS).isEmpty());
        assertTrue(pushRegistrationDataService.getPushRegistrationByType(PushType.AND).isEmpty());
        assertTrue(pushRegistrationDataService.getPushRegistrationByType(PushType.IOD).isEmpty());
        pushRegistrationDataService.upsertPushRegistration(registration);
        final var registrationsAND =
            pushRegistrationDataService.getPushRegistrationByType(PushType.AND);
        assertNotNull(registrationsAND);
        assertEquals(1, registrationsAND.size());
        final var registrationsIOS = pushRegistrationDataService
            .getPushRegistrationByType(PushType.IOS);
        assertNotNull(registrationsIOS);
        assertTrue(registrationsIOS.isEmpty());
    }

    @Test
    @Transactional
    public void removeRegistrations() {
        final var registrationIOD = createPushRegistration(PushType.IOD);
        final var registrationAND = createPushRegistration(PushType.AND);
        pushRegistrationDataService.upsertPushRegistration(registrationIOD);
        pushRegistrationDataService.upsertPushRegistration(registrationAND);
        assertEquals(1, pushRegistrationDataService.getPushRegistrationByType(PushType.IOD).size());
        pushRegistrationDataService.removeRegistrations(Collections.singletonList(registrationIOD.getPushToken()));
        assertTrue(pushRegistrationDataService.getPushRegistrationByType(PushType.IOD).isEmpty());
        assertEquals(1, pushRegistrationDataService.getPushRegistrationByType(PushType.AND).size());
    }

    private PushRegistration createPushRegistration(PushType pushType) {
        final var registration =
            PushRegistration.newBuilder()
                .setVersion(4)
                .setPushToken("token_" + pushType.toString())
                .setPushType(pushType)
                .setDeviceId("id_" + pushType.toString())
                .build();
        return registration;
    }
}
