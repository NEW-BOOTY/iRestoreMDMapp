/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.service;

import com.devinbroyal.mdm.exception.MdmCommandException;

import java.util.Map;

public interface MdmService {

    /**
     * Asynchronously sends an MDM command payload to a specific device.
     *
     * @param deviceToken The APNs device token of the target device.
     * @param payload     The MDM command payload as a map.
     * @throws MdmCommandException if the command could not be dispatched.
     */
    void sendCommand(String deviceToken, Map<String, Object> payload) throws MdmCommandException;

    /**
     * Shuts down the service and releases resources, such as closing the APNs client.
     */
    void shutdown();
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */