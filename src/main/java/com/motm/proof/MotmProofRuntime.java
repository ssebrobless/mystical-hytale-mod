package com.motm.proof;

import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Registry and execution boundary for `/motm dev proof` runners.
 */
public final class MotmProofRuntime {

    private final Map<String, MotmProofRunner> runners = new LinkedHashMap<>();

    public void initialize(DefaultProofActions actions, Logger log) {
        if (!runners.isEmpty()) {
            return;
        }

        runners.put("coating-metal",
                (playerId, player, currentStore, forward) -> actions.applyProofEffect(player, "MOTM_Proof_Coating_Metal", "coating-metal"));
        runners.put("coating-obsidian",
                (playerId, player, currentStore, forward) -> actions.applyProofEffect(player, "MOTM_Proof_Coating_Obsidian", "coating-obsidian"));
        runners.put("coating-stone",
                (playerId, player, currentStore, forward) -> actions.applyProofEffect(player, "MOTM_Proof_Coating_Stone", "coating-stone"));
        runners.put("coating-poison-target",
                (playerId, player, currentStore, forward) -> actions.applyProofTargetEffect(playerId, currentStore, "MOTM_Proof_Coating_Poison", "coating-poison-target"));
        runners.put("tempblock-metal-wall",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-metal-wall", "Metal_Iron", 2, 2, 0));
        runners.put("tempblock-stone-pillar",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-stone-pillar", "Rock_Stone_Brick_Pillar_Middle", 1, 3, 0));
        runners.put("tempblock-flower",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-flower", "Plant_Flower_Common_Purple", 1, 1, 0));
        runners.put("tempblock-sapling",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-sapling", "Plant_Sapling_Oak", 1, 1, 0));
        runners.put("tempblock-gem-cluster",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-gem-cluster", 2, 2, 1,
                        "Rock_Crystal_Green_Block",
                        "Rock_Crystal_Green_Large",
                        "Plant_Bush_Crystal",
                        "Plant_Leaves_Crystal",
                        "Plant_Sapling_Crystal"));
        runners.put("tempblock-cactus",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-cactus", 1, 2, 0,
                        "Plant_Cactus_1",
                        "Prototype_Cactus_Kit_Tall_Base",
                        "Prototype_Cactus_One",
                        "Plant_Cactus_Ball_1"));
        runners.put("tempblock-roots",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-roots", 2, 1, 0,
                        "Plant_Roots_Leafy",
                        "Plant_Roots_Cave",
                        "Plant_Roots_Cave_Small",
                        "Plant_Vine_Thick_Roots"));
        runners.put("tempblock-lava-field",
                (playerId, player, currentStore, forward) -> actions.runTempBlockProof(player, "tempblock-lava-field", 5, 1, 0,
                        "Rock_Volcanic_Cracked_Lava",
                        "Rock_Volcanic_Cracked_Incandescent",
                        "Rock_Magma_Cooled"));
        runners.put("tempfluid-water-field",
                (playerId, player, currentStore, forward) -> actions.runTempFluidProof(player, "tempfluid-water-field", 2, "Fluid_Water", "Water", "water"));
        runners.put("tempfluid-mud-field",
                (playerId, player, currentStore, forward) -> actions.runTempFluidProof(player, "tempfluid-mud-field", 3, "Fluid_Water", "Water", "water"));
        runners.put("native-projectile-fireball",
                (playerId, player, currentStore, forward) -> actions.runNativeProjectileProof(
                        player,
                        currentStore,
                        "native-projectile-fireball",
                        forward,
                        "Projectile_Config_Fireball",
                        "Weapons/Stick/Projectile_Config_Fireball"));
        runners.put("proxy-magma-blob",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-magma-blob", "Slug_Magma", "MOTM_Terra_Impact", 2.5));
        runners.put("proxy-cactus-projectile",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-cactus-projectile", "Test_Dummy_Stationary", "MOTM_Terra_Cast", 2.5));
        runners.put("proxy-gem",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-gem", "Spark_Living", "MOTM_Terra_Gem_Field", 3.0));
        runners.put("proxy-gem-aura",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-gem-aura", "Spark_Living", "MOTM_Proof_Gem_Green", 3.0));
        runners.put("proxy-glass-shards",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-glass-shards", "Spark_Living", "MOTM_Terra_Gem_Cast", 2.5));
        runners.put("proxy-sand-cloud",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-sand-cloud", "Spark_Living", "MOTM_Proof_Sand_Cloud", 3.0));
        runners.put("proxy-debris-wave",
                (playerId, player, currentStore, forward) -> actions.runProxyProof(player, "proxy-debris-wave", "Spark_Living", "MOTM_Proof_Debris_Wave", 3.0));
        runners.put("movement-burrow",
                (playerId, player, currentStore, forward) -> actions.runMovementProof(player, currentStore, "movement-burrow", forward, 4.0, false, false));
        runners.put("movement-tunnel",
                (playerId, player, currentStore, forward) -> actions.runMovementProof(player, currentStore, "movement-tunnel", forward, 2.0, true, false));
        runners.put("movement-dust-devil",
                (playerId, player, currentStore, forward) -> actions.runMovementProof(player, currentStore, "movement-dust-devil", forward, 5.0, false, true));
        runners.put("gate-r6-particle-world",
                (playerId, player, currentStore, forward) -> actions.runParticleWorldProof(
                        player, currentStore, "gate-r6-particle-world", "Block_Break_Stone", 3.0));
        runners.put("gate-r7-entity-scale",
                (playerId, player, currentStore, forward) -> actions.runEntityScaleProof(
                        player, currentStore, "gate-r7-entity-scale", forward, 3.0));
        runners.put("gate-r8-custom-particle",
                (playerId, player, currentStore, forward) -> actions.runParticleWorldProof(
                        player, currentStore, "gate-r8-custom-particle", "MOTM_Proof_Pink_Halo", 3.0));
        runners.put("gate-r9-dynamic-light",
                (playerId, player, currentStore, forward) -> actions.runDynamicLightProof(
                        player, currentStore, "gate-r9-dynamic-light", forward, 3.0));
        runners.put("gate-r10-intangible",
                (playerId, player, currentStore, forward) -> actions.runIntangibleProof(
                        player, currentStore, "gate-r10-intangible", forward, 2.0));
        runners.put("gate-r11-player-clone",
                (playerId, player, currentStore, forward) -> actions.runPlayerCloneProof(
                        player, currentStore, "gate-r11-player-clone", forward, 2.0));


        if (!runners.keySet().equals(Set.copyOf(MotmProofCatalog.ids()))) {
            log.warning("[MOTM] Proof runner registry differs from MotmProofCatalog ids.");
        }
    }

    public String run(String playerId,
                      Player player,
                      Store<EntityStore> currentStore,
                      Vector3d forward,
                      String proofId) {
        MotmProofRunner runner = runners.get(MotmProofCatalog.normalize(proofId));
        if (runner == null) {
            return "[MOTM] Proof " + proofId + " FAIL: unknown proof id.";
        }
        return runner.run(playerId, player, currentStore, forward);
    }

    public Set<String> ids() {
        return Set.copyOf(runners.keySet());
    }

    @FunctionalInterface
    public interface MotmProofRunner {
        String run(String playerId, Player player, Store<EntityStore> currentStore, Vector3d forward);
    }

    public interface DefaultProofActions {
        String applyProofEffect(Player player, String effectId, String proofId);

        String applyProofTargetEffect(String playerId, Store<EntityStore> store, String effectId, String proofId);

        String runTempBlockProof(Player player, String proofId, String blockId, int width, int height, int depth);

        String runTempBlockProof(Player player, String proofId, int width, int height, int yOffset, String... blockIds);

        String runTempFluidProof(Player player, String proofId, int radius, String... fluidIds);

        String runNativeProjectileProof(Player player,
                                        Store<EntityStore> currentStore,
                                        String proofId,
                                        Vector3d forward,
                                        String... projectileConfigIds);

        String runProxyProof(Player player, String proofId, String roleId, String effectId, double distanceAhead);

        String runParticleWorldProof(Player player,
                                     Store<EntityStore> currentStore,
                                     String proofId,
                                     String systemId,
                                     double distanceAhead);

        String runEntityScaleProof(Player player,
                                   Store<EntityStore> currentStore,
                                   String proofId,
                                   Vector3d forward,
                                   double distanceAhead);

        String runDynamicLightProof(Player player,
                                    Store<EntityStore> currentStore,
                                    String proofId,
                                    Vector3d forward,
                                    double distanceAhead);

        String runIntangibleProof(Player player,
                                  Store<EntityStore> currentStore,
                                  String proofId,
                                  Vector3d forward,
                                  double distanceAhead);

        String runPlayerCloneProof(Player player,
                                   Store<EntityStore> currentStore,
                                   String proofId,
                                   Vector3d forward,
                                   double distanceAhead);

        String runMovementProof(Player player,
                                Store<EntityStore> currentStore,
                                String proofId,
                                Vector3d forward,
                                double distance,
                                boolean preserveVerticalVelocity,
                                boolean burstVelocity);
    }
}
