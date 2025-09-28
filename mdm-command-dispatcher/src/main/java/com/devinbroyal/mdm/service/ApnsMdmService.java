/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.service;

import com.devinbroyal.mdm.config.MdmProperties;
import com.devinbroyal.mdm.domain.CommandResult;
import com.devinbroyal.mdm.exception.MdmCommandException;
import com.devinbroyal.mdm.persistence.ExecutionHistoryRepository;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ApnsMdmService implements MdmService {

    private static final Logger logger = LoggerFactory.getLogger(ApnsMdmService.class);
    private final ApnsClient apnsClient;
    private final String topic;
    private final ExecutorService notificationExecutor;
    private final ExecutionHistoryRepository historyRepository;
    private final Gson gson = new Gson();

    public ApnsMdmService(MdmProperties properties, ExecutorService notificationExecutor, ExecutionHistoryRepository historyRepository) throws MdmCommandException {
        this.topic = Objects.requireNonNull(properties.getTopic(), "APNs topic cannot be null");
        this.notificationExecutor = Objects.requireNonNull(notificationExecutor, "ExecutorService cannot be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "ExecutionHistoryRepository cannot be null");

        try {
            final String apnsHost = properties.isProduction()
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;

            this.apnsClient = new ApnsClientBuilder()
                    .setApnsServer(apnsHost)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(
                            Paths.get(properties.getAuthKeyPath()).toFile(),
                            properties.getTeamId(),
                            properties.getKeyId()))
                    .build();
            logger.info("ApnsClient initialized for host: {}", apnsHost);
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new MdmCommandException("Failed to initialize ApnsClient", null, e);
        }
    }

    @Override
    public void sendCommand(String deviceToken, Map<String, Object> payload) throws MdmCommandException {
        Objects.requireNonNull(deviceToken, "Device token cannot be null");
        Objects.requireNonNull(payload, "Payload cannot be null");

        final String sanitizedToken = TokenUtil.sanitizeTokenString(deviceToken);
        final String payloadJson = gson.toJson(payload);
        final String commandUUID = (String) payload.getOrDefault("CommandUUID", "UNKNOWN_UUID");

        logger.info("Submitting MDM command {} to device token starting with {}", commandUUID, getPartialTokenForLogging(sanitizedToken));

        final SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(
                sanitizedToken,
                this.topic,
                payloadJson.getBytes(StandardCharsets.UTF_8));
        
        CompletableFuture.runAsync(() -> {
            try {
                final PushNotificationResponse<SimpleApnsPushNotification> response = apnsClient.sendNotification(pushNotification).get();
                handleApnsResponse(response, sanitizedToken, commandUUID);
            } catch (Exception e) {
                logger.error("Failed to send notification for command {} to device {}", commandUUID, getPartialTokenForLogging(sanitizedToken), e);
                CommandResult result = new CommandResult(commandUUID, CommandResult.Status.FAILED_TO_SEND, e.getMessage());
                historyRepository.recordResult(sanitizedToken, result);
            }
        }, notificationExecutor);
    }
    
    private void handleApnsResponse(PushNotificationResponse<SimpleApnsPushNotification> response, String deviceToken, String commandUUID) {
        CommandResult result;
        if (response.isAccepted()) {
            logger.info("Command {} for device {} accepted by APNs.", commandUUID, getPartialTokenForLogging(deviceToken));
            result = new CommandResult(commandUUID, CommandResult.Status.ACCEPTED, null);
        } else {
            final String rejectionReason = response.getRejectionReason().orElse("Unknown reason");
            logger.warn("Command {} for device {} rejected by APNs. Reason: {}", commandUUID, getPartialTokenForLogging(deviceToken), rejectionReason);
            response.getTokenInvalidationTimestamp().ifPresent(timestamp ->
                    logger.error("Token for device {} was invalidated at {}. It should be removed from the system.", getPartialTokenForLogging(deviceToken), timestamp)
            );
            result = new CommandResult(commandUUID, CommandResult.Status.REJECTED, rejectionReason);
        }
        historyRepository.recordResult(deviceToken, result);
    }

    @Override
    public void shutdown() {
        if (this.apnsClient != null) {
            logger.info("Shutting down ApnsClient...");
            final CompletableFuture<Void> closeFuture = this.apnsClient.close();
            try {
                closeFuture.get();
                logger.info("ApnsClient shut down successfully.");
            } catch (Exception e) {
                logger.error("Failed to cleanly shut down ApnsClient.", e);
            }
        }
    }
    
    private String getPartialTokenForLogging(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4);
    }
}
/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */