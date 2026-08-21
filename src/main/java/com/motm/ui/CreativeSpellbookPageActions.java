package com.motm.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.util.MotmObservability;

import java.util.Map;

/**
 * Owns opening the dev-only creative sandbox spellbook custom page and its client intent
 * evidence. Mirrors {@link SpellbookPageActions}; page-opening lives here (not in the mod
 * facade) to keep a single owner for custom-page lifecycle.
 */
public final class CreativeSpellbookPageActions {

    private final boolean enabled;
    private final MenteesMod mod;
    private final Hooks hooks;

    public CreativeSpellbookPageActions(boolean enabled, MenteesMod mod, Hooks hooks) {
        this.enabled = enabled;
        this.mod = mod;
        this.hooks = hooks;
    }

    public boolean open(Player sender) {
        if (!enabled || sender == null || !mod.isDevToolsEnabled()) {
            return false;
        }

        Ref<EntityStore> entityRef = sender.getReference();
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
                "page", "MOTM_CreativeSpellbook",
                "section", "creative"
        ));
        sender.getPageManager().openCustomPage(
                entityRef,
                entityRef.getStore(),
                new CreativeSpellbookPage(playerRef, mod)
        );
        return true;
    }

    public interface Hooks {
        PlayerRef universePlayerRef(Player player);

        void recordClientIntent(String type, String traceId, Map<String, Object> data);
    }
}
