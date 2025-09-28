/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.config;

import com.devinbroyal.mdm.exception.AppConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public final class AppConfig {

    private static final String CONFIG_FILE = "config.properties";

    private AppConfig() {
        // Prevent instantiation
    }

    public static MdmProperties loadProperties() throws AppConfigurationException {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new AppConfigurationException("Unable to find " + CONFIG_FILE + " in classpath.");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new AppConfigurationException("Error reading " + CONFIG_FILE, ex);
        }

        MdmProperties mdmProps = new MdmProperties();

        // Load properties, prioritizing environment variables over the properties file.
        mdmProps.setTeamId(getProperty("APNS_TEAM_ID", "apns.team.id", properties));
        mdmProps.setKeyId(getProperty("APNS_KEY_ID", "apns.key.id", properties));
        mdmProps.setAuthKeyPath(getProperty("APNS_AUTH_KEY_PATH", "apns.auth.key.path", properties));
        mdmProps.setTopic(getProperty("APNS_TOPIC", "apns.topic", properties));
        mdmProps.setProduction(Boolean.parseBoolean(getProperty("APNS_PRODUCTION", "apns.production", properties, "false")));
        mdmProps.setHttpPort(Integer.parseInt(getProperty("SERVER_HTTP_PORT", "server.http.port", properties, "8080")));
        mdmProps.setThreadPoolSize(Integer.parseInt(getProperty("SERVER_THREAD_POOL_SIZE", "server.thread.pool.size", properties, "10")));

        validateProperties(mdmProps);
        return mdmProps;
    }

    private static String getProperty(String envVar, String propKey, Properties properties) {
        return Optional.ofNullable(System.getenv(envVar))
                .orElse(properties.getProperty(propKey));
    }

    private static String getProperty(String envVar, String propKey, Properties properties, String defaultValue) {
        return Optional.ofNullable(getProperty(envVar, propKey, properties)).orElse(defaultValue);
    }

    private static void validateProperties(MdmProperties props) throws AppConfigurationException {
        if (isNullOrBlank(props.getTeamId())) {
            throw new AppConfigurationException("APNS Team ID (APNS_TEAM_ID / apns.team.id) is not configured.");
        }
        if (isNullOrBlank(props.getKeyId())) {
            throw new AppConfigurationException("APNS Key ID (APNS_KEY_ID / apns.key.id) is not configured.");
        }
        if (isNullOrBlank(props.getTopic())) {
            throw new AppConfigurationException("APNS Topic (APNS_TOPIC / apns.topic) is not configured.");
        }
        if (isNullOrBlank(props.getAuthKeyPath())) {
            throw new AppConfigurationException("APNS Auth Key Path (APNS_AUTH_KEY_PATH / apns.auth.key.path) is not configured.");
        }
        try {
            Path authKey = Paths.get(props.getAuthKeyPath());
            if (!Files.exists(authKey) || !Files.isReadable(authKey)) {
                throw new AppConfigurationException("APNS Auth Key file does not exist or is not readable at: " + props.getAuthKeyPath());
            }
        } catch (InvalidPathException e) {
            throw new AppConfigurationException("The configured APNS Auth Key Path is invalid: " + props.getAuthKeyPath(), e);
        }
    }
    
    private static boolean isNullOrBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */