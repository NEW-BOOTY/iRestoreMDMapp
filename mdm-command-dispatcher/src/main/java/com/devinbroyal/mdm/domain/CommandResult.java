/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.domain;

import java.time.Instant;

public class CommandResult {

    public enum Status {
        ACCEPTED,
        REJECTED,
        FAILED_TO_SEND
    }

    private final String commandUUID;
    private final Status status;
    private final Instant timestamp;
    private final String rejectionReason;

    public CommandResult(String commandUUID, Status status, String rejectionReason) {
        this.commandUUID = commandUUID;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.timestamp = Instant.now();
    }

    public String getCommandUUID() {
        return commandUUID;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */