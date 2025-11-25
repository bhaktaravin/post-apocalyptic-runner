package demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Enemy {
    private double x;
    private double y;
    private double width = 40;
    private double height = 40;
    private double speed = 2;
    private Color color;
    private boolean active = true;
    
    public Enemy(double x, double y) {
        this.x = x;
        this.y = y;
        this.color = Color.rgb(150, 0, 0); // Dark red
    }
    
    public void update() {
        x -= speed; // Move left
        
        // Deactivate if off screen
        if (x + width < 0) {
            active = false;
        }
    }
    
    public void render(GraphicsContext gc) {
        if (!active) return;
        
        // Draw enemy body
        gc.setFill(color);
        gc.fillRect(x, y, width, height);
        
        // Draw eyes
        gc.setFill(Color.YELLOW);
        gc.fillOval(x + 8, y + 10, 8, 8);
        gc.fillOval(x + 24, y + 10, 8, 8);
        
        // Draw mouth
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeLine(x + 10, y + 30, x + 30, y + 30);
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
}
