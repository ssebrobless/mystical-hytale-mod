package com.motm.interaction;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.motm.manager.SpellbookManager;
import com.motm.model.PlayerData;
import com.motm.resource.HydroContainerRefillHandler;
import com.motm.runtime.state.SpellbookInputDebouncer;

import java.util.Locale;
import java.util.logging.Logger;

public final class SpellbookInputHandler {
    private final HydroContainerRefillHandler hydroRefillHandler;
    private final SpellbookInputDebouncer debouncer;
    private final long debounceMillis;
    private final Support support;
    private final Logger log;

    public SpellbookInputHandler(HydroContainerRefillHandler hydroRefillHandler,
                                 long debounceMillis,
                                 Support support,
                                 Logger log) {
        this.hydroRefillHandler = hydroRefillHandler;
        this.debounceMillis = debounceMillis;
        this.support = support;
        this.log = log;
        this.debouncer = new SpellbookInputDebouncer();
    }

    public void handleInteract(PlayerInteractEvent event) {
        try {
            logInfo("[MOTM] >>> handlePlayerInteract ENTERED action="
                    + (event != null ? event.getActionType() : "<null>"));
            Player player = event.getPlayer();
            if (player == null || support == null) {
                return;
            }

            String playerId = support.runtimePlayerId(player);
            PlayerData playerData = playerId != null ? support.playerData(playerId) : null;
            if (playerData == null) {
                return;
            }

            ItemStack eventItemInHand = event.getItemInHand();
            ItemStack inventoryItemInHand = player.getInventory() != null ? player.getInventory().getItemInHand() : null;
            ItemStack itemInHand = eventItemInHand != null && !eventItemInHand.isEmpty()
                    ? eventItemInHand
                    : inventoryItemInHand;
            boolean holdingSpellbook = support.isSpellbookItem(itemInHand);
            boolean holdingDevBook = support.isDevBookItem(itemInHand);
            boolean crouching = support.isPlayerCrouching(player);
            InteractionType actionType = event.getActionType();
            int bookSlot = resolveInteractSlot(actionType);
            String heldItemId = itemInHand != null ? itemInHand.getItemId() : "<none>";
            boolean hasSelectedStyle = hasSelectedStyle(playerData);
            if (hasSelectedStyle && support.devToolsEnabled()) {
                logInfo("[MOTM] Input trace(interact): player="
                        + playerData.getPlayerName()
                        + " action=" + actionType
                        + " item=" + heldItemId
                        + " recognizedSpellbook=" + holdingSpellbook);
            }
            if (holdingSpellbook) {
                logInfo("[MOTM] Spellbook interact input: player="
                        + playerData.getPlayerName()
                        + " action=" + actionType
                        + " slot=" + bookSlot
                        + " item=" + heldItemId);
            }

            boolean openSpellbookGesture = holdingSpellbook && crouching && actionType == InteractionType.Use;
            boolean navigateSpellbookGesture = holdingSpellbook && crouching && bookSlot > 0;
            boolean openDevBookGesture = holdingDevBook && actionType == InteractionType.Use;
            boolean navigateDevBookGesture = holdingDevBook && bookSlot > 0;

            if (openSpellbookGesture || navigateSpellbookGesture || openDevBookGesture || navigateDevBookGesture) {
                event.setCancelled(true);
                String response = handleBookNavigation(player, playerData, bookSlot, holdingDevBook, openDevBookGesture,
                        openSpellbookGesture);
                send(player, response);
                return;
            }

            if (hydroRefillHandler != null
                    && hydroRefillHandler.tryHandle(event, player, playerData, itemInHand, holdingSpellbook)) {
                return;
            }

            if (bookSlot <= 0 || !holdingSpellbook) {
                return;
            }

            event.setCancelled(true);

            if (playerData.getPlayerClass() == null || !hasSelectedStyle(playerData)) {
                support.sendMessage(player, Message.raw("[MOTM] Select a style first with /motm style <styleId>."));
                return;
            }

            send(player, tryCastSpellbookSlot(
                    player,
                    playerData,
                    bookSlot,
                    "interact:" + actionType,
                    event.getTargetRef(),
                    event.getTargetBlock()
            ));
        } catch (Exception e) {
            logSevere("[MOTM] PlayerInteract handling failed safely: " + e.getMessage());
        }
    }

