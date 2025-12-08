package demo;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FirebaseService {
    private static FirebaseService instance;
    private DatabaseReference database;
    private boolean initialized = false;
    private String currentPlayerId;
    
    private FirebaseService() {}
    
    public static FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }
    
    /**
     * Initialize Firebase with service account key
     * You'll need to download your Firebase service account key JSON file
     * from Firebase Console > Project Settings > Service Accounts
     */
    public void initialize(String serviceAccountPath) {
        try {
            FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
            
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://gamescorees-default-rtdb.firebaseio.com")
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            database = FirebaseDatabase.getInstance().getReference();
            initialized = true;
            System.out.println("Firebase initialized successfully");
            System.out.println("Database URL: https://gamescorees-default-rtdb.firebaseio.com");
            
            // Test write to verify connection
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", "connection");
            testData.put("timestamp", System.currentTimeMillis());
            database.child("_test").setValue(testData, (error, ref) -> {
                if (error != null) {
                    System.err.println("Test write failed: " + error.getMessage());
                } else {
                    System.out.println("Test write successful - Firebase is working!");
                }
            });
            
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            System.err.println("Running in offline mode");
            initialized = false;
        }
    }
    
    /**
     * Simple initialization without service account (for development)
     * This won't actually connect to Firebase but allows the game to run
     */
    public void initializeMock() {
        System.out.println("Running in mock mode - no Firebase connection");
        initialized = false;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void setCurrentPlayer(String playerId) {
        this.currentPlayerId = playerId;
    }
    
    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
    
    /**
     * Save player score to Firebase
     */
    public CompletableFuture<Void> saveScore(String playerName, int score, long survivalTime) {
        System.out.println("saveScore called - initialized: " + initialized + ", playerName: " + playerName + ", score: " + score);
        
        if (!initialized) {
            System.err.println("Firebase not initialized - score not saved");
            return CompletableFuture.completedFuture(null);
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        String playerId = currentPlayerId != null ? currentPlayerId : UUID.randomUUID().toString();
        System.out.println("Saving score for player ID: " + playerId);
        
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("playerName", playerName);
        scoreData.put("score", score);
        scoreData.put("survivalTime", survivalTime);
        scoreData.put("timestamp", System.currentTimeMillis());
        
        System.out.println("Attempting to write to Firebase using REST API");
        
        // Use REST API as fallback since Admin SDK async callbacks aren't reliable
        try {
            String scoreId = UUID.randomUUID().toString();
            String url = "https://gamescorees-default-rtdb.firebaseio.com/scores/" + playerId + "/" + scoreId + ".json";
            
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String jsonBody = String.format(
                "{\"playerName\":\"%s\",\"score\":%d,\"survivalTime\":%d,\"timestamp\":%d}",
                playerName, score, survivalTime, System.currentTimeMillis()
            );
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
            
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("========================================");
                        System.out.println("SUCCESS: Score saved to Firebase via REST!");
                        System.out.println("Player ID: " + playerId);
                        System.out.println("Score: " + score);
                        System.out.println("========================================");
                        future.complete(null);
                    } else {
                        System.err.println("Failed to save score. Status: " + response.statusCode());
                        System.err.println("Response: " + response.body());
                        future.completeExceptionally(new Exception("HTTP " + response.statusCode()));
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Exception saving score: " + ex.getMessage());
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                    return null;
                });
            
            // Update player stats via REST too
            updatePlayerStatsREST(playerId, playerName, score);
            
        } catch (Exception e) {
            System.err.println("Exception during REST API call: " + e.getMessage());
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Update player statistics using REST API
     */
    private void updatePlayerStatsREST(String playerId, String playerName, int score) {
        try {
            // First, get existing player data
            String getUrl = "https://gamescorees-default-rtdb.firebaseio.com/players/" + playerId + ".json";
            
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest getRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(getUrl))
                .GET()
                .build();
            
            client.sendAsync(getRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        int gamesPlayed = 1;
                        int highScore = score;
                        
                        if (response.statusCode() == 200 && !response.body().equals("null")) {
                            // Parse existing data (simple JSON parsing)
                            String body = response.body();
                            if (body.contains("gamesPlayed")) {
                                String gp = body.substring(body.indexOf("gamesPlayed") + 13);
                                gp = gp.substring(0, gp.indexOf(",") > 0 ? gp.indexOf(",") : gp.indexOf("}"));
                                gamesPlayed = Integer.parseInt(gp.trim()) + 1;
                            }
                            if (body.contains("highScore")) {
                                String hs = body.substring(body.indexOf("highScore") + 11);
                                hs = hs.substring(0, hs.indexOf(",") > 0 ? hs.indexOf(",") : hs.indexOf("}"));
                                highScore = Math.max(Integer.parseInt(hs.trim()), score);
                            }
                        }
                        
                        // Update player stats
                        String putUrl = "https://gamescorees-default-rtdb.firebaseio.com/players/" + playerId + ".json";
                        String jsonBody = String.format(
                            "{\"name\":\"%s\",\"gamesPlayed\":%d,\"highScore\":%d,\"lastPlayed\":%d}",
                            playerName, gamesPlayed, highScore, System.currentTimeMillis()
                        );
                        
                        java.net.http.HttpRequest putRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(putUrl))
                            .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                            .header("Content-Type", "application/json")
                            .build();
                        
                        client.sendAsync(putRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
                            .thenAccept(r -> System.out.println("Player stats updated successfully"));
                        
                    } catch (Exception e) {
                        System.err.println("Error parsing player data: " + e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            System.err.println("Failed to update player stats: " + e.getMessage());
        }
    }
    
    /**
     * Get top scores (leaderboard)
     */
    public CompletableFuture<List<ScoreEntry>> getTopScores(int limit) {
        CompletableFuture<List<ScoreEntry>> future = new CompletableFuture<>();
        
        if (!initialized) {
            future.complete(new ArrayList<>());
            return future;
        }
        
        database.child("scores")
            .orderByChild("score")
            .limitToLast(limit)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<ScoreEntry> scores = new ArrayList<>();
                    for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                        for (DataSnapshot scoreSnapshot : playerSnapshot.getChildren()) {
                            String name = scoreSnapshot.child("playerName").getValue(String.class);
                            Integer score = scoreSnapshot.child("score").getValue(Integer.class);
                            Long timestamp = scoreSnapshot.child("timestamp").getValue(Long.class);
                            
                            if (name != null && score != null) {
                                scores.add(new ScoreEntry(name, score, timestamp != null ? timestamp : 0));
                            }
                        }
                    }
                    
                    // Sort descending
                    scores.sort((a, b) -> Integer.compare(b.score, a.score));
                    future.complete(scores);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Failed to get top scores: " + error.getMessage());
                    future.complete(new ArrayList<>());
                }
            });
        
        return future;
    }
    
    /**
     * Score entry for leaderboard
     */
    public static class ScoreEntry {
        public String playerName;
        public int score;
        public long timestamp;
        
        public ScoreEntry(String playerName, int score, long timestamp) {
            this.playerName = playerName;
            this.score = score;
            this.timestamp = timestamp;
        }
    }
}
