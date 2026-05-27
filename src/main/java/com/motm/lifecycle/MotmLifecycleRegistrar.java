package com.motm.lifecycle;

import com.hypixel.hytale.registry.Registration;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.motm.MenteesMod;
import com.motm.command.MotmCommandBase;
import com.motm.system.MotmDamageEventSystem;
import com.motm.system.MotmMobRuntimeSystem;
import com.motm.system.MotmServerTickSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Owns Hytale lifecycle registration bookkeeping.
 *
 * MenteesMod still supplies behavior callbacks during the migration, but the
 * registration surface now has a named owner and cleanup ledger.
 */
public final class MotmLifecycleRegistrar {

    private final MenteesMod mod;
    private final List<Registration> registrations = new ArrayList<>();
    private boolean runtimeSystemsRegistered;

    public MotmLifecycleRegistrar(MenteesMod mod) {
        this.mod = mod;
    }

    public void register(Logger log,
                         Consumer<Player> onPlayerConnect,
                         Consumer<Player> onPlayerReady,
                         Consumer<String> onPlayerDisconnect,
                         Consumer<DamageBlockEvent> onDamageBlock,
                         Consumer<PlayerInteractEvent> onPlayerInteract,
                         Consumer<PlayerMouseButtonEvent> onPlayerMouseButton) {
        if (!registrations.isEmpty()) {
            log.info("[MOTM] Hytale event/command hooks already registered.");
            return;
        }

        registrations.add(mod.getEventRegistry().registerGlobal(
                PlayerConnectEvent.class,
                event -> onPlayerConnect.accept(event.getPlayer())
        ));
        registrations.add(mod.getEventRegistry().registerGlobal(
                PlayerReadyEvent.class,
                event -> onPlayerReady.accept(event.getPlayer())
        ));
        registrations.add(mod.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event ->
                onPlayerDisconnect.accept(event.getPlayerRef().getUuid().toString())
        ));
        registrations.add(mod.getEventRegistry().registerGlobal(DamageBlockEvent.class, onDamageBlock::accept));
        registrations.add(mod.getEventRegistry().registerGlobal(PlayerInteractEvent.class, onPlayerInteract::accept));
        registrations.add(mod.getEventRegistry().registerGlobal(PlayerMouseButtonEvent.class, onPlayerMouseButton::accept));
        registrations.add(mod.getCommandRegistry().registerCommand(new MotmCommandBase(mod)));

        if (!runtimeSystemsRegistered) {
            mod.getEntityStoreRegistry().registerSystem(new MotmServerTickSystem(mod));
            mod.getEntityStoreRegistry().registerSystem(new MotmMobRuntimeSystem(mod));
            mod.getEntityStoreRegistry().registerSystem(new MotmDamageEventSystem(mod));
            runtimeSystemsRegistered = true;
        }
    }

    public void unregister(Logger log) {
        for (Registration registration : List.copyOf(registrations)) {
            try {
                registration.unregister();
            } catch (Throwable e) {
                log.warning("[MOTM] Failed to unregister lifecycle hook: " + e.getMessage());
            }
        }
        registrations.clear();
    }
}
