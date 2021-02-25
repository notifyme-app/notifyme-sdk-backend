package ch.ubique.notifyme.sdk.backend.ws.service;

import ch.ubique.notifyme.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushType;
import ch.ubique.pushservice.pushconnector.PushConnectorService;
import ch.ubique.pushservice.shared.AndroidPushData;
import ch.ubique.pushservice.shared.ApplePushData;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneHeartbeatSilentPush {

    private static final Logger logger = LoggerFactory.getLogger(PhoneHeartbeatSilentPush.class);

    private final PushConnectorService pushConnectorService;
    private final PushRegistrationDataService pushRegistrationDataService;

    public PhoneHeartbeatSilentPush(
            final PushConnectorService pushConnectorService,
            final PushRegistrationDataService pushRegistrationDataService) {
        this.pushConnectorService = pushConnectorService;
        this.pushRegistrationDataService = pushRegistrationDataService;
    }

    public void sendHeartbeats() {

        final var iodPushTokens =
                pushRegistrationDataService.getPushRegistrationByType(PushType.IOD).stream()
                        .map(PushRegistration::getPushToken)
                        .collect(Collectors.toSet());
        final var iosPushTokens =
                pushRegistrationDataService.getPushRegistrationByType(PushType.IOS).stream()
                        .map(PushRegistration::getPushToken)
                        .collect(Collectors.toSet());
        final var androidPushTokens =
                pushRegistrationDataService.getPushRegistrationByType(PushType.AND).stream()
                        .map(PushRegistration::getPushToken)
                        .collect(Collectors.toSet());

        logger.info(
                "Found {} iOD and {} iOS and {} android push tokens",
                iodPushTokens.size(),
                iosPushTokens.size(),
                androidPushTokens.size());

        final var appleIodPushData = createAppleSilentPushData(iodPushTokens);
        appleIodPushData.setSandbox(true);

        final var appleIosPushData = createAppleSilentPushData(iosPushTokens);
        final var androidPushData = createAndroidSilentPushData(androidPushTokens);

        pushConnectorService.push(
                Arrays.asList(appleIodPushData, appleIosPushData, androidPushData));
    }

    private ApplePushData createAppleSilentPushData(final Set<String> applePushTokens) {
        final var applePushData = new ApplePushData();
        applePushData.setContentAvailable(1);
        applePushData.setPushToken(applePushTokens);
        return applePushData;
    }

    private AndroidPushData createAndroidSilentPushData(final Set<String> androidPushTokens) {
        final var androidPushData = new AndroidPushData();
        androidPushData.setPushToken(androidPushTokens);
        return androidPushData;
    }
}
