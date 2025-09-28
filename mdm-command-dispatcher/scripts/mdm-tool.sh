---

### **Management Script**

**File:** `mdm-command-dispatcher/scripts/mdm-tool.sh`
```bash
#!/bin/bash
#
# Copyright © 2025 Devin B. Royal.
# All Rights Reserved.
#
# Management script for the MDM Command Dispatcher

# --- Configuration ---
set -e
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly JAR_NAME="mdm-command-dispatcher-1.0.0-RELEASE.jar"
readonly JAR_PATH="${PROJECT_ROOT}/target/${JAR_NAME}"
readonly CONFIG_FILE="${PROJECT_ROOT}/src/main/resources/config.properties"

# --- Helper Functions ---
function log_info() {
    echo "[INFO] $1"
}

function log_error() {
    echo "[ERROR] $1" >&2
    exit 1
}

function check_deps() {
    for cmd in "$@"; do
        if ! command -v "$cmd" &> /dev/null; then
            log_error "Required command '$cmd' is not installed. Please install it and try again."
        fi
    done
}

function get_config_value() {
    grep "^${1}=" "${CONFIG_FILE}" | cut -d'=' -f2
}

# --- Main Functions ---
function build_app() {
    log_info "Building the MDM Command Dispatcher..."
    check_deps mvn
    (cd "${PROJECT_ROOT}" && mvn clean package)
    if [[ -f "${JAR_PATH}" ]]; then
        log_info "Build successful. JAR created at: ${JAR_PATH}"
    else
        log_error "Build failed. JAR not found."
    fi
}

function run_app() {
    log_info "Starting the MDM Command Dispatcher..."
    if [[ ! -f "${JAR_PATH}" ]]; then
        log_error "JAR file not found. Please build the project first with './mdm-tool.sh build'."
    fi
    
    # Export properties from config file as environment variables if they are not already set
    # This allows overriding config with shell exports
    export APNS_TEAM_ID="${APNS_TEAM_ID:-$(get_config_value apns.team.id)}"
    export APNS_KEY_ID="${APNS_KEY_ID:-$(get_config_value apns.key.id)}"
    export APNS_AUTH_KEY_PATH="${APNS_AUTH_KEY_PATH:-$(get_config_value apns.auth.key.path)}"
    export APNS_TOPIC="${APNS_TOPIC:-$(get_config_value apns.topic)}"
    export APNS_PRODUCTION="${APNS_PRODUCTION:-$(get_config_value apns.production)}"
    export SERVER_HTTP_PORT="${SERVER_HTTP_PORT:-$(get_config_value server.http.port)}"
    export SERVER_THREAD_POOL_SIZE="${SERVER_THREAD_POOL_SIZE:-$(get_config_value server.thread.pool.size)}"
    
    java -jar "${JAR_PATH}"
}

function get_status() {
    log_info "Querying service status..."
    check_deps curl
    local port="${SERVER_HTTP_PORT:-$(get_config_value server.http.port | tr -d '[:space:]')}"
    if [[ -z "${port}" ]]; then
        port=8080
    fi
    curl -s -X GET "http://localhost:${port}/status" | jq .
    echo ""
}

function send_command() {
    check_deps curl
    local device_token="$1"
    local payload="$2"
    
    if [[ -z "${device_token}" || -z "${payload}" ]]; then
        log_error "Usage: $0 send-command <DEVICE_TOKEN> '<JSON_PAYLOAD>'"
    fi
    
    log_info "Sending command to device: ${device_token}"
    
    local port="${SERVER_HTTP_PORT:-$(get_config_value server.http.port | tr -d '[:space:]')}"
    if [[ -z "${port}" ]]; then
        port=8080
    fi

    local request_body
    request_body=$(cat <<EOF
{
  "deviceToken": "${device_token}",
  "payload": ${payload}
}
EOF
)

    curl -s -X POST "http://localhost:${port}/command" \
        -H "Content-Type: application/json" \
        -d "${request_body}" | jq .
    echo ""
}

function usage() {
    echo "MDM Command Dispatcher Management Tool"
    echo "Copyright © 2025 Devin B. Royal. All Rights Reserved."
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  build           Builds the application JAR file."
    echo "  run             Runs the MDM service in the foreground."
    echo "  status          Queries the running service for command history."
    echo "  send-command    Sends an MDM command to a device."
    echo "                  Usage: $0 send-command <DEVICE_TOKEN> '<JSON_PAYLOAD>'"
    echo "  help            Displays this help message."
    echo ""
    echo "Example:"
    echo "  ./scripts/mdm-tool.sh send-command 'my-token' '{\"Command\":{\"RequestType\":\"DeviceLock\"}}'"
}

# --- Command Router ---
COMMAND="$1"
shift || true

case "${COMMAND}" in
    build)
        build_app
        ;;
    run)
        run_app
        ;;
    status)
        get_status
        ;;
    send-command)
        send_command "$@"
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        log_error "Unknown command: ${COMMAND}. Use 'help' to see available commands."
        ;;
esac

#
# Copyright © 2025 Devin B. Royal.
# All Rights Reserved.
#
