package com.motm.runtime.ability.stomp;

import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;

public record ArmedStomp(String playerId,
                         PlayerData player,
                         StyleData style,
                         AbilityData ability,
                         String traceId,
                         long armedAtMillis,
                         long expireAtMillis,
                         double previousY,
                         boolean wasAirborne) {

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }

    public ArmedStomp withObservation(double y, boolean nowAirborne) {
        return new ArmedStomp(
                playerId,
                player,
                style,
                ability,
                traceId,
                armedAtMillis,
                expireAtMillis,
                y,
                nowAirborne
        );
    }
}
