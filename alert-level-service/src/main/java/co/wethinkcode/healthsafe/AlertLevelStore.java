package co.wethinkcode.healthsafe;

/**
 * Holds the hospital Emergency Status in memory (0 = normal, 8 = full Code Blue).
 * State is lost on restart, which is fine for this exercise; a real system would persist it.
 */
public class AlertLevelStore {

    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 8;

    private volatile int level = MIN_LEVEL;

    public int get() {
        return level;
    }

    /** @throws IllegalArgumentException if the level is missing or outside 0-8 (mapped to HTTP 400). */
    public void set(Integer newLevel) {
        if (newLevel == null) {
            throw new IllegalArgumentException("level is required");
        }
        if (newLevel < MIN_LEVEL || newLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + ", got " + newLevel);
        }
        level = newLevel;
    }
}