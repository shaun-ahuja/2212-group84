package com.nudge;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.io.File;
import java.util.*;

public class starter {
    private List<Image> images;
    private ImageView selectedImageView;
    private TextField nameField;
    private String selectedPetType = "default"; // Will store the selected pet type

    public Scene createScene(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new javafx.geometry.Insets(20));

        // Create back button at the top
        Button backButton = new Button("← BACK");
        backButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");
        HBox backButtonBox = new HBox(backButton);
        backButtonBox.setPadding(new javafx.geometry.Insets(20));
        root.setTop(backButtonBox);

        // Create the main content area
        VBox contentBox = new VBox(40);
        contentBox.setAlignment(Pos.CENTER);

        // Create the selected pet display
        selectedImageView = new ImageView();
        selectedImageView.setFitWidth(200);
        selectedImageView.setFitHeight(200);
        selectedImageView.setPreserveRatio(true);

        // Load images from the images directory
        images = new ArrayList<>();
        File imageDir = new File("src/main/java/images");
        if (imageDir.exists() && imageDir.isDirectory()) {
            File[] files = imageDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".gif") ||
                            name.toLowerCase().endsWith(".jpg") ||
                            name.toLowerCase().endsWith(".webp")
            );
            if (files != null) {
                for (File file : files) {
                    images.add(new Image(file.toURI().toString()));
                }
            }
        }

        if (!images.isEmpty()) {
            selectedImageView.setImage(images.get(0));
            selectedPetType = "type1"; // Set default pet type
        }

        // Create name input field
        Label nameLabel = new Label("Name");
        nameLabel.setStyle("-fx-font-size: 18px;");
        nameField = new TextField();
        nameField.setStyle("-fx-font-size: 16px; -fx-padding: 5px; -fx-alignment: center;");
        nameField.setPrefWidth(200);
        nameField.setMaxWidth(200);
        nameField.setAlignment(javafx.geometry.Pos.CENTER);
        VBox nameBox = new VBox(10);
        nameBox.setAlignment(Pos.CENTER);
        nameBox.getChildren().addAll(nameLabel, nameField);

        // Create image selection area
        HBox imageSelectionBox = new HBox(20);
        imageSelectionBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < images.size(); i++) {
            ImageView petImage = new ImageView(images.get(i));
            petImage.setFitWidth(150);
            petImage.setFitHeight(150);
            petImage.setPreserveRatio(true);

            Rectangle frame = new Rectangle(160, 160);
            frame.setFill(Color.TRANSPARENT);
            frame.setStroke(Color.BLACK);
            frame.setStrokeWidth(2);

            javafx.scene.layout.StackPane imageStack = new javafx.scene.layout.StackPane();
            imageStack.getChildren().addAll(frame, petImage);
            
            final int index = i;
            final String petType = "type" + (i + 1); // Assign type based on index
            imageStack.setOnMouseClicked(e -> {
                selectedImageView.setImage(images.get(index));
                selectedPetType = petType;
            });

            imageSelectionBox.getChildren().add(imageStack);
        }

        // Create start game button
        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 30px;");

        // Add all components to the main content box
        contentBox.getChildren().addAll(selectedImageView, nameBox, imageSelectionBox, startButton);

        // Set up button actions
        backButton.setOnAction(e -> {
            Scene tutorialScene = new tutorial().createScene(primaryStage);
            primaryStage.setScene(tutorialScene);
        });

        startButton.setOnAction(e -> {
            String petName = nameField.getText().trim();
            if (petName.isEmpty()) {
                showError("Please enter a name for your pet");
                return;
            }

            // Generate a unique player ID
            String playerId = UUID.randomUUID().toString();
            
            // Create and switch to the game page
            GamePage gamePage = new GamePage();
            Scene gameScene = gamePage.createScene(primaryStage, playerId, petName, selectedPetType);
            if (gameScene != null) {
                primaryStage.setScene(gameScene);
            }
        });

        // Center the content in the BorderPane
        root.setCenter(contentBox);

        // Create the scene
        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("Choose Your Pet");

        return scene;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
