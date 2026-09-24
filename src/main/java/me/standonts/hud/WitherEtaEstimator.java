package me.standonts.hud;

import fr.alexdoru.mwe.api.enums.MWTeam;

/**
 * Estimates how long the last standing wither will survive, based on the health
 * decay observed on the scoreboard.
 */
final class WitherEtaEstimator {

    private MWTeam team;
    private int previousHealth = -1;
    private long previousUpdate;
    private double damagePerSecond;

    /**
     * Records the current health of the given wither and returns the remaining lifetime
     * in seconds, or a negative value when no estimate can be made yet.
     */
    int update(MWTeam currentTeam, int health, long now) {
        if (team != currentTeam) {
            reset();
            team = currentTeam;
        }
        if (previousHealth < 0) {
            previousHealth = health;
            previousUpdate = now;
            return -1;
        }
        if (health != previousHealth) {
            long elapsed = now - previousUpdate;
            if (health < previousHealth && elapsed >= 100L && elapsed <= 10_000L) {
                double instantRate = (previousHealth - health) * 1000.0D / elapsed;
                damagePerSecond = damagePerSecond == 0.0D
                        ? instantRate : damagePerSecond * 0.7D + instantRate * 0.3D;
            } else if (health > previousHealth) {
                damagePerSecond = 0.0D;
            }
            previousHealth = health;
            previousUpdate = now;
        }
        if (damagePerSecond < 0.05D) {
            return -1;
        }
        if (now - previousUpdate > 5_000L) {
            return -1;
        }
        return Math.min(5999, (int) Math.ceil(health / damagePerSecond));
    }

    void reset() {
        team = null;
        previousHealth = -1;
        previousUpdate = 0L;
        damagePerSecond = 0.0D;
    }

}
