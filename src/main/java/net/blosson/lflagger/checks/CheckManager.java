package net.blosson.lflagger.checks;

import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.List;

public class CheckManager {

    private final List<Check> checks = new ArrayList<>();
    private final NoFallCheck noFallCheck;
    private final FlyCheck flyCheck;
    private final SpeedCheck speedCheck;
    private final StrafeCheck strafeCheck;
    private final AntiKnockbackCheck antiKnockbackCheck;

    public CheckManager() {
        // Initialize all checks
        this.noFallCheck = new NoFallCheck();
        this.flyCheck = new FlyCheck();
        this.speedCheck = new SpeedCheck();
        this.strafeCheck = new StrafeCheck();
        this.antiKnockbackCheck = new AntiKnockbackCheck();

        // Add checks to the list
        checks.add(noFallCheck);
        checks.add(flyCheck);
        checks.add(speedCheck);
        checks.add(strafeCheck);
        checks.add(antiKnockbackCheck);
    }

    /**
     * Runs all registered checks for a given player.
     * This method will be called for every player each tick.
     * @param player The player to check.
     */
    public void tick(PlayerEntity player) {
        // Tick all checks
        noFallCheck.tick(player);
        flyCheck.tick(player);
        speedCheck.tick(player);
        strafeCheck.tick(player);
        antiKnockbackCheck.tick(player);
    }
}