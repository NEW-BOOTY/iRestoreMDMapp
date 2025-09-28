/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
package com.devinroyal.irestore;

import com.eatthepath.pushy.apns.*;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class MDMService {
    private static final Logger logger = LoggerFactory.getLogger(MDMService.class);
    private static final String CONFIG_FILE = "config.properties";
    private static final String RESULT_LOG_FILE = "result.log";
    private static final int MAX_RETRIES = 5;
    private static final int THREAD_POOL_SIZE = 10;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final Map<String, List<String>> executionHistory = new ConcurrentHashMap<>();
    private static String TEAM_ID;
    private static String KEY_ID;
    private static String AUTH_KEY_PATH;
    private static String TOPIC;
    private static boolean IS_PRODUCTION;
    private static final String COMMANDS_JSON =
            """
            {
              "commands": [
                {
                  "commandType": "DeviceLock",
                  "payload": {
                    "MessageType": "DeviceLock",
                    "PIN": "1234"
                  }
                }
              ]
            }
            """;

    static {
        try {
            TEAM_ID = Optional.ofNullable(System.getenv("TEAM_ID")).orElse(loadProperty("teamId"));
            KEY_ID = Optional.ofNullable(System.getenv("KEY_ID")).orElse(loadProperty("keyId"));
            AUTH_KEY_PATH = Optional.ofNullable(System.getenv("AUTH_KEY_PATH")).orElse(loadProperty("authKeyPath"));
            TOPIC = Optional.ofNullable(System.getenv("TOPIC")).orElse(loadProperty("topic"));
            IS_PRODUCTION = Boolean.parseBoolean(Optional.ofNullable(System.getenv("IS_PRODUCTION")).orElse(loadProperty("isProduction")));

            java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger(MDMService.class.getName());
            FileHandler fileHandler = new FileHandler(RESULT_LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            jdkLogger.addHandler(fileHandler);
            jdkLogger.setLevel(Level.ALL);
        } catch (Exception e) {
            logger.error("MDMService initialization failed", e);
            System.exit(1);
        }
    }

    public List<String> detectDevices() {
        try {
            logger.info("Detecting connected iOS devices for MDM...");
            List<String> devices = new DeviceManager().listDevices();
            if (devices.isEmpty()) {
                logger.warning("No iOS devices detected.");
            } else {
                logger.info("Detected devices: {}", String.join(", ", devices));
            }
            return devices;
        } catch (IOException e) {
            logger.error("Error detecting devices for MDM", e);
            return Collections.emptyList();
        }
    }

    public void sendMDMCommands(List<String> deviceTokens) {
        try (ApnsClient apnsClient = createApnsClient()) {
            List<Map<String, Object>> commands = loadCommands(COMMANDS_JSON);
            for (String deviceToken : deviceTokens) {
                for (Map<String, Object> command : commands) {
                    String commandType = (String) command.get("commandType");
                    String payload = new Gson().toJson(command.get("payload"));
                    CompletableFuture.runAsync(
                            () -> sendMDMCommandWithRetries(apnsClient, deviceToken, commandType, payload, 0),
                            executorService);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending MDM commands", e);
        }
    }

    private ApnsClient createApnsClient() throws Exception {
        String apnsHost = IS_PRODUCTION ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
        return new ApnsClientBuilder()
                .setApnsServer(apnsHost)
                .setSigningKey(ApnsSigningKey.loadFromPkcs8File(Paths.get(AUTH_KEY_PATH).toFile(), TEAM_ID, KEY_ID))
                .build();
    }

    private List<Map<String, Object>> loadCommands(String json) {
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            Type commandListType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            return new Gson().fromJson(jsonObject.get("commands"), commandListType);
        } catch (Exception e) {
            logger.error("Failed to parse command JSON", e);
            return Collections.emptyList();
        }
    }

    private void sendMDMCommandWithRetries(ApnsClient apnsClient, String deviceToken, String commandType, String payload, int retryCount) {
        try {
            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(
                    TokenUtil.sanitizeTokenString(deviceToken),
                    TOPIC,
                    payload.getBytes(StandardCharsets.UTF_8));

            apnsClient.sendNotification(pushNotification).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    logger.error("Error sending push notification for {}: {}", commandType, throwable.getMessage());
                    if (retryCount < MAX_RETRIES) {
                        logger.info("Retrying {} command, attempt {}", commandType, retryCount + 1);
                        sendMDMCommandWithRetries(apnsClient, deviceToken, commandType, payload, retryCount + 1);
                    } else {
                        logger.error("Max retries reached for {}", commandType);
                    }
                    return;
                }
                if (response.isAccepted()) {
                    logger.info("Push notification for {} accepted by APNs gateway.", commandType);
                    executionHistory.computeIfAbsent(commandType, k -> new ArrayList<>()).add("Accepted");
                } else {
                    logger.error("Notification for {} rejected by APNs: {}", commandType, response.getRejectionReason());
                    executionHistory.computeIfAbsent(commandType, k -> new ArrayList<>()).add("Rejected: " + response.getRejectionReason());
                }
            }).get();
        } catch (Exception e) {
            logger.error("Error sending push notification for {}", commandType, e);
        }
    }

    public Map<String, List<String>> getExecutionHistory() {
        return executionHistory;
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            logger.info("MDMService executor service shut down successfully.");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            logger.error("MDMService executor service interrupted during shutdown.", e);
            Thread.currentThread().interrupt();
        }
    }

    private static String loadProperty(String key) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        }
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("Property " + key + " not found in " + CONFIG_FILE);
        }
        return value;
    }
}
/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
