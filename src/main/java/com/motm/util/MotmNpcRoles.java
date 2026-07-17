package com.motm.util;

import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class MotmNpcRoles {
    private static final Set<String> UNKNOWN_ROLE_IDS = ConcurrentHashMap.newKeySet();

    private MotmNpcRoles() {
    }

    public static void applyRole(NPCEntity npc, String roleId, Logger logger) {
        applyRole(npc, roleId, HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), logger);
    }

    public static void applyRole(NPCEntity npc,
                                 String roleId,
                                 String fallbackRoleId,
                                 Logger logger) {
        if (npc == null) {
            return;
        }

        Logger effectiveLogger = logger == null
                ? Logger.getLogger(MotmNpcRoles.class.getName())
                : logger;
        if (hasRoleName(roleId)) {
            setRoleName(npc, roleId, effectiveLogger);
            return;
        }

        String unknownRoleId = String.valueOf(roleId);
        if (UNKNOWN_ROLE_IDS.add(unknownRoleId)) {
            effectiveLogger.warning("[MOTM] Unknown NPC role '" + unknownRoleId
                    + "' - falling back to '" + fallbackRoleId + "'");
        }

        if (!hasRoleName(fallbackRoleId)) {
            effectiveLogger.severe("[MOTM] Unknown fallback NPC role '" + fallbackRoleId
                    + "' - assigning it anyway");
        }
        setRoleName(npc, fallbackRoleId, effectiveLogger);
    }

    private static boolean hasRoleName(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return false;
        }
        try {
            return NPCPlugin.get().hasRoleName(roleId);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static void setRoleName(NPCEntity npc, String roleId, Logger logger) {
        try {
            npc.setRoleName(roleId);
        } catch (RuntimeException | LinkageError error) {
            logger.severe("[MOTM] Failed to assign NPC role '" + roleId + "': "
                    + error.getMessage());
        }
    }
}
