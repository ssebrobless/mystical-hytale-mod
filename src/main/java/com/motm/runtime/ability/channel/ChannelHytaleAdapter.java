package com.motm.runtime.ability.channel;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.util.AbilityPresentation;
import com.motm.util.MotmEntityLiveness;

import java.util.List;

public final class ChannelHytaleAdapter {
    private final ChannelRuntimeState channelState;
    private final ChannelActivationRuntime activationRuntime;
    private final long channelPulseIntervalMillis;
    private final long lineControlPulseIntervalMillis;
    private final Support support;

    public ChannelHytaleAdapter(ChannelRuntimeState channelState,
                                ChannelActivationRuntime activationRuntime,
                                long channelPulseIntervalMillis,
                                long lineControlPulseIntervalMillis,
                                Support support) {
        this.channelState = channelState;
        this.activationRuntime = activationRuntime;
        this.channelPulseIntervalMillis = channelPulseIntervalMillis;
        this.lineControlPulseIntervalMillis = lineControlPulseIntervalMillis;
        this.support = support;
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        channelState.removeProcessedLineControls(lineControl ->
                belongsToCurrentStore(lineControl.ownerRef(), currentStore) && processLineControlTick(lineControl, now));
        channelState.removeProcessedChannels(channel ->
                belongsToCurrentStore(channel.ownerRef(), currentStore) && processChannelTick(channel, now));
    }

    /**
     * Drops pulse registrations targeting a removed entity so no further
     * damage/updates reach a dead ref (client-crash guard, 2026-07-17).
     */
    public int purgePulsesTargetingEntity(String entityId, Store<EntityStore> store) {
        if (entityId == null || entityId.isBlank() || store == null) {
            return 0;
        }
        final int[] removed = {0};
        channelState.removeProcessedChannels(channel -> {
            if (channel == null || !entityId.equals(support.resolveEntityId(channel.targetRef(), store))) {
                return false;
            }
            removed[0]++;
            return true;
        });
        channelState.removeProcessedLineControls(lineControl -> {
            if (lineControl == null || !entityId.equals(support.resolveEntityId(lineControl.targetRef(), store))) {
                return false;
            }
            removed[0]++;
            return true;
        });
        return removed[0];
    }

    public Result startLineControl(Ref<EntityStore> ownerRef,
                                   PlayerData player,
                                   AbilityData ability,
                                   Ref<EntityStore> targetRef) {
        boolean vines = isVinesAbility(ability);
        if (!"line_control".equals(lower(ability != null ? ability.getCastType() : null))
                || (!vines && ability.getPullForce() <= 0.0)) {
            return Result.none();
        }

        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() == null
                || targetRef == null || !targetRef.isValid()) {
            return Result.none();
        }

        double durationSeconds = inferLineControlDurationSeconds(ability);
        if (durationSeconds <= 0.0) {
            return Result.none();
        }

