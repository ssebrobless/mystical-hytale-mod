package com.motm.runtime.state;

import java.util.List;

/**
 * Sequenced live style-test run for one player.
 */
public record ActiveStyleTest(
        String playerId,
        String classId,
        String styleId,
        String styleName,
        List<String> abilityIds,
        int nextAbilityIndex,
        long nextActionAtMs
) {
    public ActiveStyleTest advance(long nextActionAtMs) {
        return new ActiveStyleTest(
                playerId,
                classId,
                styleId,
                styleName,
                abilityIds,
                nextAbilityIndex + 1,
                nextActionAtMs
        );
    }
}
