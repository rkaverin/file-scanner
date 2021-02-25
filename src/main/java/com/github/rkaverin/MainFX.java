package com.github.rkaverin;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class MainFX extends Application {

    public static void main(String[] args) {
        Application.launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("FileScanner v0.1");
        stage.setWidth(300);
        stage.setHeight(200);

        try (InputStream iconStream = getClass().getResourceAsStream("/app.png")) {
            stage.getIcons().add(new Image(iconStream));
        }

        Label helloWorldLabel = new Label("Hello world!");
        helloWorldLabel.setAlignment(Pos.CENTER);
        Scene primaryScene = new Scene(helloWorldLabel);
        stage.setScene(primaryScene);

        stage.show();
    }
}
