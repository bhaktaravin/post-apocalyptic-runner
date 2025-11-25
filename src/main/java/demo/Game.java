package demo;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Game {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    
    private Canvas canvas;
    private GraphicsContext gc;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    private boolean jumpRequested = false;
    
    // Game state
    private double playerX = 100;
    private double playerY = 500;
    private double playerSpeed = 3;
    private double scrollSpeed = 0.5;
    private double velocityY = 0;
    private double gravity = 0.2;
    private double jumpStrength = -10;
    private double terminalVelocity = 12;
    private boolean isOnGround = false;
    private double groundLevel = 550;
    private boolean canAutoScroll = true;
    
    // Health and combat
    private int maxHealth = 100;
    private int currentHealth = 100;
    private boolean isInvulnerable = false;
    private long invulnerabilityTimer = 0;
    private long invulnerabilityDuration = 2_000_000_000L; // 2 seconds in nanoseconds
    
    // Enemies
    private List<Enemy> enemies = new ArrayList<>();
    private Random random = new Random();
    private long lastEnemySpawn = 0;
    private long enemySpawnInterval = 3_000_000_000L; // 3 seconds
    
    // Score
    private int score = 0;
    private long gameStartTime = 0;
    
    // Game state
    private boolean gameOver = false;
    private MainMenu menu;
    private String playerName;
    
    // Background layers for parallax effect
    private Image bgClouds1;
    private Image bgClouds2;
    private Image bgHousesBg;
    private Image bgGroundHouses;
    private Image bgRoad;
    private Image bgFence;
    
    private double clouds1X = 0;
    private double clouds2X = 0;
    private double housesBgX = 0;
    private double groundHousesX = 0;
    private double roadX = 0;
    private double fenceX = 0;
    
    public Game(Stage stage, MainMenu menu, String playerName) {
        this.menu = menu;
        this.playerName = playerName;
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        // Load background assets
        loadAssets();
        
        // Input handling
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            if ((e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.W) && !jumpRequested) {
                jumpRequested = true;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                returnToMenu();
            }
        });
        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP || e.getCode() == KeyCode.W) {
                jumpRequested = false;
            }
        });
        
        stage.setTitle("Post-Apocalyptic Game");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        
        gameStartTime = System.nanoTime();
        
        // Start game loop
        startGameLoop();
    }
    
    private void loadAssets() {
        String basePath = "/craftpix-901125-free-post-apocalyptic-pixel-art-game-backgrounds/PNG/Postapocalypce1/Bright/";
        try {
            bgClouds1 = new Image(getClass().getResourceAsStream(basePath + "clouds1.png"));
            bgClouds2 = new Image(getClass().getResourceAsStream(basePath + "clouds2.png"));
            bgHousesBg = new Image(getClass().getResourceAsStream(basePath + "ground&houses_bg.png"));
            bgGroundHouses = new Image(getClass().getResourceAsStream(basePath + "ground&houses.png"));
            bgRoad = new Image(getClass().getResourceAsStream(basePath + "road.png"));
            bgFence = new Image(getClass().getResourceAsStream(basePath + "fence.png"));
        } catch (Exception e) {
            System.err.println("Error loading assets: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startGameLoop() {
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                render();
            }
        };
        gameLoop.start();
    }
    
    private void update(long now) {
        if (gameOver) {
            // Check for restart
            if (pressedKeys.contains(KeyCode.R)) {
                restartGame();
            }
            return;
        }
        
        // Handle horizontal movement
        if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) {
            playerX -= playerSpeed;
        }
        if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) {
            playerX += playerSpeed;
        }
        
        // Jumping
        if (jumpRequested && isOnGround) {
            velocityY = jumpStrength;
            isOnGround = false;
            jumpRequested = false; // Consume the jump
        }
        
        // Apply gravity
        velocityY += gravity;
        
        // Cap falling speed (terminal velocity)
        if (velocityY > terminalVelocity) {
            velocityY = terminalVelocity;
        }
        
        playerY += velocityY;
        
        // Ground collision
        if (playerY >= groundLevel) {
            playerY = groundLevel;
            velocityY = 0;
            isOnGround = true;
        }
        
        // Keep player in horizontal bounds
        playerX = Math.max(0, Math.min(WIDTH - 50, playerX));
        
        // Toggle auto-scroll with SHIFT
        if (pressedKeys.contains(KeyCode.SHIFT)) {
            canAutoScroll = !canAutoScroll;
            pressedKeys.remove(KeyCode.SHIFT); // Prevent rapid toggling
        }
        
        // Update invulnerability
        if (isInvulnerable && now - invulnerabilityTimer > invulnerabilityDuration) {
            isInvulnerable = false;
        }
        
        // Spawn enemies
        if (now - lastEnemySpawn > enemySpawnInterval) {
            spawnEnemy();
            lastEnemySpawn = now;
        }
        
        // Update enemies
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.update();
            
            // Check collision with player
            if (!isInvulnerable && enemy.collidesWith(playerX, playerY, 50, 50)) {
                takeDamage(20);
                iterator.remove();
            }
            
            // Remove inactive enemies
            if (!enemy.isActive()) {
                iterator.remove();
                score += 10; // Points for dodging
            }
        }
        
        // Update parallax scrolling (right to left)
        if (canAutoScroll) {
            clouds1X -= scrollSpeed * 0.2;
            clouds2X -= scrollSpeed * 0.3;
            housesBgX -= scrollSpeed * 0.5;
            groundHousesX -= scrollSpeed * 0.8;
            roadX -= scrollSpeed * 1.2;
            fenceX -= scrollSpeed * 1.5;
        }
        
        // Wrap backgrounds
        if (bgClouds1 != null) {
            double cloudWidth = bgClouds1.getWidth();
            if (clouds1X <= -cloudWidth) clouds1X += cloudWidth;
            if (clouds2X <= -cloudWidth) clouds2X += cloudWidth;
        }
        if (bgRoad != null) {
            double roadWidth = bgRoad.getWidth();
            if (roadX <= -roadWidth) roadX += roadWidth;
            if (fenceX <= -roadWidth) fenceX += roadWidth;
            if (groundHousesX <= -roadWidth) groundHousesX += roadWidth;
            if (housesBgX <= -roadWidth) housesBgX += roadWidth;
        }
        
        // Increase score over time
        score++;
    }
    
    private void render() {
        // Clear screen
        gc.setFill(Color.rgb(135, 206, 235)); // Sky blue
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw parallax layers (back to front)
        drawScrollingBackground(bgClouds1, clouds1X);
        drawScrollingBackground(bgClouds2, clouds2X);
        drawScrollingBackground(bgHousesBg, housesBgX);
        drawScrollingBackground(bgGroundHouses, groundHousesX);
        drawScrollingBackground(bgRoad, roadX);
        drawScrollingBackground(bgFence, fenceX);
        
        // Draw enemies
        for (Enemy enemy : enemies) {
            enemy.render(gc);
        }
        
        // Draw player (with invulnerability flash)
        if (!isInvulnerable || (System.nanoTime() / 100_000_000) % 2 == 0) {
            gc.setFill(Color.BLUE);
            gc.fillRect(playerX, playerY, 50, 50);
            // Player eyes
            gc.setFill(Color.WHITE);
            gc.fillOval(playerX + 10, playerY + 15, 10, 10);
            gc.fillOval(playerX + 30, playerY + 15, 10, 10);
        }
        
        // Draw health bar
        drawHealthBar();
        
        // Draw score
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.fillText("Score: " + score, WIDTH - 150, 30);
        gc.fillText("Enemies: " + enemies.size(), WIDTH - 150, 55);
        
        // Draw controls
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        gc.fillText("A/D: Move | SPACE: Jump | SHIFT: Scroll", 10, HEIGHT - 10);
        
        // Draw game over screen
        if (gameOver) {
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            
            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font("Arial", 72));
            gc.fillText("GAME OVER", WIDTH / 2 - 200, HEIGHT / 2 - 50);
            
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 32));
            gc.fillText("Final Score: " + score, WIDTH / 2 - 120, HEIGHT / 2 + 20);
            gc.fillText("Press R to Restart", WIDTH / 2 - 140, HEIGHT / 2 + 70);
        }
    }
    
    private void drawHealthBar() {
        int barWidth = 200;
        int barHeight = 25;
        int barX = 10;
        int barY = 10;
        
        // Background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);
        
        // Health
        double healthPercent = (double) currentHealth / maxHealth;
        Color healthColor = healthPercent > 0.5 ? Color.GREEN : 
                           healthPercent > 0.25 ? Color.ORANGE : Color.RED;
        gc.setFill(healthColor);
        gc.fillRect(barX, barY, barWidth * healthPercent, barHeight);
        
        // Border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);
        
        // Text
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        gc.fillText("HP: " + currentHealth + "/" + maxHealth, barX + 60, barY + 18);
    }
    
    private void spawnEnemy() {
        double enemyY = groundLevel - 40; // Ground level minus enemy height
        
        // Random chance to spawn flying enemy
        if (random.nextDouble() < 0.3) {
            enemyY = groundLevel - 100 - random.nextInt(100);
        }
        
        enemies.add(new Enemy(WIDTH, enemyY));
    }
    
    private void takeDamage(int damage) {
        if (isInvulnerable) return;
        
        currentHealth -= damage;
        isInvulnerable = true;
        invulnerabilityTimer = System.nanoTime();
        
        if (currentHealth <= 0) {
            currentHealth = 0;
            gameOver = true;
            saveScoreToFirebase();
        }
    }
    
    private void restartGame() {
        currentHealth = maxHealth;
        playerX = 100;
        playerY = 500;
        velocityY = 0;
        isOnGround = false;
        isInvulnerable = false;
        enemies.clear();
        score = 0;
        gameOver = false;
        lastEnemySpawn = 0;
        gameStartTime = System.nanoTime();
    }
    
    private void saveScoreToFirebase() {
        long survivalTime = (System.nanoTime() - gameStartTime) / 1_000_000_000; // Convert to seconds
        FirebaseService.getInstance().saveScore(playerName, score, survivalTime);
    }
    
    private void returnToMenu() {
        if (menu != null) {
            menu.show();
        }
    }
    
    private void drawScrollingBackground(Image image, double offsetX) {
        if (image == null) return;
        
        double imgWidth = image.getWidth();
        double imgHeight = image.getHeight();
        
        // Scale to fit height
        double scale = HEIGHT / imgHeight;
        double scaledWidth = imgWidth * scale;
        
        // Draw two copies for seamless scrolling
        gc.drawImage(image, offsetX, 0, scaledWidth, HEIGHT);
        gc.drawImage(image, offsetX + scaledWidth, 0, scaledWidth, HEIGHT);
    }
}
