package fr.onishintan.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class OniShinTanApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Oni Shin Tan");
        stage.setScene(new Scene(new Label("Oni Shin Tan"), 1280, 720));
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}