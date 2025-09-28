/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
package com.devinroyal.irestore;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class IRestoreMDMApp {
    private static final Logger logger = LoggerFactory.getLogger(IRestoreMDMApp.class);
    private final DeviceManager deviceManager = new DeviceManager();
    private final RestoreService restoreService = new RestoreService();
    private final MDMService mdmService = new MDMService();
    private JTextArea logArea;
    private JComboBox<String> deviceComboBox;
    private JTextField ipswField;
    private JComboBox<String> restoreTypeComboBox;
    private JTextField commandField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IRestoreMDMApp().createAndShowGUI());
    }

    private void createAndShowGUI() {
        setupHttpServer();
        JFrame frame = new JFrame("iRestoreMDM - iOS Restore and MDM Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        // Device selection panel
        JPanel devicePanel = new JPanel(new BorderLayout());
        deviceComboBox = new JComboBox<>();
        devicePanel.add(new JLabel("Connected Devices:"), BorderLayout.NORTH);
        devicePanel.add(deviceComboBox, BorderLayout.CENTER);
        JButton refreshButton = new JButton("Refresh Devices");
        refreshButton.addActionListener(e -> refreshDevices());
        devicePanel.add(refreshButton, BorderLayout.SOUTH);

        // IPSW selection panel
        JPanel ipswPanel = new JPanel(new FlowLayout());
        ipswField = new JTextField(20);
        JButton browseButton = new JButton("Browse IPSW");
        browseButton.addActionListener(e -> selectIpsw());
        ipswPanel.add(new JLabel("IPSW File:"));
        ipswPanel.add(ipswField);
        ipswPanel.add(browseButton);

        // Restore type panel
        JPanel restorePanel = new JPanel(new FlowLayout());
        restoreTypeComboBox = new JComboBox<>(new String[]{"Update Device", "Erase Device"});
        JButton restoreButton = new JButton("Start Restore");
        restoreButton.addActionListener(e -> startRestore());
        restorePanel.add(new JLabel("Restore Type:"));
        restorePanel.add(restoreTypeComboBox);
        restorePanel.add(restoreButton);

        // MDM command panel
        JPanel mdmPanel = new JPanel(new FlowLayout());
        commandField = new JTextField(20);
        JButton sendMdmButton = new JButton("Send MDM Command");
        sendMdmButton.addActionListener(e -> sendMDMCommand());
        mdmPanel.add(new JLabel("MDM Command JSON:"));
        mdmPanel.add(commandField);
        mdmPanel.add(sendMdmButton);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(devicePanel, BorderLayout.NORTH);
        mainPanel.add(ipswPanel, BorderLayout.CENTER);
        mainPanel.add(restorePanel, BorderLayout.SOUTH);
        mainPanel.add(mdmPanel, BorderLayout.WEST);

        frame.add(mainPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
        refreshDevices();
    }

    private void setupHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/status", new StatusHandler());
            server.setExecutor(null);
            server.start();
            logArea.append("HTTP server started on port 8080 for status feedback.\n");
            logger.info("HTTP server started on port 8080.");
        } catch (IOException e) {
            logArea.append("Failed to start HTTP server: " + e.getMessage() + "\n");
            logger.error("Failed to start HTTP server", e);
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Command Execution History:\n" + mdmService.getExecutionHistory().toString();
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private void refreshDevices() {
        try {
            List<String> devices = deviceManager.listDevices();
            deviceComboBox.removeAllItems();
            for (String device : devices) {
                deviceComboBox.addItem(device);
            }
            logArea.append("Found " + devices.size() + " devices.\n");
            logger.info("Refreshed device list: {} devices found", devices.size());
        } catch (IOException e) {
            logArea.append("Error listing devices: " + e.getMessage() + "\n");
            logger.error("Failed to refresh devices", e);
        }
    }

    private void selectIpsw() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("IPSW Files", "ipsw"));
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            ipswField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            logger.info("Selected IPSW: {}", ipswField.getText());
        }
    }

    private void startRestore() {
        String selectedDevice = (String) deviceComboBox.getSelectedItem();
        String ipswPath = ipswField.getText();
        String restoreType = (String) restoreTypeComboBox.getSelectedItem();

        if (selectedDevice == null || ipswPath.isEmpty()) {
            logArea.append("Please select a device and IPSW file.\n");
            logger.warn("Restore attempted without device or IPSW");
            return;
        }

        RestoreService.RestoreType type = restoreType.equals("Update Device") ? RestoreService.RestoreType.UPDATE : RestoreService.RestoreType.ERASE;

        new Thread(() -> {
            try {
                String output = restoreService.performRestore(ipswPath, type);
                logArea.append(output);
            } catch (IOException e) {
                logArea.append("Restore failed: " + e.getMessage() + "\n");
                logger.error("Restore failed for IPSW: {}", ipswPath, e);
            }
        }).start();
    }

    private void sendMDMCommand() {
        String selectedDevice = (String) deviceComboBox.getSelectedItem();
        String commandJson = commandField.getText();

        if (selectedDevice == null || commandJson.isEmpty()) {
            logArea.append("Please select a device and enter a valid MDM command JSON.\n");
            logger.warn("MDM command attempted without device or JSON");
            return;
        }

        new Thread(() -> {
            try {
                mdmService.sendMDMCommands(List.of(selectedDevice), commandJson);
                logArea.append("MDM command sent to " + selectedDevice + "\n");
                logger.info("MDM command sent to {}", selectedDevice);
            } catch (Exception e) {
                logArea.append("Failed to send MDM command: " + e.getMessage() + "\n");
                logger.error("Failed to send MDM command", e);
            }
        }).start();
    }
}
/*\n * Copyright © 2024 Devin B. Royal.\n * All Rights Reserved.\n */
