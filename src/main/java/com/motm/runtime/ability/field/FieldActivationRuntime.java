package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.util.AbilityPresentation;

import java.util.List;
import java.util.Locale;

public final class FieldActivationRuntime {

    public Result activate(String ownerPlayerId,
                           Ref<EntityStore> ownerRef,
                           String classId,
                           String styleId,
                           AbilityData ability,
                           String castType,
                           Vector3d center,
                           Vector3d forwardDirection,
                           Vector3d lineDirection,
                           double radius,
                           double halfWidth,
                           double thickness,
                           long activateAtMillis,
                           long durationMillis,
                           boolean followOwner,
                           FieldVisualRuntime visual,
                           String traceId,
                           String terrainSummary,
                           int immediatePushes,
                           long delayMillis,
                           double pullStep) {
        if (ownerPlayerId == null || ownerRef == null || !ownerRef.isValid()
                || ability == null || center == null || forwardDirection == null || lineDirection == null) {
            return Result.none();
        }

        long expireAtMillis = activateAtMillis + Math.max(0L, durationMillis);
        FieldVisualRuntime resolvedVisual = visual == null ? FieldVisualRuntime.none() : visual;
        ActiveField field = new ActiveField(
                ownerPlayerId,
                ownerRef,
                classId,
                styleId,
                ability,
                center,
                forwardDirection,
                lineDirection,
                radius,
                halfWidth,
                thickness,
                expireAtMillis,
                activateAtMillis,
                activateAtMillis,
                followOwner,
                resolvedVisual.visualRefs(),
                resolvedVisual.loopEffectId(),
                resolvedVisual.nextRefreshAtMillis(),
                traceId
        );

        return new Result(field, summary(ability, castType, radius, halfWidth, activateAtMillis, durationMillis,
                terrainSummary, immediatePushes, delayMillis, pullStep));
    }

    private static String summary(AbilityData ability,
                                  String castType,
                                  double radius,
                                  double halfWidth,
                                  long activateAtMillis,
                                  long durationMillis,
                                  String terrainSummary,
                                  int immediatePushes,
                                  long delayMillis,
                                  double pullStep) {
        String normalizedCastType = lower(castType);
        String fieldLabel = switch (normalizedCastType) {
            case "barrier" -> "barrier";
            case "ground_target" -> "hazard";
            default -> "field";
        };
        String sizeLabel = "barrier".equals(normalizedCastType)
                ? "width " + formatDistance(halfWidth * 2.0) + "m"
                : "radius " + formatDistance(radius) + "m";
        String controlLabel = ability.getPullForce() > 0
                ? " | pull " + formatDistance(pullStep) + "m pulse"
                : "";
        String timingLabel = delayMillis > 0L
                ? "arms in " + AbilityPresentation.formatDecimal(delayMillis / 1000.0) + "s"
                + " | lasts " + AbilityPresentation.formatDecimal(durationMillis / 1000.0) + "s"
                : "active for " + AbilityPresentation.formatDecimal(durationMillis / 1000.0) + "s";
        String terrainLabel = terrainSummary == null || terrainSummary.isBlank() ? "" : " | " + terrainSummary;
        String pushLabel = immediatePushes > 0 ? " | pushed " + immediatePushes + " spawn-overlap target(s)" : "";
        return fieldLabel + " " + timingLabel
                + " | " + sizeLabel
                + controlLabel
                + terrainLabel
                + pushLabel;
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f", distance);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record Result(ActiveField field, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public boolean activated() {
            return field != null;
        }

        public List<ActiveField> fields() {
            return field == null ? List.of() : List.of(field);
        }

        public static Result none() {
            return new Result(null, "");
        }
    }
}
