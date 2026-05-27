package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.util.AbilityPresentation;

import java.util.List;

public final class SummonAttackHytaleAdapter {
    private final SummonAttackRuntime attackRuntime;
    private final SummonAttackEffectRuntime attackEffectRuntime;
    private final SummonSplashRuntime splashRuntime;
    private final Support support;

    public SummonAttackHytaleAdapter(SummonAttackRuntime attackRuntime,
                                     SummonAttackEffectRuntime attackEffectRuntime,
                                     SummonSplashRuntime splashRuntime,
                                     Support support) {
        this.attackRuntime = attackRuntime;
        this.attackEffectRuntime = attackEffectRuntime;
        this.splashRuntime = splashRuntime;
        this.support = support;
    }

    public void performAttack(ActiveSummon summon,
                              PlayerData owner,
                              Ref<EntityStore> targetRef,
                              Store<EntityStore> store,
                              long now) {
        if (attackRuntime == null || summon == null || store == null) {
            return;
        }

        attackRuntime.performAttack(summon, targetRef, now, new SummonAttackRuntime.Hooks() {
            @Override
            public void moveCloneBesideTarget(Ref<EntityStore> targetRef) {
                support.moveCloneBesideTarget(summon, targetRef, store);
            }

            @Override
            public String targetEntityId(Ref<EntityStore> targetRef) {
                return support.resolveEntityId(targetRef, store);
            }

            @Override
            public double incomingDamageMultiplier(String targetEntityId) {
                return support.incomingDamageMultiplier(targetEntityId);
            }

            @Override
            public double absorbDamage(String targetEntityId, double damage) {
                return support.absorbDamage(targetEntityId, damage);
            }

            @Override
            public void applyDamage(Ref<EntityStore> targetRef, double damageAmount, boolean ranged) {
                DamageCause cause = ranged ? DamageCause.PROJECTILE : DamageCause.PHYSICAL;
                Damage damage = new Damage(new Damage.EntitySource(summon.ref()), cause, (float) damageAmount);
                DamageSystems.executeDamage(targetRef, store, damage);
            }

            @Override
            public void applyPostDamage(Ref<EntityStore> targetRef, String targetEntityId, double damage) {
                support.applyPostDamageClassPassives(owner, summon.ownerRef(), targetEntityId, damage, true);
                if (owner != null) {
                    owner.getStatistics().setTotalDamageDealt(owner.getStatistics().getTotalDamageDealt() + damage);
                }
            }

            @Override
            public void applyLifesteal(double damage) {
                if (owner != null) {
                    support.applyLifesteal(summon.ownerRef(), owner.getPlayerId(), damage);
                }
            }

            @Override
            public void applyImpact(Ref<EntityStore> targetRef) {
                support.applyEffectById(targetRef, store,
                        support.resolveImpactEffectId(summon.classId(), summon.styleId(), summon.ability()));
            }

            @Override
            public void applyAttackEffects(Ref<EntityStore> targetRef, long now) {
                SummonAttackHytaleAdapter.this.applyAttackEffects(summon, owner, targetRef, store, now);
            }

            @Override
            public void logResolved(String targetEntityId, double resolvedDamage) {
                support.logInfo("[MOTM] Summon attack resolved: abilityId=" + summon.ability().getId()
                        + " summonRole=" + summon.role()
                        + " target=" + (targetEntityId == null ? "<unknown>" : targetEntityId)
                        + " damage=" + AbilityPresentation.formatDecimal(resolvedDamage));
            }
        });
    }

    private void applyAttackEffects(ActiveSummon summon,
                                    PlayerData owner,
                                    Ref<EntityStore> targetRef,
                                    Store<EntityStore> store,
                                    long now) {
        if (attackEffectRuntime == null) {
            return;
        }

        attackEffectRuntime.applyAttackEffects(summon, targetRef, now, new SummonAttackEffectRuntime.Hooks<>() {
            @Override
            public void applyToken(Ref<EntityStore> target, String token) {
                support.applyTargetToken(token, target, store, summon.ref(), summon.ownerPlayerId(), summon.ability());
            }

            @Override
            public void applySplashToken(Ref<EntityStore> primaryTarget, String token, double radius, int maxTargets) {
                SummonAttackHytaleAdapter.this.applySplashToken(summon, primaryTarget, store, token, radius, maxTargets);
            }

            @Override
            public void applySplashDamage(Ref<EntityStore> primaryTarget, double damageRatio, double radius, int maxTargets) {
                SummonAttackHytaleAdapter.this.applySplashDamage(summon, owner, primaryTarget, store, damageRatio, radius, maxTargets);
            }

            @Override
            public void applySummonShield(double shieldPercent) {
                String summonEntityId = support.resolveEntityId(summon.ref(), store);
                if (summonEntityId != null) {
                    support.applyShield(summonEntityId, summon.ref(), store, summon.ability(), shieldPercent);
                }
            }

            @Override
            public void applyOwnerShield(double shieldPercent) {
                if (owner != null && summon.ownerRef() != null && summon.ownerRef().isValid()) {
                    support.applyShield(owner.getPlayerId(), summon.ownerRef(), store, summon.ability(), shieldPercent);
                }
            }

            @Override
            public void pullTargetTowardSummon(Ref<EntityStore> target, double pullForce, double liftForce, double maxY) {
                Vector3d summonPosition = support.position(summon.ref(), store);
                if (summonPosition != null) {
                    support.applyPullTowardsPoint(target, store, summonPosition, summon.ability(), pullForce, liftForce, maxY);
                }
            }
        });
    }

