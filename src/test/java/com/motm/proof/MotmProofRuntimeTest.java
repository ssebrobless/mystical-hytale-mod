package com.motm.proof;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmProofRuntimeTest {

    @Test
    void defaultRunnerIdsMatchCatalog() {
        MotmProofRuntime runtime = new MotmProofRuntime();
        runtime.initialize(new StubActions(), Logger.getLogger("MOTM-test"));

        assertEquals(Set.copyOf(MotmProofCatalog.ids()), runtime.ids());
    }

    @Test
    void runsKnownProofAndRejectsUnknownProof() {
        MotmProofRuntime runtime = new MotmProofRuntime();
        runtime.initialize(new StubActions(), Logger.getLogger("MOTM-test"));

        String known = runtime.run("player", null, null, new Vector3d(0, 0, 1), "coating-metal");
        String unknown = runtime.run("player", null, null, new Vector3d(0, 0, 1), "not-real");

        assertEquals("effect:coating-metal:MOTM_Proof_Coating_Metal", known);
        assertTrue(unknown.contains("unknown proof id"));
    }

    private static final class StubActions implements MotmProofRuntime.DefaultProofActions {
        @Override
        public String applyProofEffect(Player player, String effectId, String proofId) {
            return "effect:" + proofId + ":" + effectId;
        }

        @Override
        public String applyProofTargetEffect(String playerId, Store<EntityStore> store, String effectId, String proofId) {
            return "target-effect:" + proofId + ":" + effectId;
        }

        @Override
        public String runTempBlockProof(Player player, String proofId, String blockId, int width, int height, int depth) {
            return "temp-block:" + proofId + ":" + blockId;
        }

        @Override
        public String runTempBlockProof(Player player, String proofId, int width, int height, int yOffset, String... blockIds) {
            return "temp-block:" + proofId + ":" + blockIds[0];
        }

        @Override
        public String runTempFluidProof(Player player, String proofId, int radius, String... fluidIds) {
            return "temp-fluid:" + proofId + ":" + fluidIds[0];
        }

        @Override
        public String runProxyProof(Player player, String proofId, String roleId, String effectId, double distanceAhead) {
            return "proxy:" + proofId + ":" + roleId;
        }

        @Override
        public String runMovementProof(Player player,
                                       Store<EntityStore> currentStore,
                                       String proofId,
                                       Vector3d forward,
                                       double distance,
                                       boolean preserveVerticalVelocity) {
            return "movement:" + proofId + ":" + distance;
        }
    }
}
