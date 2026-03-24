package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Defines an elemental reaction triggered when two elements combine on a target.
 * Loaded from elemental_reactions.json.
 */
public class ElementalReaction {

    private String id;
    private String name;
    @SerializedName("element_a")
    private String elementA;
    @SerializedName("element_b")
    private String elementB;
    @SerializedName("bonus_damage_percent")
    private double bonusDamagePercent;
    @SerializedName("applied_effects")
    private List<String> appliedEffects;
    private String color;
    private String description;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getElementA() { return elementA; }
    public String getElementB() { return elementB; }
    public double getBonusDamagePercent() { return bonusDamagePercent; }
    public List<String> getAppliedEffects() { return appliedEffects; }
    public String getColor() { return color; }
    public String getDescription() { return description; }

    /**
     * Check if this reaction matches the given pair of elements (order-independent).
     */
    public boolean matches(String elem1, String elem2) {
        return (elementA.equals(elem1) && elementB.equals(elem2))
                || (elementA.equals(elem2) && elementB.equals(elem1));
    }
}
