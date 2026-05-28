package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.runtime.state.FreeCastRuntimeState;

import java.util.List;
import java.util.logging.Logger;

/**
 * Applies live free-cast test safety while dev scenarios are running.
 */
public final class FreeCastSafetyProcessor {

    private final FreeCastRuntimeState state;
    private final Hooks hooks;
    private final Logger log;

    public FreeCastSafetyProcessor(FreeCastRuntimeState state, Hooks hooks, Logger log) {
        this.state = state;
        this.hooks = hooks;
        this.log = log;
    }

    public void process(Store<EntityStore> currentStore) {
        for (String playerId : state.enabledPlayers()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null || !hooks.isPlayerInStore(player, currentStore)) {
                continue;
            }
            applySafetyTick(playerId, player);
        }
    }

    public void ensureInvulnerability(Player player) {
        if (!setRuntimeInvulnerability(player, true) && player != null) {
            hooks.sendMessage(player, Message.raw("[MOTM] Test Protection warning: native invulnerability did not attach. "
                    + "Free-cast is still on, but arena mobs may still hit you."));
        }
    }

    public void clearInvulnerability(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        Player player = hooks.runtimePlayer(playerId);
        if (player != null) {
            setRuntimeInvulnerability(player, false);
            resetMovementNormalization(player);
        }
    }

    private void applySafetyTick(String playerId, Player player) {
        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }

            EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (entityStatMap == null) {
                return;
            }

            EntityStatValue healthBeforeSafety = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            if (healthBeforeSafety != null) {
                float currentHealth = healthBeforeSafety.get();
                Float previousHealth = state.rememberObservedHealth(playerId, currentHealth);
                if (previousHealth != null && currentHealth < previousHealth - 0.5f) {
                    PlayerData playerData = hooks.playerData(playerId);
                    log.info("[MOTM] Free-cast health drop detected: player="
                            + (playerData != null ? playerData.getPlayerName() : playerId)
                            + " class=" + (playerData != null ? playerData.getPlayerClass() : "unknown")
                            + " styles=" + (playerData != null ? playerData.getSelectedStyles() : List.of())
                            + " from=" + previousHealth
                            + " to=" + currentHealth
                            + " burn=" + hooks.hasStatusEffect(playerId, StatusEffect.Type.BURN)
                            + " dot=" + hooks.hasStatusEffect(playerId, StatusEffect.Type.DOT));
                }
            }

            entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
            maximizeStatIfPresent(entityStatMap, DefaultEntityStatTypes.getStamina());
            maximizeStatIfPresent(entityStatMap, DefaultEntityStatTypes.getMana());
            maximizeStatIfPresent(entityStatMap, DefaultEntityStatTypes.getSignatureEnergy());
            hooks.removeStatusEffect(playerId, StatusEffect.Type.BURN);
            hooks.removeStatusEffect(playerId, StatusEffect.Type.DOT);
            applyMovementNormalization(player);
            ensureInvulnerability(player);
        } catch (IllegalStateException e) {
            log.fine("[MOTM] Skipped free-cast safety tick on the wrong store for "
                    + playerId + ": " + e.getMessage());
        }
    }

    private void applyMovementNormalization(Player player) {
        if (player == null) {
            return;
        }
        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }
            MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
            if (movementManager == null || movementManager.getSettings() == null) {
                return;
            }
            var settings = movementManager.getSettings();
            settings.baseSpeed = Math.max(settings.baseSpeed, 6.25f);
            settings.forwardWalkSpeedMultiplier = Math.max(settings.forwardWalkSpeedMultiplier, 1.10f);
            settings.backwardWalkSpeedMultiplier = Math.max(settings.backwardWalkSpeedMultiplier, 1.05f);
            settings.strafeWalkSpeedMultiplier = Math.max(settings.strafeWalkSpeedMultiplier, 1.10f);
            settings.forwardRunSpeedMultiplier = Math.max(settings.forwardRunSpeedMultiplier, 1.25f);
            settings.backwardRunSpeedMultiplier = Math.max(settings.backwardRunSpeedMultiplier, 1.10f);
            settings.strafeRunSpeedMultiplier = Math.max(settings.strafeRunSpeedMultiplier, 1.25f);
            settings.forwardSprintSpeedMultiplier = Math.max(settings.forwardSprintSpeedMultiplier, 1.45f);
            settings.acceleration = Math.max(settings.acceleration, 0.16f);
            PlayerRef universePlayerRef = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
        } catch (IllegalStateException e) {
            log.fine("[MOTM] Skipped free-cast movement normalization on the wrong store: " + e.getMessage());
        } catch (Exception e) {
            log.warning("[MOTM] Failed to normalize free-cast movement: " + e.getMessage());
        }
    }

    private void resetMovementNormalization(Player player) {
        if (player == null) {
            return;
        }
        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }
            MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
            if (movementManager != null) {
                movementManager.applyDefaultSettings();
                PlayerRef universePlayerRef = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
                if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                    movementManager.update(universePlayerRef.getPacketHandler());
                }
            }
        } catch (IllegalStateException e) {
            log.fine("[MOTM] Skipped free-cast movement reset on the wrong store: " + e.getMessage());
        } catch (Exception e) {
            log.warning("[MOTM] Failed to reset free-cast movement normalization: " + e.getMessage());
        }
    }

    private void maximizeStatIfPresent(EntityStatMap entityStatMap, int statType) {
        if (entityStatMap == null) {
            return;
        }
        EntityStatValue stat = entityStatMap.get(statType);
        if (stat == null || stat.getMax() <= 0.0f) {
            return;
        }
        entityStatMap.maximizeStatValue(statType);
    }

    private boolean setRuntimeInvulnerability(Player player, boolean enabled) {
        if (player == null) {
            return false;
        }

        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return false;
            }

            Store<EntityStore> store = playerRef.getStore();
            var componentType = Invulnerable.getComponentType();
            var existing = store.getComponent(playerRef, componentType);

            if (enabled) {
                if (existing != null) {
                    return true;
                }
                store.addComponent(playerRef, componentType);
                return true;
            }

            if (existing == null) {
                return true;
            }
            store.removeComponent(playerRef, componentType);
            return true;
        } catch (Exception e) {
            log.warning("[MOTM] Failed to toggle dev invulnerability for "
                    + playerLabel(player) + ": " + e.getMessage());
            return false;
        }
    }

    private String playerLabel(Player player) {
        try {
            if (player == null || player.getReference() == null || !player.getReference().isValid()
                    || player.getReference().getStore() == null) {
                return "unknown";
            }
            PlayerRef playerRef = player.getReference().getStore()
                    .getComponent(player.getReference(), PlayerRef.getComponentType());
            if (playerRef != null && playerRef.getUsername() != null && !playerRef.getUsername().isBlank()) {
                return playerRef.getUsername();
            }
        } catch (Exception ignored) {
            // Keep fallback label.
        }
        return "unknown";
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        PlayerData playerData(String playerId);

        boolean hasStatusEffect(String playerId, StatusEffect.Type type);

        void removeStatusEffect(String playerId, StatusEffect.Type type);

        void sendMessage(Player player, Message message);
    }
}
