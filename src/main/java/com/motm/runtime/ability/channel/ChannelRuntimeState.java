package com.motm.runtime.ability.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class ChannelRuntimeState {
    private final List<ActiveChannel> activeChannels = new ArrayList<>();
    private final List<ActiveLineControl> activeLineControls = new ArrayList<>();

    public void replaceChannelForPlayer(String playerId, ActiveChannel channel) {
        removeChannelsForPlayer(playerId);
        if (channel != null) {
            activeChannels.add(channel);
        }
    }

    public void replaceLineControlForPlayer(String playerId, ActiveLineControl lineControl) {
        removeLineControlsForPlayer(playerId);
        if (lineControl != null) {
            activeLineControls.add(lineControl);
        }
    }

    public int activeChannelCount() {
        return activeChannels.size();
    }

    public int activeLineControlCount() {
        return activeLineControls.size();
    }

    public void removeProcessedChannels(Predicate<ActiveChannel> processor) {
        if (processor != null) {
            activeChannels.removeIf(processor);
        }
    }

    public void removeProcessedLineControls(Predicate<ActiveLineControl> processor) {
        if (processor != null) {
            activeLineControls.removeIf(processor);
        }
    }

    public int removeChannelsForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int before = activeChannels.size();
        activeChannels.removeIf(channel -> channel != null && playerId.equals(channel.ownerPlayerId()));
        return before - activeChannels.size();
    }

    public int removeLineControlsForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int before = activeLineControls.size();
        activeLineControls.removeIf(lineControl -> lineControl != null && playerId.equals(lineControl.ownerPlayerId()));
        return before - activeLineControls.size();
    }

    public int removeChannelsForAbility(String playerId, String normalizedAbilityId) {
        if (playerId == null || playerId.isBlank()
                || normalizedAbilityId == null || normalizedAbilityId.isBlank()) {
            return 0;
        }
        int before = activeChannels.size();
        activeChannels.removeIf(channel -> channel != null
                && playerId.equals(channel.ownerPlayerId())
                && normalizedAbilityId.equals(normalize(channel.ability() == null ? null : channel.ability().getId())));
        return before - activeChannels.size();
    }

    public int removeLineControlsForAbility(String playerId, String normalizedAbilityId) {
        if (playerId == null || playerId.isBlank()
                || normalizedAbilityId == null || normalizedAbilityId.isBlank()) {
            return 0;
        }
        int before = activeLineControls.size();
        activeLineControls.removeIf(lineControl -> lineControl != null
                && playerId.equals(lineControl.ownerPlayerId())
                && normalizedAbilityId.equals(normalize(lineControl.ability() == null ? null : lineControl.ability().getId())));
        return before - activeLineControls.size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
