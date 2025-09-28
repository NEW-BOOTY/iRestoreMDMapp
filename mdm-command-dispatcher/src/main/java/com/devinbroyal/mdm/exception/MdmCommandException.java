/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.exception;

public class MdmCommandException extends Exception {
    private final String deviceToken;

    public MdmCommandException(String message, String deviceToken, Throwable cause) {
        super(message, cause);
        this.deviceToken = deviceToken;
    }

    public String getDeviceToken() {
        return deviceToken;
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */