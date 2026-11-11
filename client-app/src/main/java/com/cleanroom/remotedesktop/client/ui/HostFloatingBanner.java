package com.cleanroom.remotedesktop.client.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class HostFloatingBanner {
    private Stage stage;
    private Timeline timerTimeline;
    private long startTime;

    public void show(String partnerId, Runnable onDisconnect, Runnable onToggleChat) {
        Platform.runLater(() -> {
            if (stage != null && stage.isShowing()) {
                return;
            }

            stage = new Stage();
            stage.initStyle(StageStyle.UTILITY);
            stage.setAlwaysOnTop(true);
            stage.setTitle("Remote Session Active");

            VBox root = new VBox(8);
            root.setPadding(new Insets(10, 15, 10, 15));
            root.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-border-color: #38bdf8; -fx-border-width: 2; -fx-border-radius: 8;");

            // Header Row: Active Dot + Status
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);

            Circle activeDot = new Circle(5, Color.web("#22c55e"));

            Label statusLabel = new Label("Controlled by: " + partnerId);
            statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            statusLabel.setTextFill(Color.WHITE);

            statusRow.getChildren().addAll(activeDot, statusLabel);

            // Timer Row
            Label durationLabel = new Label("Duration: 00:00:00");
            durationLabel.setFont(Font.font("Segoe UI", 12));
            durationLabel.setTextFill(Color.web("#94a3b8"));

            startTime = System.currentTimeMillis();
            timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                long hours = elapsedSeconds / 3600;
                long mins = (elapsedSeconds % 3600) / 60;
                long secs = elapsedSeconds % 60;
                durationLabel.setText(String.format("Duration: %02d:%02d:%02d", hours, mins, secs));
            }));
            timerTimeline.setCycleCount(Timeline.INDEFINITE);
            timerTimeline.play();

            // Button Row
            HBox btnRow = new HBox(10);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            Button chatBtn = new Button("Chat");
            chatBtn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            chatBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
            chatBtn.setOnAction(e -> {
                if (onToggleChat != null) onToggleChat.run();
            });

            Button disconnectBtn = new Button("Disconnect");
            disconnectBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            disconnectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 4 14; -fx-background-radius: 4; -fx-cursor: hand;");
            disconnectBtn.setOnAction(e -> {
                hide();
                if (onDisconnect != null) onDisconnect.run();
            });

            btnRow.getChildren().addAll(chatBtn, disconnectBtn);
            root.getChildren().addAll(statusRow, durationLabel, btnRow);

            Scene scene = new Scene(root, 280, 105);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(false);

            // Position at bottom-right of primary screen
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMaxX() - 300);
            stage.setY(screenBounds.getMaxY() - 125);

            stage.setOnCloseRequest(e -> {
                hide();
            });

            stage.show();
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            if (timerTimeline != null) {
                timerTimeline.stop();
                timerTimeline = null;
            }
            if (stage != null) {
                stage.close();
                stage = null;
            }
        });
    }
}

