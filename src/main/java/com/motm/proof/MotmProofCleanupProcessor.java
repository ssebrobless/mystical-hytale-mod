package com.motm.proof;

import com.hypixel.hytale.component.Store;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.runtime.state.ProofCleanupRuntimeState;
import com.motm.runtime.state.TemporaryProofProxy;
import com.motm.runtime.state.TemporaryProofSelection;

import java.util.logging.Logger;

public final class MotmProofCleanupProcessor {
    private final ProofCleanupRuntimeState cleanupState;
    private final Logger log;

    public MotmProofCleanupProcessor(ProofCleanupRuntimeState cleanupState, Logger log) {
        this.cleanupState = cleanupState;
        this.log = log;
    }

    public void process(Store<EntityStore> currentStore, long now) {
        if (cleanupState == null) {
            return;
        }

        World currentWorld = currentStore != null && currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : null;
        processSelections(currentWorld, now);
        processProxies(currentWorld, now);
    }

    private void processSelections(World currentWorld, long now) {
        for (TemporaryProofSelection proof : cleanupState.selections()) {
            if (now < proof.cleanupAtMillis()) {
                continue;
            }
            if (!sameWorld(currentWorld, proof.world())) {
                continue;
            }
            try {
                proof.originalSelection().place(null, proof.world(), new Vector3i(0, 0, 0), BlockMask.EMPTY);
                logInfo("[MOTM] Proof cleanup restored selection: proofId=" + proof.proofId()
                        + " anchor=" + proof.anchor());
            } catch (Throwable e) {
                logWarning("[MOTM] Proof cleanup failed for " + proof.proofId()
                        + " anchor=" + proof.anchor()
                        + ": " + e.getMessage());
            }
            cleanupState.removeSelection(proof);
        }
    }

    private void processProxies(World currentWorld, long now) {
        for (TemporaryProofProxy proof : cleanupState.proxies()) {
            if (now < proof.cleanupAtMillis()) {
                continue;
            }
            if (!sameWorld(currentWorld, proof.world())) {
                continue;
            }
            try {
                if (proof.ref() != null && proof.ref().isValid() && proof.ref().getStore() != null) {
                    NPCEntity npc = proof.ref().getStore().getComponent(proof.ref(), NPCEntity.getComponentType());
                    if (npc != null) {
                        npc.setToDespawn();
                    }
                }
                logInfo("[MOTM] Proof cleanup despawned proxy: proofId=" + proof.proofId());
            } catch (Exception e) {
                logWarning("[MOTM] Proof proxy cleanup failed for " + proof.proofId()
                        + ": " + e.getMessage());
            }
            cleanupState.removeProxy(proof);
        }
    }

    private static boolean sameWorld(World currentWorld, World proofWorld) {
        return currentWorld != null && (currentWorld == proofWorld || currentWorld.equals(proofWorld));
    }

    private void logInfo(String message) {
        if (log != null) {
            log.info(message);
        }
    }

    private void logWarning(String message) {
        if (log != null) {
            log.warning(message);
        }
    }
}
