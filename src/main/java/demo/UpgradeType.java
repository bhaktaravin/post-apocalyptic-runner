package demo;

public enum UpgradeType {
    FIRE_RATE("Fire Rate", "Shoot faster", 100, 5),
    BULLET_DAMAGE("Bullet Damage", "Deal more damage", 150, 4),
    MAX_HEALTH("Max Health", "Increase health capacity", 200, 3),
    MOVEMENT_SPEED("Movement Speed", "Move faster", 120, 4);
    
    private final String displayName;
    private final String description;
    private final int baseCost;
    private final int maxLevel;
    
    UpgradeType(String displayName, String description, int baseCost, int maxLevel) {
        this.displayName = displayName;
        this.description = description;
        this.baseCost = baseCost;
        this.maxLevel = maxLevel;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getBaseCost() {
        return baseCost;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    // Cost increases by 50% per level
    public int getCost(int currentLevel) {
        if (currentLevel >= maxLevel) return -1; // Max level reached
        return (int)(baseCost * Math.pow(1.5, currentLevel));
    }
    
    // Get effect value based on level
    public double getEffectValue(int level) {
        switch (this) {
            case FIRE_RATE:
                // Reduces cooldown: 0.5s -> 0.4s -> 0.33s -> 0.27s -> 0.23s
                return 0.5 - (level * 0.06);
            case BULLET_DAMAGE:
                // Increases damage: 20 -> 30 -> 40 -> 50
                return 20 + (level * 10);
            case MAX_HEALTH:
                // Increases max health: 100 -> 150 -> 200
                return 100 + (level * 50);
            case MOVEMENT_SPEED:
                // Increases speed: 5 -> 6 -> 7 -> 8
                return 5 + level;
            default:
                return 0;
        }
    }
}
