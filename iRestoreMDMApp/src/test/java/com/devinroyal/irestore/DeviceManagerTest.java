/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
package com.devinroyal.irestore;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DeviceManagerTest {
    private final DeviceManager deviceManager = new DeviceManager();

    @Test
    void testListDevicesNoThrow() {
        assertDoesNotThrow(() -> deviceManager.listDevices(), "Listing devices should not throw an exception");
    }
}
/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
