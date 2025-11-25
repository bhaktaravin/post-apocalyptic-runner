package demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainMenu {
    private Stage stage;
    private Scene menuScene;
    private VBox leaderboardBox;
    private String playerName = "Player";
    
    public MainMenu(Stage stage) {
        this.stage = stage;
        createMenuScene();
    }
    
    private void createMenuScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");
        
        // Title
        Label titleLabel = new Label("POST-APOCALYPTIC RUNNER");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.web("#e94560"));
        
        Label subtitleLabel = new Label("Survive the Wasteland");
        subtitleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        subtitleLabel.setTextFill(Color.web("#0f3460"));
        
        // Player name input
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER);
        Label nameLabel = new Label("Player Name:");
        nameLabel.setFont(Font.font("Arial", 16));
        nameLabel.setTextFill(Color.WHITE);
        
        TextField nameField = new TextField(playerName);
        nameField.setMaxWidth(200);
        nameField.setPromptText("Enter your name");
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                playerName = newVal;
            }
        });
        
        nameBox.getChildren().addAll(nameLabel, nameField);
        
        // Buttons
        Button playButton = createStyledButton("PLAY GAME", "#e94560");
        playButton.setOnAction(e -> startGame());
        
        Button leaderboardButton = createStyledButton("LEADERBOARD", "#0f3460");
        leaderboardButton.setOnAction(e -> showLeaderboard());
        
        Button settingsButton = createStyledButton("SETTINGS", "#0f3460");
        settingsButton.setOnAction(e -> showSettings());
        
        Button exitButton = createStyledButton("EXIT", "#533483");
        exitButton.setOnAction(e -> stage.close());
        
        // Firebase status
        Label firebaseStatus = new Label();
        firebaseStatus.setFont(Font.font("Arial", 12));
        if (FirebaseService.getInstance().isInitialized()) {
            firebaseStatus.setText("✓ Connected to Firebase");
            firebaseStatus.setTextFill(Color.LIGHTGREEN);
        } else {
            firebaseStatus.setText("✗ Firebase not connected (Playing offline)");
            firebaseStatus.setTextFill(Color.ORANGE);
        }
        
        // Leaderboard preview box (hidden initially)
        leaderboardBox = new VBox(10);
        leaderboardBox.setAlignment(Pos.CENTER);
        leaderboardBox.setVisible(false);
        leaderboardBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-padding: 20; -fx-background-radius: 10;");
        
        root.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            new Label(""), // Spacer
            nameBox,
            playButton,
            leaderboardButton,
            settingsButton,
            exitButton,
            new Label(""), // Spacer
            firebaseStatus,
            leaderboardBox
        );
        
        menuScene = new Scene(root, 1280, 720);
    }
    
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        button.setPrefWidth(300);
        button.setPrefHeight(50);
        button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-cursor: hand;"
        );
        
        // Hover effect
        button.setOnMouseEntered(e -> 
            button.setStyle(
                "-fx-background-color: derive(" + color + ", 20%);" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-cursor: hand;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            )
        );
        
        button.setOnMouseExited(e -> 
            button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-cursor: hand;"
            )
        );
        
        return button;
    }
    
    private void startGame() {
        FirebaseService.getInstance().setCurrentPlayer(playerName);
        new Game(stage, this, playerName);
    }
    
    private void showLeaderboard() {
        leaderboardBox.setVisible(!leaderboardBox.isVisible());
        
        if (leaderboardBox.isVisible()) {
            leaderboardBox.getChildren().clear();
            
            Label title = new Label("TOP SCORES");
            title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            title.setTextFill(Color.web("#e94560"));
            leaderboardBox.getChildren().add(title);
            
            if (FirebaseService.getInstance().isInitialized()) {
                Label loading = new Label("Loading...");
                loading.setTextFill(Color.WHITE);
                leaderboardBox.getChildren().add(loading);
                
                FirebaseService.getInstance().getTopScores(10).thenAccept(scores -> {
                    javafx.application.Platform.runLater(() -> {
                        leaderboardBox.getChildren().clear();
                        leaderboardBox.getChildren().add(title);
                        
                        if (scores.isEmpty()) {
                            Label noScores = new Label("No scores yet. Be the first!");
                            noScores.setTextFill(Color.WHITE);
                            leaderboardBox.getChildren().add(noScores);
                        } else {
                            int rank = 1;
                            for (FirebaseService.ScoreEntry entry : scores) {
                                Label scoreLabel = new Label(
                                    String.format("%d. %s - %d points", rank++, entry.playerName, entry.score)
                                );
                                scoreLabel.setFont(Font.font("Arial", 16));
                                scoreLabel.setTextFill(Color.WHITE);
                                leaderboardBox.getChildren().add(scoreLabel);
                            }
                        }
                    });
                });
            } else {
                Label offlineMsg = new Label("Leaderboard unavailable in offline mode");
                offlineMsg.setTextFill(Color.ORANGE);
                leaderboardBox.getChildren().add(offlineMsg);
            }
        }
    }
    
    private void showSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Game Settings");
        alert.setContentText(
            "Controls:\n" +
            "• A/D or ←/→ : Move\n" +
            "• SPACE/W/↑ : Jump\n" +
            "• SHIFT : Toggle Auto-Scroll\n" +
            "• R : Restart (when game over)\n\n" +
            "Objective:\n" +
            "Survive as long as possible and dodge enemies!"
        );
        alert.showAndWait();
    }
    
    public void show() {
        stage.setScene(menuScene);
        stage.setTitle("Post-Apocalyptic Runner - Main Menu");
    }
    
    public Scene getScene() {
        return menuScene;
    }
}
