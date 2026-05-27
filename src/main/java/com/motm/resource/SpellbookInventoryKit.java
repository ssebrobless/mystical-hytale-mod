package com.motm.resource;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.motm.util.MotmInventoryOps;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Owns spellbook/dev-book delivery and saved-inventory item migration.
 */
public final class SpellbookInventoryKit {

    private final Hooks hooks;
    private final Logger log;

    public SpellbookInventoryKit(Hooks hooks, Logger log) {
        this.hooks = hooks;
        this.log = log;
    }

    public boolean ensureSpellbookItem(Player player) {
        if (player == null) {
            return false;
        }

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        if (normalizeLegacySpellbookItem(player)) {
            return true;
        }

        if (SpellbookInventoryItems.hasSpellbook(hooks.combinedInventory(player))) {
            return false;
        }

        if (!MotmInventoryOps.grant(
                player,
                new ItemStack(SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID),
                log,
                "ensureSpellbookItem"
        )) {
            return false;
        }
        log.info("[MOTM] Granted spellbook item: " + SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID);
        player.sendMessage(Message.raw(
                "[MOTM] A Mentees spellbook has been placed in your inventory. "
                        + "Cast with Left Click / Right Click / Use while equipped. "
                        + "Ability 1 / 2 / 3 still work as alternate bindings. "
                        + "For the management/readout view, use /motm spellbook overview. "
                        + "Crouch + Use opens the spellbook overview."
        ));
        return true;
    }

    public boolean ensureDevBookItem(Player player) {
        if (!hooks.devToolsEnabled()) {
            return false;
        }
        if (player == null) {
            return false;
        }

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        if (SpellbookInventoryItems.hasDevBook(hooks.combinedInventory(player))) {
            return false;
        }

        if (!MotmInventoryOps.grant(
                player,
                new ItemStack(SpellbookInventoryItems.DEFAULT_DEV_GRIMOIRE_ITEM_ID),
                log,
                "ensureDevBookItem"
        )) {
            return false;
        }
        player.sendMessage(Message.raw(
                "[MOTM] A Dev Grimoire has been placed in your inventory. "
                        + "Use to open it, then Ability 1 / 2 / 3 to navigate."
        ));
        return true;
    }

    private boolean normalizeLegacySpellbookItem(Player player) {
        CombinedItemContainer inventory = hooks.combinedInventory(player);
        if (inventory == null) {
            return false;
        }

        List<Short> legacySlots = new ArrayList<>();
        inventory.forEach((slot, stack) -> {
            if (SpellbookInventoryItems.isLegacyNonweaponSpellbookItem(stack)) {
                legacySlots.add(slot);
            }
        });

        if (legacySlots.isEmpty()) {
            return false;
        }

        boolean hasModernSpellbook = SpellbookInventoryItems.hasDefaultSpellbook(inventory);

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        for (short slot : legacySlots) {
            MotmInventoryOps.removeSlot(inventory, slot, log, "normalizeLegacySpellbookItem");
        }

        if (!hasModernSpellbook) {
            MotmInventoryOps.grant(
                    player,
                    new ItemStack(SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID),
                    log,
                    "normalizeLegacySpellbookItem"
            );
        }

        player.sendMessage(Message.raw(
                "[MOTM] Your legacy spellbook has been updated to the new casting focus. "
                        + "Cast with Left Click / Right Click / Use while equipped."
        ));
        return true;
    }

    public interface Hooks {
        CombinedItemContainer combinedInventory(Player player);

        boolean devToolsEnabled();
    }
}
