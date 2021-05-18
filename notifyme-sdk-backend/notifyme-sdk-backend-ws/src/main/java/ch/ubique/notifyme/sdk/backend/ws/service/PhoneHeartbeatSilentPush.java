package ch.ubique.notifyme.sdk.backend.ws.service;

import ch.ubique.notifyme.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushType;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneHeartbeatSilentPush {

  private static final Logger logger = LoggerFactory.getLogger(PhoneHeartbeatSilentPush.class);

  private final PushRegistrationDataService pushRegistrationDataService;

  public PhoneHeartbeatSilentPush(
      final PushRegistrationDataService pushRegistrationDataService) {
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

    final var appleIodPushData = createAppleSilentPushDataSandbox(iodPushTokens);
    final var appleIosPushData = createAppleSilentPushData(iosPushTokens);
    final var androidPushData = createAndroidSilentPushData(androidPushTokens);

    // TODO: Send push notifications
  }

  private Object createAppleSilentPushDataSandbox(final Set<String> applePushTokens) {
      // TODO: Create apple sandbox silent push payload
    return null;
  }

  private Object createAppleSilentPushData(final Set<String> applePushTokens) {
      // TODO: Create apple silent push payload
      return null;
  }

  private Object createAndroidSilentPushData(final Set<String> androidPushTokens) {
      // TODO: Create android silent push payload
      return null;
  }
}
