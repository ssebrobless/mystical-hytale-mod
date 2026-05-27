package com.motm.runtime.ability.transformation;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;
import com.motm.util.HytaleAssetResolver;

public final class TransformationHytaleAdapter {
    private final TransformationRuntimeState transformationState;
    private final TransformationTickRuntime tickRuntime;
    private final TransformationEffectRuntime effectRuntime;
    private final long pulseIntervalMillis;
    private final Support support;

    public TransformationHytaleAdapter(TransformationRuntimeState transformationState,
                                       TransformationTickRuntime tickRuntime,
                                       TransformationEffectRuntime effectRuntime,
                                       long pulseIntervalMillis,
                                       Support support) {
        this.transformationState = transformationState;
        this.tickRuntime = tickRuntime;
        this.effectRuntime = effectRuntime;
        this.pulseIntervalMillis = pulseIntervalMillis;
        this.support = support;
    }

    public ActivationResult activate(Player runtimePlayer,
                                     PlayerData player,
                                     StyleData style,
                                     AbilityData ability) {
        if (!"transformation".equals(lower(ability != null ? ability.getCastType() : null))) {
            return ActivationResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer != null ? runtimePlayer.getReference() : null;
        if (playerRef == null || !playerRef.isValid()) {
            return ActivationResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        String effectId = TransformationRuntimeSpecs.visualEffectId(ability);
        if (effectId != null) {
            support.applyEffectById(playerRef, store, effectId);
        }

        String modelId = HytaleAssetResolver.resolveModelId(player.getPlayerClass(), style.getId(), ability);
        if (modelId == null || modelId.isBlank()) {
            modelId = ability.getName();
        }

        Vector3d origin = support.position(playerRef, store);
        ActiveTransformation form = createTransformationState(player.getPlayerId(), playerRef, ability, modelId, origin);
        transformationState.putTransformation(player.getPlayerId(), form, System.currentTimeMillis() + pulseIntervalMillis);

        return new ActivationResult(true, "form " + support.humanize(modelId) + " | " + form.summary());
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        transformationState.removeProcessedTransformations(transformation ->
                belongsToCurrentStore(transformation.ownerRef(), currentStore) && processTick(transformation, now));
    }

    public void applyWeaponRider(ActiveTransformation form,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> playerRef,
                                 String playerId) {
        if (form == null || form.weaponRiderToken() == null) {
            return;
        }
        support.applyTargetToken(form.weaponRiderToken(), targetRef, store, playerRef, playerId, form.sourceAbility());
    }

    public void applyWeaponImpact(ActiveTransformation form,
                                  PlayerData player,
                                  Ref<EntityStore> targetRef,
                                  Store<EntityStore> store,
                                  Ref<EntityStore> playerRef,
                                  double resolvedDamage) {
        if (form == null || player == null || targetRef == null || !targetRef.isValid()) {
            return;
        }

        switch (form.kind()) {
            case SMOKE -> {
                StatusEffect evasion = support.createStatusEffect("evasion", form.sourceAbility(), player.getPlayerId(), form.abilityId());
                if (evasion != null) {
                    support.applyOwnerStatusEffect(player.getPlayerId(), evasion);
                }
                support.applyTargetToken("blind", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
            }
            case PTERODACTYL -> {
                support.applyTargetToken("slow", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
                support.applyTargetToken("vulnerability", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
                support.applyKnockback(targetRef, store, playerRef, form.sourceAbility());
            }
            case TRICERATOPS -> {
                if (support.applyKnockbackCollidedWithWall(targetRef, store, playerRef, form.sourceAbility())) {
                    support.applyTargetToken("stun", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
                }
                if (resolvedDamage > 0.0) {
                    support.applyShield(player.getPlayerId(), playerRef, store, form.sourceAbility(), 6.0);
                }
            }
            case T_REX -> applyCleave(form, player, targetRef, store, playerRef, resolvedDamage * 0.45, "vulnerability");
            default -> {
            }
        }
    }

    private ActiveTransformation createTransformationState(String playerId,
                                                           Ref<EntityStore> ownerRef,
                                                           AbilityData ability,
                                                           String modelId,
                                                           Vector3d initialPosition) {
        long expireAt = System.currentTimeMillis() + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
        return ActiveTransformation.create(playerId, ownerRef, ability, modelId, expireAt, initialPosition);
    }

    private boolean processTick(ActiveTransformation form, long now) {
        return tickRuntime.process(form, now, pulseIntervalMillis, new TransformationTickRuntime.Hooks() {
            @Override
            public boolean hasOwnerStore(ActiveTransformation form) {
                return form.ownerRef() != null && form.ownerRef().getStore() != null;
            }

            @Override
            public void clearNextPulse(String playerId) {
                transformationState.clearNextPulse(playerId);
            }

            @Override
            public long nextPulseAt(String playerId, long defaultValue) {
                return transformationState.nextPulseAt(playerId, defaultValue);
            }

            @Override
            public PlayerData player(String playerId) {
                return support.player(playerId);
            }

            @Override
            public boolean shouldEnd(ActiveTransformation form, PlayerData player) {
                return shouldEndTransformation(form, player);
            }

            @Override
            public Vector3d ownerPosition(ActiveTransformation form) {
                return support.position(form.ownerRef(), form.ownerRef().getStore());
            }

            @Override
            public void refreshOwnerState(ActiveTransformation form, PlayerData player) {
                TransformationHytaleAdapter.this.refreshOwnerState(form, player, form.ownerRef().getStore());
            }

            @Override
            public void applyLocomotionPressure(ActiveTransformation form, PlayerData player, Vector3d origin) {
                effectRuntime.applyLocomotionPressure(form, origin, effectHooks(form, player, form.ownerRef().getStore()));
            }

            @Override
            public void applyFormPulse(ActiveTransformation form, PlayerData player, Vector3d origin) {
                effectRuntime.applyPulse(form, origin, effectHooks(form, player, form.ownerRef().getStore()));
            }

            @Override
            public void scheduleNextPulse(String playerId, long nextPulseAtMillis) {
                transformationState.scheduleNextPulse(playerId, nextPulseAtMillis);
            }
        });
    }

    private boolean shouldEndTransformation(ActiveTransformation form, PlayerData player) {
        if (form == null || player == null) {
            return true;
        }

        String playerId = form.playerId();
        if (playerId == null || support.isIncapacitated(playerId)) {
            return true;
        }

        if (form.endsWhenGrounded() && support.hasStatusEffect(playerId, StatusEffect.Type.GROUNDED)) {
            return true;
        }

        return !"corruptus".equalsIgnoreCase(player.getPlayerClass());
    }

    private void refreshOwnerState(ActiveTransformation form,
                                   PlayerData player,
                                   Store<EntityStore> store) {
        for (String token : form.ownerRuntimeTokens()) {
            StatusEffect effect = support.createStatusEffect(token, form.sourceAbility(), player.getPlayerId(), form.abilityId());
            if (effect != null) {
                support.applyOwnerStatusEffect(player.getPlayerId(), effect);
            }
        }
        if (form.ownerShieldAmount() > 0.0) {
            support.applyShield(player.getPlayerId(), form.ownerRef(), store, form.sourceAbility(), form.ownerShieldAmount());
        }
    }

    private TransformationEffectRuntime.Hooks<Ref<EntityStore>> effectHooks(ActiveTransformation form,
                                                                            PlayerData player,
                                                                            Store<EntityStore> store) {
        return new TransformationEffectRuntime.Hooks<>() {
            @Override
            public Ref<EntityStore> findNearest(Vector3d origin, double radius) {
                return support.findNearestNpc(store, origin, radius);
            }

            @Override
            public Iterable<Ref<EntityStore>> collectNearby(Vector3d origin, double radius, int maxTargets) {
                return support.collectNearbyNpcTargets(store, origin, radius, maxTargets);
            }

            @Override
            public Iterable<Ref<EntityStore>> collectAlong(Vector3d from, Vector3d to, double radius, int maxTargets) {
                return support.collectTargetsAlongSegment(store, from, to, radius, maxTargets);
            }

            @Override
            public void applyImpact(Ref<EntityStore> target, double damageRatio, String token, boolean knockback) {
                applyPulseImpact(form, player, target, store, damageRatio, token, knockback);
            }

            @Override
            public boolean applyChargeImpact(Ref<EntityStore> target, double damageRatio) {
                return TransformationHytaleAdapter.this.applyChargeImpact(form, player, target, store, damageRatio);
            }

            @Override
            public void applyToken(Ref<EntityStore> target, String token) {
                support.applyTargetToken(token, target, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
            }

            @Override
            public void applyKnockback(Ref<EntityStore> target) {
                support.applyKnockback(target, store, form.ownerRef(), form.sourceAbility());
            }

            @Override
            public void applyOwnerShield(double shieldPercent) {
                support.applyShield(player.getPlayerId(), form.ownerRef(), store, form.sourceAbility(), shieldPercent);
            }
        };
    }

    private void applyPulseImpact(ActiveTransformation form,
                                  PlayerData player,
                                  Ref<EntityStore> targetRef,
                                  Store<EntityStore> store,
                                  double damageRatio,
                                  String token,
                                  boolean knockback) {
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        double damage = resolveTransformationDamage(form, player, targetRef, store, damageRatio, 3.0);
        if (damage > 0.0) {
            applyTransformationDamage(form, player, targetRef, store, damage);
        }

        support.applyEffectById(targetRef, store,
                support.resolveImpactEffectId(player.getPlayerClass(), support.currentStyleId(player), form.sourceAbility()));
        if (token != null && !token.isBlank()) {
            support.applyTargetToken(token, targetRef, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
        }
        if (knockback) {
            support.applyKnockback(targetRef, store, form.ownerRef(), form.sourceAbility());
        }
    }

    private boolean applyChargeImpact(ActiveTransformation form,
                                      PlayerData player,
                                      Ref<EntityStore> target,
                                      Store<EntityStore> store,
                                      double damageRatio) {
        if (target == null || !target.isValid()) {
            return false;
        }

        double damage = resolveTransformationDamage(form, player, target, store, damageRatio, 4.0);
        if (damage > 0.0) {
            applyTransformationDamage(form, player, target, store, damage);
        }

        support.applyEffectById(target, store,
                support.resolveImpactEffectId(player.getPlayerClass(), support.currentStyleId(player), form.sourceAbility()));
        if (support.applyKnockbackCollidedWithWall(target, store, form.ownerRef(), form.sourceAbility())) {
            support.applyTargetToken("stun", target, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
        }
        return true;
    }

    private void applyCleave(ActiveTransformation form,
                             PlayerData player,
                             Ref<EntityStore> primaryTargetRef,
                             Store<EntityStore> store,
                             Ref<EntityStore> playerRef,
                             double splashDamage,
                             String token) {
        if (splashDamage <= 0.0) {
            return;
        }

        Vector3d center = support.position(primaryTargetRef, store);
        if (center == null) {
            return;
        }

        for (Ref<EntityStore> splashTarget : support.collectNearbyNpcTargets(store, center, 3.4, 3)) {
            if (splashTarget == null || !splashTarget.isValid() || splashTarget.equals(primaryTargetRef)) {
                continue;
            }

            String targetEntityId = support.resolveEntityId(splashTarget, store);
            double resolvedSplash = splashDamage;
            if (targetEntityId != null) {
                resolvedSplash *= support.incomingDamageMultiplier(targetEntityId);
                resolvedSplash = support.absorbDamage(targetEntityId, resolvedSplash);
            }
            if (resolvedSplash <= 0.0) {
                continue;
            }

            Damage cleave = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) resolvedSplash);
            DamageSystems.executeDamage(splashTarget, store, cleave);
            support.applyPostDamageClassPassives(player, playerRef, targetEntityId, resolvedSplash, false);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + resolvedSplash);
            if (token != null) {
                support.applyTargetToken(token, splashTarget, store, playerRef, player.getPlayerId(), form.sourceAbility());
            }
            support.applyEffectById(splashTarget, store,
                    support.resolveImpactEffectId(player.getPlayerClass(), support.currentStyleId(player), form.sourceAbility()));
        }
    }

    private double resolveTransformationDamage(ActiveTransformation form,
                                               PlayerData player,
                                               Ref<EntityStore> target,
                                               Store<EntityStore> store,
                                               double damageRatio,
                                               double minimumDamage) {
        String targetEntityId = support.resolveEntityId(target, store);
        double damage = Math.max(minimumDamage, support.resolveDamageAmount(player, form.sourceAbility()) * damageRatio);
        if (targetEntityId != null) {
            damage *= support.incomingDamageMultiplier(targetEntityId);
            damage = support.absorbDamage(targetEntityId, damage);
        }
        return damage;
    }

    private void applyTransformationDamage(ActiveTransformation form,
                                           PlayerData player,
                                           Ref<EntityStore> target,
                                           Store<EntityStore> store,
                                           double damage) {
        Damage impactDamage = new Damage(new Damage.EntitySource(form.ownerRef()), DamageCause.PHYSICAL, (float) damage);
        DamageSystems.executeDamage(target, store, impactDamage);
        support.applyPostDamageClassPassives(player, form.ownerRef(), support.resolveEntityId(target, store), damage, true);
        player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
        support.applyLifesteal(form.ownerRef(), player.getPlayerId(), damage);
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ref, Store<EntityStore> currentStore) {
        return ref != null && ref.isValid() && ref.getStore() == currentStore;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    public interface Support {
        PlayerData player(String playerId);

        boolean isIncapacitated(String playerId);

        boolean hasStatusEffect(String playerId, StatusEffect.Type type);

        StatusEffect createStatusEffect(String token, AbilityData ability, String sourcePlayerId, String sourceAbilityId);

        void applyOwnerStatusEffect(String playerId, StatusEffect effect);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        String currentStyleId(PlayerData player);

        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        String humanize(String value);

        Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Vector3d center, double radius);

        Iterable<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                           Vector3d center,
                                                           double radius,
                                                           int maxTargets);

        Iterable<Ref<EntityStore>> collectTargetsAlongSegment(Store<EntityStore> store,
                                                              Vector3d from,
                                                              Vector3d to,
                                                              double radius,
                                                              int maxTargets);

        boolean applyTargetToken(String token,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> sourceRef,
                                 String sourcePlayerId,
                                 AbilityData ability);

        void applyKnockback(Ref<EntityStore> targetRef,
                            Store<EntityStore> store,
                            Ref<EntityStore> sourceRef,
                            AbilityData ability);

        boolean applyKnockbackCollidedWithWall(Ref<EntityStore> targetRef,
                                               Store<EntityStore> store,
                                               Ref<EntityStore> sourceRef,
                                               AbilityData ability);

        double applyShield(String entityId,
                           Ref<EntityStore> entityRef,
                           Store<EntityStore> store,
                           AbilityData ability,
                           double shieldPercent);

        double resolveDamageAmount(PlayerData player, AbilityData ability);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void applyPostDamageClassPassives(PlayerData player,
                                          Ref<EntityStore> ownerRef,
                                          String targetEntityId,
                                          double damage,
                                          boolean abilityDamage);

        void applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt);
    }

    public record ActivationResult(boolean activated, String summary) {
        public ActivationResult {
            summary = summary == null ? "" : summary;
        }

        public static ActivationResult none() {
            return new ActivationResult(false, "");
        }
    }
}
