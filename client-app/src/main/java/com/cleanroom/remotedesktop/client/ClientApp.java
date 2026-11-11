package com.cleanroom.remotedesktop.client;

import com.cleanroom.remotedesktop.client.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow(primaryStage);
        mainWindow.initAndShow();
    }
}

