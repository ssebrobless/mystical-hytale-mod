package com.motm.runtime.ability.projectile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ProjectileRuntimeState {
    private final List<ActiveProjectile> activeProjectiles = new ArrayList<>();

    public void addProjectile(ActiveProjectile projectile) {
        if (projectile != null) {
            activeProjectiles.add(projectile);
        }
    }

    public int addProjectiles(Collection<ActiveProjectile> projectiles) {
        if (projectiles == null || projectiles.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (ActiveProjectile projectile : projectiles) {
            if (projectile != null) {
                activeProjectiles.add(projectile);
                added++;
            }
        }
        return added;
    }

    public int activeProjectileCount() {
        return activeProjectiles.size();
    }

    public void removeProcessedProjectiles(Predicate<ActiveProjectile> processor) {
        if (processor != null) {
            activeProjectiles.removeIf(processor);
        }
    }

    public int removeProjectilesForPlayer(String playerId, Consumer<ActiveProjectile> cleanup) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int[] removed = {0};
        activeProjectiles.removeIf(projectile -> {
            if (projectile == null || !playerId.equals(projectile.ownerPlayerId())) {
                return false;
            }
            if (cleanup != null) {
                cleanup.accept(projectile);
            }
            removed[0]++;
            return true;
        });
        return removed[0];
    }
}
