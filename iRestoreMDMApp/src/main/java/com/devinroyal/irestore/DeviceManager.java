/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
package com.devinroyal.irestore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DeviceManager {
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    private static final String IDEVICE_ID_PATH = "/usr/local/bin/idevice_id";

    public List<String> listDevices() throws IOException {
        List<String> devices = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder(IDEVICE_ID_PATH, "-l");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    devices.add(line.trim());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to list devices", e);
            throw new IOException("Error executing idevice_id: " + e.getMessage(), e);
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("idevice_id exited with code: {}", exitCode);
                throw new IOException("idevice_id failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for idevice_id", e);
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during device listing", e);
        }

        logger.info("Found {} devices: {}", devices.size(), devices);
        return devices;
    }
}
/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
