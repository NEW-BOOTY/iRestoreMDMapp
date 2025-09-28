/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.persistence;

import com.devinbroyal.mdm.domain.CommandResult;

import java.util.List;
import java.util.Map;

public interface ExecutionHistoryRepository {
    /**
     * Adds a command result to the history for a specific device token.
     *
     * @param deviceToken The device token the command was sent to.
     * @param result      The result of the command dispatch.
     */
    void recordResult(String deviceToken, CommandResult result);

    /**
     * Retrieves the entire history of all commands sent.
     *
     * @return A map where the key is the device token and the value is a list of command results.
     */
    Map<String, List<CommandResult>> getFullHistory();
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */