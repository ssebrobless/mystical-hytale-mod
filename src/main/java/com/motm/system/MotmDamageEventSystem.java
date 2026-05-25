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
                applyIncomingKnockbackPassive(targetUuid, damage);
                damage.setAmount(mod.getClassPassiveManager().handleIncomingPlayerDamage(
                        targetUuid.toString(),
                        targetRef,
                        store,
                        damage.getAmount()
                ));
            }
            return;
        }

        if (targetPlayer != null && targetUuid != null && damage.getAmount() > 0.0f) {
            applyIncomingKnockbackPassive(targetUuid, damage);
            damage.setAmount(mod.getClassPassiveManager().handleIncomingPlayerDamage(
                    targetUuid.toString(),
                    targetRef,
                    store,
                    damage.getAmount()
            ));
            if (damage.getAmount() <= 0.0f) {
                return;
            }
        }

        Damage.EntitySource source = (Damage.EntitySource) damage.getSource();

        Ref<EntityStore> sourceRef = source.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return;
        }

        Player runtimePlayer = store.getComponent(sourceRef, Player.getComponentType());
        if (runtimePlayer == null || runtimePlayer.getInventory() == null) {
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

        ItemStack held = runtimePlayer.getInventory().getItemInHand();
        String itemId = held != null ? held.getItemId() : null;
        String response = mod.getGameplayPlaybackManager().handleNativeWeaponDamage(
                runtimePlayer,
                playerData,
                targetRef,
                itemId,
                damage
        );
        if (response != null && !response.isBlank()) {
            runtimePlayer.sendMessage(Message.raw(response));
        }
    }

    private void applyIncomingKnockbackPassive(UUID targetUuid, Damage damage) {
        if (targetUuid == null || damage == null) {
            return;
        }

        double multiplier = mod.getClassPassiveManager().getIncomingKnockbackMultiplier(targetUuid.toString());
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
