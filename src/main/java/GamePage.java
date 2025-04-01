package com.nudge;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class GamePage {
    private String playerId;
    private Map<String, Object> playerData;
    private Map<String, Object> petData;
    private Map<String, ProgressBar> vitalBars = new HashMap<>();
    private List<String> inventory;
    private String currentPetState = "normal";
    private ImageView petImageView;
    private MediaPlayer backgroundMusic;
    private static final double HEALTH_PENALTY = 20.0;
    private static final double HAPPINESS_THRESHOLD = 50.0;
    private static final double SLEEP_RECOVERY_RATE = 5.0;
    private static final double HUNGER_HEALTH_PENALTY = 2.0;
    private static final double HUNGER_HAPPINESS_PENALTY = 3.0;
    private static final long VET_COOLDOWN = 300000; // 5 minutes in milliseconds
    private static final long PLAY_COOLDOWN = 180000; // 3 minutes in milliseconds
    private static final long REWARD_INTERVAL = 300000; // 5 minutes in milliseconds
    private long lastRewardTime;
    private Map<String, Long> lastCommandTime = new HashMap<>();
    private long sessionStartTime;
    private long totalPlayTime;
    private Timeline timeTracker;
    
    // Expand food and gift properties with descriptions and icons
    private static final Map<String, ItemProperties> FOOD_ITEMS = Map.of(
        "Basic Food", new ItemProperties("🥫", 30, "Simple pet food that provides basic nutrition"),
        "Premium Food", new ItemProperties("🍖", 50, "High-quality food that satisfies hunger well"),
        "Deluxe Food", new ItemProperties("🍗", 80, "Gourmet food that fully satisfies your pet")
    );
    
    private static final Map<String, ItemProperties> GIFT_ITEMS = Map.of(
        "Basic Toy", new ItemProperties("🎾", 20, "A simple toy for basic play"),
        "Premium Toy", new ItemProperties("🧸", 35, "A quality toy that brings more joy"),
        "Deluxe Toy", new ItemProperties("🎮", 60, "A special toy that makes your pet very happy")
    );

    private static class ItemProperties {
        final String icon;
        final int value;
        final String description;

        ItemProperties(String icon, int value, String description) {
            this.icon = icon;
            this.value = value;
            this.description = description;
        }
    }

    public Scene createScene(Stage primaryStage, String playerId, String petName, String petType) {
        this.playerId = playerId;
        
        // Try to load existing game data first, create new game if it doesn't exist
        try {
            loadExistingGame(playerId);
        } catch (IOException e) {
            try {
                createNewGame(petName, petType);
            } catch (IOException ex) {
                ex.printStackTrace();
                showError("Failed to create/load game");
                return null;
            }
        }

        // Initialize and start background music
        initializeBackgroundMusic();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Left side - Stats and Vitals
        VBox statsBox = createStatsBox();
        root.setLeft(statsBox);

        // Right side - Game Functions
        VBox functionsBox = createFunctionsBox();
        root.setRight(functionsBox);

        // Center - Pet Display
        VBox petBox = createPetDisplay();
        root.setCenter(petBox);

        // Bottom - Inventory
        HBox bottomBar = createBottomBar(primaryStage);
        root.setBottom(bottomBar);

        // Create the scene
        Scene scene = new Scene(root, 1280, 800);
        
        // Add keyboard shortcuts for inventory
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DIGIT1: useInventoryItem(0); break;
                case DIGIT2: useInventoryItem(1); break;
                case DIGIT3: useInventoryItem(2); break;
                case DIGIT4: useInventoryItem(3); break;
                case DIGIT5: useInventoryItem(4); break;
                case DIGIT6: useInventoryItem(5); break;
                case DIGIT7: useInventoryItem(6); break;
                case DIGIT8: useInventoryItem(7); break;
                case DIGIT9: useInventoryItem(8); break;
                case DIGIT0: useInventoryItem(9); break;
                default: break;
            }
        });

        // Start the game update timer
        startGameLoop();

        return scene;
    }

    private void initializeBackgroundMusic() {
        try {
            // Load music file from resources
            String musicPath = getClass().getResource("/bgmusic.mp3").toURI().toString();
            Media media = new Media(musicPath);
            backgroundMusic = new MediaPlayer(media);
            backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE); // Loop indefinitely
            backgroundMusic.setVolume(0.5); // Set volume to 50%
            backgroundMusic.play();
        } catch (Exception e) {
            System.err.println("Error initializing background music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
        }
    }

    private VBox createStatsBox() {
        VBox statsBox = new VBox(10);
        statsBox.setPadding(new Insets(20));
        statsBox.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1px;");

        // Score
        Label scoreLabel = new Label("Score: " + petData.get("score"));
        scoreLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Vital bars
        String[] vitals = {"Health", "Sleep", "Fullness", "Happiness"};
        for (String vital : vitals) {
            VBox vitalBox = new VBox(5);
            Label vitalLabel = new Label(vital);
            ProgressBar vitalBar = new ProgressBar();
            vitalBar.setPrefWidth(200);
            double value = ((Number) petData.get(vital.toLowerCase() + "_level")).doubleValue() / 100.0;
            vitalBar.setProgress(value);
            updateVitalBarColor(vitalBar, value);
            vitalBars.put(vital.toLowerCase(), vitalBar);
            vitalBox.getChildren().addAll(vitalLabel, vitalBar);
            statsBox.getChildren().add(vitalBox);
        }

        statsBox.getChildren().add(0, scoreLabel);
        return statsBox;
    }

    private void updateVitalBarColor(ProgressBar bar, double value) {
        if (value < 0.25) {
            bar.setStyle("-fx-accent: #ff0000;"); // Red for warning
        } else {
            bar.setStyle("-fx-accent: #0000ff;"); // Blue for normal
        }
    }

    private void updateVitalBars() {
        // Update score label
        VBox statsBox = (VBox) ((BorderPane) petImageView.getScene().getRoot()).getLeft();
        Label scoreLabel = (Label) statsBox.getChildren().get(0); // Score label is first child
        scoreLabel.setText("Score: " + petData.get("score"));

        // Update vital bars
        for (Map.Entry<String, ProgressBar> entry : vitalBars.entrySet()) {
            double value = ((Number) petData.get(entry.getKey() + "_level")).doubleValue() / 100.0;
            entry.getValue().setProgress(value);
            updateVitalBarColor(entry.getValue(), value);
        }
    }

    private VBox createFunctionsBox() {
        VBox functionsBox = new VBox(10);
        functionsBox.setPadding(new Insets(20));
        functionsBox.setAlignment(Pos.TOP_RIGHT);

        // Create command buttons with tooltips
        Button sleepButton = createCommandButton("Go to Bed", "🛏️");
        Button feedButton = createCommandButton("Feed", "🍽️");
        Button giftButton = createCommandButton("Give Gift", "🎁");
        Button vetButton = createCommandButton("Vet", "🏥");
        Button playButton = createCommandButton("Play", "🎮");
        Button exerciseButton = createCommandButton("Exercise", "🏃");

        functionsBox.getChildren().addAll(
            sleepButton, feedButton, giftButton,
            vetButton, playButton, exerciseButton
        );

        updateCommandAvailability(functionsBox);
        return functionsBox;
    }

    private Button createCommandButton(String command, String icon) {
        Button button = new Button(icon + " " + command);
        button.setStyle("-fx-min-width: 120px; -fx-min-height: 40px; -fx-font-size: 14px;");
        
        // Add tooltip with cooldown info for relevant commands
        if (command.equals("Vet") || command.equals("Play")) {
            Tooltip tooltip = new Tooltip("Available");
            button.setTooltip(tooltip);
        }
        
        button.setOnAction(e -> handleCommand(command));
        return button;
    }

    private void handleCommand(String command) {
        if (!isCommandAvailable(command)) {
            return;
        }

        // Get current score
        int currentScore = ((Number) petData.get("score")).intValue();

        switch (command.toLowerCase()) {
            case "go to bed":
                currentPetState = "sleeping";
                petData.put("sleep_level", 0.0); // Start sleep recovery
                currentScore += 5; // Small bonus for helping pet sleep
                break;
                
            case "feed":
                showFoodSelection();
                break;
                
            case "give gift":
                showGiftSelection();
                break;
                
            case "vet":
                if (isOnCooldown("vet")) {
                    showCooldownMessage("Vet");
                    return;
                }
                petData.put("health_level", Math.min(100,
                    ((Number) petData.get("health_level")).doubleValue() + 40));
                lastCommandTime.put("vet", System.currentTimeMillis());
                currentScore -= 10; // Penalty for needing vet care
                break;
                
            case "play":
                if (isOnCooldown("play")) {
                    showCooldownMessage("Play");
                    return;
                }
                double newHappiness = Math.min(100,
                    ((Number) petData.get("happiness_level")).doubleValue() + 25);
                petData.put("happiness_level", newHappiness);
                lastCommandTime.put("play", System.currentTimeMillis());
                currentScore += 15; // Bonus for playing with pet
                if (newHappiness >= HAPPINESS_THRESHOLD && currentPetState.equals("angry")) {
                    currentPetState = "normal";
                }
                break;
                
            case "exercise":
                double currentSleep = ((Number) petData.get("sleep_level")).doubleValue();
                double currentFullness = ((Number) petData.get("fullness_level")).doubleValue();
                double currentHealth = ((Number) petData.get("health_level")).doubleValue();
                
                petData.put("sleep_level", Math.max(0, currentSleep - 10));
                petData.put("fullness_level", Math.max(0, currentFullness - 15));
                petData.put("health_level", Math.min(100, currentHealth + 15));
                currentScore += 10; // Bonus for exercising pet
                break;
        }
        
        // Update score in petData
        petData.put("score", currentScore);
        
        updateVitalBars();
        updatePetSprite();
        updateCommandAvailability((VBox) ((BorderPane) petImageView.getScene().getRoot()).getRight());
        saveGame();
    }

    private void showFoodSelection() {
        List<String> availableFood = inventory.stream()
            .filter(item -> item.contains("Food"))
            .distinct()
            .toList();

        if (availableFood.isEmpty()) {
            showError("No food items in inventory!");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableFood.get(0), availableFood);
        dialog.setTitle("Select Food");
        dialog.setHeaderText("Choose food to feed your pet");
        dialog.setContentText("Available food:");

        dialog.showAndWait().ifPresent(food -> {
            int increase = FOOD_ITEMS.get(food).value;
            double currentFullness = ((Number) petData.get("fullness_level")).doubleValue();
            petData.put("fullness_level", Math.min(100, currentFullness + increase));
            
            // Remove the used food item
            inventory.remove(food);
            petData.put("inventory", inventory);
            
            // Update score based on food type
            int currentScore = ((Number) petData.get("score")).intValue();
            switch (food) {
                case "Basic Food":
                    currentScore += 5;
                    break;
                case "Premium Food":
                    currentScore += 10;
                    break;
                case "Deluxe Food":
                    currentScore += 15;
                    break;
            }
            petData.put("score", currentScore);
            
            if (currentPetState.equals("hungry")) {
                currentPetState = "normal";
            }
            
            updateVitalBars();
            updatePetSprite();
            saveGame();
        });
    }

    private void showGiftSelection() {
        List<String> availableGifts = inventory.stream()
            .filter(item -> item.contains("Toy"))
            .distinct()
            .toList();

        if (availableGifts.isEmpty()) {
            showError("No gifts in inventory!");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableGifts.get(0), availableGifts);
        dialog.setTitle("Select Gift");
        dialog.setHeaderText("Choose a gift for your pet");
        dialog.setContentText("Available gifts:");

        dialog.showAndWait().ifPresent(gift -> {
            int increase = GIFT_ITEMS.get(gift).value;
            double currentHappiness = ((Number) petData.get("happiness_level")).doubleValue();
            petData.put("happiness_level", Math.min(100, currentHappiness + increase));
            
            // Remove the used gift
            inventory.remove(gift);
            petData.put("inventory", inventory);
            
            // Update score based on gift type
            int currentScore = ((Number) petData.get("score")).intValue();
            switch (gift) {
                case "Basic Toy":
                    currentScore += 8;
                    break;
                case "Premium Toy":
                    currentScore += 15;
                    break;
                case "Deluxe Toy":
                    currentScore += 25;
                    break;
            }
            petData.put("score", currentScore);
            
            if (currentHappiness >= HAPPINESS_THRESHOLD && currentPetState.equals("angry")) {
                currentPetState = "normal";
            }
            
            updateVitalBars();
            updatePetSprite();
            saveGame();
        });
    }

    private boolean isCommandAvailable(String command) {
        // Dead state: No commands
        if (currentPetState.equals("dead")) {
            showError("Your pet has passed away. Please start a new game or load a saved game.");
            return false;
        }

        // Sleeping state: No commands
        if (currentPetState.equals("sleeping") && !command.equalsIgnoreCase("go to bed")) {
            showError("Your pet is sleeping and cannot perform actions right now.");
            return false;
        }

        // Angry state: Only Give Gift and Play
        if (currentPetState.equals("angry") && 
            !command.equalsIgnoreCase("give gift") && 
            !command.equalsIgnoreCase("play")) {
            showError("Your pet is angry and will only respond to gifts or play!");
            return false;
        }

        // Hungry and Normal states: All commands available
        return true;
    }

    private void updateCommandAvailability(VBox functionsBox) {
        functionsBox.getChildren().forEach(node -> {
            if (node instanceof Button) {
                Button button = (Button) node;
                String command = button.getText().substring(2); // Remove emoji
                
                // Update button state based on availability
                button.setDisable(!isCommandAvailable(command));
                
                // Update cooldown tooltips
                if (command.equals("Vet") || command.equals("Play")) {
                    updateCooldownTooltip(button, command.toLowerCase());
                }
            }
        });
    }

    private boolean isOnCooldown(String command) {
        long lastUsed = lastCommandTime.getOrDefault(command, 0L);
        long cooldown = command.equals("vet") ? VET_COOLDOWN : PLAY_COOLDOWN;
        return System.currentTimeMillis() - lastUsed < cooldown;
    }

    private void updateCooldownTooltip(Button button, String command) {
        long lastUsed = lastCommandTime.getOrDefault(command, 0L);
        long cooldown = command.equals("vet") ? VET_COOLDOWN : PLAY_COOLDOWN;
        long timeLeft = (lastUsed + cooldown - System.currentTimeMillis()) / 1000;
        
        if (timeLeft > 0) {
            button.getTooltip().setText(String.format("Available in %d seconds", timeLeft));
        } else {
            button.getTooltip().setText("Available");
        }
    }

    private void showCooldownMessage(String command) {
        long lastUsed = lastCommandTime.get(command.toLowerCase());
        long cooldown = command.equals("Vet") ? VET_COOLDOWN : PLAY_COOLDOWN;
        long timeLeft = (lastUsed + cooldown - System.currentTimeMillis()) / 1000;
        
        showError(String.format("%s command is on cooldown. Available in %d seconds.", 
            command, timeLeft));
    }

    private VBox createPetDisplay() {
        VBox petBox = new VBox(20);
        petBox.setAlignment(Pos.CENTER);
        petBox.setStyle("-fx-background-color: #f0f0f0;"); // Light gray background

        // Create a container for the pet image
        StackPane petContainer = new StackPane();
        petContainer.setAlignment(Pos.CENTER);
        petContainer.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2px;");
        petContainer.setPrefSize(500, 500); // Larger container

        petImageView = new ImageView();
        updatePetSprite();

        // Set the size of the pet image
        petImageView.setFitWidth(400); // Larger image
        petImageView.setFitHeight(400);
        petImageView.setPreserveRatio(true);
        
        // Add ground line
        Region groundLine = new Region();
        groundLine.setPrefHeight(2);
        groundLine.setStyle("-fx-background-color: black;");
        groundLine.setPrefWidth(500);

        // Add all elements to the container
        petContainer.getChildren().addAll(petImageView, groundLine);
        petBox.getChildren().add(petContainer);
        
        // Add some padding around the container
        petBox.setPadding(new Insets(20));
        
        return petBox;
    }

    private void updatePetSprite() {
        try {
            String petType = (String) petData.get("pet_type");
            String statePrefix = currentPetState.equals("normal") ? "" : "_" + currentPetState;
            
            // Try different possible image paths
            String[] possiblePaths = {
                String.format("src/main/java/images/%s%s.png", petType, statePrefix),
                String.format("src/main/resources/images/%s%s.png", petType, statePrefix),
                String.format("images/%s%s.png", petType, statePrefix)
            };
            
            Image image = null;
            for (String path : possiblePaths) {
                File imageFile = new File(path);
                if (imageFile.exists()) {
                    image = new Image(imageFile.toURI().toString());
                    break;
                }
            }
            
            if (image == null) {
                // If no image found, create a placeholder
                Rectangle placeholder = new Rectangle(400, 400);
                placeholder.setFill(Color.LIGHTGRAY);
                placeholder.setStroke(Color.BLACK);
                placeholder.setStrokeWidth(2);
                
                // Create a text label for the pet type
                Label textLabel = new Label(petType);
                textLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                
                // Create a StackPane to hold both
                StackPane placeholderPane = new StackPane(placeholder, textLabel);
                image = placeholderPane.snapshot(null, null);
            }
            
            petImageView.setImage(image);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Create a placeholder if there's an error
            Rectangle placeholder = new Rectangle(400, 400);
            placeholder.setFill(Color.LIGHTGRAY);
            placeholder.setStroke(Color.BLACK);
            placeholder.setStrokeWidth(2);
            
            Label errorLabel = new Label("Error loading image");
            errorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            
            StackPane placeholderPane = new StackPane(placeholder, errorLabel);
            petImageView.setImage(placeholderPane.snapshot(null, null));
        }
    }

    private HBox createBottomBar(Stage primaryStage) {
        HBox bottomBar = new HBox(20);
        bottomBar.setPadding(new Insets(20));
        bottomBar.setAlignment(Pos.CENTER);

        // Exit button
        Button exitButton = new Button("exit");
        exitButton.setOnAction(e -> {
            stopBackgroundMusic(); // Stop music before exiting
            saveGame();
            Scene mainMenuScene = new mainmenu().createScene(primaryStage);
            primaryStage.setScene(mainMenuScene);
        });

        // Save button
        Button saveButton = new Button("Save Game");
        saveButton.setOnAction(e -> {
            saveGame();
            // Show confirmation dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Saved");
            alert.setHeaderText(null);
            alert.setContentText("Your game has been saved successfully!");
            alert.showAndWait();
        });

        // Inventory
        HBox inventoryBar = createInventoryBar();
        
        // Backpack button for full inventory
        Button backpackButton = new Button();
        backpackButton.setStyle("-fx-min-width: 60px; -fx-min-height: 60px;");
        backpackButton.setText("🎒");
        backpackButton.setOnAction(e -> showFullInventory());

        bottomBar.getChildren().addAll(exitButton, saveButton, inventoryBar, backpackButton);
        return bottomBar;
    }

    private HBox createInventoryBar() {
        HBox inventoryBar = new HBox(5);
        inventoryBar.setAlignment(Pos.CENTER);

        // Create inventory slots
        for (int i = 0; i < 5; i++) {
            StackPane slot = createInventorySlot(i);
            inventoryBar.getChildren().add(slot);
        }

        return inventoryBar;
    }

    private StackPane createInventorySlot(int index) {
        StackPane slot = new StackPane();
        slot.setPadding(new Insets(5));

        // Create background
        Rectangle background = new Rectangle(60, 60);
        background.setFill(Color.WHITE);
        background.setStroke(Color.BLACK);

        // Create item display
        VBox itemDisplay = new VBox(2);
        itemDisplay.setAlignment(Pos.CENTER);

        if (index < inventory.size()) {
            String item = inventory.get(index);
            ItemProperties props = FOOD_ITEMS.containsKey(item) ? 
                FOOD_ITEMS.get(item) : GIFT_ITEMS.get(item);

            // Item icon
            Label iconLabel = new Label(props.icon);
            iconLabel.setStyle("-fx-font-size: 20px;");

            // Item count
            long count = inventory.stream().filter(i -> i.equals(item)).count();
            Label countLabel = new Label("x" + count);
            countLabel.setStyle("-fx-font-size: 12px;");

            itemDisplay.getChildren().addAll(iconLabel, countLabel);

            // Add tooltip with item details
            Tooltip tooltip = new Tooltip(String.format("%s\n%s\nEffect: +%d %s",
                item, props.description, props.value,
                FOOD_ITEMS.containsKey(item) ? "Fullness" : "Happiness"));
            Tooltip.install(slot, tooltip);
        }

        // Add hotkey number
        Label hotkeyLabel = new Label(String.valueOf(index + 1));
        hotkeyLabel.setStyle("-fx-font-size: 10px;");
        StackPane.setAlignment(hotkeyLabel, Pos.TOP_LEFT);

        slot.getChildren().addAll(background, itemDisplay, hotkeyLabel);

        // Add click handler
        slot.setOnMouseClicked(e -> useInventoryItem(index));

        return slot;
    }

    private void createNewGame(String petName, String petType) throws IOException {
        // Create player data
        playerData = new HashMap<>();
        playerData.put("player_id", playerId);
        playerData.put("username", petName);
        playerData.put("account_type", "child");
        playerData.put("login_streak", 1);
        playerData.put("achievements", new ArrayList<>());
        playerData.put("creation_date", LocalDateTime.now().toString());
        Map<String, Boolean> settings = new HashMap<>();
        settings.put("sound_enabled", true);
        settings.put("parental_controls", false);
        playerData.put("settings", settings);
        
        // Create pet data
        petData = new HashMap<>();
        petData.put("player_id", playerId);
        petData.put("pet_name", petName);
        petData.put("pet_type", petType);
        petData.put("score", 0);
        petData.put("health_level", 100);
        petData.put("sleep_level", 100);
        petData.put("fullness_level", 100);
        petData.put("happiness_level", 100);
        petData.put("last_fed", LocalDateTime.now().toString());
        petData.put("last_slept", LocalDateTime.now().toString());
        inventory = new ArrayList<>();
        // Add starting items
        addItemToInventory("Basic Food", 3);
        addItemToInventory("Premium Food", 2);
        addItemToInventory("Basic Toy", 2);
        addItemToInventory("Premium Toy", 1);
        petData.put("inventory", inventory);
        
        // Initialize reward timer
        lastRewardTime = System.currentTimeMillis();
        petData.put("last_reward_time", lastRewardTime);

        // Initialize command cooldowns
        lastCommandTime.put("vet", 0L);
        lastCommandTime.put("play", 0L);

        // Initialize time tracking
        sessionStartTime = System.currentTimeMillis();
        totalPlayTime = 0;
        petData.put("total_play_time", totalPlayTime);
        petData.put("last_session_start", sessionStartTime);

        // Save initial data
        JsonManager.savePlayerData(playerData);
        JsonManager.savePetData(petData);
    }

    private void loadExistingGame(String playerId) throws IOException {
        // Load existing player and pet data
        playerData = JsonManager.loadPlayerData(playerId);
        petData = JsonManager.loadPetData(playerId);
        inventory = (List<String>) petData.get("inventory");
        
        if (playerData == null || petData == null) {
            throw new IOException("Failed to load game data");
        }
        lastRewardTime = ((Number) petData.getOrDefault("last_reward_time", 0L)).longValue();

        // Load time tracking data
        totalPlayTime = ((Number) petData.getOrDefault("total_play_time", 0L)).longValue();
        sessionStartTime = System.currentTimeMillis();
        petData.put("last_session_start", sessionStartTime);
    }

    private void startGameLoop() {
        // Start time tracking
        timeTracker = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                updateVitals();
                checkAndGiveReward();
                updateTimeTracking();
                saveGame();
            })
        );
        timeTracker.setCycleCount(Animation.INDEFINITE);
        timeTracker.play();
    }

    private void updateTimeTracking() {
        long currentTime = System.currentTimeMillis();
        long sessionTime = currentTime - sessionStartTime;
        totalPlayTime += sessionTime;
        sessionStartTime = currentTime;
        petData.put("total_play_time", totalPlayTime);
        petData.put("last_session_start", sessionStartTime);
    }

    private void updateVitals() {
        // Get current values
        double health = ((Number) petData.get("health_level")).doubleValue();
        double sleep = ((Number) petData.get("sleep_level")).doubleValue();
        double fullness = ((Number) petData.get("fullness_level")).doubleValue();
        double happiness = ((Number) petData.get("happiness_level")).doubleValue();

        // Handle sleep state
        if (currentPetState.equals("sleeping")) {
            sleep = Math.min(100, sleep + SLEEP_RECOVERY_RATE);
            if (sleep >= 100) {
                currentPetState = "normal";
                updatePetSprite();
            }
        } else {
            sleep = Math.max(0, sleep - 1);
            if (sleep <= 0 && !currentPetState.equals("dead")) {
                health = Math.max(0, health - HEALTH_PENALTY);
                currentPetState = "sleeping";
                updatePetSprite();
            }
        }

        // Handle hunger state
        if (fullness <= 0 && !currentPetState.equals("dead") && !currentPetState.equals("sleeping")) {
            health = Math.max(0, health - HUNGER_HEALTH_PENALTY);
            happiness = Math.max(0, happiness - HUNGER_HAPPINESS_PENALTY);
            currentPetState = "hungry";
            updatePetSprite();
        } else if (fullness > 0 && currentPetState.equals("hungry")) {
            currentPetState = "normal";
            updatePetSprite();
        }

        // Handle happiness state
        if (happiness <= 0 && !currentPetState.equals("dead") && !currentPetState.equals("sleeping")) {
            currentPetState = "angry";
            updatePetSprite();
        } else if (happiness >= HAPPINESS_THRESHOLD && currentPetState.equals("angry")) {
            currentPetState = "normal";
            updatePetSprite();
        }

        // Normal vital decreases if not sleeping
        if (!currentPetState.equals("sleeping")) {
            fullness = Math.max(0, fullness - 1);
            happiness = Math.max(0, happiness - (currentPetState.equals("hungry") ? 2 : 1));
        }

        // Check for death
        if (health <= 0 && !currentPetState.equals("dead")) {
            currentPetState = "dead";
            updatePetSprite();
            handleDeath();
        }

        // Update petData and progress bars
        petData.put("health_level", health);
        petData.put("sleep_level", sleep);
        petData.put("fullness_level", fullness);
        petData.put("happiness_level", happiness);
        updateVitalBars();
    }

    private void handleDeath() {
        // Disable all function buttons except new game and load game
        VBox functionsBox = (VBox) ((BorderPane) petImageView.getScene().getRoot()).getRight();
        functionsBox.getChildren().forEach(node -> {
            if (node instanceof Button) {
                Button button = (Button) node;
                String command = button.getText().substring(2); // Remove emoji
                if (!command.equals("New Game") && !command.equals("Load Game")) {
                    button.setDisable(true);
                }
            }
        });

        // Show death message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("Your pet has died!");
        alert.setContentText("You can start a new game or load a saved game.");
        alert.showAndWait();
    }

    private void useInventoryItem(int index) {
        // Check if pet is dead
        if (currentPetState.equals("dead")) {
            showError("Your pet has passed away. Please start a new game or load a saved game.");
            return;
        }

        // Check if pet is sleeping
        if (currentPetState.equals("sleeping")) {
            showError("Your pet is sleeping and cannot use items right now.");
            return;
        }

        // Check if pet is angry and item isn't a toy
        if (currentPetState.equals("angry") && (index >= inventory.size() || !inventory.get(index).equalsIgnoreCase("toy"))) {
            showError("Your pet is angry and will only accept toys!");
            return;
        }

        if (index < inventory.size()) {
            String item = inventory.get(index);
            switch (item.toLowerCase()) {
                case "food":
                    petData.put("fullness_level", 100);
                    if (currentPetState.equals("hungry")) {
                        currentPetState = "normal";
                    }
                    break;
                case "medicine":
                    petData.put("health_level", 100);
                    break;
                case "toy":
                    double newHappiness = Math.min(100, 
                        ((Number) petData.get("happiness_level")).doubleValue() + 30);
                    petData.put("happiness_level", newHappiness);
                    if (newHappiness >= HAPPINESS_THRESHOLD && currentPetState.equals("angry")) {
                        currentPetState = "normal";
                    }
                    break;
            }
            updateVitalBars();
            updatePetSprite();
            saveGame();
        }
    }

    private void showFullInventory() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Inventory");
        dialog.setHeaderText("Your Items");

        // Create a grid for the inventory display
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Create sections for food and gifts
        addInventorySection(grid, 0, "Food Items", FOOD_ITEMS);
        addInventorySection(grid, FOOD_ITEMS.size() + 2, "Gift Items", GIFT_ITEMS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void addInventorySection(GridPane grid, int startRow, String title, Map<String, ItemProperties> items) {
        // Add section title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(titleLabel, 0, startRow, 2, 1);

        // Add headers
        grid.add(new Label("Item"), 0, startRow + 1);
        grid.add(new Label("Count"), 1, startRow + 1);
        grid.add(new Label("Effect"), 2, startRow + 1);
        grid.add(new Label("Description"), 3, startRow + 1);

        // Add items
        int row = startRow + 2;
        for (Map.Entry<String, ItemProperties> entry : items.entrySet()) {
            String itemName = entry.getKey();
            ItemProperties props = entry.getValue();
            long count = inventory.stream().filter(item -> item.equals(itemName)).count();

            grid.add(new Label(props.icon + " " + itemName), 0, row);
            grid.add(new Label(String.valueOf(count)), 1, row);
            grid.add(new Label("+" + props.value + (title.contains("Food") ? " Fullness" : " Happiness")), 2, row);
            grid.add(new Label(props.description), 3, row);
            row++;
        }
    }

    private void checkAndGiveReward() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRewardTime >= REWARD_INTERVAL) {
            // Give random reward
            Random random = new Random();
            if (random.nextBoolean()) {
                // Give food item
                String[] foods = FOOD_ITEMS.keySet().toArray(new String[0]);
                addItemToInventory(foods[random.nextInt(foods.length)], 1);
            } else {
                // Give gift item
                String[] gifts = GIFT_ITEMS.keySet().toArray(new String[0]);
                addItemToInventory(gifts[random.nextInt(gifts.length)], 1);
            }

            // Update last reward time
            lastRewardTime = currentTime;
            petData.put("last_reward_time", lastRewardTime);
            saveGame();

            // Show reward notification
            showRewardNotification();

            // Update inventory display
            updateInventoryDisplay();
        }
    }

    private void addItemToInventory(String item, int count) {
        for (int i = 0; i < count; i++) {
            inventory.add(item);
        }
        petData.put("inventory", inventory);
    }

    private void showRewardNotification() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Item Received!");
        alert.setHeaderText(null);
        alert.setContentText("You've received a new item! Check your inventory.");
        
        // Show notification without blocking
        Platform.runLater(() -> {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            alert.show();
            // Auto-close after 3 seconds
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), evt -> alert.close()));
            timeline.play();
        });
    }

    private void updateInventoryDisplay() {
        HBox bottomBar = (HBox) ((BorderPane) petImageView.getScene().getRoot()).getBottom();
        // Find and update the inventory bar
        bottomBar.getChildren().stream()
            .filter(node -> node instanceof HBox)
            .map(node -> (HBox) node)
            .findFirst()
            .ifPresent(inventoryBar -> {
                int currentSlot = 0;
                for (Node node : inventoryBar.getChildren()) {
                    if (node instanceof StackPane && currentSlot < 5) {
                        StackPane slot = (StackPane) node;
                        slot.getChildren().clear();
                        slot.getChildren().addAll(createInventorySlot(currentSlot).getChildren());
                        currentSlot++;
                    }
                }
            });
    }

    private void saveGame() {
        try {
            // Update time tracking before saving
            updateTimeTracking();
            JsonManager.savePetData(petData);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to save game");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 