package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;

public final class TerrainAbilityHytaleAdapter {
    private final TerrainPlacementHytaleAdapter placementAdapter;
    private final Support support;

    public TerrainAbilityHytaleAdapter(TerrainPlacementHytaleAdapter placementAdapter, Support support) {
        this.placementAdapter = placementAdapter;
        this.support = support;
    }

    public String placeAbilityTerrainSelection(Player runtimePlayer,
                                               PlayerData player,
                                               AbilityData ability,
                                               Ref<EntityStore> explicitTargetRef,
                                               Vector3i targetBlock,
                                               String reason) {
        return placeAbilityTerrainSelection(runtimePlayer, player, ability, explicitTargetRef, targetBlock, reason, 0L);
    }

    public String placeAbilityTerrainSelection(Player runtimePlayer,
                                               PlayerData player,
                                               AbilityData ability,
                                               Ref<EntityStore> explicitTargetRef,
                                               Vector3i targetBlock,
                                               String reason,
                                               long forcedExpireAtMillis) {
        if (runtimePlayer == null || ability == null || placementAdapter == null || support == null) {
            return "";
        }

        World world = runtimePlayer.getWorld();
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        Vector3d origin = support.position(playerRef, store);
        Vector3d forward = support.direction(playerRef, store);
        if (world == null || origin == null || forward == null) {
            return "";
        }

        long expireAt = forcedExpireAtMillis > 0
                ? forcedExpireAtMillis
                : System.currentTimeMillis()
                + (long) (Math.max(2.0, ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 4.0) * 1000);
        Vector3d center = targetBlock != null
                ? new Vector3d(targetBlock.x + 0.5,
                targetBlock.y + 1.0,
                targetBlock.z + 0.5)
                : com.motm.util.MotmVectors.addScaled(origin, new Vector3d(forward.x, 0.0, forward.z), 3.5);

        return switch (reason) {
            case "obsidian_skin" -> "";
            case "rooted" -> placementAdapter.placeSurfacePatchSelection(world, reason, origin, 1, expireAt,
                    "Plant_Roots_Leafy", "Plant_Roots_Cave", "Plant_Vine_Thick_Roots");
            case "sapling" -> placementAdapter.placeSurfaceColumnSelection(world, reason, center, 1, expireAt,
                    "Furniture_Temple_Emerald_Statue", "Plant_Sapling_Oak", "Plant_Sapling_Crystal");
            case "nightshade" -> placementAdapter.placeSurfaceColumnSelection(world, reason, center, 1, expireAt,
                    "Plant_Flower_Tall_Red", "Plant_Flower_Common_Purple", "Plant_Flower_Common_Blue");
            case "frolick" -> {
                boolean started = placementAdapter.startMovingTerrainTrail(world, playerRef, reason, expireAt,
                        "Plant_Flower_Common_Purple", "Plant_Flower_Common_Yellow", "Plant_Flower_Common_Blue");
                yield started ? "moving flower trail" : "";
            }
            case "cacti_cluster" -> placementAdapter.placeSurfaceColumnSelection(world, reason, center, 2, expireAt,
                    "Plant_Cactus_Ball_1", "Plant_Cactus_1", "Prototype_Cactus_Kit_Tall_Base", "Prototype_Cactus_One");
            case "lapidary" -> {
                String placed = placementAdapter.placeFloatingClusterSelection(world, reason, center,
                        2, 2, 2, expireAt,
                        "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Plant_Bush_Crystal");
                support.applyEffectById(playerRef, store, "MOTM_Proof_Gem_Green");
                String hpProxy = support.spawnLapidaryGemProxy(world, player, ability, center, expireAt);
                yield placed.isBlank()
                        ? "green gem aura" + hpProxy
                        : placed + " + green aura" + hpProxy;
            }
            case "fracture" -> {
                Vector3d gemCenter = support.resolveActiveLapidaryGemCenter(player, ability, store);
                Vector3d burstCenter = gemCenter != null ? gemCenter : center;
                String terrain = placementAdapter.placeRingBlockSelection(world, reason, burstCenter, Math.max(2.0, ability.getRadius()),
                        Math.min(expireAt, System.currentTimeMillis() + 1200L),
                        "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Plant_Bush_Crystal");
                yield terrain.isBlank() ? "green fracture burst" : "green fracture burst + " + terrain;
            }
            case "refraction" -> {
                Vector3d gemCenter = support.resolveActiveLapidaryGemCenter(player, ability, store);
                Vector3d auraCenter = gemCenter != null ? gemCenter : center;
                String terrain = placementAdapter.placeRingBlockSelection(world, reason, auraCenter, Math.max(2.0, ability.getRadius()),
                        expireAt, "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Plant_Bush_Crystal");
                yield terrain.isBlank() ? "green refraction aura" : "green refraction aura + " + terrain;
            }
            case "glare" -> {
                if (explicitTargetRef != null) {
                    support.applyEffectById(explicitTargetRef, store, "MOTM_Proof_Coating_Stone");
                    String terrain = placementAdapter.placeSurfacePatchSelection(world, reason, center, 1, expireAt,
                            "Furniture_Temple_Light_Statue", "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
                    yield terrain.isBlank() ? "target stone coating" : "target stone coating + " + terrain;
                }
                yield "";
            }
            case "gargoyle" -> placementAdapter.placeSurfaceColumnSelection(world, reason, origin, 1, expireAt,
                    "Furniture_Ancient_Statue", "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
            case "debris" -> placementAdapter.placeTrailSelection(world, reason, origin, forward, System.currentTimeMillis() + 2400L,
                    "Soil_Dirt", "Rock_Stone", "Rock_Stone_Brick");
            case "sandstorm" -> placementAdapter.placeRingBlockSelection(world, reason, origin, Math.max(2.0, ability.getRadius()),
                    System.currentTimeMillis() + 2200L, "Soil_Sand", "Rock_Sandstone", "Rock_Sandstone_White");
            case "tunnel" -> placementAdapter.placeTrailSelection(world, reason, origin, forward, System.currentTimeMillis() + 2400L,
                    "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
            default -> "";
        };
    }

    public interface Support {
        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        Vector3d direction(Ref<EntityStore> ref, Store<EntityStore> store);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String spawnLapidaryGemProxy(World world,
                                     PlayerData player,
                                     AbilityData ability,
                                     Vector3d center,
                                     long expireAtMillis);

        Vector3d resolveActiveLapidaryGemCenter(PlayerData player, AbilityData ability, Store<EntityStore> store);
    }
}
