package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.util.MotmEntityLiveness;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FieldPulseHytaleAdapter {

    public void applyPulse(ActiveField field,
                           PlayerData player,
                           Store<EntityStore> store,
                           List<Ref<EntityStore>> targets,
                           Support support) {
        if (field == null || player == null || store == null || targets == null || targets.isEmpty()
                || support == null) {
            return;
        }

        double totalDamage = 0.0;
        double pulseDamage = support.pulseDamage(player, field.ability());
        String impactEffectId = support.resolveImpactEffectId(field.classId(), field.styleId(), field.ability());

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }
            if (!MotmEntityLiveness.isLiveTarget(targetRef, store)) {
                continue;
            }

            String entityId = support.resolveEntityId(targetRef, store);
            if (entityId == null || entityId.equals(player.getPlayerId())) {
                continue;
            }

            if (pulseDamage > 0.0) {
                double resolvedDamage = pulseDamage * support.outgoingDamageMultiplier(player);
                resolvedDamage *= support.incomingDamageMultiplier(entityId);
                resolvedDamage = support.absorbDamage(entityId, resolvedDamage);
                if (resolvedDamage > 0.0) {
                    if (!MotmEntityLiveness.isLiveTarget(targetRef, store)) {
                        continue;
                    }
                    Damage damage = new Damage(new Damage.EntitySource(field.ownerRef()),
                            DamageCause.PHYSICAL,
                            (float) resolvedDamage);
                    DamageSystems.executeDamage(targetRef, store, damage);
                    support.applyPostDamageClassPassives(player, field.ownerRef(), entityId, resolvedDamage, true);
                    totalDamage += resolvedDamage;
                }
            }

            if (FieldRuntimeSpecs.shouldApplyRepeatingTargetTokens(field.ability())) {
                support.applyEffect(targetRef, store, impactEffectId);
                applyTargetEffects(field, player, targetRef, store, support);
            }
        }

        if (totalDamage > 0.0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + totalDamage);
            support.applyLifesteal(field.ownerRef(), player.getPlayerId(), totalDamage);
        }
    }

    private void applyTargetEffects(ActiveField field,
                                    PlayerData player,
                                    Ref<EntityStore> targetRef,
                                    Store<EntityStore> store,
                                    Support support) {
        String entityId = support.resolveEntityId(targetRef, store);
        if (entityId == null || entityId.equals(player.getPlayerId())) {
            return;
        }

        for (String token : parseEffectTokens(field.ability().getEffect())) {
            if (!support.isTargetEffectToken(token)) {
                continue;
            }

            support.applyTargetToken(token, targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
        }

        applyTerrainEffects(field, player, targetRef, store, support);

        if (field.ability().getPullForce() > 0 && !"barrier".equals(lower(field.ability().getCastType()))) {
            support.applyFieldPull(targetRef, store, field);
        }

        if ("barrier".equals(lower(field.ability().getCastType()))) {
            support.applyBarrierRepulsion(targetRef, store, field);
        }
    }

    private void applyTerrainEffects(ActiveField field,
                                     PlayerData player,
                                     Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     Support support) {
        String terrainEffect = lower(field.ability().getTerrainEffect());
        if (terrainEffect.isBlank()) {
            return;
        }

        if (terrainEffect.contains("sinkhole")) {
            support.applyTargetToken("root", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("mudpit")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("tide_pool")) {
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("falling_rocks")) {
            support.applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("living_flame") || terrainEffect.contains("ember_trail")) {
            support.applyTargetToken("burn", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("ice_skate_trail")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("tunnel_path") || terrainEffect.contains("ruptured_earth")) {
            support.applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("cyclone_shield")) {
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("pressure_burst")) {
            support.applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("twister") || terrainEffect.contains("dust_devil")) {
            support.applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("tempest")) {
            String entityId = support.resolveEntityId(targetRef, store);
            boolean stunned = support.applyTargetToken("stun", targetRef, store, field.ownerRef(),
                    player.getPlayerId(), field.ability());
            boolean slowed = support.applyTargetToken("slow", targetRef, store, field.ownerRef(),
                    player.getPlayerId(), field.ability());
            support.logInfo("[MOTM] Tempest field tick applied: target=" + entityId
                    + " stun=" + stunned
                    + " slow=" + slowed);
            return;
        }

        if (terrainEffect.contains("funnel_cloud")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("snowstorm")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("attack_slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("sandstorm")) {
            support.applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("smog")) {
            String entityId = support.resolveEntityId(targetRef, store);
            boolean blinded = support.applyTargetToken("blind", targetRef, store, field.ownerRef(),
                    player.getPlayerId(), field.ability());
            boolean slowed = support.applyTargetToken("slow", targetRef, store, field.ownerRef(),
                    player.getPlayerId(), field.ability());
            boolean dotted = support.applyTargetToken("dot", targetRef, store, field.ownerRef(),
                    player.getPlayerId(), field.ability());
            support.logInfo("[MOTM] Smog field tick applied: target=" + entityId
                    + " blind=" + blinded
                    + " slow=" + slowed
                    + " dot=" + dotted);
            return;
        }

        if (terrainEffect.contains("acid")) {
            support.applyTargetToken("attack_slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("piercing_rain")) {
            support.applyTargetToken("attack_slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("dot", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("oil_spill")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("toxic", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("glacier")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("ice_shell")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("void_rift")) {
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("infernal_ground")) {
            support.applyTargetToken("burn", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("psychic_shatter") || terrainEffect.contains("psychic_link")) {
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("purifying_aura")) {
            support.applyTargetToken("burn", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("shadow_zone")) {
            support.applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("smoke_bomb")) {
            support.applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("vanish")
                || terrainEffect.contains("umbral_shroud")) {
            support.applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("resonant_aura")) {
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("psychic_link")) {
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("steam_pressure")) {
            support.applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            support.applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
        }
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

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        double pulseDamage(PlayerData player, AbilityData ability);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        double outgoingDamageMultiplier(PlayerData player);

        double incomingDamageMultiplier(String entityId);

        double absorbDamage(String entityId, double damage);

        void applyPostDamageClassPassives(PlayerData player,
                                          Ref<EntityStore> sourceRef,
                                          String targetEntityId,
                                          double damage,
                                          boolean abilityDamage);

        boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        void applyLifesteal(Ref<EntityStore> ownerRef, String ownerPlayerId, double damage);

        boolean isTargetEffectToken(String token);

        boolean applyTargetToken(String token,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> sourceRef,
                                 String sourcePlayerId,
                                 AbilityData ability);

        boolean applyFieldPull(Ref<EntityStore> targetRef, Store<EntityStore> store, ActiveField field);

        boolean applyBarrierRepulsion(Ref<EntityStore> targetRef, Store<EntityStore> store, ActiveField field);

        void logInfo(String message);
    }
}
