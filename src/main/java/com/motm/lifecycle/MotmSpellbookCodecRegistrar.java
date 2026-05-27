package com.motm.lifecycle;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry;
import com.motm.MenteesMod;
import com.motm.interaction.MotmSpellbookInteraction;

import java.util.logging.Logger;

/**
 * Owns MOTM custom spellbook interaction codec registration.
 *
 * Hytale's codec registry owns shutdown cleanup internally; this registrar keeps
 * the registration shape out of the main plugin shell.
 */
public final class MotmSpellbookCodecRegistrar {

    public void register(MenteesMod mod,
                         CodecMapRegistry.Assets<Interaction, ?> interactionCodecRegistry,
                         Logger log) {
        MotmSpellbookInteraction.setMod(mod);
        interactionCodecRegistry.register(
                "motm_spellbook_primary",
                MotmSpellbookInteraction.Primary.class,
                MotmSpellbookInteraction.Primary.CODEC);
        interactionCodecRegistry.register(
                "motm_spellbook_secondary",
                MotmSpellbookInteraction.Secondary.class,
                MotmSpellbookInteraction.Secondary.CODEC);
        interactionCodecRegistry.register(
                "motm_spellbook_use",
                MotmSpellbookInteraction.Use.class,
                MotmSpellbookInteraction.Use.CODEC);
        log.info("[MOTM] Registered MOTM spellbook custom interactions: primary/secondary/use");
    }
}
