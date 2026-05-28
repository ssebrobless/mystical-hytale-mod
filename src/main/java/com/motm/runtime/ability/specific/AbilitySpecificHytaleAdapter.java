package com.motm.runtime.ability.specific;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.ability.self.SelfHytaleAdapter;
import com.motm.runtime.ability.terrain.LavaHazardRuntimeState;
import com.motm.runtime.ability.terrain.TerrainAbilityHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainPlacementHytaleAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AbilitySpecificHytaleAdapter {
    private final LavaHazardRuntimeState lavaHazardState;
    private final TerrainPlacementHytaleAdapter terrainPlacementAdapter;
    private final TerrainAbilityHytaleAdapter terrainAbilityAdapter;
    private final TerrainHytaleAdapter terrainHytaleAdapter;
    private final SelfHytaleAdapter selfAdapter;
    private final Support support;

    public AbilitySpecificHytaleAdapter(LavaHazardRuntimeState lavaHazardState,
                                        TerrainPlacementHytaleAdapter terrainPlacementAdapter,
                                        TerrainAbilityHytaleAdapter terrainAbilityAdapter,
                                        TerrainHytaleAdapter terrainHytaleAdapter,
                                        SelfHytaleAdapter selfAdapter,
                                        Support support) {
        this.lavaHazardState = lavaHazardState;
        this.terrainPlacementAdapter = terrainPlacementAdapter;
        this.terrainAbilityAdapter = terrainAbilityAdapter;
        this.terrainHytaleAdapter = terrainHytaleAdapter;
        this.selfAdapter = selfAdapter;
        this.support = support;
    }

    public Result apply(Player runtimePlayer,
                        PlayerData player,
                        AbilityData ability,
                        Ref<EntityStore> explicitTargetRef,
                        Vector3i targetBlock,
                        boolean movementApplied) {
        if (runtimePlayer == null || player == null || ability == null || support == null) {
            return Result.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        if (playerRef == null || store == null) {
            return Result.none();
        }

        String abilityId = lower(ability.getId());
        List<String> parts = new ArrayList<>();

        switch (abilityId) {
            case "metal_coat" -> {
                if (support.applyEffectById(playerRef, store, "MOTM_Proof_Coating_Metal")) {
                    parts.add("metal coating");
                }
            }
            case "alloy_enhancement" -> {
                if (support.applyEffectById(playerRef, store, "MOTM_Proof_Alloy_Enhancement")) {
                    parts.add("alloy coating");
                }
            }
            case "obsidian_skin" -> applyObsidianSkin(runtimePlayer, player, ability, playerRef, store, parts);
            case "rooted" -> addTerrain(parts, runtimePlayer, player, ability, explicitTargetRef, targetBlock, "rooted");
            case "sapling", "nightshade", "frolick", "cacti_cluster", "lapidary", "glare", "debris",
                 "fracture", "refraction" ->
                    addTerrain(parts, runtimePlayer, player, ability, explicitTargetRef, targetBlock, abilityId);
            case "gargoyle" -> {
                if (support.applyEffectById(playerRef, store, "MOTM_Proof_Coating_Stone")) {
                    parts.add("stone coating");
                }
            }
            case "sandstorm" -> {
                String terrain = placeTerrain(runtimePlayer, player, ability, explicitTargetRef, targetBlock, abilityId);
                if (!terrain.isBlank()) {
                    parts.add("sand surface ring");
                    parts.add(terrain);
                }
            }
            case "tunnel" -> {
                if (support.applyEffectById(playerRef, store, "MOTM_Proof_Coating_Stone")) {
                    parts.add("stone block form cue");
                }
                addTerrain(parts, runtimePlayer, player, ability, explicitTargetRef, targetBlock, abilityId);
                if (movementApplied) {
                    parts.add("surface-safe tunnel move");
                }
            }
            default -> {
                return Result.none();
            }
        }

        return parts.isEmpty() ? Result.none() : new Result(String.join(" | ", parts));
    }

    private void applyObsidianSkin(Player runtimePlayer,
                                   PlayerData player,
                                   AbilityData ability,
                                   Ref<EntityStore> playerRef,
                                   Store<EntityStore> store,
                                   List<String> parts) {
        long nowMillis = System.currentTimeMillis();
        long lavaExpireAt = nowMillis + 1_800L;
        long guardExpireAt = nowMillis + 7_500L;
        if (lavaHazardState != null) {
            lavaHazardState.protectUntil(player.getPlayerId(), guardExpireAt);
        }

        String lavaShell = terrainPlacementAdapter == null
                ? ""
                : terrainPlacementAdapter.placeObsidianBlockShellSelection(
                runtimePlayer.getWorld(),
                "obsidian_skin",
                support.position(playerRef, store),
                lavaExpireAt,
                terrainHytaleAdapter,
                "Rock_Volcanic_Cracked_Lava",
                "Rock_Volcanic_Cracked_Incandescent",
                "Rock_Magma_Cooled"
        );
        if (!lavaShell.isBlank()) {
            parts.add(lavaShell.replace("terrain ", "lava shell "));
        } else if (support.applyEffectById(playerRef, store, "MOTM_Proof_Obsidian_Lava_Wrap")) {
            parts.add("lava wrap");
            if (selfAdapter != null) {
                selfAdapter.startActiveSelfEffect(
                        playerRef,
                        player.getPlayerId(),
                        "MOTM_Proof_Obsidian_Lava_Wrap",
                        lavaExpireAt
                );
            }
        }

        if (selfAdapter != null) {
            selfAdapter.startPlayerAnchor(player, playerRef, store, lavaExpireAt, "MOTM_Proof_Coating_Obsidian");
            selfAdapter.startActiveSelfEffect(
                    playerRef,
                    player.getPlayerId(),
                    "MOTM_Proof_Coating_Obsidian",
                    guardExpireAt,
                    lavaExpireAt
            );
        }
        support.applyRoot(player.getPlayerId(), ability, 1.8);
        parts.add("obsidian root");
        parts.add("queued obsidian coating");
    }

    private void addTerrain(List<String> parts,
                            Player runtimePlayer,
                            PlayerData player,
                            AbilityData ability,
                            Ref<EntityStore> explicitTargetRef,
                            Vector3i targetBlock,
                            String reason) {
        String terrain = placeTerrain(runtimePlayer, player, ability, explicitTargetRef, targetBlock, reason);
        if (!terrain.isBlank()) {
            parts.add(terrain);
        }
    }

    private String placeTerrain(Player runtimePlayer,
                                PlayerData player,
                                AbilityData ability,
                                Ref<EntityStore> explicitTargetRef,
                                Vector3i targetBlock,
                                String reason) {
        return terrainAbilityAdapter == null
                ? ""
                : terrainAbilityAdapter.placeAbilityTerrainSelection(
                runtimePlayer,
                player,
                ability,
                explicitTargetRef,
                targetBlock,
                reason
        );
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record Result(String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result("");
        }
    }

    public interface Support {
        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        void applyRoot(String playerId, AbilityData ability, double seconds);
    }
}
