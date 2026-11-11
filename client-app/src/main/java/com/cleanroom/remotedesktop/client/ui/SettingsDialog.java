package com.cleanroom.remotedesktop.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class SettingsDialog {

    public static class AppSettings {
        private String relayHost = "127.0.0.1";
        private int relayPort = 2112;
        private boolean useTls = true;
        private String customPassword = "";

        public AppSettings() {}

        public String getRelayHost() { return relayHost; }
        public void setRelayHost(String relayHost) { this.relayHost = relayHost; }
        public int getRelayPort() { return relayPort; }
        public void setRelayPort(int relayPort) { this.relayPort = relayPort; }
        public boolean isUseTls() { return useTls; }
        public void setUseTls(boolean useTls) { this.useTls = useTls; }
        public String getCustomPassword() { return customPassword; }
        public void setCustomPassword(String customPassword) { this.customPassword = customPassword; }
    }

    public static void show(Stage owner, AppSettings currentSettings, Consumer<AppSettings> onSave) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Settings - Options");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Application & Network Settings");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));

        TabPane tabPane = new TabPane();

        // 1. Network Tab
        Tab networkTab = new Tab("Network & Relay");
        networkTab.setClosable(false);
        GridPane netGrid = new GridPane();
        netGrid.setHgap(10);
        netGrid.setVgap(10);
        netGrid.setPadding(new Insets(15));

        TextField hostField = new TextField(currentSettings.getRelayHost());
        TextField portField = new TextField(String.valueOf(currentSettings.getRelayPort()));
        CheckBox tlsCheck = new CheckBox("Enable TLS 1.3 Encryption");
        tlsCheck.setSelected(currentSettings.isUseTls());

        netGrid.add(new Label("Relay Server Host:"), 0, 0);
        netGrid.add(hostField, 1, 0);
        netGrid.add(new Label("Relay Server Port:"), 0, 1);
        netGrid.add(portField, 1, 1);
        netGrid.add(tlsCheck, 1, 2);
        networkTab.setContent(netGrid);

        // 2. Security Tab
        Tab secTab = new Tab("Security");
        secTab.setClosable(false);
        GridPane secGrid = new GridPane();
        secGrid.setHgap(10);
        secGrid.setVgap(10);
        secGrid.setPadding(new Insets(15));

        PasswordField customPwdField = new PasswordField();
        customPwdField.setText(currentSettings.getCustomPassword());
        customPwdField.setPromptText("Leave empty to disable");

        secGrid.add(new Label("Custom Password:"), 0, 0);
        secGrid.add(customPwdField, 1, 0);
        secGrid.add(new Label("(Allows unattended access with static password)"), 1, 1);
        secTab.setContent(secGrid);

        tabPane.getTabs().addAll(networkTab, secTab);

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Save & Apply");
        saveBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> {
            try {
                currentSettings.setRelayHost(hostField.getText().trim());
                currentSettings.setRelayPort(Integer.parseInt(portField.getText().trim()));
                currentSettings.setUseTls(tlsCheck.isSelected());
                currentSettings.setCustomPassword(customPwdField.getText().trim());
                if (onSave != null) onSave.accept(currentSettings);
                dialog.close();
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Invalid port number: " + portField.getText());
                a.showAndWait();
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(title, tabPane, btnBox);

        Scene scene = new Scene(root, 460, 320);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();
    }
}
