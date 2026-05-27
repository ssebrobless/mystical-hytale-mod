package com.motm.runtime.ability.summon;

import java.util.List;

public final class SummonSplashRuntime {

    public <T> void applySplashToken(T primaryTarget, String token, double radius, int maxTargets, TokenHooks<T> hooks) {
        if (primaryTarget == null || hooks == null) {
            return;
        }

        for (T splashTarget : nearbyTargets(primaryTarget, radius, maxTargets, hooks)) {
            if (splashTarget == null || splashTarget.equals(primaryTarget)) {
                continue;
            }
            hooks.applyToken(splashTarget, token);
        }
    }

    public <T> void applySplashDamage(ActiveSummon summon,
                                      T primaryTarget,
                                      double damageRatio,
                                      double radius,
                                      int maxTargets,
                                      DamageHooks<T> hooks) {
        if (summon == null || primaryTarget == null || hooks == null || damageRatio <= 0.0) {
            return;
        }

        for (T splashTarget : nearbyTargets(primaryTarget, radius, maxTargets, hooks)) {
            if (splashTarget == null || splashTarget.equals(primaryTarget)) {
                continue;
            }

            String targetEntityId = hooks.targetEntityId(splashTarget);
            double damageAmount = summon.baseDamage() * damageRatio;
            if (targetEntityId != null) {
                damageAmount *= hooks.incomingDamageMultiplier(targetEntityId);
                damageAmount = hooks.absorbDamage(targetEntityId, damageAmount);
            }
            if (damageAmount <= 0.0) {
                continue;
            }

            hooks.applyDamage(splashTarget, damageAmount, summon.ranged());
            hooks.applyPostDamage(splashTarget, targetEntityId, damageAmount);
            hooks.applyImpact(splashTarget);
        }
    }

    private static <T> List<T> nearbyTargets(T primaryTarget, double radius, int maxTargets, TargetCollector<T> hooks) {
        List<T> targets = hooks.collectNearbyTargets(primaryTarget, radius, maxTargets + 1);
        return targets == null ? List.of() : targets;
    }

    public interface TargetCollector<T> {
        List<T> collectNearbyTargets(T primaryTarget, double radius, int maxCandidates);
    }

    public interface TokenHooks<T> extends TargetCollector<T> {
        void applyToken(T target, String token);
    }

    public interface DamageHooks<T> extends TargetCollector<T> {
        String targetEntityId(T target);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void applyDamage(T target, double damageAmount, boolean ranged);

        void applyPostDamage(T target, String targetEntityId, double damage);

        void applyImpact(T target);
    }
}
