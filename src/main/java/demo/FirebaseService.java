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
                .setDatabaseUrl("https://gamescorees-default-rtdb.firebaseio.com/")
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            database = FirebaseDatabase.getInstance().getReference();
            initialized = true;
            System.out.println("Firebase initialized successfully");
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
        if (!initialized) {
            return CompletableFuture.completedFuture(null);
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        String playerId = currentPlayerId != null ? currentPlayerId : UUID.randomUUID().toString();
        
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("playerName", playerName);
        scoreData.put("score", score);
        scoreData.put("survivalTime", survivalTime);
        scoreData.put("timestamp", System.currentTimeMillis());
        
        database.child("scores").child(playerId).push().setValue(scoreData, (error, ref) -> {
            if (error != null) {
                System.err.println("Failed to save score: " + error.getMessage());
                future.completeExceptionally(error.toException());
            } else {
                System.out.println("Score saved successfully");
                future.complete(null);
            }
        });
        
        // Update player stats
        updatePlayerStats(playerId, playerName, score);
        
        return future;
    }
    
    /**
     * Update player statistics
     */
    private void updatePlayerStats(String playerId, String playerName, int score) {
        if (!initialized) return;
        
        DatabaseReference playerRef = database.child("players").child(playerId);
        
        playerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("name", playerName);
                stats.put("lastPlayed", System.currentTimeMillis());
                
                if (snapshot.exists()) {
                    int gamesPlayed = snapshot.child("gamesPlayed").getValue(Integer.class) != null ? 
                                     snapshot.child("gamesPlayed").getValue(Integer.class) : 0;
                    int highScore = snapshot.child("highScore").getValue(Integer.class) != null ? 
                                   snapshot.child("highScore").getValue(Integer.class) : 0;
                    
                    stats.put("gamesPlayed", gamesPlayed + 1);
                    stats.put("highScore", Math.max(highScore, score));
                } else {
                    stats.put("gamesPlayed", 1);
                    stats.put("highScore", score);
                }
                
                playerRef.updateChildren(stats, null);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Failed to update player stats: " + error.getMessage());
            }
        });
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