    public void handleMouseButton(PlayerMouseButtonEvent event) {
        try {
            logInfo("[MOTM] >>> handlePlayerMouseButton ENTERED button="
                    + (event != null && event.getMouseButton() != null
                    ? event.getMouseButton().mouseButtonType + "/" + event.getMouseButton().state
                    : "<null>"));
            Player player = event.getPlayer();
            if (player == null || event.getMouseButton() == null || support == null) {
                return;
            }

            if (event.getMouseButton().state != MouseButtonState.Pressed) {
                return;
            }

            String playerId = support.runtimePlayerId(player);
            PlayerData playerData = playerId != null ? support.playerData(playerId) : null;
            if (playerData == null) {
                return;
            }

            var eventItemInHand = event.getItemInHand();
            ItemStack inventoryItemInHand = player.getInventory() != null ? player.getInventory().getItemInHand() : null;
            String itemId = resolveMouseButtonItemId(eventItemInHand, inventoryItemInHand);
            boolean hasSelectedStyle = hasSelectedStyle(playerData);
            String loggedItemId = itemId == null || itemId.isBlank() ? "<none>" : itemId;
            if (hasSelectedStyle && support.devToolsEnabled()) {
                logInfo("[MOTM] Input trace(mouse): player="
                        + playerData.getPlayerName()
                        + " button=" + event.getMouseButton().mouseButtonType
                        + " item=" + loggedItemId
                        + " recognizedSpellbook=" + support.isSpellbookItemId(loggedItemId));
            }
            if (itemId == null || itemId.isBlank()) {
                return;
            }

            if (support.isSpellbookItemId(itemId)) {
                int slot = resolveMouseSlot(event.getMouseButton().mouseButtonType);
                logInfo("[MOTM] Spellbook mouse input: player="
                        + playerData.getPlayerName()
                        + " button=" + event.getMouseButton().mouseButtonType
                        + " slot=" + slot
                        + " item=" + itemId);
                if (slot > 0) {
                    event.setCancelled(true);
                    send(player, tryCastSpellbookSlot(
                            player,
                            playerData,
                            slot,
                            "mouse:" + event.getMouseButton().mouseButtonType,
                            null,
                            null
                    ));
                }
                return;
            }

            // Update 5 mouse events do not expose a hit entity; native weapon follow-up
            // confirmation stays on the damage/block interaction paths.
        } catch (Exception e) {
            logSevere("[MOTM] PlayerMouseButton handling failed safely: " + e.getMessage());
        }
    }

    public void castSlotFromInteraction(Player runtimePlayer, int slot) {
        if (runtimePlayer == null || slot <= 0 || support == null) {
            return;
        }
        String playerId = support.runtimePlayerId(runtimePlayer);
        if (playerId == null) {
            return;
        }
        PlayerData playerData = support.playerData(playerId);
        if (playerData == null) {
            return;
        }
        send(runtimePlayer, tryCastSpellbookSlot(
                runtimePlayer,
                playerData,
                slot,
                "interaction:custom",
                null,
                null
        ));
    }

    public void clearPlayer(String playerId) {
        debouncer.clearPlayer(playerId);
    }

    private String handleBookNavigation(Player player,
                                        PlayerData playerData,
                                        int bookSlot,
                                        boolean holdingDevBook,
                                        boolean openDevBookGesture,
                                        boolean openSpellbookGesture) {
        if (openDevBookGesture) {
            return support.devToolsEnabled()
                    ? support.cycleDevPage(playerData)
                    : support.devToolsDisabledMessage();
        }
        if (openSpellbookGesture) {
            return support.openSpellbook(player, SpellbookManager.Section.OVERVIEW)
                    ? null
                    : support.openSpellbookPage(playerData);
        }
        if (holdingDevBook) {
            return support.devToolsEnabled()
                    ? support.handleDevBookAction(playerData, bookSlot)
                    : support.devToolsDisabledMessage();
        }
        return support.handleSpellbookAction(playerData, bookSlot);
    }

