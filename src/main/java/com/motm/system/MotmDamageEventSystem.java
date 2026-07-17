package com.motm.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.model.PlayerData;

import java.util.UUID;

/**
 * Hooks native Hytale damage so Alloy Enhancement changes the real hit number.
 */
public class MotmDamageEventSystem extends DamageEventSystem {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(MotmDamageEventSystem.class.getName());

    private final MenteesMod mod;

    public MotmDamageEventSystem(MenteesMod mod) {
        this.mod = mod;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int entityIndex,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        if (chunk == null || store == null || damage == null) {
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(entityIndex);
        Player targetPlayer = targetRef != null && targetRef.isValid()
                ? store.getComponent(targetRef, Player.getComponentType())
                : null;
        UUIDComponent targetUuidComponent = targetRef != null && targetRef.isValid()
                ? store.getComponent(targetRef, UUIDComponent.getComponentType())
                : null;
        UUID targetUuid = targetUuidComponent != null ? targetUuidComponent.getUuid() : null;

        if (!(damage.getSource() instanceof Damage.EntitySource)) {
            if (targetPlayer != null
                    && targetUuid != null
                    && mod.getGameplayPlaybackManager().isMagmaHazardProtected(targetUuid.toString())) {
                float before = damage.getAmount();
                damage.setAmount(0.0f);
                mod.getStatusEffectManager().removeEffect(targetUuid.toString(), com.motm.model.StatusEffect.Type.BURN);
                mod.getStatusEffectManager().removeEffect(targetUuid.toString(), com.motm.model.StatusEffect.Type.DOT);
                mod.getStatusEffectManager().removeEffect(targetUuid.toString(), com.motm.model.StatusEffect.Type.SLOW);
                mod.getStatusEffectManager().removeEffect(targetUuid.toString(), com.motm.model.StatusEffect.Type.SLOW_STACK);
                if (before > 0.0f) {
                    LOG.info("[MOTM] Magma self-hazard damage suppressed: playerId="
                            + targetUuid
                            + " amount=" + before
                            + " source=" + damage.getSource().getClass().getSimpleName());
                }
            }
            if (targetPlayer != null && targetUuid != null && damage.getAmount() > 0.0f) {
                PlayerData targetData = mod.getPlayerDataManager().getOnlinePlayer(targetUuid.toString());
                applyIncomingKnockbackPassive(targetUuid, targetData, damage);
                damage.setAmount(mod.getClassPassiveManager().handleIncomingPlayerDamage(
                        targetUuid.toString(),
                        targetRef,
                        store,
                        damage.getAmount()
                ));
                damage.setAmount(applyPlayerIncomingStatReduction(targetUuid.toString(), damage.getAmount()));
                damage.setAmount(applyBlacksmithArmorReduction(targetUuid.toString(), damage.getAmount()));
                if (mod.getRuntimePerkManager() != null) {
                    damage.setAmount(mod.getRuntimePerkManager().modifyIncomingDamage(
                            targetData,
                            targetRef,
                            store,
                            damage,
                            damage.getAmount()
                    ));
                }
            }
            return;
        }

        if (targetPlayer != null && targetUuid != null && damage.getAmount() > 0.0f) {
            PlayerData targetData = mod.getPlayerDataManager().getOnlinePlayer(targetUuid.toString());
            applyIncomingKnockbackPassive(targetUuid, targetData, damage);
            damage.setAmount(mod.getClassPassiveManager().handleIncomingPlayerDamage(
                    targetUuid.toString(),
                    targetRef,
                    store,
                    damage.getAmount()
            ));
            damage.setAmount(applyPlayerIncomingStatReduction(targetUuid.toString(), damage.getAmount()));
            damage.setAmount(applyBlacksmithArmorReduction(targetUuid.toString(), damage.getAmount()));
            if (mod.getRuntimePerkManager() != null) {
                damage.setAmount(mod.getRuntimePerkManager().modifyIncomingDamage(
                        targetData,
                        targetRef,
                        store,
                        damage,
                        damage.getAmount()
                ));
            }
            if (damage.getAmount() <= 0.0f) {
                return;
            }
        }

        Damage.EntitySource source = (Damage.EntitySource) damage.getSource();

        Ref<EntityStore> sourceRef = source.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return;
        }

        if (mod.getGameplayPlaybackManager().shouldSuppressFriendlySummonDamage(sourceRef, targetRef)) {
            float before = damage.getAmount();
            damage.setAmount(0.0f);
            if (before > 0.0f) {
                LOG.info("[MOTM] Friendly summon damage suppressed: amount=" + before);
            }
            return;
        }

        Player runtimePlayer = store.getComponent(sourceRef, Player.getComponentType());
        if (runtimePlayer == null) {
            return;
        }

        UUIDComponent uuidComponent = store.getComponent(sourceRef, UUIDComponent.getComponentType());
        UUID playerUuid = uuidComponent != null ? uuidComponent.getUuid() : null;
        if (playerUuid == null) {
            return;
        }

        PlayerData playerData = mod.getPlayerDataManager().getOnlinePlayer(playerUuid.toString());
        if (playerData == null) {
            return;
        }

        ItemStack held = InventoryComponent.getItemInHand(store, sourceRef);
        String itemId = held != null ? held.getItemId() : null;
        String response = mod.getGameplayPlaybackManager().handleNativeWeaponDamage(
                runtimePlayer,
                playerData,
                targetRef,
                itemId,
                damage
        );
        applyPlayerOutgoingStatDamage(playerData, damage, itemId);
        if (mod.getRuntimePerkManager() != null) {
            damage.setAmount(mod.getRuntimePerkManager().modifyOutgoingDamage(
                    playerData,
                    sourceRef,
                    store,
                    targetRef,
                    damage,
                    damage.getAmount()
            ));
            mod.getRuntimePerkManager().tryTriggerTerror(playerData, sourceRef, store, held);
            mod.getRuntimePerkManager().afterSuccessfulHit(
                    playerData,
                    sourceRef,
                    store,
                    targetRef,
                    damage.getAmount()
            );
        }
        if (response != null && !response.isBlank()) {
            mod.sendPlayerMessage(runtimePlayer, Message.raw(response));
        }
    }

