package ch.ubique.notifyme.sdk.backend.ws.service;

import ch.ubique.notifyme.sdk.backend.model.PushRegistrationOuterClass.PushType;
import ch.ubique.pushservice.pushconnector.PushConnectorService;
import java.util.Map;

public class PhoneBackgroundTaskTrigger {

    private final Map<PushType, PushConnectorService> type2PushService;

    public PhoneBackgroundTaskTrigger(final Map<PushType, PushConnectorService> type2PushService) {
        this.type2PushService = type2PushService;
    }
}
