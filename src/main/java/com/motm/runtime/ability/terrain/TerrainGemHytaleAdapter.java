package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.ability.field.FieldVisualHytaleAdapter;
import com.motm.runtime.state.VisualProxyRuntimeState;
import com.motm.util.AbilityPresentation;
import com.motm.util.HytaleAssetResolver;
import com.motm.util.MotmNpcRoles;

import java.util.logging.Logger;

public final class TerrainGemHytaleAdapter {
    private static final Logger LOG = Logger.getLogger("MOTM");

    private final LapidaryGemRuntimeState gemState;
    private final TerrainActivationRuntime activationRuntime;
    private final VisualProxyRuntimeState visualProxyState;
    private final Support support;

    public TerrainGemHytaleAdapter(LapidaryGemRuntimeState gemState,
                                   TerrainActivationRuntime activationRuntime,
                                   VisualProxyRuntimeState visualProxyState,
                                   Support support) {
        this.gemState = gemState;
        this.activationRuntime = activationRuntime;
        this.visualProxyState = visualProxyState;
        this.support = support;
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        if (gemState == null) {
            return;
        }
        gemState.removeProcessedGems(gem -> processLapidaryGem(gem, currentStore, now));
    }

    public int activeGemCount() {
        return gemState == null ? 0 : gemState.activeGemCount();
    }

    public int removeGemsForPlayer(String playerId) {
        return gemState == null ? 0 : gemState.removeGemsForPlayer(playerId, this::despawnLapidaryGem);
    }

    public String spawnLapidaryGemProxy(World world,
                                        PlayerData player,
                                        AbilityData ability,
                                        Vector3d center,
                                        long expireAtMillis) {
        if (world == null || player == null || ability == null || center == null
                || gemState == null || activationRuntime == null) {
            return "";
        }

        gemState.removeGemsForPlayer(player.getPlayerId(), this::despawnLapidaryGem);

        double gemHealth = Math.max(1.0, support.resolvePlayerMaxHealth(player.getPlayerId())
                * Math.max(0.10, ability.getShieldPercent() / 100.0));
        Vector3d proxyPosition = new Vector3d(center).add(1.0, 2.35, 1.0);
        NPCEntity proxy = new NPCEntity(world);
        MotmNpcRoles.applyRole(proxy, HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), LOG);
        proxy.setDespawnTime((float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.5));
        world.spawnEntity(proxy, proxyPosition, new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef == null || !proxyRef.isValid() || proxyRef.getStore() == null) {
            return "";
        }

