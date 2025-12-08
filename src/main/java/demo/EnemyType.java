package demo;

public enum EnemyType {
    ZOMBIE(60, 1.5, 40, 50, false, 0),           // Slow, high health, ground
    RUNNER(30, 3.0, 40, 40, false, 0),           // Fast, medium health, ground
    FLYING(40, 2.0, 35, 35, true, 4.0),          // Flying, bobs high
    SHOOTER(50, 1.8, 40, 40, false, 3.0);        // Shoots projectiles
    
    private final int maxHealth;
    private final double speed;
    private final double width;
    private final double height;
    private final boolean canFly;
    private final double shootCooldown; // seconds, 0 = doesn't shoot
    
    EnemyType(int maxHealth, double speed, double width, double height, boolean canFly, double shootCooldown) {
        this.maxHealth = maxHealth;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.canFly = canFly;
        this.shootCooldown = shootCooldown;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public boolean canFly() {
        return canFly;
    }
    
    public double getShootCooldown() {
        return shootCooldown;
    }
    
    public boolean canShoot() {
        return shootCooldown > 0;
    }
}
