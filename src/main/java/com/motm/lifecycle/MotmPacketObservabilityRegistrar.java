package com.motm.lifecycle;

import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.motm.util.MotmObservability;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Owns packet watcher registration for the observability harness.
 */
public final class MotmPacketObservabilityRegistrar {

    private PacketFilter inboundPacketFilter;
    private PacketFilter outboundPacketFilter;

    public void register(Logger log,
                         boolean enabled,
                         MotmObservability observability,
                         Supplier<String> traceIdSupplier) {
        if (!enabled || observability == null || inboundPacketFilter != null) {
            return;
        }

        try {
            inboundPacketFilter = PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) ->
                    observability.recordPacket("inbound", traceIdSupplier.get(), playerRef, packet));
            outboundPacketFilter = PacketAdapters.registerOutbound((PlayerPacketWatcher) (playerRef, packet) ->
                    observability.recordPacket("outbound", traceIdSupplier.get(), playerRef, packet));
            log.info("[MOTM] Observability packet watchers registered: scope="
                    + observability.getPacketScope());
        } catch (Throwable e) {
            log.warning("[MOTM] Observability packet watcher registration failed: " + e.getMessage());
            inboundPacketFilter = null;
            outboundPacketFilter = null;
        }
    }

    public void unregister(Logger log) {
        try {
            if (inboundPacketFilter != null) {
                PacketAdapters.deregisterInbound(inboundPacketFilter);
                inboundPacketFilter = null;
            }
            if (outboundPacketFilter != null) {
                PacketAdapters.deregisterOutbound(outboundPacketFilter);
                outboundPacketFilter = null;
            }
        } catch (Throwable e) {
            log.warning("[MOTM] Observability packet watcher deregistration failed: " + e.getMessage());
        }
    }
}