    private String tryCastSpellbookSlot(Player player,
                                        PlayerData playerData,
                                        int slot,
                                        String source,
                                        Ref<EntityStore> targetRef,
                                        Vector3i targetBlock) {
        if (player == null || playerData == null || slot <= 0 || support == null) {
            return "";
        }

        String playerId = playerData.getPlayerId();
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        if (isDuplicateSpellbookInput(playerId, slot)) {
            return "";
        }

        logInfo("[MOTM] Spellbook cast attempt: player="
                + playerData.getPlayerName()
                + " slot=" + slot
                + " source=" + source);

        return support.castAbilityBySlot(player, slot, targetRef, targetBlock);
    }

    private int resolveInteractSlot(InteractionType actionType) {
        if (actionType == null) {
            return 0;
        }

        String normalized = String.valueOf(actionType).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "primary", "ability1", "ability_1" -> 1;
            case "secondary", "ability2", "ability_2" -> 2;
            case "use", "ability3", "ability_3" -> 3;
            default -> 0;
        };
    }

    private int resolveMouseSlot(MouseButtonType mouseButtonType) {
        if (mouseButtonType == null) {
            return 0;
        }

        return switch (String.valueOf(mouseButtonType).toLowerCase(Locale.ROOT)) {
            case "left" -> 1;
            case "right" -> 2;
            default -> 0;
        };
    }

    private boolean isDuplicateSpellbookInput(String playerId, int slot) {
        if (playerId == null || playerId.isBlank() || slot <= 0) {
            return false;
        }

        return debouncer.isDuplicate(playerId, slot, System.currentTimeMillis(), debounceMillis, 1000L);
    }

    private String resolveMouseButtonItemId(
            com.hypixel.hytale.server.core.asset.type.item.config.Item eventItemInHand,
            ItemStack inventoryItemInHand
    ) {
        if (eventItemInHand != null && eventItemInHand.getId() != null && !eventItemInHand.getId().isBlank()) {
            return eventItemInHand.getId();
        }
        return inventoryItemInHand != null ? inventoryItemInHand.getItemId() : null;
    }

    private static boolean hasSelectedStyle(PlayerData playerData) {
        return playerData != null
                && playerData.getSelectedStyles() != null
                && !playerData.getSelectedStyles().isEmpty();
    }

    private void send(Player player, String response) {
        if (player != null && response != null && !response.isBlank() && support != null) {
            support.sendMessage(player, Message.raw(response));
        }
    }

    private void logInfo(String message) {
        if (log != null) {
            log.info(message);
        }
    }

    private void logSevere(String message) {
        if (log != null) {
            log.severe(message);
        }
    }

    public interface Support {
        String runtimePlayerId(Player player);

        PlayerData playerData(String playerId);

        boolean isSpellbookItem(ItemStack stack);

        boolean isDevBookItem(ItemStack stack);

        boolean isSpellbookItemId(String itemId);

        boolean isDevBookItemId(String itemId);

        boolean isPlayerCrouching(Player player);

        boolean devToolsEnabled();

        String devToolsDisabledMessage();

        boolean openSpellbook(Player player, SpellbookManager.Section section);

        String openSpellbookPage(PlayerData playerData);

        String cycleDevPage(PlayerData playerData);

        String handleDevBookAction(PlayerData playerData, int slot);

        String handleSpellbookAction(PlayerData playerData, int slot);

        String castAbilityBySlot(Player player, int slot, Ref<EntityStore> targetRef, Vector3i targetBlock);

        String handleWeaponFollowUpHit(Player player, PlayerData playerData, Ref<EntityStore> targetRef, String itemId);

        void sendMessage(Player player, Message message);
    }
}
