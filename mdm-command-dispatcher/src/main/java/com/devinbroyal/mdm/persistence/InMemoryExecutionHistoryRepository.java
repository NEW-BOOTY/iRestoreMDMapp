/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.persistence;

import com.devinbroyal.mdm.domain.CommandResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryExecutionHistoryRepository implements ExecutionHistoryRepository {

    private final Map<String, List<CommandResult>> history = new ConcurrentHashMap<>();

    @Override
    public void recordResult(String deviceToken, CommandResult result) {
        // Defensive programming: ensure non-null inputs
        if (deviceToken == null || deviceToken.isBlank() || result == null) {
            return;
        }
        history.computeIfAbsent(deviceToken, k -> new CopyOnWriteArrayList<>()).add(result);
    }

    @Override
    public Map<String, List<CommandResult>> getFullHistory() {
        // Return a defensive copy to prevent modification of the internal state.
        return new ConcurrentHashMap<>(history);
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */