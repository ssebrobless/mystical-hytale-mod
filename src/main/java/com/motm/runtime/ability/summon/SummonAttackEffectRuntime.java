package com.motm.runtime.ability.summon;

import java.util.Locale;

public final class SummonAttackEffectRuntime {

    public <T> void applyAttackEffects(ActiveSummon summon, T primaryTarget, long now, Hooks<T> hooks) {
        if (summon == null || primaryTarget == null || hooks == null) {
            return;
        }

        hooks.applyToken(primaryTarget, summon.attackToken());

        if ("tank".equals(summon.role())) {
            hooks.applySummonShield(4.0);
            hooks.pullTargetTowardSummon(primaryTarget, 1.0, 0.55, 0.0);
        }

        if (summon.nowWithinBuffWindow(now)
                && ("swarm".equals(summon.role()) || "hatchling".equals(summon.role()))) {
            hooks.applyToken(primaryTarget, "dot");
        }

        switch (summonName(summon)) {
            case "skeleton_minion" -> hooks.applyToken(primaryTarget, "dot");
            case "snow_imp" -> {
                hooks.applyToken(primaryTarget, "attack_slow");
                hooks.applySplashToken(primaryTarget, "slow", 2.6, 1);
            }
            case "frosty_golem" -> {
                hooks.applySplashToken(primaryTarget, "slow", 3.4, 2);
                hooks.applySplashToken(primaryTarget, "root", 2.0, 1);
            }
            case "swamp_monster" -> {
                hooks.applySplashToken(primaryTarget, "dot", 3.2, 2);
                hooks.applySplashToken(primaryTarget, "slow", 3.2, 2);
            }
            case "treant_sapling" -> {
                hooks.applySplashToken(primaryTarget, "root", 2.8, 2);
                hooks.applyOwnerShield(4.5);
            }
            case "void_spawn" -> {
                hooks.applySplashToken(primaryTarget, "vulnerability", 3.6, 2);
                hooks.applySplashDamage(primaryTarget, 0.35, 3.4, 2);
            }
            case "scarak_egg" -> {
                if (summon.awakened()) {
                    hooks.applyToken(primaryTarget, "vulnerability");
                    hooks.applySplashToken(primaryTarget, "dot", 2.8, 2);
                }
            }
            case "locust_queen" -> {
                hooks.applySplashToken(primaryTarget, "dot", 3.8, 3);
                if (summon.nowWithinBuffWindow(now)) {
                    hooks.applySplashToken(primaryTarget, "vulnerability", 3.8, 2);
                }
            }
            case "shadow_clone" -> hooks.applyToken(primaryTarget, "blind");
            default -> {
            }
        }
    }

    private static String summonName(ActiveSummon summon) {
        return summon.ability() == null || summon.ability().getSummonName() == null
                ? ""
                : summon.ability().getSummonName().toLowerCase(Locale.ROOT);
    }

    public interface Hooks<T> {
        void applyToken(T target, String token);

        void applySplashToken(T primaryTarget, String token, double radius, int maxTargets);

        void applySplashDamage(T primaryTarget, double damageRatio, double radius, int maxTargets);

        void applySummonShield(double shieldPercent);

        void applyOwnerShield(double shieldPercent);

        void pullTargetTowardSummon(T target, double pullForce, double liftForce, double maxY);
    }
}