    private float applyBlacksmithArmorReduction(String playerId, float amount) {
        if (amount <= 0.0f || playerId == null) {
            return amount;
        }
        double reduction = mod.getBlacksmithArmorDamageReduction(playerId);
        if (!Double.isFinite(reduction) || reduction <= 0.0) {
            return amount;
        }
        double clamped = Math.max(0.0, Math.min(0.95, reduction));
        float adjusted = (float) (amount * (1.0 - clamped));
        LOG.info("[MOTM] Runtime perk armor reduction applied: playerId=" + playerId
                + " reduction=" + String.format(java.util.Locale.ROOT, "%.4f", clamped)
                + " before=" + amount
                + " after=" + adjusted);
        return adjusted;
    }

    private float applyPlayerIncomingStatReduction(String playerId, float amount) {
        if (amount <= 0.0f || playerId == null) {
            return amount;
        }
        if (mod.isStartupSelectionProtected(playerId)) {
            LOG.info("[MOTM] Startup protection suppressed incoming damage: playerId=" + playerId
                    + " amount=" + amount);
            return 0.0f;
        }
        PlayerData target = mod.getPlayerDataManager().getOnlinePlayer(playerId);
        double reduction = mod.getPlayerStatModifierManager().getDamageReduction(target);
        if (!Double.isFinite(reduction) || reduction <= 0.0) {
            return amount;
        }
        double clamped = Math.max(0.0, Math.min(0.95, reduction));
        float adjusted = (float) (amount * (1.0 - clamped));
        LOG.info("[MOTM] Player stat damage reduction applied: playerId=" + playerId
                + " reduction=" + String.format(java.util.Locale.ROOT, "%.4f", clamped)
                + " before=" + amount
                + " after=" + adjusted);
        return adjusted;
    }

    private void applyPlayerOutgoingStatDamage(PlayerData playerData, Damage damage, String itemId) {
        if (playerData == null || damage == null || damage.getAmount() <= 0.0f) {
            return;
        }
        double multiplier = mod.getPlayerStatModifierManager().getDamageMultiplier(playerData);
        double critChance = mod.getLevelingManager().getLuckCritChanceBonus(playerData);
        boolean crit = critChance > 0.0
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < Math.min(1.0, critChance);
        double critMultiplier = crit ? 1.5 + mod.getLevelingManager().getTenacityCritDamageBonus(playerData) : 1.0;
        double adjusted = damage.getAmount() * multiplier * critMultiplier;
        if (!Double.isFinite(adjusted) || adjusted <= 0.0) {
            return;
        }
        damage.setAmount((float) adjusted);
        LOG.info("[MOTM] Player stat outgoing damage applied: playerId=" + playerData.getPlayerId()
                + " item=" + itemId
                + " multiplier=" + String.format(java.util.Locale.ROOT, "%.4f", multiplier)
                + " crit=" + crit
                + " amount=" + String.format(java.util.Locale.ROOT, "%.2f", adjusted));
    }

    private void applyIncomingKnockbackPassive(UUID targetUuid, PlayerData targetData, Damage damage) {
        if (targetUuid == null || damage == null) {
            return;
        }

        double multiplier = mod.getClassPassiveManager().getIncomingKnockbackMultiplier(targetUuid.toString());
        if (mod.getRuntimePerkManager() != null) {
            multiplier *= mod.getRuntimePerkManager().getIncomingKnockbackMultiplier(targetData);
        }
        if (multiplier >= 0.999) {
            return;
        }

        KnockbackComponent knockback = damage.getIfPresentMetaObject(Damage.KNOCKBACK_COMPONENT);
        if (knockback == null) {
            LOG.info("[MOTM] Incoming knockback passive had no knockback component: playerId="
                    + targetUuid
                    + " multiplier="
                    + String.format(java.util.Locale.ROOT, "%.3f", multiplier));
            return;
        }

        knockback.addModifier(multiplier);
        LOG.info("[MOTM] Incoming knockback passive applied: playerId="
                + targetUuid
                + " multiplier="
                + String.format(java.util.Locale.ROOT, "%.3f", multiplier)
                + " velocity="
                + knockback.getVelocity()
                + " duration="
                + knockback.getDuration());
    }
}
