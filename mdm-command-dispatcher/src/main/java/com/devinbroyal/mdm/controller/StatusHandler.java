/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.controller;

import com.devinbroyal.mdm.persistence.ExecutionHistoryRepository;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StatusHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
    private final ExecutionHistoryRepository historyRepository;
    private final Gson gson;

    public StatusHandler(ExecutionHistoryRepository historyRepository, Gson gson) {
        this.historyRepository = historyRepository;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        try {
            String jsonResponse = gson.toJson(historyRepository.getFullHistory());
            sendResponse(exchange, 200, jsonResponse);
        } catch (Exception e) {
            logger.error("Failed to retrieve and serialize execution history", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */