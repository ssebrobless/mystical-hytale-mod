package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class SummonAttackRuntime {
    private static final double BUFFED_DAMAGE_MULTIPLIER = 1.35;

    public Result performAttack(ActiveSummon summon,
                                Ref<EntityStore> targetRef,
                                long now,
                                Hooks hooks) {
        if (summon == null || targetRef == null || !targetRef.isValid() || hooks == null) {
            return Result.none();
        }

        if ("clone".equals(summon.role())) {
            hooks.moveCloneBesideTarget(targetRef);
        }

        String targetEntityId = hooks.targetEntityId(targetRef);
        double resolvedDamage = summon.baseDamage();
        if (summon.nowWithinBuffWindow(now)) {
            resolvedDamage *= BUFFED_DAMAGE_MULTIPLIER;
        }
        if (targetEntityId != null) {
            resolvedDamage *= hooks.incomingDamageMultiplier(targetEntityId);
            resolvedDamage = hooks.absorbDamage(targetEntityId, resolvedDamage);
        }

        if (resolvedDamage > 0.0) {
            hooks.applyDamage(targetRef, resolvedDamage, summon.ranged());
            hooks.applyPostDamage(targetRef, targetEntityId, resolvedDamage);
            hooks.applyLifesteal(resolvedDamage);
        }

        hooks.applyImpact(targetRef);
        hooks.applyAttackEffects(targetRef, now);
        summon.scheduleNextAttack(now);
        hooks.logResolved(targetEntityId, resolvedDamage);
        summon.expireCloneAfterStrike(now);
        return new Result(true, targetEntityId, resolvedDamage);
    }

    public record Result(boolean attacked, String targetEntityId, double resolvedDamage) {
        public static Result none() {
            return new Result(false, null, 0.0);
        }
    }

    public interface Hooks {
        void moveCloneBesideTarget(Ref<EntityStore> targetRef);

        String targetEntityId(Ref<EntityStore> targetRef);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void applyDamage(Ref<EntityStore> targetRef, double damage, boolean ranged);

        void applyPostDamage(Ref<EntityStore> targetRef, String targetEntityId, double damage);

        void applyLifesteal(double damage);

        void applyImpact(Ref<EntityStore> targetRef);

        void applyAttackEffects(Ref<EntityStore> targetRef, long now);

        void logResolved(String targetEntityId, double resolvedDamage);
    }
}
