package com.motm.runtime.ability;

import com.motm.content.ability.AbilityShape;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;

/**
 * Narrow context object for future ability-family runtimes.
 *
 * Keep this intentionally small while migrating behavior out of
 * GameplayPlaybackManager. Hytale store/player references can be added when a
 * family runtime takes ownership of real execution.
 */
public record AbilityRuntimeContext(
        PlayerData player,
        StyleData style,
        AbilityData ability,
        AbilityShape shape,
        String traceId
) {
}
