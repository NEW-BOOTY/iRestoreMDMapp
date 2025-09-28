/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
package com.devinroyal.irestore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestoreService {
    private static final Logger logger = LoggerFactory.getLogger(RestoreService.class);
    private static final String IDEVICERESTORE_PATH = "/usr/local/bin/idevicerestore";
    private final AtomicBoolean isRestoring = new AtomicBoolean(false);

    public enum RestoreType {
        UPDATE("--update"), ERASE("--erase");

        private final String flag;
        RestoreType(String flag) { this.flag = flag; }
        public String getFlag() { return flag; }
    }

    public String performRestore(String ipswPath, RestoreType type) throws IOException {
        if (!Files.exists(Paths.get(ipswPath))) {
            logger.error("IPSW file does not exist: {}", ipswPath);
            throw new IOException("IPSW file not found: " + ipswPath);
        }
        if (isRestoring.get()) {
            logger.warn("Restore already in progress");
            throw new IOException("Restore operation already in progress");
        }

        isRestoring.set(true);
        StringBuilder output = new StringBuilder();
        output.append("Starting ").append(type.name()).append(" restore with ").append(ipswPath).append("\n");

        List<String> command = new ArrayList<>();
        command.add(IDEVICERESTORE_PATH);
        command.add(type.getFlag());
        command.add(ipswPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.info("Restore output: {}", line);
            }
        } catch (IOException e) {
            logger.error("Error during restore process", e);
            isRestoring.set(false);
            throw new IOException("Restore failed: " + e.getMessage(), e);
        }

        try {
            int exitCode = process.waitFor();
            output.append("Restore completed with exit code: ").append(exitCode).append("\n");
            if (exitCode == 0) {
                output.append("Success! Device should be restored.\n");
                logger.info("Restore completed successfully");
            } else {
                output.append("Error occurred. Check logs.\n");
                logger.warn("Restore failed with exit code: {}", exitCode);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted during restore", e);
            Thread.currentThread().interrupt();
            output.append("Restore interrupted.\n");
            isRestoring.set(false);
            throw new IOException("Restore interrupted", e);
        }

        isRestoring.set(false);
        return output.toString();
    }
}
/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
