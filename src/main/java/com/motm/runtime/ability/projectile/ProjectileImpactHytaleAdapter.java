package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.motm.util.MotmEntityLiveness;
import com.motm.model.PlayerData;
import com.motm.util.AbilityPresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProjectileImpactHytaleAdapter {
    private final ProjectileHitHytaleAdapter hitAdapter;
    private final double defaultLightningArcRadius;

    public ProjectileImpactHytaleAdapter(ProjectileHitHytaleAdapter hitAdapter,
                                         double defaultLightningArcRadius) {
        this.hitAdapter = hitAdapter;
        this.defaultLightningArcRadius = defaultLightningArcRadius;
    }

    public void applyImpact(ActiveProjectile projectile,
                            PlayerData player,
                            Store<EntityStore> store,
                            Vector3d impactPosition,
                            Ref<EntityStore> directHit,
                            Support support) {
        if (projectile == null || player == null || store == null || support == null || hitAdapter == null) {
            return;
        }

        if (isChainLightning(projectile.ability())) {
            applyChainImpact(projectile, player, store, impactPosition, directHit, support);
            return;
        }
        List<Ref<EntityStore>> targets = hitAdapter.collectImpactTargets(projectile, store, impactPosition, directHit);
        if (targets.isEmpty()) {
            return;
        }

        String impactEffectId = support.resolveImpactEffectId(projectile.classId(), projectile.styleId(), projectile.ability());
        double castBuffMultiplier = support.outgoingDamageMultiplier(player);
        double totalDamage = 0.0;

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !MotmEntityLiveness.isLiveTarget(targetRef, store)) {
                continue;
            }

            String targetEntityId = support.resolveEntityId(targetRef, store);
            double resolvedDamage = projectile.baseDamage() * castBuffMultiplier;
            if (targetEntityId != null) {
                resolvedDamage = support.applySpecialDamageModifiers(
                        player,
                        projectile.ability(),
                        targetRef,
                        store,
                        targetEntityId,
                        resolvedDamage
                );
                resolvedDamage *= support.incomingDamageMultiplier(targetEntityId);
                resolvedDamage = support.absorbDamage(targetEntityId, resolvedDamage);
            }

            if (resolvedDamage > 0.0) {
                Damage damage = new Damage(new Damage.EntitySource(projectile.ownerRef()),
                        DamageCause.PROJECTILE,
                        (float) resolvedDamage);
                DamageSystems.executeDamage(targetRef, store, damage);
                support.reportAbilityKillIfDead(projectile.ownerPlayerId(), player, targetRef, store, targetEntityId);
                support.applyPostDamageClassPassives(player, projectile.ownerRef(), targetEntityId, resolvedDamage, true);
                totalDamage += resolvedDamage;
            }

            support.applyEffect(targetRef, store, impactEffectId);
            applyTravelTypeEffects(projectile, player, store, targetRef, impactPosition, true, support);
        }

        support.logInfo("[MOTM] Projectile impact resolved: abilityId=" + abilityId(projectile.ability())
                + " targets=" + targets.size()
                + " damage=" + AbilityPresentation.formatDecimal(totalDamage)
                + " effect=" + safe(projectile.ability() == null ? null : projectile.ability().getEffect())
                + " impact=" + formatVector(impactPosition));
        support.recordProjectileImpact(projectile, targets.size(), totalDamage, impactPosition);

        if (totalDamage > 0.0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + totalDamage);
            support.applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), totalDamage);
        }

        applyTargetEffects(projectile, player, store, targets, support);
        if (isLightningProjectile(projectile.ability())
                && directHit != null && MotmEntityLiveness.isLiveTarget(directHit, store)) {
            String directEntityId = support.resolveEntityId(directHit, store);
            if (directEntityId != null) {
                projectile.hitEntityIds().add(directEntityId);
            }
            applyLightningArcSplash(projectile, player, store, directHit, support);
        }
    }
    private void applyChainImpact(ActiveProjectile projectile,
                                  PlayerData player,
                                  Store<EntityStore> store,
                                  Vector3d impactPosition,
                                  Ref<EntityStore> directHit,
                                  Support support) {
        if (impactPosition == null || !MotmEntityLiveness.isLiveTarget(directHit, store)) {
            return;
        }
        ChainLightningState chain = new ChainLightningState();
        Ref<EntityStore> current = directHit;
        Vector3d center = impactPosition;
        int hops = 0;
        double totalDamage = 0.0;
        while (current != null && hops < ChainLightningState.MAX_HOPS && chain.visit(current, store)) {
            if (!MotmEntityLiveness.isLiveTarget(current, store)) {
                break;
            }
            Vector3d targetPosition = getPosition(current, store);
            if (targetPosition == null) {
                break;
            }
            String targetEntityId = support.resolveEntityId(current, store);
            double resolvedDamage = projectile.baseDamage() * support.outgoingDamageMultiplier(player);
            resolvedDamage *= support.targetSequenceDamageMultiplier(
                    projectile.ability(), "chain", hops);
            if (targetEntityId != null) {
                resolvedDamage = support.applySpecialDamageModifiers(
                        player, projectile.ability(), current, store, targetEntityId, resolvedDamage);
                resolvedDamage *= support.incomingDamageMultiplier(targetEntityId);
                resolvedDamage = support.absorbDamage(targetEntityId, resolvedDamage);
            }
            if (resolvedDamage > 0.0) {
                Damage damage = new Damage(new Damage.EntitySource(projectile.ownerRef()),
                        DamageCause.PROJECTILE, (float) resolvedDamage);
                DamageSystems.executeDamage(current, store, damage);
                support.reportAbilityKillIfDead(projectile.ownerPlayerId(), player, current, store, targetEntityId);
                support.applyPostDamageClassPassives(
                        player, projectile.ownerRef(), targetEntityId, resolvedDamage, true);
                support.applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), resolvedDamage);
                totalDamage += resolvedDamage;
            }
            String impactEffectId = support.resolveImpactEffectId(
                    projectile.classId(), projectile.styleId(), projectile.ability());
            support.applyEffect(current, store, impactEffectId);
            applyTargetEffects(projectile, player, store, List.of(current), support);
            projectile.hitEntityIds().add(targetEntityId);
            ParticleUtil.spawnParticleEffect("Spell/Lightning", targetPosition, store);
            hops++;
            center = targetPosition;
            current = chain.nearestNext(hitAdapter, store, center);
        }
        support.recordProjectileImpact(projectile, hops, totalDamage, center);
        if (totalDamage > 0.0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + totalDamage);
        }
    }


    public void applyTraversalHits(ActiveProjectile projectile,
                                   PlayerData player,
                                   Store<EntityStore> store,
                                   Vector3d from,
                                   Vector3d to,
                                   Support support) {
        if (projectile == null || player == null || store == null || support == null || hitAdapter == null) {
            return;
        }

        List<Ref<EntityStore>> targets = hitAdapter.collectTraversalTargets(
                projectile,
                store,
                from,
                to,
                support::resolveEntityId
        );
        if (targets.isEmpty()) {
            return;
        }

        String impactEffectId = support.resolveImpactEffectId(projectile.classId(), projectile.styleId(), projectile.ability());
        double castBuffMultiplier = support.outgoingDamageMultiplier(player);
        int hitIndex = projectile.hitEntityIds().size();
        int resolvedTargets = 0;
        double totalDamage = 0.0;

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !MotmEntityLiveness.isLiveTarget(targetRef, store)) {
                continue;
            }

            String targetEntityId = support.resolveEntityId(targetRef, store);
            if (targetEntityId == null || targetEntityId.equals(projectile.ownerPlayerId())) {
                continue;
            }

            double resolvedDamage = projectile.baseDamage() * castBuffMultiplier;
            resolvedDamage *= support.targetSequenceDamageMultiplier(
                    projectile.ability(),
                    lower(projectile.ability() == null ? null : projectile.ability().getCastType()),
                    hitIndex
            );
            resolvedDamage = support.applySpecialDamageModifiers(
                    player,
                    projectile.ability(),
                    targetRef,
                    store,
                    targetEntityId,
                    resolvedDamage
            );
            resolvedDamage *= support.incomingDamageMultiplier(targetEntityId);
            resolvedDamage = support.absorbDamage(targetEntityId, resolvedDamage);

            if (resolvedDamage > 0.0) {
                Damage damage = new Damage(new Damage.EntitySource(projectile.ownerRef()),
                        DamageCause.PROJECTILE,
                        (float) resolvedDamage);
                DamageSystems.executeDamage(targetRef, store, damage);
                support.applyPostDamageClassPassives(player, projectile.ownerRef(), targetEntityId, resolvedDamage, true);
                player.getStatistics().setTotalDamageDealt(
                        player.getStatistics().getTotalDamageDealt() + resolvedDamage);
                support.applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), resolvedDamage);
                totalDamage += resolvedDamage;
            }

            support.applyEffect(targetRef, store, impactEffectId);
            applyTargetEffects(projectile, player, store, List.of(targetRef), support);
            applyTravelTypeEffects(projectile, player, store, targetRef, to, false, support);
            projectile.hitEntityIds().add(targetEntityId);
            hitIndex++;
            resolvedTargets++;

            if (isLightningProjectile(projectile.ability())) {
                applyLightningArcSplash(projectile, player, store, targetRef, support);
            }
        }

        if (resolvedTargets > 0) {
            support.logInfo("[MOTM] Projectile traversal resolved: abilityId=" + abilityId(projectile.ability())
                    + " targets=" + resolvedTargets
                    + " damage=" + AbilityPresentation.formatDecimal(totalDamage)
                    + " effect=" + safe(projectile.ability() == null ? null : projectile.ability().getEffect())
                    + " position=" + formatVector(to));
            support.recordProjectileImpact(projectile, resolvedTargets, totalDamage, to);
        }
    }

    private void applyTravelTypeEffects(ActiveProjectile projectile,
                                        PlayerData player,
                                        Store<EntityStore> store,
                                        Ref<EntityStore> primaryTarget,
                                        Vector3d impactPosition,
                                        boolean allowSplash,
                                        Support support) {
        if (projectile == null || player == null || store == null || primaryTarget == null
                || !MotmEntityLiveness.isLiveTarget(primaryTarget, store)) {
            return;
        }

        String travelType = lower(projectile.ability() == null ? null : projectile.ability().getTravelType());
        if (travelType.isBlank()) {
            return;
        }

        if (travelType.contains("gust")) {
            support.applyTargetToken("disoriented", primaryTarget, store,
                    projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            if (allowSplash) {
                applySplashToken(projectile, player, store, impactPosition, primaryTarget, "knockback", 2.4, 1, support);
            }
            return;
        }

        if (travelType.contains("compressed_air")) {
            support.applyTargetToken("knockback", primaryTarget, store,
                    projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            support.applyTargetToken("grounded", primaryTarget, store,
                    projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            return;
        }

        if (travelType.contains("psychic")) {
            support.applyTargetToken("disoriented", primaryTarget, store,
                    projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            if (allowSplash) {
                applySplashToken(projectile, player, store, impactPosition, primaryTarget, "vulnerability", 2.6, 2, support);
            }
            return;
        }

        if (travelType.contains("boiling_jet")) {
            if (allowSplash) {
                applySplashToken(projectile, player, store, impactPosition, primaryTarget, "burn", 2.1, 2, support);
            }
            return;
        }

        if (travelType.contains("arcing_shot") && allowSplash) {
            applySplashToken(projectile, player, store, impactPosition, primaryTarget, "slow", 1.8, 1, support);
        }
    }

    private void applySplashToken(ActiveProjectile projectile,
                                  PlayerData player,
                                  Store<EntityStore> store,
                                  Vector3d impactPosition,
                                  Ref<EntityStore> primaryTarget,
                                  String token,
                                  double radius,
                                  int maxTargets,
                                  Support support) {
        if (projectile == null || player == null || store == null || impactPosition == null
                || token == null || token.isBlank() || radius <= 0.0 || maxTargets <= 0) {
            return;
        }

        int applied = 0;
        for (Ref<EntityStore> splashTarget : hitAdapter.collectNearbyTargets(store, impactPosition, radius, maxTargets + 1)) {
            if (splashTarget == null || !MotmEntityLiveness.isLiveTarget(splashTarget, store)
                    || splashTarget.equals(primaryTarget)) {
                continue;
            }
            support.applyTargetToken(token, splashTarget, store,
                    projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            applied++;
            if (applied >= maxTargets) {
                return;
            }
        }
    }

    private void applyLightningArcSplash(ActiveProjectile projectile,
                                         PlayerData player,
                                         Store<EntityStore> store,
                                         Ref<EntityStore> directTargetRef,
                                         Support support) {
        Vector3d center = getPosition(directTargetRef, store);
        if (center == null) {
            return;
        }

        String impactEffectId = support.resolveImpactEffectId(projectile.classId(), projectile.styleId(), projectile.ability());
        double radius = projectile.ability() != null && projectile.ability().getRadius() > 0
                ? Math.max(defaultLightningArcRadius, projectile.ability().getRadius())
                : defaultLightningArcRadius;
        List<Ref<EntityStore>> arcTargets = hitAdapter.collectNearbyTargets(store, center, radius, 2);
        double castBuffMultiplier = support.outgoingDamageMultiplier(player);

        for (Ref<EntityStore> arcTarget : arcTargets) {
            if (arcTarget == null || !MotmEntityLiveness.isLiveTarget(arcTarget, store)
                    || arcTarget.equals(directTargetRef)) {
                continue;
            }

            String entityId = support.resolveEntityId(arcTarget, store);
            if (entityId == null || projectile.hitEntityIds().contains(entityId)) {
                continue;
            }

            double resolvedDamage = projectile.baseDamage() * 0.55 * castBuffMultiplier;
            resolvedDamage *= support.incomingDamageMultiplier(entityId);
            resolvedDamage = support.absorbDamage(entityId, resolvedDamage);
            if (resolvedDamage > 0.0) {
                Damage arcDamage = new Damage(new Damage.EntitySource(projectile.ownerRef()),
                        DamageCause.PROJECTILE,
                        (float) resolvedDamage);
                DamageSystems.executeDamage(arcTarget, store, arcDamage);
                support.applyPostDamageClassPassives(player, projectile.ownerRef(), entityId, resolvedDamage, true);
                player.getStatistics().setTotalDamageDealt(
                        player.getStatistics().getTotalDamageDealt() + resolvedDamage);
                support.applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), resolvedDamage);
            }

            support.applyEffect(arcTarget, store, impactEffectId);
            support.applyTargetToken("shocked", arcTarget, store,
                    projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            projectile.hitEntityIds().add(entityId);
        }
    }

    private void applyTargetEffects(ActiveProjectile projectile,
                                    PlayerData player,
                                    Store<EntityStore> store,
                                    List<Ref<EntityStore>> targets,
                                    Support support) {
        List<String> tokens = parseEffectTokens(projectile.ability() == null ? null : projectile.ability().getEffect());
        if (tokens.isEmpty()) {
            return;
        }

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !MotmEntityLiveness.isLiveTarget(targetRef, store)) {
                continue;
            }
            String entityId = support.resolveEntityId(targetRef, store);
            if (entityId == null || entityId.equals(player.getPlayerId())) {
                continue;
            }

            for (String token : tokens) {
                if (!support.isTargetEffectToken(token)) {
                    continue;
                }

                support.applyTargetToken(token, targetRef, store,
                        projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            }
        }
    }

    private static Vector3d getPosition(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private static boolean isLightningProjectile(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        String abilityId = lower(ability.getId());
        String travelType = lower(ability.getTravelType());
        return abilityId.contains("smite")
                || abilityId.contains("lightning")
                || travelType.contains("lightning")
                || travelType.contains("thunder");
    }
    private static boolean isChainLightning(AbilityData ability) {
        return ability != null
                && "chain".equalsIgnoreCase(ability.getCastType())
                && lower(ability.getTravelType()).contains("chain_lightning");
    }

    private static List<String> parseEffectTokens(String effect) {
        if (effect == null || effect.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String part : effect.toLowerCase(Locale.ROOT).split("\\+")) {
            String token = part.trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String abilityId(AbilityData ability) {
        return ability == null ? "" : safe(ability.getId());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "(unknown)";
        }
        return "("
                + formatDistance(vector.x) + ", "
                + formatDistance(vector.y) + ", "
                + formatDistance(vector.z) + ")";
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f", distance);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        double outgoingDamageMultiplier(PlayerData player);

        double applySpecialDamageModifiers(PlayerData player,
                                           AbilityData ability,
                                           Ref<EntityStore> targetRef,
                                           Store<EntityStore> store,
                                           String targetEntityId,
                                           double damage);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void reportAbilityKillIfDead(String ownerPlayerId,
                                     PlayerData player,
                                     Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     String targetEntityId);

        void applyPostDamageClassPassives(PlayerData player,
                                          Ref<EntityStore> ownerRef,
                                          String targetEntityId,
                                          double damage,
                                          boolean abilityDamage);

        boolean applyEffect(Ref<EntityStore> targetRef, Store<EntityStore> store, String effectId);

        void applyLifesteal(Ref<EntityStore> ownerRef, String ownerPlayerId, double damage);

        boolean applyTargetToken(String token,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> sourceRef,
                                 String sourcePlayerId,
                                 AbilityData ability);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        double targetSequenceDamageMultiplier(AbilityData ability, String castType, int hitIndex);

        boolean isTargetEffectToken(String token);

        void logInfo(String message);

        void recordProjectileImpact(ActiveProjectile projectile,
                                    int targets,
                                    double totalDamage,
                                    Vector3d impactPosition);
    }
}
