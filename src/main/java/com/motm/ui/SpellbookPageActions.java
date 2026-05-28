package com.motm.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.motm.MenteesMod;
import com.motm.manager.SpellbookManager;
import com.motm.util.MotmObservability;

import java.util.Map;

/**
 * Owns MOTM spellbook custom-page opening and client intent evidence.
 */
public final class SpellbookPageActions {

    private final boolean enabled;
    private final MenteesMod mod;
    private final Hooks hooks;

    public SpellbookPageActions(boolean enabled, MenteesMod mod, Hooks hooks) {
        this.enabled = enabled;
        this.mod = mod;
        this.hooks = hooks;
    }

    public boolean open(Player sender, SpellbookManager.Section section) {
        if (!enabled || sender == null) {
            return false;
        }

        var entityRef = sender.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        PlayerRef playerRef = hooks.universePlayerRef(sender);
        if (playerRef == null) {
            return false;
        }

        hooks.recordClientIntent("custom_page_open", null, MotmObservability.mapOf(
                "playerId", playerRef.getUuid() != null ? playerRef.getUuid().toString() : null,
                "username", playerRef.getUsername(),
                "page", "MOTM_Spellbook",
                "section", String.valueOf(section)
        ));
        sender.getPageManager().openCustomPage(
                entityRef,
                entityRef.getStore(),
                new SpellbookPage(playerRef, mod, section)
        );
        return true;
    }

    public interface Hooks {
        PlayerRef universePlayerRef(Player player);

        void recordClientIntent(String type, String traceId, Map<String, Object> data);
    }
}
