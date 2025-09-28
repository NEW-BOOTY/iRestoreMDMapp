#!/bin/bash
#
# Copyright © 2025 Devin B. Royal.
# All Rights Reserved.
#
# This script reconstructs the original single-file "legacy" Java application
# from the current enterprise source code for reference and compatibility purposes.
#
# WARNING: The output of this script is NOT recommended for production use.
# It is insecure by design and lacks the robustness of the main application.

set -e

# --- Configuration ---
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly LEGACY_DIR="${PROJECT_ROOT}/legacy"
readonly LEGACY_SRC_DIR="${LEGACY_DIR}/src/main/java"
readonly LEGACY_JAVA_FILE="${LEGACY_SRC_DIR}/PurpleRestoreAppMDM.java"
readonly RESOURCES_DIR="${PROJECT_ROOT}/src/main/resources"

# --- Helper Functions ---
function log_info() {
    echo "[INFO] $1"
}

function log_warn() {
    echo "[WARN] $1"
}

# --- Main Logic ---
log_info "Starting reconstruction of the legacy application..."

# 1. Create the directory structure for the legacy project
mkdir -p "${LEGACY_SRC_DIR}"
log_info "Created legacy project directory at: ${LEGACY_DIR}"

# 2. Create the legacy pom.xml
log_info "Generating legacy pom.xml..."
cat > "${LEGACY_DIR}/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.devinbroyal.legacy</groupId>
    <artifactId>purple-restore-app-legacy</artifactId>
    <version>0.1.0</version>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.eatthepath</groupId>
            <artifactId>pushy</artifactId>
            <version>0.15.4</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
    </dependencies>
    <build>
        <sourceDirectory>src/main/java</sourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <mainClass>PurpleRestoreAppMDM</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# 3. Copy the configuration file
log_info "Copying config.properties..."
cp "${RESOURCES_DIR}/config.properties" "${LEGACY_DIR}/config.properties"

# 4. Generate the README for the legacy project
log_info "Generating README-LEGACY.md..."
cat > "${LEGACY_DIR}/README-LEGACY.md" <<'EOF'
# Legacy PurpleRestoreAppMDM

This directory contains the reconstructed single-file Java application, generated for reference and compatibility purposes only.

## CRITICAL SECURITY WARNING

This legacy application is **NOT SUITABLE FOR PRODUCTION**.

* **Insecure Design**: It lacks the proper configuration management, error handling, and architectural separation of the main enterprise application.
* **Dangerous Placeholders**: It contains non-functional methods related to "jailbreaking". These are dangerous anti-patterns and should not be treated as viable code.
* **Low Maintainability**: The monolithic nature of the code makes it extremely difficult to maintain, test, or debug.

**Use the primary `mdm-command-dispatcher` application for all production and operational tasks.**

## How to Run

This project uses a minimal Maven `pom.xml` to manage dependencies and compilation.

**1. Configure:**
Edit the `config.properties` file in this directory with your APNs credentials.

**2. Compile:**
```bash
mvn compile