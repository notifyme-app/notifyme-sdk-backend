package ch.ubique.swisscovid.cn.sdk.backend.ws.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.proxy.HttpProxyHandlerFactory;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

import ch.ubique.swisscovid.cn.sdk.backend.data.PushRegistrationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushRegistration;
import ch.ubique.swisscovid.cn.sdk.backend.model.PushRegistrationOuterClass.PushType;

/**
 * Sends periodic silent pushes to all iOS devices. This should wake up the app
 * and let the app sync problematic events. (Note: Only EN disabled apps
 * register with our server.)
 * 
 * @author alig
 *
 */
public class IOSHeartbeatSilentPush {

	private static final Logger logger = LoggerFactory.getLogger(IOSHeartbeatSilentPush.class);

	private final PushRegistrationDataService pushRegistrationDataService;

	private final ApnsClient apnsClient;
	private final ApnsClient apnsClientSandbox;

	private final String topic;

	public IOSHeartbeatSilentPush(final PushRegistrationDataService pushRegistrationDataService, InputStream signingKey,
			String teamId, String keyId, String topic)
			throws InvalidKeyException, SSLException, NoSuchAlgorithmException, IOException, URISyntaxException {
		this.pushRegistrationDataService = pushRegistrationDataService;

		ApnsSigningKey key = ApnsSigningKey.loadFromInputStream(signingKey, teamId, keyId);

		this.apnsClient = new ApnsClientBuilder().setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
				.setSigningKey(key).setProxyHandlerFactory(
						HttpProxyHandlerFactory.fromSystemProxies(ApnsClientBuilder.PRODUCTION_APNS_HOST))
				.build();

		this.apnsClientSandbox = new ApnsClientBuilder().setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
				.setSigningKey(key).setProxyHandlerFactory(
						HttpProxyHandlerFactory.fromSystemProxies(ApnsClientBuilder.DEVELOPMENT_APNS_HOST))
				.build();

		this.topic = topic;
	}

	public void sendHeartbeats() {
		logger.info("Send iOS heartbeat push");
		logger.info("Load tokens from database.");
		final var iodPushTokens = pushRegistrationDataService.getPushRegistrationByType(PushType.IOD).stream()
				.map(PushRegistration::getPushToken).collect(Collectors.toSet());
		final var iosPushTokens = pushRegistrationDataService.getPushRegistrationByType(PushType.IOS).stream()
				.map(PushRegistration::getPushToken).collect(Collectors.toSet());
		logger.info("Found {} iOD and {} iOS push tokens.", iodPushTokens.size(), iosPushTokens.size());

		final var appleIodPushData = createAppleSilentNotificationSandbox(iodPushTokens);
		final var appleIosPushData = createAppleSilentPushNotifications(iosPushTokens);

		List<PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>>> responseList = new ArrayList<>();
		for (ApnsPushNotification notification : appleIosPushData) {
			PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> f = apnsClient
					.sendNotification(notification);
			responseList.add(f);
		}
		for (ApnsPushNotification notification : appleIodPushData) {
			PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> f = apnsClientSandbox
					.sendNotification(notification);
			responseList.add(f);
		}

		List<String> tokensToRemove = new ArrayList<>();
		for (PushNotificationFuture<ApnsPushNotification, PushNotificationResponse<ApnsPushNotification>> f : responseList) {
			try {
				PushNotificationResponse<ApnsPushNotification> r = f.get();
				if (!r.isAccepted() && r.getRejectionReason() != null) {
					// collect bad/unregistered device tokens and remove from database
					if (r.getRejectionReason().equals("BadDeviceToken") || r.getRejectionReason().equals("Unregistered")
							|| r.getRejectionReason().equals("DeviceTokenNotForTopic")) {
						tokensToRemove.add(r.getPushNotification().getToken());
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				logger.info("Exception waiting for response. Continue", e);
			}
		}

		logger.info("All notification sent. Got {} invalid tokens to remove", tokensToRemove.size());
		pushRegistrationDataService.removeRegistrations(tokensToRemove);
		logger.info("iOS hearbeat push done");
	}

	private List<ApnsPushNotification> createAppleSilentNotificationSandbox(final Set<String> applePushTokens) {
		List<ApnsPushNotification> result = new ArrayList<>();
		final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
		payloadBuilder.setContentAvailable(true);
		String payload = payloadBuilder.build();
		for (String token : applePushTokens) {
			SimpleApnsPushNotification notification = new SimpleApnsPushNotification(token, this.topic, payload, null,
					DeliveryPriority.CONSERVE_POWER, com.eatthepath.pushy.apns.PushType.BACKGROUND);
			result.add(notification);
		}
		return result;
	}

	private List<ApnsPushNotification> createAppleSilentPushNotifications(final Set<String> applePushTokens) {
		List<ApnsPushNotification> result = new ArrayList<>();
		final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
		payloadBuilder.setContentAvailable(true);
		String payload = payloadBuilder.build();
		for (String token : applePushTokens) {
			SimpleApnsPushNotification notification = new SimpleApnsPushNotification(token, this.topic, payload, null,
					DeliveryPriority.CONSERVE_POWER, com.eatthepath.pushy.apns.PushType.BACKGROUND);
			result.add(notification);
		}
		return result;
	}

}
