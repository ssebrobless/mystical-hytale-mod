package com.motm.runtime.ability.followup;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.ability.transformation.ActiveTransformation;

public final class WeaponFollowUpHytaleAdapter {
    private final WeaponFollowUpNativeAlloyRuntime nativeAlloyRuntime;
    private final Support support;

    public WeaponFollowUpHytaleAdapter(WeaponFollowUpNativeAlloyRuntime nativeAlloyRuntime,
                                       Support support) {
        this.nativeAlloyRuntime = nativeAlloyRuntime;
        this.support = support;
    }

    public void applyPrimaryHit(ActiveWeaponFollowUp followUp,
                                ActiveTransformation form,
                                double resolvedDamage,
                                PlayerData player,
                                Ref<EntityStore> playerRef,
                                Ref<EntityStore> targetRef,
                                Store<EntityStore> store,
                                String targetEntityId) {
        WeaponFollowUpPrimaryHitRuntime.applyPrimaryHit(followUp, resolvedDamage, new WeaponFollowUpPrimaryHitRuntime.PrimaryHitHooks() {
            @Override
            public void applyDamage(double damage) {
                Damage primary = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) damage);
                DamageSystems.executeDamage(targetRef, store, primary);
            }

            @Override
            public void applyPostDamage(double damage) {
                support.applyPostDamageClassPassives(player, playerRef, targetEntityId, damage, false);
                player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
            }

            @Override
            public void applyLifesteal(double damage) {
                support.applyLifesteal(playerRef, player.getPlayerId(), damage);
            }

            @Override
            public void applyImpact() {
                AbilityData ability = followUp != null ? followUp.sourceAbility() : (form != null ? form.sourceAbility() : null);
                support.applyEffectById(targetRef, store,
                        support.resolveImpactEffectId(player.getPlayerClass(), support.currentStyleId(player), ability));
            }

            @Override
            public void applyRiderToken(String token) {
                support.applyTargetToken(token, targetRef, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
            }

            @Override
            public void applySecondaryRiderToken(String token) {
                support.applyTargetToken(token, targetRef, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
            }
        });
    }

    public void applyPayoffs(Ref<EntityStore> playerRef,
                             PlayerData player,
                             Ref<EntityStore> targetRef,
                             Store<EntityStore> store,
                             ActiveWeaponFollowUp followUp,
                             double resolvedDamage) {
        WeaponFollowUpHitEffects.applyPayoffs(followUp, resolvedDamage, new WeaponFollowUpHitEffects.PayoffHooks() {
            @Override
            public void applyShield(double shieldPercent) {
                support.applyShield(player.getPlayerId(), playerRef, store, followUp.sourceAbility(), shieldPercent);
            }

            @Override
            public void heal(double amount) {
                support.healEntityFlat(playerRef, store, amount);
            }

            @Override
            public void applySplash() {
                WeaponFollowUpHytaleAdapter.this.applySplash(playerRef, player, targetRef, store, followUp, resolvedDamage);
            }
        });
    }

    public String handleNativeWeaponDamage(Player runtimePlayer,
                                           PlayerData player,
                                           Ref<EntityStore> playerRef,
                                           Ref<EntityStore> targetRef,
                                           Store<EntityStore> store,
                                           ActiveWeaponFollowUp followUp,
                                           String itemId,
                                           Damage damage) {
        if (nativeAlloyRuntime == null) {
            return null;
        }
        return nativeAlloyRuntime.applyNativeDamage(
                followUp,
                itemId,
                damage.getAmount(),
                new NativeHooks(runtimePlayer, player, playerRef, targetRef, store, followUp, damage)
        );
    }

    public String handleToolUse(Player runtimePlayer,
                                Ref<EntityStore> playerRef,
                                Store<EntityStore> store,
                                ActiveWeaponFollowUp followUp,
                                String itemId) {
        if (nativeAlloyRuntime == null) {
            return null;
        }
        return nativeAlloyRuntime.applyToolUse(
                followUp,
                itemId,
                new NativeHooks(runtimePlayer, null, playerRef, null, store, followUp, null)
        );
    }

    public void applyAlloyHeldItemVisual(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (playerRef == null || store == null) {
            return;
        }
        WeaponFollowUpVisualEffects.applyAlloyVisual(new WeaponFollowUpVisualEffects.VisualHooks() {
            @Override
            public void applyEffect(String effectId) {
                support.applyEffectById(playerRef, store, effectId);
            }

            @Override
            public boolean removeEffect(String effectId) {
                return false;
            }
        });
    }

