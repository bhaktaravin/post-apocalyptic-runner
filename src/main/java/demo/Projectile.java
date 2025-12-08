package demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Projectile {
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private double width = 8;
    private double height = 8;
    private boolean active = true;
    private boolean fromPlayer;
    private Color color;
    
    public Projectile(double x, double y, double velocityX, double velocityY, boolean fromPlayer) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.fromPlayer = fromPlayer;
        this.color = fromPlayer ? Color.YELLOW : Color.RED;
    }
    
    public void update() {
        x += velocityX;
        y += velocityY;
        
        // Deactivate if off screen
        if (x < -50 || x > 1400 || y < -50 || y > 800) {
            active = false;
        }
    }
    
    public void render(GraphicsContext gc) {
        if (!active) return;
        
        // Draw projectile with glow effect
        gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.3));
        gc.fillOval(x - 4, y - 4, width + 8, height + 8);
        
        gc.setFill(color);
        gc.fillOval(x, y, width, height);
        
        // Inner bright core
        gc.setFill(Color.WHITE);
        gc.fillOval(x + 2, y + 2, width - 4, height - 4);
    }
    
    public boolean collidesWith(double px, double py, double pWidth, double pHeight) {
        if (!active) return false;
        return x < px + pWidth && 
               x + width > px && 
               y < py + pHeight && 
               y + height > py;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void deactivate() {
        active = false;
    }
    
    public boolean isFromPlayer() {
        return fromPlayer;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
}
