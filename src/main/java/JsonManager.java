package com.nudge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

public class JsonManager {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String DATA_DIR = "src/main/resources/data/";

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        new File(DATA_DIR).mkdirs();
    }

    // Player data methods
    public static void savePlayerData(Map<String, Object> playerData) throws IOException {
        String playerId = (String) playerData.get("player_id");
        File file = new File(DATA_DIR + "player_" + playerId + ".json");
        mapper.writeValue(file, playerData);
    }

    public static Map<String, Object> loadPlayerData(String playerId) throws IOException {
        File file = new File(DATA_DIR + "player_" + playerId + ".json");
        return mapper.readValue(file, Map.class);
    }

    // Pet data methods
    public static void savePetData(Map<String, Object> petData) throws IOException {
        String playerId = (String) petData.get("player_id");
        File file = new File(DATA_DIR + "pet_" + playerId + ".json");
        mapper.writeValue(file, petData);
    }

    public static Map<String, Object> loadPetData(String playerId) throws IOException {
        File file = new File(DATA_DIR + "pet_" + playerId + ".json");
        return mapper.readValue(file, Map.class);
    }

    // Task tracker methods
    public static void saveTaskLog(Map<String, Object> taskLog) throws IOException {
        String playerId = (String) taskLog.get("player_id");
        String date = (String) taskLog.get("date");
        File file = new File(DATA_DIR + "tasks_" + playerId + "_" + date + ".json");
        mapper.writeValue(file, taskLog);
    }

    public static Map<String, Object> loadTaskLog(String playerId, String date) throws IOException {
        File file = new File(DATA_DIR + "tasks_" + playerId + "_" + date + ".json");
        return mapper.readValue(file, Map.class);
    }

    // Parental controls methods
    public static void saveParentalControls(Map<String, Object> controls) throws IOException {
        String parentId = (String) controls.get("parent_id");
        File file = new File(DATA_DIR + "parental_" + parentId + ".json");
        mapper.writeValue(file, controls);
    }

    public static Map<String, Object> loadParentalControls(String parentId) throws IOException {
        File file = new File(DATA_DIR + "parental_" + parentId + ".json");
        return mapper.readValue(file, Map.class);
    }

    // Score tracking methods
    public static void saveScoreData(Map<String, Object> scoreData) throws IOException {
        String playerId = (String) scoreData.get("player_id");
        File file = new File(DATA_DIR + "score_" + playerId + ".json");
        mapper.writeValue(file, scoreData);
    }

    public static Map<String, Object> loadScoreData(String playerId) throws IOException {
        File file = new File(DATA_DIR + "score_" + playerId + ".json");
        return mapper.readValue(file, Map.class);
    }

    // Utility method to list all saved games
    public static List<Map<String, Object>> listSavedGames() {
        List<Map<String, Object>> savedGames = new ArrayList<>();
        File dir = new File(DATA_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith("player_") && name.endsWith(".json"));
        
        if (files != null) {
            for (File file : files) {
                try {
                    Map<String, Object> playerData = mapper.readValue(file, Map.class);
                    savedGames.add(playerData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Sort by creation date (oldest first)
        savedGames.sort((a, b) -> {
            LocalDateTime timeA = LocalDateTime.parse((String) a.get("creation_date"));
            LocalDateTime timeB = LocalDateTime.parse((String) b.get("creation_date"));
            return timeA.compareTo(timeB);
        });
        
        // Limit to 5 games
        return savedGames.size() > 5 ? savedGames.subList(0, 5) : savedGames;
    }

    // Create example data for testing
    public static void createExampleData() throws IOException {
        // Example player data
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("player_id", "123456");
        playerData.put("username", "PetLover99");
        playerData.put("account_type", "child");
        playerData.put("login_streak", 5);
        playerData.put("achievements", Arrays.asList("First Feed", "5-Day Streak"));
        Map<String, Object> settings = new HashMap<>();
        settings.put("sound_enabled", true);
        settings.put("parental_controls", true);
        playerData.put("settings", settings);
        playerData.put("creation_date", LocalDateTime.now().toString());
        savePlayerData(playerData);

        // Example pet data
        Map<String, Object> petData = new HashMap<>();
        petData.put("player_id", "123456");
        petData.put("pet_name", "Fluffy");
        petData.put("pet_type", "dog");
        petData.put("hunger_level", 40);
        petData.put("happiness_level", 85);
        petData.put("last_fed", LocalDateTime.now().toString());
        petData.put("last_groomed", LocalDateTime.now().toString());
        petData.put("inventory", Arrays.asList("Food", "Brush", "Toy"));
        savePetData(petData);

        // Example task log
        Map<String, Object> taskLog = new HashMap<>();
        taskLog.put("player_id", "123456");
        taskLog.put("date", "2025-03-08");
        List<Map<String, String>> tasks = new ArrayList<>();
        tasks.add(Map.of("task", "Fed Pet", "time", "08:00"));
        tasks.add(Map.of("task", "Played with Pet", "time", "12:30"));
        tasks.add(Map.of("task", "Groomed Pet", "time", "18:15"));
        taskLog.put("tasks_completed", tasks);
        saveTaskLog(taskLog);

        // Example parental controls
        Map<String, Object> parentalControls = new HashMap<>();
        parentalControls.put("parent_id", "789123");
        parentalControls.put("child_accounts", Arrays.asList("123456", "654321"));
        Map<String, Integer> playtimeLimit = new HashMap<>();
        playtimeLimit.put("daily_limit", 60);
        playtimeLimit.put("weekly_limit", 300);
        parentalControls.put("playtime_limit", playtimeLimit);
        Map<String, Object> restrictions = new HashMap<>();
        restrictions.put("restricted_hours", Arrays.asList("22:00-06:00"));
        parentalControls.put("restrictions", restrictions);
        saveParentalControls(parentalControls);

        // Example score data
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("player_id", "123456");
        scoreData.put("total_score", 850);
        List<Map<String, Object>> recentActions = new ArrayList<>();
        recentActions.add(Map.of(
            "action", "fed_pet",
            "points", 10,
            "timestamp", LocalDateTime.now().toString()
        ));
        recentActions.add(Map.of(
            "action", "groomed_pet",
            "points", 15,
            "timestamp", LocalDateTime.now().toString()
        ));
        recentActions.add(Map.of(
            "action", "played_with_pet",
            "points", 20,
            "timestamp", LocalDateTime.now().toString()
        ));
        scoreData.put("recent_actions", recentActions);
        saveScoreData(scoreData);
    }
} 