    public void clearAlloyHeldItemVisual(Player runtimePlayer) {
        if (runtimePlayer == null) {
            return;
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        clearAlloyHeldItemVisual(playerRef, store);
    }

    public void clearAlloyHeldItemVisual(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (playerRef == null || store == null) {
            return;
        }
        boolean removed = WeaponFollowUpVisualEffects.clearAlloyVisual(new WeaponFollowUpVisualEffects.VisualHooks() {
            @Override
            public void applyEffect(String effectId) {
            }

            @Override
            public boolean removeEffect(String effectId) {
                return support.removeEffectById(playerRef, store, effectId);
            }
        });
        if (removed) {
            support.logInfo("[MOTM] Alloy Enhancement visual cleared.");
        }
    }

    private void applySplash(Ref<EntityStore> playerRef,
                             PlayerData player,
                             Ref<EntityStore> primaryTargetRef,
                             Store<EntityStore> store,
                             ActiveWeaponFollowUp followUp,
                             double resolvedDamage) {
        Vector3d center = support.position(primaryTargetRef, store);
        if (center == null) {
            return;
        }

        WeaponFollowUpSplashRuntime.applySplash(
                followUp,
                resolvedDamage,
                new WeaponFollowUpSplashRuntime.SplashHooks<Ref<EntityStore>>() {
                    @Override
                    public Iterable<Ref<EntityStore>> collectTargets(double radius, int maxTargets) {
                        return support.collectNearbyNpcTargets(store, center, radius, maxTargets);
                    }

                    @Override
                    public boolean isValidTarget(Ref<EntityStore> target) {
                        return target != null && target.isValid();
                    }

                    @Override
                    public boolean isPrimaryTarget(Ref<EntityStore> target) {
                        return target.equals(primaryTargetRef);
                    }

                    @Override
                    public String entityId(Ref<EntityStore> target) {
                        return support.resolveEntityId(target, store);
                    }

                    @Override
                    public double incomingMultiplier(String entityId) {
                        return support.incomingDamageMultiplier(entityId);
                    }

                    @Override
                    public double absorbDamage(String entityId, double damage) {
                        return support.absorbDamage(entityId, damage);
                    }

                    @Override
                    public void applyDamage(Ref<EntityStore> target, double damage) {
                        Damage splash = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) damage);
                        DamageSystems.executeDamage(target, store, splash);
                    }

                    @Override
                    public void applyPostDamage(Ref<EntityStore> target, String entityId, double damage) {
                        support.applyPostDamageClassPassives(player, playerRef, entityId, damage, false);
                        player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
                    }

                    @Override
                    public void applySecondaryRiderToken(Ref<EntityStore> target, String token) {
                        support.applyTargetToken(token, target, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
                    }

                    @Override
                    public void applyImpact(Ref<EntityStore> target) {
                        support.applyEffectById(target, store, support.resolveImpactEffectId(
                                player.getPlayerClass(),
                                support.currentStyleId(player),
                                followUp.sourceAbility()
                        ));
                    }
                }
        );
    }

    private final class NativeHooks implements WeaponFollowUpNativeAlloyRuntime.Hooks {
        private final Player runtimePlayer;
        private final PlayerData player;
        private final Ref<EntityStore> playerRef;
        private final Ref<EntityStore> targetRef;
        private final Store<EntityStore> store;
        private final ActiveWeaponFollowUp followUp;
        private final Damage damage;

        private NativeHooks(Player runtimePlayer,
                            PlayerData player,
                            Ref<EntityStore> playerRef,
                            Ref<EntityStore> targetRef,
                            Store<EntityStore> store,
                            ActiveWeaponFollowUp followUp,
                            Damage damage) {
            this.runtimePlayer = runtimePlayer;
            this.player = player;
            this.playerRef = playerRef;
            this.targetRef = targetRef;
            this.store = store;
            this.followUp = followUp;
            this.damage = damage;
        }

        @Override
        public void applyVisual() {
            applyAlloyHeldItemVisual(playerRef, store);
        }

        @Override
        public void clearVisual() {
            if (playerRef != null && store != null) {
                clearAlloyHeldItemVisual(playerRef, store);
            } else {
                clearAlloyHeldItemVisual(runtimePlayer);
            }
        }

        @Override
        public void setDamage(float amount) {
            if (damage != null) {
                damage.setAmount(amount);
            }
        }

        @Override
        public void applyImpact() {
            if (player == null || targetRef == null || store == null || followUp == null) {
                return;
            }
            support.applyEffectById(targetRef, store, support.resolveImpactEffectId(
                    player.getPlayerClass(),
                    support.currentStyleId(player),
                    followUp.sourceAbility()
            ));
        }

        @Override
        public void applySecondaryRiderToken(String token) {
            if (player == null || targetRef == null || store == null || followUp == null) {
                return;
            }
            support.applyTargetToken(token, targetRef, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
        }

        @Override
        public boolean restoreDurability(String itemId) {
            return support.restoreHeldItemDurability(runtimePlayer, itemId);
        }

        @Override
        public void removeFollowUp(String playerId) {
            support.removeFollowUp(playerId);
        }

        @Override
        public void logNativeDamage(String message, String playerId, String itemId) {
            support.logInfo(message + " playerId=" + playerId + " item=" + itemId);
        }

        @Override
        public void logBound(String playerId, String itemId, int remainingUses) {
            support.logInfo("[MOTM] Alloy Enhancement bound: playerId=" + playerId
                    + " item=" + itemId
                    + " uses=" + remainingUses);
        }

        @Override
        public void logRejected(String message, String playerId) {
            support.logInfo(message + " playerId=" + playerId);
        }
    }

    public interface Support {
        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        boolean removeEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        String currentStyleId(PlayerData player);

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

        double healEntityFlat(Ref<EntityStore> targetRef, Store<EntityStore> store, double amount);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void applyPostDamageClassPassives(PlayerData player,
                                          Ref<EntityStore> ownerRef,
                                          String targetEntityId,
                                          double damage,
                                          boolean abilityDamage);

        void applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt);

        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        Iterable<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                           Vector3d center,
                                                           double radius,
                                                           int maxTargets);

        boolean restoreHeldItemDurability(Player runtimePlayer, String itemId);

        void removeFollowUp(String playerId);

        void logInfo(String message);
    }
}
