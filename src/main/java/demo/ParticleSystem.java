package demo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private List<Particle> particles;
    private Random random;
    
    public ParticleSystem() {
        this.particles = new ArrayList<>();
        this.random = new Random();
    }
    
    public void update() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update();
            if (!particle.isActive()) {
                iterator.remove();
            }
        }
    }
    
    public void render(GraphicsContext gc) {
        for (Particle particle : particles) {
            particle.render(gc);
        }
    }
    
    // Dust particles when player lands
    public void createLandingDust(double x, double y) {
        for (int i = 0; i < 15; i++) {
            double vx = (random.nextDouble() - 0.5) * 4;
            double vy = -random.nextDouble() * 3;
            double size = 3 + random.nextDouble() * 4;
            Color dustColor = new Color(0.6, 0.5, 0.4, 0.7);
            particles.add(new Particle(x + random.nextDouble() * 50, y + 50, vx, vy, size, dustColor, 30));
        }
    }
    
    // Running dust trail
    public void createRunningDust(double x, double y) {
        if (random.nextDouble() < 0.3) { // Not every frame
            double vx = -random.nextDouble() * 2;
            double vy = -random.nextDouble() * 2;
            double size = 2 + random.nextDouble() * 3;
            Color dustColor = new Color(0.6, 0.5, 0.4, 0.5);
            particles.add(new Particle(x + random.nextDouble() * 30, y + 50, vx, vy, size, dustColor, 20));
        }
    }
    
    // Explosion effect for enemies
    public void createExplosion(double x, double y, Color baseColor) {
        for (int i = 0; i < 25; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 2 + random.nextDouble() * 4;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double size = 4 + random.nextDouble() * 6;
            
            // Vary the color slightly while keeping values in valid range [0.0, 1.0]
            double r = Math.max(0.0, Math.min(1.0, baseColor.getRed() + (random.nextDouble() - 0.5) * 0.3));
            double g = Math.max(0.0, Math.min(1.0, baseColor.getGreen() + (random.nextDouble() - 0.5) * 0.3));
            double b = Math.max(0.0, Math.min(1.0, baseColor.getBlue() + (random.nextDouble() - 0.5) * 0.3));
            Color particleColor = new Color(r, g, b, 0.8);
            
            particles.add(new Particle(x + 20, y + 20, vx, vy, size, particleColor, 40));
        }
    }
    
    // Hit effect when player takes damage
    public void createHitEffect(double x, double y) {
        for (int i = 0; i < 10; i++) {
            double vx = (random.nextDouble() - 0.5) * 6;
            double vy = (random.nextDouble() - 0.5) * 6;
            double size = 3 + random.nextDouble() * 4;
            Color hitColor = new Color(1.0, 0, 0, 0.8);
            particles.add(new Particle(x + 25, y + 25, vx, vy, size, hitColor, 25));
        }
    }
    
    // Ambient environmental particles (ash, debris)
    public void createAmbientParticles(double screenWidth, double screenHeight) {
        if (random.nextDouble() < 0.05) { // Spawn occasionally
            double x = screenWidth + 10;
            double y = random.nextDouble() * screenHeight * 0.7; // Upper portion of screen
            double vx = -0.5 - random.nextDouble() * 1.5;
            double vy = random.nextDouble() * 0.5;
            double size = 2 + random.nextDouble() * 3;
            Color ashColor = new Color(0.7, 0.7, 0.7, 0.4);
            particles.add(new Particle(x, y, vx, vy, size, ashColor, 200));
        }
    }
    
    public int getParticleCount() {
        return particles.size();
    }
}
