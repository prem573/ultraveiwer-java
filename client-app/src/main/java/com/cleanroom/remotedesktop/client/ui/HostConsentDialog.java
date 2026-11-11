package com.cleanroom.remotedesktop.client.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class HostConsentDialog {

    public static void show(Stage owner, String partnerId, Consumer<Boolean> callback) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null && owner.isShowing()) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Incoming Remote Control Request");
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setAlwaysOnTop(true);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1;");

            Label titleLabel = new Label("Incoming Connection Request");
            titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            titleLabel.setTextFill(Color.web("#212529"));

            Label infoLabel = new Label("Partner ID: " + partnerId + "\nis requesting permission to remotely view and control this computer.");
            infoLabel.setFont(Font.font("Segoe UI", 13));
            infoLabel.setWrapText(true);
            infoLabel.setAlignment(Pos.CENTER);

            Label timerLabel = new Label("Auto-reject in: 30s");
            timerLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            timerLabel.setTextFill(Color.web("#dc3545"));

            HBox btnBox = new HBox(15);
            btnBox.setAlignment(Pos.CENTER);

            Button allowBtn = new Button("Allow Session");
            allowBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            allowBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 4; -fx-cursor: hand;");

            Button denyBtn = new Button("Deny / Reject");
            denyBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            denyBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 4; -fx-cursor: hand;");

            btnBox.getChildren().addAll(allowBtn, denyBtn);
            root.getChildren().addAll(titleLabel, infoLabel, timerLabel, btnBox);

            AtomicBoolean responded = new AtomicBoolean(false);

            Timeline countdown = new Timeline();
            final int[] secondsLeft = {30};
            KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
                secondsLeft[0]--;
                timerLabel.setText("Auto-reject in: " + secondsLeft[0] + "s");
                if (secondsLeft[0] <= 0) {
                    countdown.stop();
                    if (responded.compareAndSet(false, true)) {
                        dialog.close();
                        callback.accept(false);
                    }
                }
            });
            countdown.getKeyFrames().add(frame);
            countdown.setCycleCount(30);
            countdown.play();

            allowBtn.setOnAction(e -> {
                countdown.stop();
                if (responded.compareAndSet(false, true)) {
                    dialog.close();
                    callback.accept(true);
                }
            });

            denyBtn.setOnAction(e -> {
                countdown.stop();
                if (responded.compareAndSet(false, true)) {
                    dialog.close();
                    callback.accept(false);
                }
            });

            dialog.setOnCloseRequest(e -> {
                countdown.stop();
                if (responded.compareAndSet(false, true)) {
                    callback.accept(false);
                }
            });

            Scene scene = new Scene(root, 380, 210);
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
        });
    }
}

