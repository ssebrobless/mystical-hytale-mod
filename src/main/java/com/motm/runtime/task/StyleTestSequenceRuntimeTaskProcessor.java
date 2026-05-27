package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3i;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.state.ActiveStyleTest;
import com.motm.runtime.state.StyleTestRuntimeState;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Advances live style-test ability sequences.
 */
public final class StyleTestSequenceRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final StyleTestRuntimeState state;
    private final StyleTestTargetResolver targetResolver;
    private final Hooks hooks;

    public StyleTestSequenceRuntimeTaskProcessor(StyleTestRuntimeState state,
                                                 StyleTestTargetResolver targetResolver,
                                                 Hooks hooks) {
        this.state = state;
        this.targetResolver = targetResolver;
        this.hooks = hooks;
    }

    @Override
    public String id() {
        return "style-test-sequence";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        long now = System.currentTimeMillis();
        for (ActiveStyleTest test : state.activeTests()) {
            processTest(currentStore, test, now);
        }
    }

    private void processTest(Store<EntityStore> currentStore, ActiveStyleTest test, long now) {
        Player player = hooks.runtimePlayer(test.playerId());
        if (player == null) {
            state.stop(test.playerId());
            hooks.recordServerTruth("style_test_sequence_stopped", Map.of(
                    "playerId", test.playerId(),
                    "reason", "player_unavailable"
            ));
            return;
        }
        if (!hooks.isPlayerInStore(player, currentStore) || now < test.nextActionAtMs()) {
            return;
        }

        PlayerData playerData = hooks.playerData(test.playerId());
        if (playerData == null) {
            state.stop(test.playerId());
            hooks.recordServerTruth("style_test_sequence_stopped", Map.of(
                    "playerId", test.playerId(),
                    "reason", "player_data_unavailable"
            ));
            return;
        }

        if (test.nextAbilityIndex() >= test.abilityIds().size()) {
            hooks.sendMessage(player, "[MOTM] Live style test complete: "
                    + humanize(test.classId()) + " > " + test.styleName() + ".");
            state.stop(test.playerId());
            hooks.recordServerTruth("style_test_sequence_completed", Map.of(
                    "playerId", test.playerId(),
                    "classId", String.valueOf(test.classId()),
                    "styleId", String.valueOf(test.styleId()),
                    "styleName", String.valueOf(test.styleName()),
                    "abilityCount", test.abilityIds().size()
            ));
            return;
        }

        int stepIndex = test.nextAbilityIndex();
        String abilityId = test.abilityIds().get(stepIndex);
        AbilityData ability = hooks.findAbility(playerData, abilityId);
        if (ability == null) {
            hooks.sendMessage(player, "[MOTM] Live style test skipped a missing ability at step "
                    + (stepIndex + 1) + ".");
            state.advance(test.playerId(), now + 1200L);
            hooks.recordServerTruth("style_test_sequence_missing_ability", Map.of(
                    "playerId", test.playerId(),
                    "abilityId", String.valueOf(abilityId),
                    "step", stepIndex + 1
            ));
            return;
        }

        Ref<EntityStore> targetRef = targetResolver.findNearestNpc(currentStore, player, 28.0);
        Vector3i targetBlock = targetResolver.resolveTargetBlock(currentStore, player, targetRef);
        hooks.sendMessage(player, "[MOTM] Live test step "
                + (stepIndex + 1) + "/" + test.abilityIds().size()
                + ": " + ability.getName());

        hooks.queueAbilityCast(test.playerId(), ability.getId(), targetRef, targetBlock, true);
        state.advance(test.playerId(), now + resolveDelayMs(ability));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", test.playerId());
        payload.put("abilityId", String.valueOf(ability.getId()));
        payload.put("step", stepIndex + 1);
        payload.put("abilityCount", test.abilityIds().size());
        payload.put("targetFound", targetRef != null && targetRef.isValid());
        payload.put("targetBlock", targetBlock != null ? targetBlock.toString() : null);
        hooks.recordServerTruth("style_test_sequence_step", payload);
    }

    private long resolveDelayMs(AbilityData ability) {
        if (ability == null) {
            return 1500L;
        }

        double seconds = Math.max(
                1.2,
                hooks.castTimeSeconds(ability)
                        + hooks.recoveryTimeSeconds(ability)
                        + Math.max(ability.getDurationSeconds(), ability.getDelaySeconds()) * 0.6
        );
        return Math.min(5000L, Math.round(seconds * 1000.0));
    }

    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }
        String value = raw.replace('_', ' ').replace('-', ' ').trim();
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        PlayerData playerData(String playerId);

        AbilityData findAbility(PlayerData playerData, String abilityId);

        double castTimeSeconds(AbilityData ability);

        double recoveryTimeSeconds(AbilityData ability);

        void queueAbilityCast(String playerId,
                              String abilityId,
                              Ref<EntityStore> targetRef,
                              Vector3i targetBlock,
                              boolean notifyFailures);

        void recordServerTruth(String type, Map<String, Object> data);

        void sendMessage(Player player, String message);
    }
}
