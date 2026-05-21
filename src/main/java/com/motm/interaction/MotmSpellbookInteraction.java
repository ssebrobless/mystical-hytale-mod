package com.motm.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;

import java.util.logging.Logger;

/**
 * Custom interactions registered as the spellbook's Primary/Secondary/Use slot bindings.
 * Each subclass routes to a slot (1/2/3) and calls into MenteesMod's existing cast pipeline.
 * MenteesMod handle is injected once at plugin setup via setMod().
 */
public abstract class MotmSpellbookInteraction extends SimpleInstantInteraction {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static volatile MenteesMod MOD;

    public static void setMod(MenteesMod mod) {
        MOD = mod;
    }

    protected abstract int slot();

    @Override
    protected void firstRun(InteractionType interactionType,
                            InteractionContext interactionContext,
                            CooldownHandler cooldownHandler) {
        MenteesMod mod = MOD;
        if (mod == null) {
            LOG.warning("[MOTM] MotmSpellbookInteraction fired before mod registration");
            return;
        }

        Player player = null;
        try {
            Ref<EntityStore> entity = interactionContext.getEntity();
            player = mod.getRuntimePlayer(entity);
        } catch (Throwable t) {
            LOG.warning("[MOTM] MotmSpellbookInteraction: could not read entity from context: " + t.getMessage());
        }
        if (player == null) {
            return;
        }

        LOG.info("[MOTM] Custom spellbook interaction fired: type=" + interactionType
                + " slot=" + slot() + " player=" + player);
        mod.castSpellbookSlotFromInteraction(player, slot());
    }

    public static final class Primary extends MotmSpellbookInteraction {
        public static final BuilderCodec<Primary> CODEC =
                BuilderCodec.builder(Primary.class, Primary::new, SimpleInstantInteraction.CODEC).build();

        @Override
        protected int slot() {
            return 1;
        }
    }

    public static final class Secondary extends MotmSpellbookInteraction {
        public static final BuilderCodec<Secondary> CODEC =
                BuilderCodec.builder(Secondary.class, Secondary::new, SimpleInstantInteraction.CODEC).build();

        @Override
        protected int slot() {
            return 2;
        }
    }

    public static final class Use extends MotmSpellbookInteraction {
        public static final BuilderCodec<Use> CODEC =
                BuilderCodec.builder(Use.class, Use::new, SimpleInstantInteraction.CODEC).build();

        @Override
        protected int slot() {
            return 3;
        }
    }
}