        Store<EntityStore> store = proxyRef.getStore();
        FieldVisualHytaleAdapter.configureRenderlessProxy(proxyRef, store);
        String label = lapidaryGemLabel(gemHealth, gemHealth);
        applyLapidaryGemLabel(proxyRef, store, label);
        support.applyEffectById(proxyRef, store, "MOTM_Proof_Gem_Green");
        visualProxyState.add(proxyRef);
        ActiveLapidaryGem gem = activationRuntime.createLapidaryGem(
                player.getPlayerId(),
                proxyRef,
                center,
                gemHealth,
                gemHealth,
                expireAtMillis,
                label
        );
        if (gem == null) {
            return "";
        }
        gemState.addGem(gem);
        support.logInfo("[MOTM] Lapidary gem HP proxy spawned: owner=" + player.getPlayerName()
                + " hp=" + AbilityPresentation.formatDecimal(gemHealth)
                + " position=" + formatVector(proxyPosition));
        return " + HP nameplate";
    }

    /**
     * Spawns a bounded ring + raised dome of visible green glow motes (Spark_Living model carrying
     * the green gem effect) so the gem's AoE reads as a spherical green glow at its true radius.
     * Uses visible proxies (not the renderless field path) because 0.6.1 renders the glow model;
     * bounded count keeps it cheap and avoids the old mass-spawn client stalls.
     */
    public int spawnGemGlowRing(World world, Vector3d center, double radius, long expireAtMillis) {
        if (world == null || center == null || visualProxyState == null) {
            return 0;
        }
        double r = Math.max(1.5, radius);
        int ringPoints = (int) Math.min(18L, Math.max(8L, Math.round(r)));
        float despawn = (float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.5);
        java.util.List<Vector3d> points = new java.util.ArrayList<>();
        for (int i = 0; i < ringPoints; i++) {
            double a = (2.0 * Math.PI * i) / ringPoints;
            points.add(new Vector3d(center.x + r * Math.cos(a), center.y + 0.4, center.z + r * Math.sin(a)));
        }
        int domePoints = Math.max(4, ringPoints / 2);
        double domeR = r * 0.6;
        double domeH = r * 0.55;
        for (int i = 0; i < domePoints; i++) {
            double a = (2.0 * Math.PI * i) / domePoints + 0.3;
            points.add(new Vector3d(center.x + domeR * Math.cos(a), center.y + domeH, center.z + domeR * Math.sin(a)));
        }
        points.add(new Vector3d(center.x, center.y + r * 0.85, center.z));
        int spawned = 0;
        for (Vector3d p : points) {
            NPCEntity proxy = new NPCEntity(world);
            MotmNpcRoles.applyRole(proxy, "Spark_Living",
                    HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), LOG);
            proxy.setDespawnTime(despawn);
            world.spawnEntity(proxy, new Vector3d(p), new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));
            Ref<EntityStore> ref = proxy.getReference();
            if (ref == null || !ref.isValid() || ref.getStore() == null) {
                continue;
            }
            Store<EntityStore> store = ref.getStore();
            // Keep the glow model (visible); drop nameplate/label so motes stay inert decoration.
            store.removeComponentIfExists(ref, Nameplate.getComponentType());
            store.removeComponentIfExists(ref, DisplayNameComponent.getComponentType());
            support.applyEffectById(ref, store, "MOTM_Terra_Gem_Field");
            visualProxyState.add(ref);
            spawned++;
        }
        return spawned;
    }

    public Vector3d resolveActiveLapidaryGemCenter(PlayerData player, AbilityData ability, Store<EntityStore> store) {
        return player == null ? null : resolveActiveLapidaryGemCenter(player.getPlayerId(), ability, store);
    }

    public Vector3d resolveActiveLapidaryGemCenter(String ownerPlayerId, AbilityData ability, Store<EntityStore> store) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank() || ability == null || store == null
                || !isGemAnchoredAbility(ability) || gemState == null) {
            return null;
        }
        Vector3d center = gemState.firstCenterForPlayer(ownerPlayerId,
                gem -> gem.ref() != null && gem.ref().isValid() && gem.ref().getStore() == store);
        if (center != null) {
            return center;
        }
        support.logInfo("[MOTM][terra-audit] event=gem.anchor.missing playerId=" + safe(ownerPlayerId)
                + " abilityId=" + safe(ability.getId()));
        return null;
    }

    private boolean processLapidaryGem(ActiveLapidaryGem gem,
                                       Store<EntityStore> currentStore,
                                       long now) {
        if (gem == null || gem.ref() == null || !gem.ref().isValid()) {
            return true;
        }
        if (gem.ref().getStore() != currentStore) {
            return false;
        }
        Store<EntityStore> store = gem.ref().getStore();
        if (store == null || gem.expired(now)) {
            return despawnLapidaryGem(gem);
        }

        double current = resolveCurrentHealth(gem.ref(), store);
        if (current <= 0.0) {
            current = gem.currentHp();
        }
        current = clamp(current, 0.0, gem.maxHp());
        String label = lapidaryGemLabel(current, gem.maxHp());
        if (gem.updateHealthLabel(current, label)) {
            applyLapidaryGemLabel(gem.ref(), store, label);
        }
        return false;
    }

    private void applyLapidaryGemLabel(Ref<EntityStore> ref, Store<EntityStore> store, String label) {
        if (ref == null || !ref.isValid() || store == null || label == null) {
            return;
        }
        store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(label));
        store.putComponent(ref, DisplayNameComponent.getComponentType(),
                new DisplayNameComponent(Message.raw(label).color("#6CFF8C")));
    }

    private boolean despawnLapidaryGem(ActiveLapidaryGem gem) {
        if (gem == null || gem.ref() == null || !gem.ref().isValid()) {
            return true;
        }
        visualProxyState.despawn(gem.ref());
        return true;
    }

    private static double resolveCurrentHealth(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null) {
            return 0.0;
        }
        return health.get();
    }

    private static boolean isGemAnchoredAbility(AbilityData ability) {
        String abilityId = ability == null || ability.getId() == null ? "" : ability.getId().toLowerCase(java.util.Locale.ROOT);
        return "fracture".equals(abilityId) || "refraction".equals(abilityId);
    }

    private static String lapidaryGemLabel(double currentHp, double maxHp) {
        return "Lapidary HP "
                + Math.max(0, (int) Math.ceil(currentHp))
                + "/"
                + Math.max(1, (int) Math.ceil(maxHp));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "(null)";
        }
        return "("
                + String.format(java.util.Locale.US, "%.2f", vector.x)
                + ","
                + String.format(java.util.Locale.US, "%.2f", vector.y)
                + ","
                + String.format(java.util.Locale.US, "%.2f", vector.z)
                + ")";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public interface Support {
        double resolvePlayerMaxHealth(String playerId);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        void logInfo(String message);
    }
}