        long now = System.currentTimeMillis();
        ActiveLineControl lineControl = activationRuntime.createLineControl(
                player.getPlayerId(),
                ownerRef,
                targetRef,
                ability,
                now + (long) (durationSeconds * 1000),
                now + lineControlPulseIntervalMillis
        );
        if (lineControl == null) {
            return Result.none();
        }
        channelState.replaceLineControlForPlayer(player.getPlayerId(), lineControl);
        return new Result(
                true,
                (vines ? "vines root/dot " : "current pull ")
                        + AbilityPresentation.formatDecimal(durationSeconds)
                        + "s"
        );
    }

    public Result startChannel(Ref<EntityStore> ownerRef,
                               PlayerData player,
                               AbilityData ability,
                               Ref<EntityStore> targetRef) {
        if (!"channel".equals(lower(ability != null ? ability.getCastType() : null))) {
            return Result.none();
        }

        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() == null) {
            return Result.none();
        }

        if (targetRef == null || !targetRef.isValid()) {
            return new Result(false, "channel failed: no target");
        }

        long now = System.currentTimeMillis();
        long expireAt = now + (long) (Math.max(1.5, ability.getDurationSeconds()) * 1000);
        ActiveChannel channel = activationRuntime.createChannel(
                player.getPlayerId(),
                ownerRef,
                targetRef,
                ability,
                expireAt,
                now + channelPulseIntervalMillis
        );
        if (channel == null) {
            return new Result(false, "channel failed: no target");
        }
        channelState.replaceChannelForPlayer(player.getPlayerId(), channel);
        return new Result(true,
                "channeling " + support.humanize(ability.getName()) + " for "
                        + AbilityPresentation.formatDecimal((expireAt - now) / 1000.0) + "s");
    }

    private boolean processLineControlTick(ActiveLineControl lineControl, long now) {
        if (lineControl.ownerRef() == null || !lineControl.ownerRef().isValid()
                || lineControl.targetRef() == null || !lineControl.targetRef().isValid()) {
            return true;
        }

        if (now >= lineControl.expireAtMillis()) {
            return true;
        }

        if (now < lineControl.nextPulseAtMillis()) {
            return false;
        }

        Store<EntityStore> store = lineControl.ownerRef().getStore();
        if (store == null) {
            return true;
        }
        if (!MotmEntityLiveness.isLiveTarget(lineControl.targetRef(), store)) {
            return true;
        }

        Vector3d ownerPosition = support.position(lineControl.ownerRef(), store);
        Vector3d targetPosition = support.position(lineControl.targetRef(), store);
        if (ownerPosition == null || targetPosition == null
                || distance(ownerPosition, targetPosition) > support.range(lineControl.ability()) + 3.0) {
            return true;
        }

        PlayerData player = support.player(lineControl.ownerPlayerId());
        if (player == null) {
            return true;
        }

        support.renderTetherLink(ownerPosition, targetPosition, player, lineControl.ability(), store);

        if (lineControl.ability().getPullForce() > 0.0) {
            support.applyLineControlPull(lineControl.targetRef(), store, lineControl.ownerRef(), lineControl.ability());
        }
        applyRepeatingLineControlEffects(lineControl, player, store);
        lineControl.scheduleNextPulse(now, lineControlPulseIntervalMillis);
        return false;
    }

    private boolean processChannelTick(ActiveChannel channel, long now) {
        if (channel.ownerRef() == null || !channel.ownerRef().isValid()
                || channel.targetRef() == null || !channel.targetRef().isValid()) {
            return true;
        }

        if (now >= channel.expireAtMillis()) {
            return true;
        }

        if (now < channel.nextPulseAtMillis()) {
            return false;
        }

        Store<EntityStore> store = channel.ownerRef().getStore();
        if (store == null) {
            return true;
        }
        if (!MotmEntityLiveness.isLiveTarget(channel.targetRef(), store)) {
            return true;
        }

        PlayerData player = support.player(channel.ownerPlayerId());
        if (player == null) {
            return true;
        }

        Vector3d ownerPosition = support.position(channel.ownerRef(), store);
        Vector3d targetPosition = support.position(channel.targetRef(), store);
        if (ownerPosition == null || targetPosition == null
                || distance(ownerPosition, targetPosition) > support.range(channel.ability()) + 2.0) {
            return true;
        }

        support.renderTetherLink(ownerPosition, targetPosition, player, channel.ability(), store);

        String targetEntityId = support.resolveEntityId(channel.targetRef(), store);
        double damage = support.resolveDamageAmount(player, channel.ability()) * 0.55 * support.outgoingDamageMultiplier(player);
        if (targetEntityId != null) {
            damage *= support.incomingDamageMultiplier(targetEntityId);
            damage = support.absorbDamage(targetEntityId, damage);
        }

        if (damage > 0.0) {
            Damage pulseDamage = new Damage(new Damage.EntitySource(channel.ownerRef()), DamageCause.PHYSICAL, (float) damage);
            DamageSystems.executeDamage(channel.targetRef(), store, pulseDamage);
            support.applyPostDamageClassPassives(player, channel.ownerRef(), targetEntityId, damage, true);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
            if ("life_drain".equals(lower(channel.ability().getId()))) {
                // Life Drain siphons a single 50% of actual damage dealt (no extra lifesteal stack on the drain).
                double siphoned = support.healEntityFlat(channel.ownerRef(), store, damage * 0.50);
                if (siphoned > 0.0) {
                    player.getStatistics().setTotalHealingDone(player.getStatistics().getTotalHealingDone() + siphoned);
                }
            } else {
                support.applyLifesteal(channel.ownerRef(), player.getPlayerId(), damage);
            }
        }

        support.applyEffectById(channel.targetRef(), store,
                support.resolveImpactEffectId(player.getPlayerClass(), support.currentStyleId(player), channel.ability()));
        channel.scheduleNextPulse(now, channelPulseIntervalMillis);
        return false;
    }

    private void applyRepeatingLineControlEffects(ActiveLineControl lineControl,
                                                  PlayerData player,
                                                  Store<EntityStore> store) {
        if (!MotmEntityLiveness.isLiveTarget(lineControl.targetRef(), store)) {
            return;
        }
        String targetEntityId = support.resolveEntityId(lineControl.targetRef(), store);
        if (targetEntityId == null || targetEntityId.equals(player.getPlayerId())) {
            return;
        }

        for (String token : support.effectTokens(lineControl.ability())) {
            if (!support.shouldApplyRepeatingLineControlToken(token)) {
                continue;
            }
            support.applyTargetToken(token, lineControl.targetRef(), store,
                    lineControl.ownerRef(), player.getPlayerId(), lineControl.ability());
        }
    }

    private static boolean isVinesAbility(AbilityData ability) {
        return ability != null && "vines".equals(lower(ability.getId()));
    }

    private static double inferLineControlDurationSeconds(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        if (ability.getDurationSeconds() > 0.0) {
            return Math.max(1.0, ability.getDurationSeconds());
        }

        String travelType = lower(ability.getTravelType());
        if (travelType.contains("current") || travelType.contains("undertow")) {
            return 1.8;
        }
        return 1.2;
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ref, Store<EntityStore> currentStore) {
        return ref != null && ref.isValid() && ref.getStore() == currentStore;
    }

    private static double distance(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return Double.MAX_VALUE;
        }
        double dx = left.x - right.x;
        double dy = left.y - right.y;
        double dz = left.z - right.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    public interface Support {
        PlayerData player(String playerId);

        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        double range(AbilityData ability);

        double resolveDamageAmount(PlayerData player, AbilityData ability);

        double outgoingDamageMultiplier(PlayerData player);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        double incomingDamageMultiplier(String targetEntityId);

        double absorbDamage(String targetEntityId, double damage);

        void applyPostDamageClassPassives(PlayerData player,
                                          Ref<EntityStore> ownerRef,
                                          String targetEntityId,
                                          double damage,
                                          boolean abilityDamage);

        void applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt);

        double healEntityFlat(Ref<EntityStore> targetRef, Store<EntityStore> store, double amount);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        String currentStyleId(PlayerData player);

        boolean applyLineControlPull(Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     Ref<EntityStore> ownerRef,
                                     AbilityData ability);

        List<String> effectTokens(AbilityData ability);

        boolean shouldApplyRepeatingLineControlToken(String token);

        boolean applyTargetToken(String token,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> sourceRef,
                                 String sourcePlayerId,
                                 AbilityData ability);

        String humanize(String value);

        void renderTetherLink(Vector3d from,
                              Vector3d to,
                              PlayerData player,
                              AbilityData ability,
                              Store<EntityStore> store);
    }

    public record Result(boolean started, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result(false, "");
        }
    }
}
