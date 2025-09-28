/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.domain;

import java.util.Map;

public class CommandRequest {
    private String deviceToken;
    private Map<String, Object> payload;

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */