package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FieldSupportPulseHytaleAdapter {
    private static final double DEFAULT_SUPPORT_HEAL_RATIO = 0.16;
    private static final double DEFAULT_SUPPORT_SHIELD_RATIO = 0.12;

    public void applySupportPulse(ActiveField field, PlayerData player, Support support) {
        if (field == null || player == null || field.ability() == null || support == null) {
            return;
        }

        Ref<EntityStore> ownerRef = field.ownerRef();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() == null) {
            return;
        }

        Store<EntityStore> store = ownerRef.getStore();
        Vector3d ownerPosition = position(ownerRef, store);
        if (ownerPosition == null || !isOwnerInsideField(field, ownerPosition)) {
            return;
        }

        applyOwnerEffects(field, player, support);
        double sustainMultiplier = support.sustainMultiplier(player);

        double pulseHealPercent = field.ability().getHealPercent() * DEFAULT_SUPPORT_HEAL_RATIO * sustainMultiplier;
        if (pulseHealPercent > 0.0) {
            double healed = support.heal(ownerRef, store, pulseHealPercent);
            if (healed > 0.0) {
                player.getStatistics().setTotalHealingDone(player.getStatistics().getTotalHealingDone() + healed);
            }
        }

        double pulseShieldPercent = field.ability().getShieldPercent() * DEFAULT_SUPPORT_SHIELD_RATIO * sustainMultiplier;
        if (pulseShieldPercent > 0.0) {
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), pulseShieldPercent);
        }

        applyOwnerTerrainEffects(field, player, ownerRef, store, sustainMultiplier, support);
    }

    private void applyOwnerEffects(ActiveField field, PlayerData player, Support support) {
        for (String token : parseEffectTokens(field.ability().getEffect())) {
            if (!shouldPulseOwnerEffectToken(field, token, support)) {
                continue;
            }

            support.applyOwnerStatusToken(token, field, player);
        }

        String terrainEffect = lower(field.ability().getTerrainEffect());
        if (terrainEffect.contains("shadow") || terrainEffect.contains("smoke")) {
            support.applyOwnerStatusToken("evasion", field, player);
        }
        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("condensation_veil")
                || terrainEffect.contains("vanish")
                || terrainEffect.contains("umbral_shroud")) {
            support.applyOwnerStatusToken("evasion", field, player);
        }
        if (terrainEffect.contains("tide_pool") || terrainEffect.contains("rainbow")) {
            support.applyOwnerStatusToken("speed", field, player);
        }
        if (terrainEffect.contains("sanctuary") || terrainEffect.contains("glacier") || terrainEffect.contains("purifying")) {
            support.applyOwnerStatusToken("defense_buff", field, player);
        }
        if (terrainEffect.contains("ice_shell")) {
            support.applyOwnerStatusToken("defense_buff", field, player);
        }
        if (terrainEffect.contains("lava_pool")) {
            // Lava Pool protects the caster standing in it (enemies take the burn pulse).
            support.applyOwnerStatusToken("defense_buff", field, player);
        }
    }

    private boolean shouldPulseOwnerEffectToken(ActiveField field, String token, Support support) {
        if (field == null || token == null || token.isBlank()) {
            return false;
        }
        if (!support.isCasterEffectToken(token)) {
            return false;
        }

        String terrainEffect = lower(field.ability().getTerrainEffect());
        return !"stealth".equals(lower(token))
                || (!terrainEffect.contains("vanish") && !terrainEffect.contains("umbral_shroud"));
    }

    private void applyOwnerTerrainEffects(ActiveField field,
                                          PlayerData player,
                                          Ref<EntityStore> ownerRef,
                                          Store<EntityStore> store,
                                          double sustainMultiplier,
                                          Support support) {
        String terrainEffect = lower(field.ability().getTerrainEffect());
        if (terrainEffect.isBlank()) {
            return;
        }

        if (terrainEffect.contains("sanctuary") || terrainEffect.contains("purifying")) {
            support.clearNegativeEffects(player.getPlayerId());
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 4.0 * sustainMultiplier);
            return;
        }

        if (terrainEffect.contains("rainbow")) {
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 3.5 * sustainMultiplier);
            support.applyOwnerStatusToken("speed", field, player);
            return;
        }

        if (terrainEffect.contains("root_circle")) {
            support.heal(ownerRef, store, 2.5 * sustainMultiplier);
            support.applyOwnerStatusToken("defense_buff", field, player);
            return;
        }

        if (terrainEffect.contains("eye_of_the_storm")) {
            support.heal(ownerRef, store, 2.0 * sustainMultiplier);
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 2.5 * sustainMultiplier);
            support.applyOwnerStatusToken("evasion", field, player);
            return;
        }

        if (terrainEffect.contains("cyclone_shield")) {
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 2.0 * sustainMultiplier);
            support.applyOwnerStatusToken("defense_buff", field, player);
            return;
        }

        if (terrainEffect.contains("ice_shell")) {
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 2.0 * sustainMultiplier);
            support.applyOwnerStatusToken("defense_buff", field, player);
            return;
        }

        if (terrainEffect.contains("tide_pool")) {
            support.applyOwnerStatusToken("speed", field, player);
            return;
        }

        if (terrainEffect.contains("glacier")) {
            support.applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 3.0 * sustainMultiplier);
            return;
        }

        if (terrainEffect.contains("shadow") || terrainEffect.contains("smoke")) {
            support.applyOwnerStatusToken("evasion", field, player);
            return;
        }

        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("condensation_veil")) {
            support.applyOwnerStatusToken("evasion", field, player);
            return;
        }

        if (terrainEffect.contains("resonant_aura")) {
            support.applyOwnerStatusToken("attack_buff", field, player);
            support.applyOwnerStatusToken("speed", field, player);
            return;
        }

        if (terrainEffect.contains("psychic_link")) {
            support.applyOwnerStatusToken("attack_buff", field, player);
            return;
        }

        if (terrainEffect.contains("steam_pressure")) {
            support.applyOwnerStatusToken("attack_buff", field, player);
            support.applyOwnerStatusToken("speed", field, player);
        }
    }

    private static boolean isOwnerInsideField(ActiveField field, Vector3d position) {
        if ("barrier".equals(lower(field.ability().getCastType()))) {
            Vector3d relative = subtract(position, field.center());
            double lateral = Math.abs(dot(relative, field.lineDirection()));
            double depth = Math.abs(dot(relative, field.forwardDirection()));
            return lateral <= field.halfWidth() && depth <= field.thickness();
        }
        return distance(field.center(), position) <= field.radius();
    }

    private static Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
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

    private static Vector3d subtract(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return new Vector3d();
        }
        return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private static double dot(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return 0.0;
        }
        return (left.x * right.x) + (left.y * right.y) + (left.z * right.z);
    }

    private static double distance(Vector3d left, Vector3d right) {
        Vector3d delta = subtract(left, right);
        return Math.sqrt(dot(delta, delta));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        double sustainMultiplier(PlayerData player);

        double heal(Ref<EntityStore> entityRef, Store<EntityStore> store, double healPercent);

        double applyShield(String entityId,
                           Ref<EntityStore> entityRef,
                           Store<EntityStore> store,
                           AbilityData ability,
                           double shieldPercent);

        int clearNegativeEffects(String entityId);

        boolean isCasterEffectToken(String token);

        void applyOwnerStatusToken(String token, ActiveField field, PlayerData player);
    }
}
