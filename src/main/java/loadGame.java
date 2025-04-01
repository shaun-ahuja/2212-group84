package com.nudge;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.File;
import java.util.*;
import java.io.IOException;

public class loadGame {
    private List<Map<String, Object>> savedGames;

    public Scene createScene(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Create back button at the top
        Button backButton = new Button("← BACK");
        backButton.setStyle("-fx-font-size: 18px; -fx-padding: 10px 20px;");
        HBox backButtonBox = new HBox(backButton);
        root.setTop(backButtonBox);

        // Create the main content area
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setPadding(new Insets(20));

        // Create the scrollable container for saved games
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: white; -fx-border-color: black;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(600);

        VBox savedGamesBox = new VBox(10);
        savedGamesBox.setPadding(new Insets(10));

        // Load saved games from JSON files
        savedGames = JsonManager.listSavedGames();

        // Create rows for each saved game
        int gameNumber = 1;
        for (Map<String, Object> game : savedGames) {
            HBox gameRow = new HBox(20);
            gameRow.setAlignment(Pos.CENTER_LEFT);
            gameRow.setPadding(new Insets(10));
            gameRow.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

            // Number label
            Label numberLabel = new Label(gameNumber + ".");
            numberLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
            numberLabel.setMinWidth(30);

            // Load pet image if available
            ImageView petImage;
            try {
                Map<String, Object> petData = JsonManager.loadPetData((String) game.get("player_id"));
                String petType = (String) petData.get("pet_type");
                // TODO: Load actual pet image based on type
                petImage = new ImageView(new Rectangle(50, 50).snapshot(null, null));
            } catch (Exception e) {
                petImage = new ImageView(new Rectangle(50, 50).snapshot(null, null));
            }
            petImage.setFitWidth(50);
            petImage.setFitHeight(50);

            // Name label
            Label nameLabel = new Label((String) game.get("username"));
            nameLabel.setFont(Font.font("System", 16));
            nameLabel.setMinWidth(200);

            // Score label
            Label scoreLabel = new Label("Score: " + getPlayerScore((String) game.get("player_id")));
            scoreLabel.setFont(Font.font("System", 16));
            scoreLabel.setMinWidth(100);

            // Delete button
            Button deleteButton = new Button("🗑️");
            deleteButton.setStyle("-fx-font-size: 16px; -fx-padding: 5px 10px;");
            deleteButton.setOnAction(e -> {
                String playerId = (String) game.get("player_id");
                showDeleteConfirmation(primaryStage, playerId, gameRow);
            });

            // Add all elements to the row
            gameRow.getChildren().addAll(numberLabel, petImage, nameLabel, scoreLabel, deleteButton);

            // Make the row clickable (except for the delete button)
            final String playerId = (String) game.get("player_id");
            gameRow.setOnMouseClicked(e -> {
                if (!(e.getTarget() instanceof Button)) {
                    loadSavedGame(primaryStage, playerId);
                }
            });

            // Add hover effect
            gameRow.setOnMouseEntered(e -> gameRow
                    .setStyle("-fx-border-color: black; -fx-border-width: 1px; -fx-background-color: #f0f0f0;"));
            gameRow.setOnMouseExited(e -> gameRow
                    .setStyle("-fx-border-color: black; -fx-border-width: 1px; -fx-background-color: transparent;"));

            savedGamesBox.getChildren().add(gameRow);
            gameNumber++;
        }

        scrollPane.setContent(savedGamesBox);
        contentBox.getChildren().add(scrollPane);

        // Set up back button action
        backButton.setOnAction(e -> {
            Scene mainMenuScene = new mainmenu().createScene(primaryStage);
            primaryStage.setScene(mainMenuScene);
        });

        root.setCenter(contentBox);

        Scene scene = new Scene(root, 1280, 800);
        return scene;
    }

    private void showDeleteConfirmation(Stage primaryStage, String playerId, HBox gameRow) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Game");
        alert.setHeaderText("Are you sure you want to delete this game?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Delete all associated files
                    deleteGameFiles(playerId);

                    // Remove the row from the UI
                    ((VBox) gameRow.getParent()).getChildren().remove(gameRow);

                    // Show success message
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Game deleted successfully!");
                    successAlert.showAndWait();
                } catch (IOException e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Failed to delete game. Please try again.");
                    errorAlert.showAndWait();
                }
            }
        });
    }

    private void deleteGameFiles(String playerId) throws IOException {
        // Delete player data file
        File playerFile = new File("src/main/resources/data/player_" + playerId + ".json");
        if (playerFile.exists()) {
            playerFile.delete();
        }

        // Delete pet data file
        File petFile = new File("src/main/resources/data/pet_" + playerId + ".json");
        if (petFile.exists()) {
            petFile.delete();
        }

        // Delete score data file
        File scoreFile = new File("src/main/resources/data/score_" + playerId + ".json");
        if (scoreFile.exists()) {
            scoreFile.delete();
        }

        // Delete any task log files
        File dataDir = new File("src/main/resources/data/");
        File[] taskFiles = dataDir.listFiles((dir, name) -> name.startsWith("tasks_" + playerId + "_"));
        if (taskFiles != null) {
            for (File file : taskFiles) {
                file.delete();
            }
        }
    }

    private String getPlayerScore(String playerId) {
        try {
            Map<String, Object> scoreData = JsonManager.loadScoreData(playerId);
            return String.valueOf(scoreData.get("total_score"));
        } catch (Exception e) {
            return "0";
        }
    }

    private void loadSavedGame(Stage primaryStage, String playerId) {
        try {
            // Load the player and pet data
            Map<String, Object> playerData = JsonManager.loadPlayerData(playerId);
            Map<String, Object> petData = JsonManager.loadPetData(playerId);

            // Get the pet name and type from the loaded data
            String petName = (String) petData.get("pet_name");
            String petType = (String) petData.get("pet_type");

            // Create and switch to the game page with loaded data
            GamePage gamePage = new GamePage();
            Scene gameScene = gamePage.createScene(primaryStage, playerId, petName, petType);
            if (gameScene != null) {
                primaryStage.setScene(gameScene);
            } else {
                throw new IOException("Failed to create game scene");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load game");
            alert.setContentText("Could not load the selected game. Please try again.");
            alert.showAndWait();
        }
    }
}