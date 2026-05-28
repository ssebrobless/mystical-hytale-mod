package com.motm.runtime.ability.followup;

import com.motm.model.AbilityData;

public final class ActiveWeaponFollowUp {
    private final String playerId;
    private final AbilityData sourceAbility;
    private final long expireAtMillis;
    private int remainingUses;
    private final WeaponFollowUpSpec spec;
    private String boundItemId;

    private ActiveWeaponFollowUp(String playerId,
                                 AbilityData sourceAbility,
                                 long expireAtMillis,
                                 WeaponFollowUpSpec spec,
                                 String boundItemId) {
        this.playerId = playerId;
        this.sourceAbility = sourceAbility;
        this.expireAtMillis = expireAtMillis;
        this.remainingUses = spec.uses();
        this.spec = spec;
        this.boundItemId = boundItemId;
    }

    public static ActiveWeaponFollowUp create(String playerId,
                                              AbilityData sourceAbility,
                                              long expireAtMillis,
                                              WeaponFollowUpSpec spec) {
        return new ActiveWeaponFollowUp(playerId, sourceAbility, expireAtMillis, spec, null);
    }

    public String playerId() { return playerId; }
    public String sourceAbilityId() { return sourceAbility != null ? sourceAbility.getId() : ""; }
    public AbilityData sourceAbility() { return sourceAbility; }
    public long expireAtMillis() { return expireAtMillis; }
    public int remainingUses() { return remainingUses; }
    public int decrementRemainingUses() { return --remainingUses; }
    public double flatDamageBonus() { return spec.flatDamageBonus(); }
    public String riderToken() { return spec.riderToken(); }
    public double lifestealBonus() { return spec.lifestealBonus(); }
    public double shieldPercentOnHit() { return spec.shieldPercentOnHit(); }
    public double healRatioOnHit() { return spec.healRatioOnHit(); }
    public double splashRadius() { return spec.splashRadius(); }
    public double splashDamageRatio() { return spec.splashDamageRatio(); }
    public String secondaryRiderToken() { return spec.secondaryRiderToken(); }
    public double damageMultiplierBonus() { return spec.damageMultiplierBonus(); }
    public boolean alloyFollowUp() { return spec.alloyFollowUp(); }
    public String boundItemId() { return boundItemId; }
    public void bindItemId(String boundItemId) { this.boundItemId = boundItemId; }

    public WeaponFollowUpItemBinding bindOrRejectItem(String itemId) {
        if (!alloyFollowUp() || itemId == null || itemId.isBlank()) {
            return WeaponFollowUpItemBinding.accepted(false);
        }
        if (boundItemId == null || boundItemId.isBlank()) {
            boundItemId = itemId;
            return WeaponFollowUpItemBinding.accepted(true);
        }
        if (!boundItemId.equalsIgnoreCase(itemId)) {
            return WeaponFollowUpItemBinding.rejected(
                    "[MOTM] Alloy Enhancement ended: switched from " + boundItemId + " to " + itemId + ".");
        }
        return WeaponFollowUpItemBinding.accepted(false);
    }
}
