package com.motm.runtime.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.state.RuntimePlayerRegistry;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only live-player lookup and geometry view used by commands and harness
 * tasks.
 */
public final class RuntimePlayerView {

    private final RuntimePlayerRegistry registry;
    private final Logger log;

    public RuntimePlayerView(RuntimePlayerRegistry registry, Logger log) {
        this.registry = registry;
        this.log = log;
    }

    public Player get(String playerId) {
        return playerId == null || registry == null ? null : registry.get(playerId);
    }

    public Player get(Ref<EntityStore> entityRef) {
        if (entityRef == null || !entityRef.isValid() || registry == null) {
            return null;
        }
        for (Player player : registry.players()) {
            if (player == null) {
                continue;
            }
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef != null
                    && playerRef.isValid()
                    && playerRef.getStore() == entityRef.getStore()
                    && playerRef.getIndex() == entityRef.getIndex()) {
                return player;
            }
        }
        return null;
    }

    public String findOnlinePlayerId(Player runtimePlayer) {
        if (runtimePlayer == null || registry == null) {
            return null;
        }

        for (Map.Entry<String, Player> entry : registry.entries()) {
            if (entry.getValue() == runtimePlayer) {
                return entry.getKey();
            }
        }
        return null;
    }

    public PlayerRef universePlayerRef(Player player) {
        if (player == null) {
            return null;
        }

        try {
            var entityRef = player.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                return null;
            }

            return entityRef.getStore().getComponent(entityRef, PlayerRef.getComponentType());
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public String runtimePlayerId(Player player) {
        String cachedPlayerId = findOnlinePlayerId(player);
        if (cachedPlayerId != null) {
            return cachedPlayerId;
        }

        try {
            PlayerRef playerRef = universePlayerRef(player);
            return playerRef != null && playerRef.getUuid() != null
                    ? playerRef.getUuid().toString()
                    : null;
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
        if (player == null) {
            return false;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return false;
        }

        if (currentStore == null) {
            return true;
        }

        Store<EntityStore> playerStore = playerRef.getStore();
        if (playerStore == currentStore || playerStore.equals(currentStore)) {
            return true;
        }

        World playerWorld = player.getWorld();
        World currentWorld = currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : null;

        if (playerWorld != null && currentWorld != null) {
            return playerWorld == currentWorld || playerWorld.equals(currentWorld);
        }

        return false;
    }

    public String describePosition(String playerId) {
        Player player = get(playerId);
        try {
            Vector3d position = playerPosition(player);
            Vector3d forward = normalizeHorizontal(playerForward(player));
            String worldId = player != null && player.getWorld() != null ? player.getWorld().getName() : "unknown";
            String summary = "[MOTM] Dev position: world=" + worldId
                    + " position=" + formatVector(position)
                    + " forward=" + formatVector(forward);
            if (log != null) {
                log.info(summary);
            }
            return summary;
        } catch (Throwable e) {
            String summary = "[MOTM] Dev position failed safely: " + e.getMessage();
            if (log != null) {
                log.log(Level.WARNING, summary, e);
            }
            return summary;
        }
    }

    public Vector3d playerPosition(Player player) {
        if (player == null) {
            return null;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }

        return entityPosition(playerRef.getStore(), playerRef);
    }

    public Vector3d playerForward(Player player) {
        if (player == null) {
            return null;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }

        TransformComponent transform = playerRef.getStore().getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        Vector3d direction = new Vector3d(transform.getTransform().getDirection());
        if (!direction.isFinite() || direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return direction;
    }

    public Vector3d entityPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }

        return transform.getTransform().getPosition();
    }

    public boolean isCrouching(Player player) {
        if (player == null) {
            return false;
        }

        Ref<EntityStore> entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            return false;
        }

        MovementStatesComponent movementStates = store.getComponent(
                entityRef,
                MovementStatesComponent.getComponentType()
        );
        return movementStates != null
                && movementStates.getMovementStates() != null
                && movementStates.getMovementStates().crouching;
    }

    public static Vector3d normalizeHorizontal(Vector3d direction) {
        if (direction == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        Vector3d horizontal = new Vector3d(direction.x, 0.0, direction.z);
        if (!horizontal.isFinite() || horizontal.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        horizontal.normalize();
        return horizontal;
    }

    public static String formatVector(Vector3d position) {
        if (position == null) {
            return "(unknown)";
        }
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", position.x, position.y, position.z);
    }
}
