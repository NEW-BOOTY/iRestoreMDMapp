/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */

package com.devinbroyal.mdm.controller;

import com.devinbroyal.mdm.domain.CommandRequest;
import com.devinbroyal.mdm.exception.MdmCommandException;
import com.devinbroyal.mdm.service.MdmService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class CommandHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final MdmService mdmService;
    private final Gson gson;

    public CommandHandler(MdmService mdmService, Gson gson) {
        this.mdmService = mdmService;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            CommandRequest request = gson.fromJson(reader, CommandRequest.class);

            if (request == null || request.getDeviceToken() == null || request.getDeviceToken().isBlank() || request.getPayload() == null) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid request body: deviceToken and payload are required\"}");
                return;
            }

            // Ensure a CommandUUID exists for tracking
            if (!request.getPayload().containsKey("CommandUUID")) {
                String generatedUUID = UUID.randomUUID().toString();
                request.getPayload().put("CommandUUID", generatedUUID);
                logger.warn("No CommandUUID found in payload. Generated new UUID: {}", generatedUUID);
            }
            String commandUUID = (String) request.getPayload().get("CommandUUID");

            mdmService.sendCommand(request.getDeviceToken(), request.getPayload());

            String responseBody = gson.toJson(Map.of(
                "message", "Command submitted for processing",
                "deviceToken", request.getDeviceToken(),
                "commandUUID", commandUUID
            ));
            sendResponse(exchange, 202, responseBody);

        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse JSON request body", e);
            sendResponse(exchange, 400, "{\"error\":\"Malformed JSON request body\"}");
        } catch (MdmCommandException e) {
            logger.error("Error processing MDM command request for token {}", getPartialTokenForLogging(e.getDeviceToken()), e);
            sendResponse(exchange, 500, "{\"error\":\"Failed to send MDM command\"}");
        } catch (Exception e) {
            logger.error("An unexpected error occurred in CommandHandler", e);
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
    
    private String getPartialTokenForLogging(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}

/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 */