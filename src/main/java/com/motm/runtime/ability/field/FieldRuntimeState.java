package com.motm.runtime.ability.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FieldRuntimeState {
    private final List<ActiveField> activeFields = new ArrayList<>();
    private final Map<String, List<BuriedVictim>> buriedVictimsByField = new HashMap<>();

    public void addField(ActiveField field) {
        if (field != null) {
            activeFields.add(field);
        }
    }

    public int addFields(Collection<ActiveField> fields) {
        if (fields == null || fields.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (ActiveField field : fields) {
            if (field != null) {
                activeFields.add(field);
                added++;
            }
        }
        return added;
    }

    public int activeFieldCount() {
        return activeFields.size();
    }

    public void removeProcessedFields(Predicate<ActiveField> processor) {
        if (processor != null) {
            activeFields.removeIf(processor);
        }
    }

    public int removeFieldsForPlayer(String playerId, Consumer<ActiveField> cleanup) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        return removeFieldsMatching(field -> playerId.equals(field.ownerPlayerId()), cleanup);
    }

    public int removeFieldsForAbility(String playerId, String normalizedAbilityId, Consumer<ActiveField> cleanup) {
        if (playerId == null || playerId.isBlank()
                || normalizedAbilityId == null || normalizedAbilityId.isBlank()) {
            return 0;
        }
        return removeFieldsMatching(field -> playerId.equals(field.ownerPlayerId())
                && normalizedAbilityId.equals(normalize(field.ability() == null ? null : field.ability().getId())), cleanup);
    }

    public boolean hasBurialEntry(ActiveField field) {
        return buriedVictimsByField.containsKey(burialKey(field));
    }

    public void putBuriedVictims(ActiveField field, List<BuriedVictim> victims) {
        buriedVictimsByField.put(burialKey(field), victims == null ? List.of() : List.copyOf(victims));
    }

    public List<BuriedVictim> buriedVictims(ActiveField field) {
        List<BuriedVictim> victims = buriedVictimsByField.get(burialKey(field));
        return victims == null ? List.of() : victims;
    }

    public List<BuriedVictim> removeBuriedVictims(ActiveField field) {
        List<BuriedVictim> victims = buriedVictimsByField.remove(burialKey(field));
        return victims == null ? List.of() : victims;
    }

    private int removeFieldsMatching(Predicate<ActiveField> matcher, Consumer<ActiveField> cleanup) {
        int[] removed = {0};
        activeFields.removeIf(field -> {
            if (field == null || !matcher.test(field)) {
                return false;
            }
            if (cleanup != null) {
                cleanup.accept(field);
            }
            removed[0]++;
            return true;
        });
        return removed[0];
    }

    private static String burialKey(ActiveField field) {
        if (field == null) {
            return "";
        }
        return field.ownerPlayerId() + "::" + normalize(field.ability() == null ? null : field.ability().getId())
                + "::" + field.activateAtMillis();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
