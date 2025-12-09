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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private double playerY = 510;  // Adjusted to match enemy ground position
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
    private long enemySpawnInterval = 3_000_000_000L; // 3 seconds (base)
    private long currentSpawnInterval = 3_000_000_000L; // Adjusted by level
    
    // Projectiles
    private List<Projectile> projectiles = new ArrayList<>();
    private long lastPlayerShot = 0;
    private long playerShootCooldown = 500_000_000L; // 0.5 seconds
    private int bulletDamage = 20;
    
    // Score and Currency
    private int score = 0;
    private int currency = 0; // Coins for upgrades
    private long gameStartTime = 0;
    
    // Level System
    private int playerLevel = 1;
    private int experience = 0;
    private int experienceToNextLevel = 100;
    private boolean showLevelUpNotification = false;
    private long levelUpNotificationTime = 0;
    
    // Upgrade System
    private Map<UpgradeType, Integer> upgradeLevels = new HashMap<>();
    private boolean showUpgradeMenu = false;
    
    // Game state
    private boolean gameOver = false;
    private MainMenu menu;
    private String playerName;
    
    // Visual effects
    private ParticleSystem particleSystem;
    private boolean wasOnGroundLastFrame = false;
    private double playerBounce = 0;
    private double playerRotation = 0;
    private long lastFootstepTime = 0;
    private long footstepInterval = 200_000_000L; // 0.2 seconds
    private boolean debugMode = true; // Show hitboxes
    
    // Screen shake
    private double shakeX = 0;
    private double shakeY = 0;
    private double shakeIntensity = 0;
    private long shakeStartTime = 0;
    private long shakeDuration = 200_000_000L; // 0.2 seconds
    
    // Combo system
    private int comboCount = 0;
    private long lastKillTime = 0;
    private long comboTimeout = 3_000_000_000L; // 3 seconds
    private double comboMultiplier = 1.0;
    private String comboText = "";
    private long comboTextTime = 0;
    private long comboTextDuration = 1_000_000_000L; // 1 second
    
    // Pause
    private boolean paused = false;
    
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
        particleSystem = new ParticleSystem();
        
        // Initialize upgrades to level 0
        for (UpgradeType type : UpgradeType.values()) {
            upgradeLevels.put(type, 0);
        }
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        // Load background assets
        loadAssets();
        
        // Input handling
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            if ((e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP) && !jumpRequested) {
                jumpRequested = true;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                returnToMenu();
            }
            if (e.getCode() == KeyCode.H) {
                debugMode = !debugMode; // Toggle hitboxes
            }
            if (e.getCode() == KeyCode.P) {
                paused = !paused; // Toggle pause
            }
            if (e.getCode() == KeyCode.U) {
                showUpgradeMenu = !showUpgradeMenu; // Toggle upgrade menu
            }
            // Upgrade purchase keys
            if (showUpgradeMenu) {
                if (e.getCode() == KeyCode.DIGIT1) purchaseUpgrade(UpgradeType.FIRE_RATE);
                if (e.getCode() == KeyCode.DIGIT2) purchaseUpgrade(UpgradeType.BULLET_DAMAGE);
                if (e.getCode() == KeyCode.DIGIT3) purchaseUpgrade(UpgradeType.MAX_HEALTH);
                if (e.getCode() == KeyCode.DIGIT4) purchaseUpgrade(UpgradeType.MOVEMENT_SPEED);
            }
            if (e.getCode() == KeyCode.W) {
                // Shooting handled in update
            }
        });
        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP) {
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
        
        if (paused) {
            return; // Skip update when paused
        }
        
        // Update screen shake
        updateScreenShake(now);
        
        // Update combo timeout
        if (comboCount > 0 && now - lastKillTime > comboTimeout) {
            comboCount = 0;
            comboMultiplier = 1.0;
        }
        
        // Handle horizontal movement
        if (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A)) {
            playerX -= playerSpeed;
        }
        if (pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D)) {
            playerX += playerSpeed;
        }
        
        // Handle shooting
        if (pressedKeys.contains(KeyCode.W) && now - lastPlayerShot > playerShootCooldown) {
            shoot();
            lastPlayerShot = now;
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
            
            // Landing particle effect (check before resetting velocity)
            if (!wasOnGroundLastFrame) {
                particleSystem.createLandingDust(playerX, playerY);
                playerBounce = 5; // Squash effect
            }
            
            velocityY = 0;
            isOnGround = true;
        } else {
            isOnGround = false;
        }
        
        // Create running dust when moving on ground
        if (isOnGround && (pressedKeys.contains(KeyCode.LEFT) || pressedKeys.contains(KeyCode.A) ||
                           pressedKeys.contains(KeyCode.RIGHT) || pressedKeys.contains(KeyCode.D))) {
            if (now - lastFootstepTime > footstepInterval) {
                particleSystem.createRunningDust(playerX, playerY);
                lastFootstepTime = now;
            }
        }
        
        wasOnGroundLastFrame = isOnGround;
        
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
        
        // Update difficulty based on level
        updateDifficulty();
        
        // Spawn enemies
        if (now - lastEnemySpawn > currentSpawnInterval) {
            spawnEnemy();
            lastEnemySpawn = now;
        }
        
        // Update enemies
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.update();
            
            // Enemy shooting (check if enemy is on screen and can see player)
            if (enemy.canShoot() && enemy.getX() < WIDTH - 100 && enemy.getX() > 50) {
                enemyShoot(enemy);
                enemy.resetShootCooldown();
            }
            
            // Check collision with player
            if (!isInvulnerable && enemy.collidesWith(playerX, playerY, 50, 50)) {
                takeDamage(20);
                particleSystem.createExplosion(enemy.getX(), enemy.getY(), Color.rgb(150, 0, 0));
                addScreenShake(8);
                iterator.remove();
            }
            
            // Remove inactive enemies
            if (!enemy.isActive()) {
                iterator.remove();
                score += 10; // Points for dodging
            }
        }
        
        // Update projectiles
        updateProjectiles();
        
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
        
        // Update particle system
        particleSystem.update();
        
        // Create ambient particles (ash/debris)
        particleSystem.createAmbientParticles(WIDTH, HEIGHT);
        
        // Smooth out player bounce animation
        if (playerBounce > 0) {
            playerBounce -= 0.5;
        }
        
        // Add rotation when jumping
        if (!isOnGround) {
            playerRotation = Math.min(15, playerRotation + 1);
        } else {
            playerRotation = Math.max(0, playerRotation - 2);
        }
        
        // Increase score over time
        score++;
    }
    
    private void render() {
        // Clear screen
        gc.setFill(Color.rgb(135, 206, 235)); // Sky blue
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Apply screen shake
        gc.save();
        gc.translate(shakeX, shakeY);
        
        // Draw parallax layers (back to front)
        drawScrollingBackground(bgClouds1, clouds1X);
        drawScrollingBackground(bgClouds2, clouds2X);
        drawScrollingBackground(bgHousesBg, housesBgX);
        drawScrollingBackground(bgGroundHouses, groundHousesX);
        drawScrollingBackground(bgRoad, roadX);
        drawScrollingBackground(bgFence, fenceX);
        
        // Draw particles (background layer)
        particleSystem.render(gc);
        
        // Draw enemies
        for (Enemy enemy : enemies) {
            enemy.render(gc, debugMode);
        }
        
        // Draw projectiles
        for (Projectile projectile : projectiles) {
            projectile.render(gc);
        }
        
        // Draw player (with invulnerability flash and animations)
        if (!isInvulnerable || (System.nanoTime() / 100_000_000) % 2 == 0) {
            gc.save();
            
            // Apply squash/stretch effect
            double bounceEffect = playerBounce / 2;
            double playerWidth = 50 + bounceEffect;
            double playerHeight = 50 - bounceEffect;
            double adjustedY = playerY + bounceEffect / 2;
            
            // Rotate when jumping
            if (playerRotation > 0) {
                gc.translate(playerX + 25, adjustedY + 25);
                gc.rotate(playerRotation);
                gc.translate(-(playerX + 25), -(adjustedY + 25));
            }
            
            // Draw player body with gradient effect
            gc.setFill(Color.DARKBLUE);
            gc.fillRect(playerX, adjustedY, playerWidth, playerHeight);
            gc.setFill(Color.BLUE);
            gc.fillRect(playerX + 3, adjustedY + 3, playerWidth - 6, playerHeight - 6);
            
            // Player eyes
            gc.setFill(Color.WHITE);
            gc.fillOval(playerX + 10, adjustedY + 15, 10, 10);
            gc.fillOval(playerX + 30, adjustedY + 15, 10, 10);
            
            // Eye pupils
            gc.setFill(Color.BLACK);
            gc.fillOval(playerX + 13, adjustedY + 18, 4, 4);
            gc.fillOval(playerX + 33, adjustedY + 18, 4, 4);
            
            gc.restore();
            
            // Debug: Draw hitbox
            if (debugMode) {
                gc.setStroke(Color.LIME);
                gc.setLineWidth(2);
                gc.strokeRect(playerX, playerY, 50, 50);
            }
        }
        
        // Draw health bar
        drawHealthBar();
        
        // Restore from screen shake before drawing UI
        gc.restore();
        
        // Draw enhanced HUD
        drawEnhancedHUD();
        
        // Draw combo text
        if (System.nanoTime() - comboTextTime < comboTextDuration) {
            drawComboText();
        }
        
        // Draw level up notification
        if (showLevelUpNotification && System.nanoTime() - levelUpNotificationTime < 3_000_000_000L) {
            drawLevelUpNotification();
        } else if (System.nanoTime() - levelUpNotificationTime >= 3_000_000_000L) {
            showLevelUpNotification = false;
        }
        
        // Draw pause overlay
        if (paused) {
            gc.setFill(Color.rgb(0, 0, 0, 0.5));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 72));
            gc.fillText("PAUSED", WIDTH / 2 - 140, HEIGHT / 2);
            
            gc.setFont(javafx.scene.text.Font.font("Arial", 24));
            gc.fillText("Press P to Resume", WIDTH / 2 - 120, HEIGHT / 2 + 50);
            gc.fillText("Press U for Upgrades", WIDTH / 2 - 120, HEIGHT / 2 + 85);
            gc.fillText("Press ESC for Menu", WIDTH / 2 - 120, HEIGHT / 2 + 120);
        }
        
        // Draw upgrade menu
        if (showUpgradeMenu) {
            drawUpgradeMenu();
        }
        
        // Draw game over screen
        if (gameOver) {
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            
            // Animated pulsing text
            double pulse = Math.sin(System.nanoTime() / 200_000_000.0) * 5 + 72;
            gc.setFill(Color.RED);
            gc.setFont(javafx.scene.text.Font.font("Arial", pulse));
            gc.fillText("GAME OVER", WIDTH / 2 - 200, HEIGHT / 2 - 50);
            
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 32));
            gc.fillText("Final Score: " + score, WIDTH / 2 - 120, HEIGHT / 2 + 20);
            
            // Show level reached
            gc.setFill(Color.CYAN);
            gc.fillText("Level Reached: " + playerLevel, WIDTH / 2 - 140, HEIGHT / 2 + 60);
            
            gc.setFill(Color.WHITE);
            gc.fillText("Press R to Restart", WIDTH / 2 - 140, HEIGHT / 2 + 100);
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
        // Random enemy type based on score
        EnemyType type;
        double rand = random.nextDouble();
        
        if (score < 500) {
            // Early game: mostly runners
            type = rand < 0.7 ? EnemyType.RUNNER : EnemyType.ZOMBIE;
        } else if (score < 2000) {
            // Mid game: introduce flying and shooters
            if (rand < 0.3) type = EnemyType.ZOMBIE;
            else if (rand < 0.6) type = EnemyType.RUNNER;
            else if (rand < 0.8) type = EnemyType.FLYING;
            else type = EnemyType.SHOOTER;
        } else {
            // Late game: more variety and difficulty
            if (rand < 0.2) type = EnemyType.ZOMBIE;
            else if (rand < 0.4) type = EnemyType.RUNNER;
            else if (rand < 0.7) type = EnemyType.FLYING;
            else type = EnemyType.SHOOTER;
        }
        
        // Calculate Y position based on enemy type
        double enemyY;
        if (type.canFly()) {
            // Flying enemies spawn in the air
            enemyY = 350; // Fixed height in the air
        } else {
            // Ground enemies spawn aligned with player
            // Player top is at groundLevel (550), bottom at groundLevel + 50 (600)
            // Enemy should have same bottom, so: enemyY = 600 - enemyHeight
            enemyY = (groundLevel + 50) - type.getHeight();
        }
        
        enemies.add(new Enemy(WIDTH, enemyY, type));
    }
    
    private void takeDamage(int damage) {
        if (isInvulnerable) return;
        
        currentHealth -= damage;
        isInvulnerable = true;
        invulnerabilityTimer = System.nanoTime();
        
        // Create hit effect particles
        particleSystem.createHitEffect(playerX, playerY);
        addScreenShake(10);
        
        if (currentHealth <= 0) {
            currentHealth = 0;
            gameOver = true;
            saveScoreToFirebase();
        }
    }
    
    private void restartGame() {
        currentHealth = maxHealth;
        playerX = 100;
        playerY = 510;
        velocityY = 0;
        isOnGround = false;
        isInvulnerable = false;
        enemies.clear();
        projectiles.clear();
        score = 0;
        gameOver = false;
        lastEnemySpawn = 0;
        gameStartTime = System.nanoTime();
        playerBounce = 0;
        playerRotation = 0;
        wasOnGroundLastFrame = false;
        particleSystem = new ParticleSystem();
        comboCount = 0;
        comboMultiplier = 1.0;
        lastKillTime = 0;
        shakeIntensity = 0;
        paused = false;
    }
    
    private void shoot() {
        // Create projectile from player
        double projectileX = playerX + 50; // From right side of player
        double projectileY = playerY + 20; // Center height
        double velocityX = 8; // Fast horizontal speed
        projectiles.add(new Projectile(projectileX, projectileY, velocityX, 0, true));
    }
    
    private void enemyShoot(Enemy enemy) {
        // Enemy shoots towards player
        double projectileX = enemy.getX();
        double projectileY = enemy.getY() + 20;
        
        // Calculate direction to player
        double dx = playerX - projectileX;
        double dy = playerY - projectileY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Normalize and set speed
        double speed = 5;
        double velocityX = (dx / distance) * speed;
        double velocityY = (dy / distance) * speed;
        
        projectiles.add(new Projectile(projectileX, projectileY, velocityX, velocityY, false));
    }
    
    private void updateProjectiles() {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update();
            
            if (projectile.isFromPlayer()) {
                // Check collision with enemies
                for (Enemy enemy : enemies) {
                    if (enemy.isActive() && projectile.collidesWith(enemy.getX(), enemy.getY(), 
                            enemy.getType().getWidth(), enemy.getType().getHeight())) {
                        enemy.takeDamage(bulletDamage);
                        projectile.deactivate();
                        particleSystem.createHitEffect(enemy.getX(), enemy.getY());
                        
                        if (!enemy.isActive()) {
                            addKill();
                            particleSystem.createExplosion(enemy.getX(), enemy.getY(), Color.rgb(150, 0, 0));
                            addScreenShake(5);
                        }
                        break;
                    }
                }
            } else {
                // Enemy projectile - check collision with player
                if (!isInvulnerable && projectile.collidesWith(playerX, playerY, 50, 50)) {
                    takeDamage(15);
                    projectile.deactivate();
                }
            }
            
            if (!projectile.isActive()) {
                iterator.remove();
            }
        }
    }
    
    private void saveScoreToFirebase() {
        long survivalTime = (System.nanoTime() - gameStartTime) / 1_000_000_000; // Convert to seconds
        System.out.println("Calling saveScore with: playerName=" + playerName + ", score=" + score + ", survivalTime=" + survivalTime);
        
        FirebaseService.getInstance().saveScore(playerName, score, survivalTime)
            .thenAccept(v -> System.out.println("Firebase save completed successfully!"))
            .exceptionally(ex -> {
                System.err.println("Firebase save failed: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
    }
    
    private void drawUpgradeMenu() {
        // Semi-transparent background
        gc.setFill(Color.rgb(0, 0, 0, 0.85));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Title
        gc.setFill(Color.GOLD);
        gc.setFont(javafx.scene.text.Font.font("Arial", 48));
        gc.fillText("UPGRADES", WIDTH / 2 - 110, 80);
        
        // Level and currency display
        gc.setFill(Color.CYAN);
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.fillText("Level " + playerLevel, WIDTH / 2 - 180, 120);
        
        gc.setFill(Color.YELLOW);
        gc.setFont(javafx.scene.text.Font.font("Arial", 24));
        gc.fillText("Coins: " + currency, WIDTH / 2 + 20, 120);
        
        // Draw each upgrade option
        int startY = 160;
        int spacing = 120;
        int index = 0;
        
        for (UpgradeType type : UpgradeType.values()) {
            int currentLevel = upgradeLevels.get(type);
            int cost = type.getCost(currentLevel);
            boolean maxed = currentLevel >= type.getMaxLevel();
            
            double y = startY + (index * spacing);
            
            // Upgrade box
            gc.setFill(Color.rgb(40, 40, 60));
            gc.fillRect(100, y, 600, 100);
            gc.setStroke(maxed ? Color.GOLD : (currency >= cost ? Color.LIGHTGREEN : Color.DARKGRAY));
            gc.setLineWidth(3);
            gc.strokeRect(100, y, 600, 100);
            
            // Upgrade name and description
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 20));
            gc.fillText(type.getDisplayName(), 120, y + 30);
            
            gc.setFont(javafx.scene.text.Font.font("Arial", 14));
            gc.setFill(Color.LIGHTGRAY);
            gc.fillText(type.getDescription(), 120, y + 50);
            
            // Current level and stats
            String levelText = "Level: " + currentLevel + "/" + type.getMaxLevel();
            gc.setFill(Color.CYAN);
            gc.fillText(levelText, 120, y + 75);
            
            // Current effect
            String effectText = getEffectDescription(type, currentLevel);
            gc.fillText(effectText, 300, y + 75);
            
            // Cost or MAX indicator
            gc.setFont(javafx.scene.text.Font.font("Arial", 18));
            if (maxed) {
                gc.setFill(Color.GOLD);
                gc.fillText("MAX", 630, y + 60);
            } else {
                gc.setFill(currency >= cost ? Color.LIGHTGREEN : Color.RED);
                gc.fillText("Cost: " + cost, 600, y + 60);
                
                // Key hint
                gc.setFont(javafx.scene.text.Font.font("Arial", 14));
                gc.setFill(Color.YELLOW);
                gc.fillText("[" + (index + 1) + "]", 650, y + 35);
            }
            
            index++;
        }
        
        // Instructions
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 18));
        gc.fillText("Press 1-4 to purchase upgrades", WIDTH / 2 - 140, HEIGHT - 60);
        gc.fillText("Press U to close", WIDTH / 2 - 70, HEIGHT - 30);
    }
    
    private String getEffectDescription(UpgradeType type, int level) {
        switch (type) {
            case FIRE_RATE:
                return String.format("%.2fs cooldown", type.getEffectValue(level));
            case BULLET_DAMAGE:
                return String.format("%.0f damage", type.getEffectValue(level));
            case MAX_HEALTH:
                return String.format("%.0f HP", type.getEffectValue(level));
            case MOVEMENT_SPEED:
                return String.format("%.0f speed", type.getEffectValue(level));
            default:
                return "";
        }
    }
    
    private void purchaseUpgrade(UpgradeType type) {
        int currentLevel = upgradeLevels.get(type);
        int cost = type.getCost(currentLevel);
        
        if (currentLevel >= type.getMaxLevel()) {
            return; // Already maxed
        }
        
        if (currency >= cost) {
            currency -= cost;
            upgradeLevels.put(type, currentLevel + 1);
            applyUpgrade(type);
        }
    }
    
    private void applyUpgrade(UpgradeType type) {
        int level = upgradeLevels.get(type);
        
        switch (type) {
            case FIRE_RATE:
                playerShootCooldown = (long)(type.getEffectValue(level) * 1_000_000_000L);
                break;
            case BULLET_DAMAGE:
                bulletDamage = (int)type.getEffectValue(level);
                break;
            case MAX_HEALTH:
                int oldMax = maxHealth;
                maxHealth = (int)type.getEffectValue(level);
                // Heal the difference
                currentHealth += (maxHealth - oldMax);
                break;
            case MOVEMENT_SPEED:
                playerSpeed = type.getEffectValue(level);
                break;
        }
    }
    
    private void addExperience(int xp) {
        experience += xp;
        
        // Check for level up
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            playerLevel++;
            
            // Scale XP requirement (increases by 50 each level)
            experienceToNextLevel = 100 + (playerLevel - 1) * 50;
            
            // Level up rewards
            currency += 50 * playerLevel; // Bonus coins
            currentHealth = Math.min(currentHealth + 20, maxHealth); // Heal 20 HP
            
            // Show notification
            showLevelUpNotification = true;
            levelUpNotificationTime = System.nanoTime();
        }
    }
    
    private void updateDifficulty() {
        // Spawn enemies faster as level increases (max 0.8 seconds)
        double spawnReduction = Math.min(2.2, (playerLevel - 1) * 0.15);
        currentSpawnInterval = (long)((3.0 - spawnReduction) * 1_000_000_000L);
        currentSpawnInterval = Math.max(800_000_000L, currentSpawnInterval);
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
    
    private void addScreenShake(double intensity) {
        shakeIntensity = intensity;
        shakeStartTime = System.nanoTime();
    }
    
    private void updateScreenShake(long now) {
        if (shakeIntensity > 0) {
            long elapsed = now - shakeStartTime;
            if (elapsed < shakeDuration) {
                double progress = (double) elapsed / shakeDuration;
                double currentIntensity = shakeIntensity * (1 - progress);
                
                shakeX = (random.nextDouble() - 0.5) * currentIntensity * 2;
                shakeY = (random.nextDouble() - 0.5) * currentIntensity * 2;
            } else {
                shakeX = 0;
                shakeY = 0;
                shakeIntensity = 0;
            }
        }
    }
    
    private void addKill() {
        long now = System.nanoTime();
        
        // Check if combo timed out
        if (now - lastKillTime > comboTimeout) {
            comboCount = 0;
        }
        
        comboCount++;
        lastKillTime = now;
        
        // Calculate multiplier
        if (comboCount >= 10) {
            comboMultiplier = 5.0;
            comboText = "UNSTOPPABLE!";
        } else if (comboCount >= 7) {
            comboMultiplier = 3.0;
            comboText = "INCREDIBLE!";
        } else if (comboCount >= 5) {
            comboMultiplier = 2.5;
            comboText = "AWESOME!";
        } else if (comboCount >= 3) {
            comboMultiplier = 2.0;
            comboText = "NICE!";
        } else {
            comboMultiplier = 1.0;
            comboText = "";
        }
        
        comboTextTime = now;
        
        // Add score with multiplier
        int killScore = (int) (50 * comboMultiplier);
        score += killScore;
        
        // Award currency (10 coins base + combo bonus)
        int coinReward = (int) (10 * comboMultiplier);
        currency += coinReward;
        
        // Award experience (20 XP base + combo bonus)
        int xpReward = (int) (20 * comboMultiplier);
        addExperience(xpReward);
    }
    
    private void drawComboText() {
        if (comboText.isEmpty()) return;
        
        // Pulse animation
        double pulse = Math.sin(System.nanoTime() / 100_000_000.0) * 5 + 50;
        
        // Rainbow color based on combo
        Color textColor;
        if (comboCount >= 10) {
            textColor = Color.GOLD;
        } else if (comboCount >= 7) {
            textColor = Color.ORANGE;
        } else if (comboCount >= 5) {
            textColor = Color.YELLOW;
        } else {
            textColor = Color.WHITE;
        }
        
        // Draw with outline
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, pulse));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeText(comboText, WIDTH / 2 - 100, HEIGHT / 2 - 100);
        gc.setFill(textColor);
        gc.fillText(comboText, WIDTH / 2 - 100, HEIGHT / 2 - 100);
    }
    
    private void drawLevelUpNotification() {
        long elapsed = System.nanoTime() - levelUpNotificationTime;
        double progress = elapsed / 3_000_000_000.0; // 3 seconds duration
        
        // Pulse and fade effect
        double pulse = Math.sin(elapsed / 100_000_000.0) * 10 + 60;
        double alpha = 1.0 - progress; // Fade out
        
        // Draw notification box
        gc.setFill(Color.rgb(255, 215, 0, alpha * 0.3)); // Gold with transparency
        gc.fillRect(WIDTH / 2 - 200, HEIGHT / 2 - 150, 400, 120);
        gc.setStroke(Color.rgb(255, 215, 0, alpha));
        gc.setLineWidth(4);
        gc.strokeRect(WIDTH / 2 - 200, HEIGHT / 2 - 150, 400, 120);
        
        // Level up text
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, pulse));
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeText("LEVEL UP!", WIDTH / 2 - 100, HEIGHT / 2 - 100);
        gc.setFill(Color.rgb(255, 215, 0, alpha));
        gc.fillText("LEVEL UP!", WIDTH / 2 - 100, HEIGHT / 2 - 100);
        
        // New level
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 36));
        String levelText = "Level " + playerLevel;
        gc.strokeText(levelText, WIDTH / 2 - 60, HEIGHT / 2 - 50);
        gc.setFill(Color.rgb(135, 206, 235, alpha)); // Light blue
        gc.fillText(levelText, WIDTH / 2 - 60, HEIGHT / 2 - 50);
        
        // Rewards
        gc.setFont(javafx.scene.text.Font.font("Arial", 18));
        int coinBonus = 50 * playerLevel;
        String rewardsText = "+" + coinBonus + " Coins • +20 HP";
        gc.setFill(Color.rgb(255, 255, 255, alpha));
        gc.fillText(rewardsText, WIDTH / 2 - 80, HEIGHT / 2 - 10);
    }
    
    private void drawEnhancedHUD() {
        // Draw health bar (already exists, keep it)
        drawHealthBar();
        
        // Score panel (top right) - expanded for level info
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(WIDTH - 200, 10, 190, 210);
        
        // Level display
        gc.setFill(Color.CYAN);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 28));
        gc.fillText("Level " + playerLevel, WIDTH - 190, 35);
        
        // XP Bar
        double xpBarWidth = 170;
        double xpBarHeight = 15;
        double xpBarX = WIDTH - 190;
        double xpBarY = 45;
        double xpPercent = (double) experience / experienceToNextLevel;
        
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(xpBarX, xpBarY, xpBarWidth, xpBarHeight);
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(xpBarX, xpBarY, xpBarWidth * xpPercent, xpBarHeight);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(xpBarX, xpBarY, xpBarWidth, xpBarHeight);
        
        // XP Text
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 12));
        gc.fillText(experience + "/" + experienceToNextLevel + " XP", xpBarX + 5, xpBarY + 12);
        
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        gc.fillText("Score: " + score, WIDTH - 190, 90);
        
        // Currency display
        gc.setFill(Color.GOLD);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 20));
        gc.fillText("Coins: " + currency, WIDTH - 190, 115);
        
        // Combo display
        if (comboCount > 1) {
            Color comboColor = comboCount >= 5 ? Color.GOLD : Color.YELLOW;
            gc.setFill(comboColor);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 18));
            gc.fillText("Combo: " + comboCount + "x", WIDTH - 190, 140);
            gc.fillText("x" + String.format("%.1f", comboMultiplier), WIDTH - 190, 160);
        }
        
        // Enemy count
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 16));
        gc.fillText("Enemies: " + enemies.size(), WIDTH - 190, 190);
        
        // Control hints (bottom left)
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(10, HEIGHT - 110, 200, 100);
        
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        gc.fillText("Controls:", 20, HEIGHT - 90);
        gc.fillText("Arrow/WASD - Move", 20, HEIGHT - 70);
        gc.fillText("W - Shoot", 20, HEIGHT - 50);
        gc.fillText("P - Pause", 20, HEIGHT - 30);
        gc.fillText("U - Upgrades", 20, HEIGHT - 10);
        
        // Debug info
        if (debugMode) {
            gc.setFont(javafx.scene.text.Font.font("Arial", 12));
            gc.fillText("Particles: " + particleSystem.getParticleCount(), WIDTH - 190, 150);
        }
        
        // Controls (bottom left)
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(10, HEIGHT - 45, 450, 35);
        
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        gc.fillText("A/D: Move | SPACE: Jump | W: Shoot | P: Pause | H: Hitboxes", 20, HEIGHT - 20);
    }
}
