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
    private double animationTimer = 0;
    private double bobOffset = 0;
    
    public Enemy(double x, double y) {
        this.x = x;
        this.y = y;
        this.color = Color.rgb(150, 0, 0); // Dark red
    }
    
    public void update() {
        x -= speed; // Move left
        animationTimer += 0.1;
        
        // Bob up and down for floating enemies
        bobOffset = Math.sin(animationTimer) * 3;
        
        // Deactivate if off screen
        if (x + width < 0) {
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
        
        double[] hornX2 = {x + 30, x + 35, x + 33};
        double[] hornY2 = {renderY, renderY, renderY - 8};
        gc.fillPolygon(hornX2, hornY2, 3);
        
        // Debug: Draw hitbox
        if (debugMode) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(2);
            gc.strokeRect(x, y, width, height);
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
}
