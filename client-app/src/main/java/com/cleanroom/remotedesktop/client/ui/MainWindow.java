package com.cleanroom.remotedesktop.client.ui;

import com.cleanroom.remotedesktop.client.host.HostAgent;
import com.cleanroom.remotedesktop.client.net.ClientNetworkManager;
import com.cleanroom.remotedesktop.client.viewer.ViewerSession;
import com.cleanroom.remotedesktop.security.RandomPasswordGenerator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainWindow {
    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final Stage stage;
    private final SettingsDialog.AppSettings settings = new SettingsDialog.AppSettings();

    private ClientNetworkManager networkManager;
    private HostAgent hostAgent;
    private ViewerSession viewerSession;
    private HostFloatingBanner hostFloatingBanner;

    // Left Pane UI Controls
    private TextField yourIdField;
    private TextField passwordField;
    private Circle statusDot;
    private Label statusLabel;
    private Button hostDisconnectBtn;

    // Right Pane UI Controls
    private TextField partnerIdField;
    private PasswordField partnerPasswordField;
    private Button connectButton;
    private Button transferFilesButton;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.hostFloatingBanner = new HostFloatingBanner();
    }

    public void initAndShow() {
        stage.setTitle("Clean-Room Remote Desktop - [Ready]");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f8fafc;");

        // 1. Menu Bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // 2. Center Content: Dual Pane
        HBox contentBox = new HBox(20);
        contentBox.setPadding(new Insets(20));
        contentBox.setAlignment(Pos.CENTER);

        VBox leftPane = createLeftPane();
        VBox rightPane = createRightPane();

        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        contentBox.getChildren().addAll(leftPane, new Separator(javafx.geometry.Orientation.VERTICAL), rightPane);
        root.setCenter(contentBox);

        // 3. Bottom Status Bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 680, 420);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // Connect to relay in background
        connectToRelay();
    }

    private MenuBar createMenuBar() {
        MenuBar bar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            disconnectAll();
            Platform.exit();
            System.exit(0);
        });
        fileMenu.getItems().add(exitItem);

        Menu settingsMenu = new Menu("Settings");
        MenuItem optionsItem = new MenuItem("Options / Configuration...");
        optionsItem.setOnAction(e -> SettingsDialog.show(stage, settings, updated -> {
            log.info("Settings updated. Reconnecting to Relay...");
            connectToRelay();
        }));
        settingsMenu.getItems().add(optionsItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Clean-Room Java Remote Desktop v1.0.0\nFunctionally modeled on UltraViewer workflow.\nOriginal clean-room architecture & Netty TLS relay.");
            a.setHeaderText("About Remote Desktop");
            a.showAndWait();
        });
        helpMenu.getItems().add(aboutItem);

        bar.getMenus().addAll(fileMenu, settingsMenu, helpMenu);
        return bar;
    }

    private VBox createLeftPane() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10));

        Label header = new Label("Allow Remote Control");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        header.setTextFill(Color.web("#1e293b"));

        Label desc = new Label("Please tell your partner the following ID and Password if you would like to allow remote control.");
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setTextFill(Color.web("#64748b"));
        desc.setWrapText(true);

        // Your ID Box
        Label idTitle = new Label("Your ID");
        idTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        HBox idRow = new HBox(6);
        yourIdField = new TextField("Connecting...");
        yourIdField.setEditable(false);
        yourIdField.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        yourIdField.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");
        HBox.setHgrow(yourIdField, Priority.ALWAYS);

        Button copyIdBtn = new Button("Copy");
        copyIdBtn.setStyle("-fx-cursor: hand;");
        copyIdBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(yourIdField.getText().replaceAll("\\s+", ""));
            clipboard.setContent(content);
        });
        idRow.getChildren().addAll(yourIdField, copyIdBtn);

        // Password Box
        Label pwdTitle = new Label("Password");
        pwdTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        HBox pwdRow = new HBox(6);
        passwordField = new TextField();
        passwordField.setEditable(false);
        passwordField.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        passwordField.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #0f172a; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        Button refreshPwdBtn = new Button("Refresh");
        refreshPwdBtn.setStyle("-fx-cursor: hand;");
        refreshPwdBtn.setOnAction(e -> {
            if (hostAgent != null) {
                hostAgent.regenerateRandomPassword();
                passwordField.setText(hostAgent.getRandomPassword());
            }
        });
        pwdRow.getChildren().addAll(passwordField, refreshPwdBtn);

        Hyperlink customPwdLink = new Hyperlink("Custom password options...");
        customPwdLink.setOnAction(e -> SettingsDialog.show(stage, settings, s -> {
            if (hostAgent != null) {
                hostAgent.setCustomPassword(s.getCustomPassword());
            }
        }));

        box.getChildren().addAll(header, desc, idTitle, idRow, pwdTitle, pwdRow, customPwdLink);
        return box;
    }

    private VBox createRightPane() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10));

        Label header = new Label("Control a Remote Computer");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        header.setTextFill(Color.web("#1e293b"));

        Label desc = new Label("Please enter your partner's ID to remote control your partner's computer.");
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setTextFill(Color.web("#64748b"));
        desc.setWrapText(true);

        Label partnerIdLabel = new Label("Partner ID");
        partnerIdLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        partnerIdField = new TextField();
        partnerIdField.setPromptText("e.g. 135 210 707");
        partnerIdField.setFont(Font.font("Segoe UI", 13));

        Label partnerPwdLabel = new Label("Password");
        partnerPwdLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));

        partnerPasswordField = new PasswordField();
        partnerPasswordField.setPromptText("Partner password");
        partnerPasswordField.setFont(Font.font("Segoe UI", 13));

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        connectButton = new Button("Connect to partner");
        connectButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        connectButton.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 4; -fx-cursor: hand;");
        connectButton.setOnAction(e -> handleConnectToPartner());

        transferFilesButton = new Button("Transfer Files");
        transferFilesButton.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        transferFilesButton.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 8 14; -fx-background-radius: 4; -fx-cursor: hand;");
        transferFilesButton.setOnAction(e -> {
            if (viewerSession != null && viewerSession.isConnected()) {
                new FileTransferDialog(viewerSession).show();
            } else {
                handleConnectToPartner();
            }
        });

        btnRow.getChildren().addAll(connectButton, transferFilesButton);
        box.getChildren().addAll(header, desc, partnerIdLabel, partnerIdField, partnerPwdLabel, partnerPasswordField, btnRow);
        return box;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(6, 15, 6, 15));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

        statusDot = new Circle(4, Color.web("#f59e0b")); // Orange while connecting
        statusLabel = new Label("Connecting to relay server...");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#64748b"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hostDisconnectBtn = new Button("Disconnect Partner");
        hostDisconnectBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        hostDisconnectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 4; -fx-cursor: hand;");
        hostDisconnectBtn.setVisible(false);
        hostDisconnectBtn.setOnAction(e -> {
            if (hostAgent != null) {
                hostAgent.disconnectSession();
            }
        });

        bar.getChildren().addAll(statusDot, statusLabel, spacer, hostDisconnectBtn);
        return bar;
    }

    private void connectToRelay() {
        new Thread(() -> {
            disconnectAll();
            try {
                Platform.runLater(() -> {
                    statusDot.setFill(Color.web("#f59e0b"));
                    statusLabel.setText("Connecting to relay at " + settings.getRelayHost() + ":" + settings.getRelayPort() + "...");
                });

                networkManager = new ClientNetworkManager(settings.getRelayHost(), settings.getRelayPort(), settings.isUseTls());
                networkManager.connect();

                hostAgent = new HostAgent(networkManager);
                hostAgent.setCustomPassword(settings.getCustomPassword());
                hostAgent.setRequireConfirmationPrompt(settings.isRequireConfirmationPrompt());

                // Set callback for Host ID
                hostAgent.setOnHostIdAssigned(assignedId -> Platform.runLater(() -> {
                    yourIdField.setText(RandomPasswordGenerator.formatHostId(assignedId));
                    passwordField.setText(hostAgent.getRandomPassword());
                    statusDot.setFill(Color.web("#22c55e"));
                    statusLabel.setText("Ready to connect (Host ID: " + assignedId + ")");
                    stage.setTitle("Clean-Room Remote Desktop - [Ready: " + assignedId + "]");
                }));

                // Mandatory Host Consent Handler
                hostAgent.setConsentPromptHandler((partnerId, callback) -> {
                    HostConsentDialog.show(stage, partnerId, callback);
                });

                // Host active session state
                hostAgent.setOnSessionStateChanged(active -> Platform.runLater(() -> {
                    if (active) {
                        statusDot.setFill(Color.web("#22c55e"));
                        statusLabel.setText("Active: Controlled by " + hostAgent.getActivePartnerId());
                        hostDisconnectBtn.setVisible(true);
                        if (settings.isShowFloatingBanner()) {
                            hostFloatingBanner.show(hostAgent.getActivePartnerId(), () -> {
                                hostAgent.disconnectSession();
                            }, () -> {
                                // Chat trigger
                            });
                        }
                    } else {
                        statusDot.setFill(Color.web("#22c55e"));
                        statusLabel.setText("Ready to connect (Host ID: " + hostAgent.getHostId() + ")");
                        hostDisconnectBtn.setVisible(false);
                        hostFloatingBanner.hide();
                    }
                }));

                viewerSession = new ViewerSession(networkManager);
                viewerSession.setOnSessionAccepted(accepted -> Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    connectButton.setText("Connect to partner");
                    ViewerWindow viewerWindow = new ViewerWindow(viewerSession);
                    viewerWindow.show();
                }));

                viewerSession.setOnSessionRejected(reason -> Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    connectButton.setText("Connect to partner");
                    Alert a = new Alert(Alert.AlertType.ERROR, reason);
                    a.setHeaderText("Connection Failed");
                    a.showAndWait();
                }));

                // Register Host ID with relay
                hostAgent.registerWithRelay(null);

            } catch (Exception e) {
                log.error("Failed to connect to relay server", e);
                Platform.runLater(() -> {
                    statusDot.setFill(Color.web("#ef4444"));
                    statusLabel.setText("Relay connection offline (" + e.getMessage() + ")");
                });
            }
        }, "Relay-Connector-Thread").start();
    }

    private void handleConnectToPartner() {
        String partnerId = partnerIdField.getText().trim();
        String password = partnerPasswordField.getText().trim();

        if (partnerId.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please enter your partner's ID.");
            a.showAndWait();
            return;
        }

        if (password.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please enter your partner's password.");
            a.showAndWait();
            return;
        }

        connectButton.setDisable(true);
        connectButton.setText("Connecting...");

        viewerSession.connectToPartner(partnerId, password);
    }

    private void disconnectAll() {
        if (hostFloatingBanner != null) hostFloatingBanner.hide();
        if (hostAgent != null) hostAgent.disconnectSession();
        if (viewerSession != null) viewerSession.disconnect();
        if (networkManager != null) networkManager.disconnect();
    }
}

