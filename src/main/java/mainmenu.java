package com.nudge;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class mainmenu extends Application {
    public static Scene createScene(Stage primaryStage) {
        // Create the main container
        BorderPane root = new BorderPane();

        // Create the title label
        Label title = new Label("Nudge");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 108));

        // Create a rectangle to surround the title
        Rectangle box = new Rectangle(500, 200);
        box.setFill(Color.TRANSPARENT);
        box.setStroke(Color.BLACK);
        box.setStrokeWidth(2);

        // Create a StackPane to overlay the title and rectangle
        StackPane titleStack = new StackPane();
        titleStack.getChildren().addAll(box, title);
        titleStack.setAlignment(Pos.CENTER);

        // Create buttons
        Button newButton = new Button("New");
        Button loadButton = new Button("Load");
        Button tutorialButton = new Button("Tutorial");
        Button parentalButton = new Button("Parental");
        Button exitButton = new Button("Exit");

        // Style the buttons
        newButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");
        loadButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");
        tutorialButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");
        parentalButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");
        exitButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");

        // Add functionality to the New button
        newButton.setOnAction(e -> {
            starter starterPage = new starter();
            Scene starterScene = starterPage.createScene(primaryStage);
            primaryStage.setScene(starterScene);
        });

        // Add tutorial button functionality
        tutorialButton.setOnAction(e -> {
            tutorial tutorialPage = new tutorial();
            Scene tutorialScene = tutorialPage.createScene(primaryStage);
            primaryStage.setScene(tutorialScene);
        });

        // Add exit functionality to the exit button
        exitButton.setOnAction(e -> {
            primaryStage.close();
            System.exit(0);
        });

        // Add functionality to the load button
        loadButton.setOnAction(e -> {
            Scene loadGameScene = new loadGame().createScene(primaryStage);
            primaryStage.setScene(loadGameScene);
        });

        // Add functionality to the parental button
        parentalButton.setOnAction(e -> {
            Scene passwordScene = new parentalPassword().createScene(primaryStage);
            primaryStage.setScene(passwordScene);
        });

        // Create HBox for buttons
        HBox buttonBox = new HBox(20); // 20 is the spacing between buttons
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(newButton, loadButton, tutorialButton, parentalButton, exitButton);

        // Create VBox to hold title and buttons
        VBox contentBox = new VBox(30); // 30 is the spacing between title and buttons
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(titleStack, buttonBox);

        // Center the content in the BorderPane
        root.setCenter(contentBox);

        // Create the scene
        Scene scene = new Scene(root, 1280, 800);

        // Set up the stage
        primaryStage.setTitle("Nudge");

        return scene;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setScene(createScene(primaryStage));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
