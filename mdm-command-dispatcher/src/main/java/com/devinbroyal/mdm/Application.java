/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm;

import com.devinbroyal.mdm.config.AppConfig;
import com.devinbroyal.mdm.config.MdmProperties;
import com.devinbroyal.mdm.controller.CommandHandler;
import com.devinbroyal.mdm.controller.StatusHandler;
import com.devinbroyal.mdm.persistence.ExecutionHistoryRepository;
import com.devinbroyal.mdm.persistence.InMemoryExecutionHistoryRepository;
import com.devinbroyal.mdm.service.ApnsMdmService;
import com.devinbroyal.mdm.service.MdmService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Initializing MDM Command Dispatcher...");

        try {
            // 1. Load and validate configuration
            final MdmProperties properties = AppConfig.loadProperties();
            logger.info("Configuration loaded successfully. APNs Environment: {}", properties.isProduction() ? "Production" : "Development");

            // 2. Setup dependencies
            final ExecutorService notificationExecutor = Executors.newFixedThreadPool(properties.getThreadPoolSize());
            final ExecutionHistoryRepository historyRepository = new InMemoryExecutionHistoryRepository();
            final Gson gson = new Gson();

            // 3. Initialize the APNs service layer
            final MdmService mdmService = new ApnsMdmService(properties, notificationExecutor, historyRepository);

            // 4. Start the HTTP server for API endpoints
            startHttpServer(properties, mdmService, historyRepository, gson);

            // 5. Add a shutdown hook for graceful termination
            addShutdownHook(mdmService, notificationExecutor);

        } catch (Exception e) {
            logger.error("Fatal error during application startup. The application will now exit.", e);
            System.exit(1);
        }
    }

    private static void startHttpServer(MdmProperties properties, MdmService mdmService, ExecutionHistoryRepository historyRepository, Gson gson) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(properties.getHttpPort()), 0);
        server.createContext("/status", new StatusHandler(historyRepository, gson));
        server.createContext("/command", new CommandHandler(mdmService, gson));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        logger.info("HTTP server started successfully on port {}. Endpoints available at /status and /command", properties.getHttpPort());
    }

    private static void addShutdownHook(MdmService mdmService, ExecutorService notificationExecutor) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received. Shutting down gracefully...");
            try {
                // Shutdown MDM service (closes ApnsClient)
                mdmService.shutdown();

                // Shutdown executor service
                notificationExecutor.shutdown();
                if (!notificationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate in 10 seconds. Forcing shutdown.");
                    notificationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted during graceful shutdown.", e);
                notificationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error during service shutdown.", e);
            }
            logger.info("MDM Command Dispatcher shut down complete.");
        }));
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */