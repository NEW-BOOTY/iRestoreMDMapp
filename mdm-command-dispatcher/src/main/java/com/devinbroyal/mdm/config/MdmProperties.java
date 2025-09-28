/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.config;

public class MdmProperties {
    private String teamId;
    private String keyId;
    private String authKeyPath;
    private String topic;
    private boolean isProduction;
    private int httpPort;
    private int threadPoolSize;

    // Getters and Setters
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getAuthKeyPath() { return authKeyPath; }
    public void setAuthKeyPath(String authKeyPath) { this.authKeyPath = authKeyPath; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public boolean isProduction() { return isProduction; }
    public void setProduction(boolean production) { isProduction = production; }

    public int getHttpPort() { return httpPort; }
    public void setHttpPort(int httpPort) { this.httpPort = httpPort; }

    public int getThreadPoolSize() { return threadPoolSize; }
    public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */