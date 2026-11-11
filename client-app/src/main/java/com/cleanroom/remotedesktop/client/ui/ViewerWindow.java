package com.cleanroom.remotedesktop.client.ui;

import com.cleanroom.remotedesktop.client.viewer.ViewerSession;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.ChatMessage;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteKeyboardEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.RemoteMouseEvent;
import com.cleanroom.remotedesktop.protocol.model.InputModels.SystemAction;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.QualitySettings;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenMetadata;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenTile;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenTileBatch;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ViewerWindow {
    private final ViewerSession session;
    private Stage stage;

    private Canvas canvas;
    private GraphicsContext gc;
    private StackPane canvasHolder;

    private VBox chatPane;
    private TextArea chatMessagesArea;
    private TextField chatInputField;

    private int remoteWidth = 1920;
    private int remoteHeight = 1080;
    private boolean bestScaled = true;
    private boolean viewOnly = false;

    private FileTransferDialog fileTransferDialog;

    public ViewerWindow(ViewerSession session) {
        this.session = session;
        this.fileTransferDialog = new FileTransferDialog(session);
    }

    public void show() {
        Platform.runLater(() -> {
            stage = new Stage();
            stage.setTitle("Clean-Room Remote Desktop - Remote Session with Partner ID: " + session.getTargetPartnerId());

            BorderPane root = new BorderPane();

            // 1. Top In-Session Toolbar
            ToolBar toolBar = createToolbar();
            root.setTop(toolBar);

            // 2. Center Viewport
            canvas = new Canvas(remoteWidth, remoteHeight);
            gc = canvas.getGraphicsContext2D();
            gc.setImageSmoothing(true);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, remoteWidth, remoteHeight);

            canvasHolder = new StackPane(canvas);
            canvasHolder.setStyle("-fx-background-color: #0f172a;");
            root.setCenter(canvasHolder);

            // Attach canvas mouse & key listeners
            attachInputListeners();

            // 3. Right Chat Drawer
            chatPane = createChatDrawer();
            chatPane.setVisible(false);
            chatPane.setManaged(false);
            root.setRight(chatPane);

            // Register session callbacks
            session.setOnScreenMetadataReceived(this::onMetadataReceived);
            session.setOnScreenTilesReceived(this::onTilesReceived);
            session.setOnChatMessageReceived(this::onChatMessageReceived);
            session.setOnSessionTerminated(() -> Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "The remote session has been terminated.");
                alert.showAndWait();
                stage.close();
            }));

            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setMaximized(true);

            stage.setOnCloseRequest(e -> {
                session.disconnect();
            });

            // Handle resize
            scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustCanvasScale());
            scene.heightProperty().addListener((obs, oldVal, newVal) -> adjustCanvasScale());

            stage.show();
            canvas.requestFocus();

            // Request initial High Quality (90% JPEG) for crystal-clear remote display
            session.sendQualitySettings(new QualitySettings(QualitySettings.QualityMode.QUALITY, 0.90f, 25, 0));
        });
    }

    private ToolBar createToolbar() {
        ToolBar toolBar = new ToolBar();
        toolBar.setStyle("-fx-background-color: #1e293b; -fx-padding: 4 8;");

        // Menu: Actions
        MenuButton actionsMenu = new MenuButton("Actions");
        actionsMenu.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        MenuItem ctrlAltDelItem = new MenuItem("Send Ctrl+Alt+Del");
        ctrlAltDelItem.setOnAction(e -> session.sendSystemAction(new SystemAction(SystemAction.ActionType.CTRL_ALT_DEL)));

        MenuItem lockScreenItem = new MenuItem("Lock Screen");
        lockScreenItem.setOnAction(e -> session.sendSystemAction(new SystemAction(SystemAction.ActionType.LOCK_SCREEN)));

        MenuItem disconnectItem = new MenuItem("Disconnect");
        disconnectItem.setOnAction(e -> {
            session.disconnect();
            stage.close();
        });

        actionsMenu.getItems().addAll(ctrlAltDelItem, lockScreenItem, new SeparatorMenuItem(), disconnectItem);

        // Menu: Display
        MenuButton displayMenu = new MenuButton("Display");

        CheckMenuItem bestScaledItem = new CheckMenuItem("Best Scaled");
        bestScaledItem.setSelected(true);
        bestScaledItem.setOnAction(e -> {
            bestScaled = bestScaledItem.isSelected();
            adjustCanvasScale();
        });

        CheckMenuItem fullScreenItem = new CheckMenuItem("Full Screen");
        fullScreenItem.setOnAction(e -> stage.setFullScreen(fullScreenItem.isSelected()));

        Menu qualitySubMenu = new Menu("Quality");
        RadioMenuItem ultraItem = new RadioMenuItem("Ultra HD (95% - Crystal Clear)");
        RadioMenuItem highItem = new RadioMenuItem("High Quality (90% - Sharp Text)");
        RadioMenuItem balancedItem = new RadioMenuItem("Balanced (75%)");
        RadioMenuItem speedItem = new RadioMenuItem("Optimize Speed (50% - Low Bandwidth)");

        ToggleGroup qualityGroup = new ToggleGroup();
        ultraItem.setToggleGroup(qualityGroup);
        highItem.setToggleGroup(qualityGroup);
        balancedItem.setToggleGroup(qualityGroup);
        speedItem.setToggleGroup(qualityGroup);

        highItem.setSelected(true);

        ultraItem.setOnAction(e -> session.sendQualitySettings(new QualitySettings(QualitySettings.QualityMode.QUALITY, 0.95f, 25, 0)));
        highItem.setOnAction(e -> session.sendQualitySettings(new QualitySettings(QualitySettings.QualityMode.QUALITY, 0.90f, 25, 0)));
        balancedItem.setOnAction(e -> session.sendQualitySettings(new QualitySettings(QualitySettings.QualityMode.AUTO, 0.75f, 25, 0)));
        speedItem.setOnAction(e -> session.sendQualitySettings(new QualitySettings(QualitySettings.QualityMode.SPEED, 0.50f, 30, 0)));

        qualitySubMenu.getItems().addAll(ultraItem, highItem, balancedItem, speedItem);
        displayMenu.getItems().addAll(bestScaledItem, fullScreenItem, new SeparatorMenuItem(), qualitySubMenu);

        // Buttons
        ToggleButton viewOnlyBtn = new ToggleButton("View-Only");
        viewOnlyBtn.setOnAction(e -> {
            this.viewOnly = viewOnlyBtn.isSelected();
            session.setViewOnly(viewOnly);
        });

        Button fileTransferBtn = new Button("File Transfer");
        fileTransferBtn.setOnAction(e -> fileTransferDialog.show());

        ToggleButton chatBtn = new ToggleButton("Chat");
        chatBtn.setOnAction(e -> {
            boolean show = chatBtn.isSelected();
            chatPane.setVisible(show);
            chatPane.setManaged(show);
        });

        Button closeBtn = new Button("End Session");
        closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            session.disconnect();
            stage.close();
        });

        toolBar.getItems().addAll(
                actionsMenu,
                displayMenu,
                new Separator(),
                viewOnlyBtn,
                fileTransferBtn,
                chatBtn,
                new Separator(),
                closeBtn
        );
        return toolBar;
    }

    private VBox createChatDrawer() {
        VBox box = new VBox(8);
        box.setPrefWidth(260);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-width: 0 0 0 1;");

        Label chatTitle = new Label("In-Session Chat");
        chatTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        chatMessagesArea = new TextArea();
        chatMessagesArea.setEditable(false);
        chatMessagesArea.setWrapText(true);
        VBox.setVgrow(chatMessagesArea, Priority.ALWAYS);

        HBox inputRow = new HBox(6);
        chatInputField = new TextField();
        HBox.setHgrow(chatInputField, Priority.ALWAYS);
        chatInputField.setPromptText("Type a message...");

        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");

        Runnable doSend = () -> {
            String text = chatInputField.getText();
            if (text != null && !text.trim().isEmpty()) {
                session.sendChatMessage(text);
                appendChatMessage("Me", text);
                chatInputField.clear();
            }
        };

        sendBtn.setOnAction(e -> doSend.run());
        chatInputField.setOnAction(e -> doSend.run());

        inputRow.getChildren().addAll(chatInputField, sendBtn);
        box.getChildren().addAll(chatTitle, chatMessagesArea, inputRow);
        return box;
    }

    private void appendChatMessage(String sender, String text) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        chatMessagesArea.appendText("[" + time + "] " + sender + ": " + text + "\n");
    }

    private void onChatMessageReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            appendChatMessage(msg.getSenderName() != null ? msg.getSenderName() : "Partner", msg.getText());
            if (!chatPane.isVisible()) {
                chatPane.setVisible(true);
                chatPane.setManaged(true);
            }
        });
    }

    private void onMetadataReceived(ScreenMetadata metadata) {
        Platform.runLater(() -> {
            this.remoteWidth = metadata.getScreenWidth();
            this.remoteHeight = metadata.getScreenHeight();
            canvas.setWidth(remoteWidth);
            canvas.setHeight(remoteHeight);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, remoteWidth, remoteHeight);
            adjustCanvasScale();
        });
    }

    private void onTilesReceived(ScreenTileBatch batch) {
        Platform.runLater(() -> {
            for (ScreenTile tile : batch.getTiles()) {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(tile.getData());
                    java.awt.image.BufferedImage awtImage = ImageIO.read(bais);
                    if (awtImage != null) {
                        Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
                        gc.setImageSmoothing(true);
                        gc.drawImage(fxImage, tile.getX(), tile.getY(), tile.getWidth(), tile.getHeight());
                    }
                } catch (Exception e) {
                    // Ignore decoding glitch on dropped packet
                }
            }
        });
    }

    private void adjustCanvasScale() {
        if (!bestScaled || canvasHolder.getWidth() <= 0 || canvasHolder.getHeight() <= 0) {
            canvas.setScaleX(1.0);
            canvas.setScaleY(1.0);
            return;
        }

        double availableWidth = canvasHolder.getWidth();
        double availableHeight = canvasHolder.getHeight();

        double scaleX = availableWidth / remoteWidth;
        double scaleY = availableHeight / remoteHeight;
        double scale = Math.min(scaleX, scaleY);

        canvas.setScaleX(scale);
        canvas.setScaleY(scale);
    }

    private void attachInputListeners() {
        canvas.setFocusTraversable(true);

        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> forwardMouse(e, RemoteMouseEvent.Action.MOVE));
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> forwardMouse(e, RemoteMouseEvent.Action.MOVE));
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            canvas.requestFocus();
            forwardMouse(e, RemoteMouseEvent.Action.PRESS);
        });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> forwardMouse(e, RemoteMouseEvent.Action.RELEASE));

        canvas.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (viewOnly) return;
            int delta = (int) -Math.signum(e.getDeltaY());
            double xPercent = Math.max(0.0, Math.min(1.0, e.getX() / canvas.getWidth()));
            double yPercent = Math.max(0.0, Math.min(1.0, e.getY() / canvas.getHeight()));
            session.sendMouseEvent(new RemoteMouseEvent(RemoteMouseEvent.Action.WHEEL, xPercent, yPercent, RemoteMouseEvent.Button.NONE, delta));
        });

        canvas.addEventHandler(KeyEvent.KEY_PRESSED, e -> forwardKey(e, RemoteKeyboardEvent.Action.PRESS));
        canvas.addEventHandler(KeyEvent.KEY_RELEASED, e -> forwardKey(e, RemoteKeyboardEvent.Action.RELEASE));
    }

    private void forwardMouse(MouseEvent e, RemoteMouseEvent.Action action) {
        if (viewOnly) return;

        double xPercent = Math.max(0.0, Math.min(1.0, e.getX() / canvas.getWidth()));
        double yPercent = Math.max(0.0, Math.min(1.0, e.getY() / canvas.getHeight()));

        RemoteMouseEvent.Button button = RemoteMouseEvent.Button.NONE;
        if (e.getButton() == MouseButton.PRIMARY) button = RemoteMouseEvent.Button.LEFT;
        else if (e.getButton() == MouseButton.SECONDARY) button = RemoteMouseEvent.Button.RIGHT;
        else if (e.getButton() == MouseButton.MIDDLE) button = RemoteMouseEvent.Button.MIDDLE;

        session.sendMouseEvent(new RemoteMouseEvent(action, xPercent, yPercent, button, 0));
    }

    private void forwardKey(KeyEvent e, RemoteKeyboardEvent.Action action) {
        if (viewOnly) return;

        int awtKeyCode = mapFxToAwtKeyCode(e.getCode());
        char keyChar = e.getCharacter() != null && !e.getCharacter().isEmpty() ? e.getCharacter().charAt(0) : '\0';

        session.sendKeyboardEvent(new RemoteKeyboardEvent(action, awtKeyCode, keyChar, 0));
        e.consume();
    }

    private int mapFxToAwtKeyCode(KeyCode code) {
        return switch (code) {
            case ENTER -> java.awt.event.KeyEvent.VK_ENTER;
            case BACK_SPACE -> java.awt.event.KeyEvent.VK_BACK_SPACE;
            case TAB -> java.awt.event.KeyEvent.VK_TAB;
            case ESCAPE -> java.awt.event.KeyEvent.VK_ESCAPE;
            case SPACE -> java.awt.event.KeyEvent.VK_SPACE;
            case DELETE -> java.awt.event.KeyEvent.VK_DELETE;
            case UP -> java.awt.event.KeyEvent.VK_UP;
            case DOWN -> java.awt.event.KeyEvent.VK_DOWN;
            case LEFT -> java.awt.event.KeyEvent.VK_LEFT;
            case RIGHT -> java.awt.event.KeyEvent.VK_RIGHT;
            case SHIFT -> java.awt.event.KeyEvent.VK_SHIFT;
            case CONTROL -> java.awt.event.KeyEvent.VK_CONTROL;
            case ALT -> java.awt.event.KeyEvent.VK_ALT;
            case WINDOWS -> java.awt.event.KeyEvent.VK_WINDOWS;
            default -> {
                String name = code.getName();
                if (name != null && name.length() == 1) {
                    yield (int) name.toUpperCase().charAt(0);
                }
                yield java.awt.event.KeyEvent.VK_UNDEFINED;
            }
        };
    }
}
