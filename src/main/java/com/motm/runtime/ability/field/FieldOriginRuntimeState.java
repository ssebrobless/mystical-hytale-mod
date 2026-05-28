package com.motm.runtime.ability.field;

import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

public final class FieldOriginRuntimeState {
    private static final long IRON_WALL_WINDOW_MILLIS = 4_000L;
    private static final long CASTER_CENTERED_WINDOW_MILLIS = 10_000L;
    private static final double MAX_PLAUSIBLE_JUMP_DISTANCE = 24.0;

    private final Map<String, RecentFieldOrigin> recentIronWallOriginByPlayer = new HashMap<>();
    private final Map<String, RecentFieldOrigin> recentCasterCenteredOriginByPlayer = new HashMap<>();

    public StableOrigin resolveIronWallOrigin(String playerId, Vector3d origin, long now) {
        return resolveOrigin(recentIronWallOriginByPlayer, playerId, origin, now, IRON_WALL_WINDOW_MILLIS);
    }

    public StableOrigin resolveCasterCenteredOrigin(String playerId, Vector3d origin, long now) {
        return resolveOrigin(recentCasterCenteredOriginByPlayer, playerId, origin, now, CASTER_CENTERED_WINDOW_MILLIS);
    }

    public void clearCasterCenteredOrigin(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            recentCasterCenteredOriginByPlayer.remove(playerId);
        }
    }

    int ironWallOriginCount() {
        return recentIronWallOriginByPlayer.size();
    }

    int casterCenteredOriginCount() {
        return recentCasterCenteredOriginByPlayer.size();
    }

    private StableOrigin resolveOrigin(Map<String, RecentFieldOrigin> origins,
                                       String playerId,
                                       Vector3d origin,
                                       long now,
                                       long windowMillis) {
        if (playerId == null || playerId.isBlank() || origin == null) {
            return StableOrigin.current(origin);
        }

        RecentFieldOrigin previous = origins.get(playerId);
        if (previous != null
                && previous.withinWindow(now, windowMillis)
                && distance(previous.position(), origin) > MAX_PLAUSIBLE_JUMP_DISTANCE) {
            return StableOrigin.reused(previous.position(), previous, origin);
        }

        origins.put(playerId, new RecentFieldOrigin(origin, now));
        return StableOrigin.current(origin);
    }

    private static double distance(Vector3d first, Vector3d second) {
        if (first == null || second == null) {
            return Double.MAX_VALUE;
        }
        double dx = first.x - second.x;
        double dy = first.y - second.y;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public record StableOrigin(Vector3d origin,
                               RecentFieldOrigin previous,
                               Vector3d rejectedOrigin,
                               boolean reusedPrevious) {
        public StableOrigin {
            origin = origin == null ? null : new Vector3d(origin);
            rejectedOrigin = rejectedOrigin == null ? null : new Vector3d(rejectedOrigin);
        }

        static StableOrigin current(Vector3d origin) {
            return new StableOrigin(origin, null, null, false);
        }

        static StableOrigin reused(Vector3d origin, RecentFieldOrigin previous, Vector3d rejectedOrigin) {
            return new StableOrigin(origin, previous, rejectedOrigin, true);
        }
    }
}
