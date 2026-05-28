package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class TerrainRuntimeState {
    private final List<TemporaryTerrainSelection> activeSelections = new ArrayList<>();
    private final List<ActiveMovingTerrainTrail> movingTrails = new ArrayList<>();
    private final List<ActiveStackingColumn> stackingColumns = new ArrayList<>();

    public void addSelection(TemporaryTerrainSelection selection) {
        if (selection != null) {
            activeSelections.add(selection);
        }
    }

    public void addMovingTrail(ActiveMovingTerrainTrail trail) {
        if (trail != null) {
            movingTrails.add(trail);
        }
    }

    public void addStackingColumn(ActiveStackingColumn column) {
        if (column != null) {
            stackingColumns.add(column);
        }
    }

    public int activeSelectionCount() {
        return activeSelections.size();
    }

    public int movingTrailCount() {
        return movingTrails.size();
    }

    public int stackingColumnCount() {
        return stackingColumns.size();
    }

    public void removeProcessedSelections(Predicate<TemporaryTerrainSelection> processor) {
        if (processor != null) {
            activeSelections.removeIf(processor);
        }
    }

    public void removeProcessedMovingTrails(Predicate<ActiveMovingTerrainTrail> processor) {
        if (processor != null) {
            movingTrails.removeIf(processor);
        }
    }

    public void removeProcessedStackingColumns(Predicate<ActiveStackingColumn> processor) {
        if (processor != null) {
            stackingColumns.removeIf(processor);
        }
    }

    public int removeMovingTrailsForWorld(World world) {
        if (world == null) {
            return 0;
        }
        int before = movingTrails.size();
        movingTrails.removeIf(trail -> trail == null || sameWorld(trail.world(), world));
        return before - movingTrails.size();
    }

    public int removeStackingColumnsForWorld(World world) {
        if (world == null) {
            return 0;
        }
        int before = stackingColumns.size();
        stackingColumns.removeIf(column -> column != null && column.belongsTo(world));
        return before - stackingColumns.size();
    }

    public int restoreSelectionsForWorld(World world, Predicate<TemporaryTerrainSelection> restorer) {
        if (world == null || restorer == null) {
            return 0;
        }
        int[] restored = {0};
        activeSelections.removeIf(selection -> {
            if (selection == null || !selection.belongsTo(world)) {
                return false;
            }
            if (restorer.test(selection)) {
                restored[0]++;
            }
            return true;
        });
        return restored[0];
    }

    public void restoreSelectionsByReason(World world,
                                          String reason,
                                          Predicate<TemporaryTerrainSelection> restorer) {
        if (world == null || reason == null || reason.isBlank() || restorer == null) {
            return;
        }
        activeSelections.removeIf(selection -> {
            if (selection == null || !selection.matches(world, reason)) {
                return false;
            }
            restorer.test(selection);
            return true;
        });
    }

    private static boolean sameWorld(World first, World second) {
        return first != null && second != null && (first == second || first.equals(second));
    }
}
