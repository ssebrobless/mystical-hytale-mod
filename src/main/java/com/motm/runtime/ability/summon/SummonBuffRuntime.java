package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

import java.util.List;

public final class SummonBuffRuntime {
    private static final double DEFAULT_RADIUS = 12.0;
    private static final long COMMAND_ATTACK_DELAY_MS = 150L;

    public Result apply(List<ActiveSummon> summons,
                        AbilityData ability,
                        Vector3d origin,
                        long now,
                        Hooks hooks) {
        if (summons == null || summons.isEmpty()) {
            return new Result(0, 0, "no active summons");
        }
        if (ability == null || origin == null || hooks == null) {
            return Result.none();
        }

        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_RADIUS;
        long durationMillis = durationMillis(ability);
        int buffed = 0;
        int commanded = 0;

        for (ActiveSummon summon : summons) {
            if (summon == null || summon.ref() == null || !summon.ref().isValid()) {
                continue;
            }

            Vector3d position = hooks.position(summon);
            if (position == null || distance(origin, position) > radius) {
                continue;
            }

            hooks.applyBuffVisual(summon);
            summon.extend(durationMillis);
            summon.extendBuffUntil(now + durationMillis);
            summon.commandAttackSoon(now, COMMAND_ATTACK_DELAY_MS);

            if (summon.awakened() || now >= summon.hatchAtMillis()) {
                Ref<EntityStore> targetRef = hooks.resolveTarget(summon, now);
                if (targetRef != null && targetRef.isValid()) {
                    hooks.attack(summon, targetRef, now);
                    commanded++;
                }
            }
            buffed++;
        }

        if (buffed <= 0) {
            return new Result(0, 0, "no summons in range");
        }
        return new Result(
                buffed,
                commanded,
                "buffed " + buffed + " summon" + (buffed == 1 ? "" : "s")
                        + (commanded > 0 ? " | commanded " + commanded + " strike" + (commanded == 1 ? "" : "s") : "")
        );
    }

    private static long durationMillis(AbilityData ability) {
        return (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
    }

    private static double distance(Vector3d left, Vector3d right) {
        double dx = left.x - right.x;
        double dy = left.y - right.y;
        double dz = left.z - right.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    public record Result(int buffed, int commanded, String summary) {
        public static Result none() {
            return new Result(0, 0, "");
        }
    }

    public interface Hooks {
        Vector3d position(ActiveSummon summon);

        void applyBuffVisual(ActiveSummon summon);

        Ref<EntityStore> resolveTarget(ActiveSummon summon, long now);

        void attack(ActiveSummon summon, Ref<EntityStore> targetRef, long now);
    }
}
