package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;
import com.motm.util.HytaleAssetResolver;

import java.util.Locale;

public final class SummonLifecycleHytaleAdapter {
    private final SummonRuntimeState summonState;
    private final SummonActivationRuntime activationRuntime;
    private final Support support;

    public SummonLifecycleHytaleAdapter(SummonRuntimeState summonState,
                                        SummonActivationRuntime activationRuntime,
                                        Support support) {
        this.summonState = summonState;
        this.activationRuntime = activationRuntime;
        this.support = support;
    }

    public Result spawnSummon(Player runtimePlayer,
                              PlayerData player,
                              StyleData style,
                              AbilityData ability,
                              Vector3i targetBlock) {
        if (runtimePlayer == null || player == null || style == null || ability == null
                || summonState == null || activationRuntime == null) {
            return Result.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return Result.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return Result.none();
        }

        String modelId = resolveSummonModelId(player.getPlayerClass(), style.getId(), ability);
        if (modelId == null || modelId.isBlank()) {
            support.logWarning("[MOTM] No summon model mapping for " + ability.getId());
            return Result.none();
        }

        Vector3d spawnPosition = resolveSummonPosition(playerRef, store, ability, targetBlock);
        if (spawnPosition == null) {
            return Result.none();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return Result.none();
        }
        NPCEntity summon = new NPCEntity(world);
        summon.setRoleName(modelId);
        summon.setDespawnTime((float) Math.max(2.0, ability.getDurationSeconds()));
        world.spawnEntity(summon, spawnPosition, new Vector3f(0f, 0f, 0f));

        Ref<EntityStore> summonRef = summon.getReference();
        if (summonRef == null || !summonRef.isValid()) {
            return Result.none();
        }

        NPCEntity.setAppearance(summonRef, modelId, summonRef.getStore());
        support.applyEffectById(summonRef, summonRef.getStore(),
                support.resolveImpactEffectId(player.getPlayerClass(), style.getId(), ability));

        long now = System.currentTimeMillis();
        long expireAt = now + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
        ActiveSummon activeSummon = activationRuntime.create(
                player.getPlayerId(),
                summonRef,
                playerRef,
                player.getPlayerClass(),
                style.getId(),
                ability,
                now,
                expireAt,
                resolveSummonRawBaseDamage(player, ability)
        );
        if (activeSummon == null) {
            return Result.none();
        }
        summonState.addSummon(player.getPlayerId(), activeSummon);

        support.logInfo("[MOTM] Summon spawned: abilityId=" + ability.getId()
                + " model=" + modelId
                + " position=" + formatVector(spawnPosition)
                + " duration=" + AbilityPresentation.formatDecimal(Math.max(2.0, ability.getDurationSeconds())) + "s");

        return new Result(1, "summoned " + humanize(modelId));
    }

    public int removeSummonsForPlayer(String playerId) {
        return summonState == null ? 0 : summonState.removeSummonsForPlayer(playerId, this::despawnSummon);
    }

    public boolean despawnSummon(ActiveSummon summon) {
        if (summon == null) {
            return true;
        }
        Store<EntityStore> store = summon.ref() != null ? summon.ref().getStore() : null;
        NPCEntity npc = store != null ? store.getComponent(summon.ref(), NPCEntity.getComponentType()) : null;
        if (npc != null) {
            npc.setToDespawn();
        }
        return true;
    }

    private Vector3d resolveSummonPosition(Ref<EntityStore> playerRef,
                                           Store<EntityStore> store,
                                           AbilityData ability,
                                           Vector3i targetBlock) {
        Vector3d origin = position(playerRef, store);
        Vector3d forward = direction(playerRef, store);
        if (origin == null || forward == null) {
            return null;
        }

        if (targetBlock != null) {
            return new Vector3d(targetBlock.x + 0.5, targetBlock.y + 1.0, targetBlock.z + 0.5);
        }

        double distance = ability.getRange() > 0 ? Math.min(ability.getRange(), 4.0) : 2.5;
        return new Vector3d(origin.x + (forward.x * distance), origin.y, origin.z + (forward.z * distance));
    }

    private String resolveSummonModelId(String classId, String styleId, AbilityData ability) {
        SummonRuntimeSpec summonSpec = SummonRuntimeSpecs.resolve(ability);
        if (summonSpec.modelId() != null && !summonSpec.modelId().isBlank()) {
            return summonSpec.modelId();
        }

        return HytaleAssetResolver.resolveModelId(classId, styleId, ability);
    }

    private double resolveSummonRawBaseDamage(PlayerData player, AbilityData ability) {
        return SummonRuntimeSpecs.rawBaseDamage(
                ability.getDamagePercent(),
                player.getLevel(),
                support.abilityPowerMultiplier(player.getLevel())
        );
    }

    private static Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        Transform transform = transform(ref, store);
        return transform == null ? null : transform.getPosition();
    }

    private static Vector3d direction(Ref<EntityStore> ref, Store<EntityStore> store) {
        Transform transform = transform(ref, store);
        if (transform == null || transform.getDirection() == null) {
            return null;
        }

        Vector3d direction = transform.getDirection().clone();
        if (!direction.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        if (direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        direction.normalize();
        return direction;
    }

    private static Transform transform(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform();
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "(null)";
        }
        return "("
                + String.format(Locale.US, "%.2f", vector.x)
                + ","
                + String.format(Locale.US, "%.2f", vector.y)
                + ","
                + String.format(Locale.US, "%.2f", vector.z)
                + ")";
    }

    public interface Support {
        double abilityPowerMultiplier(int playerLevel);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        void logInfo(String message);

        void logWarning(String message);
    }

    public record Result(int spawned, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result(0, "");
        }
    }
}