    private void applySplashToken(ActiveSummon summon,
                                  Ref<EntityStore> primaryTargetRef,
                                  Store<EntityStore> store,
                                  String token,
                                  double radius,
                                  int maxTargets) {
        if (splashRuntime == null) {
            return;
        }
        splashRuntime.applySplashToken(primaryTargetRef, token, radius, maxTargets, new SummonSplashRuntime.TokenHooks<>() {
            @Override
            public List<Ref<EntityStore>> collectNearbyTargets(Ref<EntityStore> primaryTarget, double radius, int maxCandidates) {
                Vector3d center = support.position(primaryTarget, store);
                return center == null ? List.of() : support.collectNearbyNpcTargets(store, center, radius, maxCandidates);
            }

            @Override
            public void applyToken(Ref<EntityStore> target, String token) {
                if (target != null && target.isValid()) {
                    support.applyTargetToken(token, target, store, summon.ref(), summon.ownerPlayerId(), summon.ability());
                }
            }
        });
    }

    private void applySplashDamage(ActiveSummon summon,
                                   PlayerData owner,
                                   Ref<EntityStore> primaryTargetRef,
                                   Store<EntityStore> store,
                                   double damageRatio,
                                   double radius,
                                   int maxTargets) {
        if (splashRuntime == null) {
            return;
        }
        splashRuntime.applySplashDamage(summon, primaryTargetRef, damageRatio, radius, maxTargets, new SummonSplashRuntime.DamageHooks<>() {
            @Override
            public List<Ref<EntityStore>> collectNearbyTargets(Ref<EntityStore> primaryTarget, double radius, int maxCandidates) {
                Vector3d center = support.position(primaryTarget, store);
                return center == null ? List.of() : support.collectNearbyNpcTargets(store, center, radius, maxCandidates);
            }

            @Override
            public String targetEntityId(Ref<EntityStore> target) {
                return support.resolveEntityId(target, store);
            }

            @Override
            public double incomingDamageMultiplier(String targetEntityId) {
                return support.incomingDamageMultiplier(targetEntityId);
            }

            @Override
            public double absorbDamage(String targetEntityId, double damage) {
                return support.absorbDamage(targetEntityId, damage);
            }

            @Override
            public void applyDamage(Ref<EntityStore> target, double damageAmount, boolean ranged) {
                Damage splash = new Damage(new Damage.EntitySource(summon.ref()),
                        ranged ? DamageCause.PROJECTILE : DamageCause.PHYSICAL,
                        (float) damageAmount);
                DamageSystems.executeDamage(target, store, splash);
            }

            @Override
            public void applyPostDamage(Ref<EntityStore> target, String targetEntityId, double damage) {
                support.applyPostDamageClassPassives(owner, summon.ownerRef(), targetEntityId, damage, true);
                if (owner != null) {
                    owner.getStatistics().setTotalDamageDealt(owner.getStatistics().getTotalDamageDealt() + damage);
                }
            }

            @Override
            public void applyImpact(Ref<EntityStore> target) {
                support.applyEffectById(target, store,
                        support.resolveImpactEffectId(summon.classId(), summon.styleId(), summon.ability()));
            }
        });
    }

    public interface Support {
        void moveCloneBesideTarget(ActiveSummon summon,
                                   Ref<EntityStore> targetRef,
                                   Store<EntityStore> store);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void applyPostDamageClassPassives(PlayerData owner,
                                          Ref<EntityStore> ownerRef,
                                          String targetEntityId,
                                          double damage,
                                          boolean abilityDamage);

        void applyLifesteal(Ref<EntityStore> ownerRef, String ownerPlayerId, double damage);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        boolean applyTargetToken(String token,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> sourceRef,
                                 String sourcePlayerId,
                                 AbilityData ability);

        double applyShield(String entityId,
                           Ref<EntityStore> entityRef,
                           Store<EntityStore> store,
                           AbilityData ability,
                           double shieldPercent);

        boolean applyPullTowardsPoint(Ref<EntityStore> targetRef,
                                      Store<EntityStore> store,
                                      Vector3d point,
                                      AbilityData ability,
                                      double pullForce,
                                      double liftForce,
                                      double maxY);

        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        List<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                       Vector3d center,
                                                       double radius,
                                                       int maxTargets);

        void logInfo(String message);
    }
}
