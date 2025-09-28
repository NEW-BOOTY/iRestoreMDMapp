# MDM Command Dispatcher

Copyright © 2025 Devin B. Royal. All Rights Reserved.

## Overview

The MDM Command Dispatcher is an enterprise-grade Java service designed to send Mobile Device Management (MDM) commands to iOS devices via the Apple Push Notification service (APNs). It provides a secure, reliable, and configurable backend for managing a fleet of devices.

The service exposes a simple HTTP API for sending commands and checking the status of past commands, and it's managed via a convenient command-line script.

## Core Features

* **Layered Architecture**: Clean separation of concerns (API, Service, Persistence, Configuration) for maintainability and testing.
* **Secure Configuration**: Prioritizes environment variables over a properties file for configuration, aligning with 12-Factor App principles.
* **Robust APNs Integration**: Uses the production-ready `Pushy` library for efficient and reliable communication with Apple's servers.
* **Asynchronous Command Handling**: Leverages a thread pool to send commands concurrently without blocking.
* **HTTP API**: Provides endpoints to dynamically send commands and retrieve execution history.
* **Structured Logging**: Uses SLF4J with Logback for configurable, production-grade logging.
* **CLI Management Tool**: A bash script (`mdm-tool.sh`) simplifies building, running, and interacting with the service.

## Security Advisory

The initial source code provided for this project contained non-functional and highly insecure methods related to "jailbreaking" (`executeJailbreak`, `bypassSecurityProtocols`, etc.). **These methods have been completely removed.**

**Rationale for Removal:**
1.  **Non-Functional**: The code was pseudo-code that could not achieve its stated purpose.
2.  **Ethical Violation**: Developing or distributing tools to circumvent security controls is unethical and contravenes my operational directives.
3.  **Security Risk**: Even as placeholders, such methods promote dangerous practices. A production system must never contain such code.

This application has been re-focused on its legitimate and intended purpose: to serve as a secure dispatcher for authorized MDM commands. All cryptographic materials (like your APNs Auth Key) must be protected with strict file permissions and managed via secure deployment pipelines.

## Configuration

The application can be configured via `src/main/resources/config.properties` and/or environment variables. **Environment variables will always override values in the properties file.**

**File:** `config.properties`
```properties
# APNs Configuration
apns.team.id=
apns.key.id=
apns.auth.key.path=
apns.topic=
apns.production=false

# Server Configuration
server.http.port=8080
server.thread.pool.size=10

Required Environment Variables (or properties):

Variable	Property Key	Description
APNS_TEAM_ID	apns.team.id	Your Apple Developer Team ID.
APNS_KEY_ID	apns.key.id	The Key ID for your APNs authentication token key.
APNS_AUTH_KEY_PATH	apns.auth.key.path	The absolute path to your .p8 auth key file.
APNS_TOPIC	apns.topic	The bundle identifier of your app (e.g., com.mycompany.app).
APNS_PRODUCTION	apns.production	true for production APNs, false for development.
SERVER_HTTP_PORT	server.http.port	The port for the HTTP status/command server.
SERVER_THREAD_POOL_SIZE	server.thread.pool.size	The number of threads for sending APNs notifications.
Build Instructions
This project uses Apache Maven. Ensure you have Maven and a JDK (17+) installed.

To build the executable "fat JAR":

Bash
mvn clean package
This will create target/mdm-command-dispatcher-1.0.0-RELEASE.jar.

Usage with mdm-tool.sh
The scripts/mdm-tool.sh script is the recommended way to manage the service.

Prerequisites: mvn, java, curl.

Make the script executable:

Bash
chmod +x scripts/mdm-tool.sh
Available Commands:

Build the application:

Bash
./scripts/mdm-tool.sh build
Run the service (foreground):
Ensure your config.properties is populated or environment variables are set.

Bash
./scripts/mdm-tool.sh run
Check service status:
Retrieves the execution history from the running service.

Bash
./scripts/mdm-tool.sh status
Send an MDM command:
Sends a JSON payload to a specified device token.

Bash
# Usage: ./scripts/mdm-tool.sh send-command <DEVICE_TOKEN> '<JSON_PAYLOAD>'

# Example: Device Lock command
./scripts/mdm-tool.sh send-command "your_device_token_here" '{"CommandUUID":"SomeUUID-1234","Command":{"RequestType":"DeviceLock"}}'
API Endpoints
The service runs an HTTP server on the configured port (default 8080).

GET /status: Retrieves a JSON representation of the command execution history.

Success Response (200 OK):

JSON
{
  "your_device_token_here": [
    {
      "commandUUID": "SomeUUID-1234",
      "status": "ACCEPTED",
      "timestamp": "2025-09-26T21:30:00.123Z",
      "rejectionReason": null
    }
  ]
}
POST /command: Submits a new MDM command.

Request Body:

JSON
{
  "deviceToken": "your_device_token_here",
  "payload": {
    "CommandUUID": "SomeUUID-5678",
    "Command": {
      "RequestType": "DeviceInformation"
    }
  }
}
Success Response (202 Accepted):

JSON
{
  "message": "Command submitted for processing",
  "deviceToken": "your_device_token_here",
  "commandUUID": "SomeUUID-5678"
}
Error Response (400 Bad Request):

JSON
{
  "error": "Invalid request body: deviceToken is required"
}