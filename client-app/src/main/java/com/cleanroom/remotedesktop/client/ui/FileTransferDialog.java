package com.cleanroom.remotedesktop.client.ui;

import com.cleanroom.remotedesktop.client.file.FileTransferService;
import com.cleanroom.remotedesktop.client.viewer.ViewerSession;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.FileItem;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.FileListResp;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileTransferDialog {
    private final ViewerSession viewerSession;
    private final FileTransferService localFileService = new FileTransferService();

    private Stage stage;
    private TableView<FileItem> localTable;
    private TableView<FileItem> remoteTable;
    private TextField localPathField;
    private TextField remotePathField;
    private ProgressBar progressBar;
    private Label statusLabel;

    private String currentLocalPath;
    private String currentRemotePath;

    public FileTransferDialog(ViewerSession viewerSession) {
        this.viewerSession = viewerSession;
    }

    public void show() {
        Platform.runLater(() -> {
            if (stage != null && stage.isShowing()) {
                stage.toFront();
                return;
            }

            stage = new Stage();
            stage.setTitle("File Transfer - Clean-Room Remote Desktop");

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));

            // Top Header
            Label title = new Label("File Transfer Manager");
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
            root.setTop(title);

            // Center: Dual Pane Split
            SplitPane splitPane = new SplitPane();

            // Local Pane
            VBox localBox = createSideBox("Local Computer", true);
            // Remote Pane
            VBox remoteBox = createSideBox("Remote Partner", false);

            splitPane.getItems().addAll(localBox, remoteBox);
            splitPane.setDividerPositions(0.5);
            root.setCenter(splitPane);

            // Bottom Actions & Progress
            VBox bottomBox = new VBox(8);
            bottomBox.setPadding(new Insets(10, 0, 0, 0));

            HBox actionRow = new HBox(15);
            actionRow.setAlignment(Pos.CENTER);

            Button uploadBtn = new Button("Upload to Remote ->");
            uploadBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");
            uploadBtn.setOnAction(e -> handleUpload());

            Button refreshBtn = new Button("Refresh Both");
            refreshBtn.setOnAction(e -> {
                refreshLocal(currentLocalPath);
                refreshRemote(currentRemotePath);
            });

            actionRow.getChildren().addAll(uploadBtn, refreshBtn);

            progressBar = new ProgressBar(0);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setVisible(false);

            statusLabel = new Label("Ready");
            statusLabel.setFont(Font.font("Segoe UI", 12));

            bottomBox.getChildren().addAll(actionRow, progressBar, statusLabel);
            root.setBottom(bottomBox);

            // Listener for remote file responses
            viewerSession.setOnFileListReceived(this::onRemoteFileListReceived);

            // Load initial paths
            refreshLocal(System.getProperty("user.home"));
            refreshRemote("");

            Scene scene = new Scene(root, 780, 500);
            stage.setScene(scene);
            stage.show();
        });
    }

    private VBox createSideBox(String titleText, boolean isLocal) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(6));

        Label label = new Label(titleText);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        HBox pathBar = new HBox(6);
        TextField pathField = new TextField();
        pathField.setEditable(false);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button upBtn = new Button("Up");
        pathBar.getChildren().addAll(pathField, upBtn);

        TableView<FileItem> table = new TableView<>();
        table.setPlaceholder(new Label("No files"));

        TableColumn<FileItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(
                (data.getValue().isDirectory() ? "[Dir] " : "") + data.getValue().getName()
        ));
        nameCol.setPrefWidth(200);

        TableColumn<FileItem, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isDirectory() ? "" : formatSize(data.getValue().getSize())
        ));
        sizeCol.setPrefWidth(80);

        TableColumn<FileItem, String> dateCol = new TableColumn<>("Modified");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLastModified() > 0 ? sdf.format(new Date(data.getValue().getLastModified())) : ""
        ));
        dateCol.setPrefWidth(120);

        table.getColumns().addAll(nameCol, sizeCol, dateCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        if (isLocal) {
            localPathField = pathField;
            localTable = table;
            upBtn.setOnAction(e -> {
                if (currentLocalPath != null) {
                    Path parent = Paths.get(currentLocalPath).getParent();
                    if (parent != null) refreshLocal(parent.toString());
                }
            });
            table.setRowFactory(tv -> {
                TableRow<FileItem> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        FileItem item = row.getItem();
                        if (item.isDirectory()) {
                            Path next = Paths.get(currentLocalPath, item.getName());
                            refreshLocal(next.toString());
                        }
                    }
                });
                return row;
            });
        } else {
            remotePathField = pathField;
            remoteTable = table;
            upBtn.setOnAction(e -> {
                if (currentRemotePath != null) {
                    Path parent = Paths.get(currentRemotePath).getParent();
                    if (parent != null) refreshRemote(parent.toString());
                }
            });
            table.setRowFactory(tv -> {
                TableRow<FileItem> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        FileItem item = row.getItem();
                        if (item.isDirectory()) {
                            String next = currentRemotePath.endsWith(File.separator)
                                    ? currentRemotePath + item.getName()
                                    : currentRemotePath + File.separator + item.getName();
                            refreshRemote(next);
                        }
                    }
                });
                return row;
            });
        }

        box.getChildren().addAll(label, pathBar, table);
        return box;
    }

    private void refreshLocal(String pathStr) {
        FileListResp resp = localFileService.listFiles(pathStr);
        currentLocalPath = resp.getPath();
        Platform.runLater(() -> {
            localPathField.setText(currentLocalPath);
            localTable.setItems(FXCollections.observableArrayList(resp.getItems()));
        });
    }

    private void refreshRemote(String pathStr) {
        viewerSession.requestRemoteFileList(pathStr);
    }

    private void onRemoteFileListReceived(FileListResp resp) {
        currentRemotePath = resp.getPath();
        Platform.runLater(() -> {
            remotePathField.setText(currentRemotePath);
            remoteTable.setItems(FXCollections.observableArrayList(resp.getItems()));
        });
    }

    private void handleUpload() {
        FileItem selected = localTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) {
            statusLabel.setText("Please select a file on the local machine to upload.");
            return;
        }

        File localFile = Paths.get(currentLocalPath, selected.getName()).toFile();
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setText("Uploading " + selected.getName() + "...");

        viewerSession.sendFileToRemote(localFile, currentRemotePath, progress -> {
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                if (progress >= 1.0) {
                    statusLabel.setText("Upload complete: " + selected.getName());
                    refreshRemote(currentRemotePath);
                }
            });
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}

