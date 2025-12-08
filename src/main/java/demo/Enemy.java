package demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Enemy {
    private double x;
    private double y;
    private double width;
    private double height;
    private double speed;
    private Color color;
    private boolean active = true;
    private double animationTimer = 0;
    private double bobOffset = 0;
    private EnemyType type;
    private int currentHealth;
    private int maxHealth;
    private double shootCooldown = 0;
    private double timeSinceLastShot = 0;
    
    public Enemy(double x, double y, EnemyType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.width = type.getWidth();
        this.height = type.getHeight();
        this.speed = type.getSpeed();
        this.maxHealth = type.getMaxHealth();
        this.currentHealth = maxHealth;
        this.shootCooldown = type.getShootCooldown();
        
        // Set color based on type
        switch (type) {
            case ZOMBIE:
                this.color = Color.rgb(100, 150, 100); // Greenish
                break;
            case RUNNER:
                this.color = Color.rgb(150, 0, 0); // Red
                break;
            case FLYING:
                this.color = Color.rgb(100, 100, 150); // Bluish
                break;
            case SHOOTER:
                this.color = Color.rgb(150, 100, 0); // Orange
                break;
            default:
                this.color = Color.rgb(150, 0, 0);
        }
    }
    
    public void update() {
        x -= speed; // Move left
        animationTimer += 0.1;
        
        // Bob up and down for flying enemies
        if (type.canFly()) {
            bobOffset = Math.sin(animationTimer) * 8;
        } else {
            bobOffset = Math.sin(animationTimer) * 3;
        }
        
        // Update shoot cooldown
        if (type.canShoot()) {
            timeSinceLastShot += 0.016; // ~60fps
        }
        
        // Deactivate if off screen
        if (x + width < 0) {
            active = false;
        }
    }
    
    public boolean canShoot() {
        return type.canShoot() && timeSinceLastShot >= shootCooldown;
    }
    
    public void resetShootCooldown() {
        timeSinceLastShot = 0;
    }
    
    public void takeDamage(int damage) {
        currentHealth -= damage;
        if (currentHealth <= 0) {
            currentHealth = 0;
            active = false;
        }
    }
    
    public void render(GraphicsContext gc) {
        render(gc, false);
    }
    
    public void render(GraphicsContext gc, boolean debugMode) {
        if (!active) return;
        
        double renderY = y + bobOffset;
        
        // Draw shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(x + 5, y + height + 5, width - 10, 8);
        
        // Draw enemy body with gradient effect
        gc.setFill(Color.rgb(100, 0, 0)); // Darker outline
        gc.fillRect(x - 2, renderY - 2, width + 4, height + 4);
        gc.setFill(color);
        gc.fillRect(x, renderY, width, height);
        
        // Add highlight
        gc.setFill(Color.rgb(200, 50, 50, 0.5));
        gc.fillRect(x + 5, renderY + 5, width - 20, height / 3);
        
        // Draw eyes with animation (blinking)
        double eyeSize = (animationTimer % 3.0 < 2.9) ? 8 : 2;
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 8, renderY + 10, eyeSize, eyeSize);
        gc.fillOval(x + 24, renderY + 10, eyeSize, eyeSize);
        
        // Draw pupils
        if (eyeSize > 2) {
            gc.setFill(Color.RED);
            gc.fillOval(x + 11, renderY + 13, 3, 3);
            gc.fillOval(x + 27, renderY + 13, 3, 3);
        }
        
        // Draw menacing mouth
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeLine(x + 10, renderY + 28, x + 15, renderY + 32);
        gc.strokeLine(x + 15, renderY + 32, x + 20, renderY + 28);
        gc.strokeLine(x + 20, renderY + 28, x + 25, renderY + 32);
        gc.strokeLine(x + 25, renderY + 32, x + 30, renderY + 28);
        
        // Draw horns or spikes
        gc.setFill(Color.rgb(80, 0, 0));
        double[] hornX1 = {x + 5, x + 10, x + 7};
        double[] hornY1 = {renderY, renderY, renderY - 8};
        gc.fillPolygon(hornX1, hornY1, 3);
        
        double[] hornX2 = {x + width - 10, x + width - 5, x + width - 7};
        double[] hornY2 = {renderY, renderY, renderY - 8};
        gc.fillPolygon(hornX2, hornY2, 3);
        
        // Draw health bar if damaged
        if (currentHealth < maxHealth) {
            drawHealthBar(gc);
        }
        
        // Draw type indicator
        drawTypeIndicator(gc, renderY);
        
        // Debug: Draw hitbox
        if (debugMode) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            gc.strokeRect(x, y, width, height);
        }
    }
    
    private void drawHealthBar(GraphicsContext gc) {
        double barWidth = width;
        double barHeight = 4;
        double barX = x;
        double barY = y - 8;
        
        // Background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(barX, barY, barWidth, barHeight);
        
        // Health
        double healthPercent = (double) currentHealth / maxHealth;
        Color healthColor = healthPercent > 0.5 ? Color.GREEN : 
                           healthPercent > 0.25 ? Color.YELLOW : Color.RED;
        gc.setFill(healthColor);
        gc.fillRect(barX, barY, barWidth * healthPercent, barHeight);
    }
    
    private void drawTypeIndicator(GraphicsContext gc, double renderY) {
        // Draw small icon or symbol based on type
        switch (type) {
            case ZOMBIE:
                // Draw Z
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Arial", 10));
                gc.fillText("Z", x + width / 2 - 3, renderY - 10);
                break;
            case RUNNER:
                // Draw speed lines
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1);
                gc.strokeLine(x - 5, renderY + height/2, x - 2, renderY + height/2);
                gc.strokeLine(x - 8, renderY + height/2 + 3, x - 5, renderY + height/2 + 3);
                break;
            case FLYING:
                // Draw wings
                gc.setFill(new Color(1, 1, 1, 0.5));
                gc.fillOval(x - 8, renderY + height/3, 8, 5);
                gc.fillOval(x + width, renderY + height/3, 8, 5);
                break;
            case SHOOTER:
                // Draw crosshair
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(2);
                double centerY = renderY + height/2;
                gc.strokeLine(x - 6, centerY, x - 2, centerY);
                gc.strokeLine(x - 4, centerY - 2, x - 4, centerY + 2);
                break;
        }
    }
    
    public boolean collidesWith(double px, double py, double pWidth, double pHeight) {
        if (!active) return false;
        return px < x + width && 
               px + pWidth > x && 
               py < y + height && 
               py + pHeight > y;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public EnemyType getType() {
        return type;
    }
    
    public int getCurrentHealth() {
        return currentHealth;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
}
