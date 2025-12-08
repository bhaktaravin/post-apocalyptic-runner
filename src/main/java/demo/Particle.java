package demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Particle {
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private double size;
    private Color color;
    private double life;
    private double maxLife;
    private boolean active;
    
    public Particle(double x, double y, double velocityX, double velocityY, double size, Color color, double life) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.size = size;
        this.color = color;
        this.life = life;
        this.maxLife = life;
        this.active = true;
    }
    
    public void update() {
        if (!active) return;
        
        x += velocityX;
        y += velocityY;
        velocityY += 0.1; // Gravity
        life--;
        
        if (life <= 0) {
            active = false;
        }
    }
    
    public void render(GraphicsContext gc) {
        if (!active) return;
        
        double alpha = life / maxLife;
        Color fadedColor = new Color(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            alpha * color.getOpacity()
        );
        
        gc.setFill(fadedColor);
        gc.fillOval(x - size / 2, y - size / 2, size, size);
    }
    
    public boolean isActive() {
        return active;
    }
}
