package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a combat style within a class.
 * Each class has 10 styles, each style has 3 abilities.
 */
public class StyleData {

    private String id;
    private String name;
    @SerializedName("class_id")
    private String classId;
    private String theme;
    @SerializedName("resource_type")
    private String resourceType; // rocks, water, tp, souls, etc.
    private List<AbilityData> abilities;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getClassId() { return classId; }
    public String getTheme() { return theme; }
    public String getResourceType() { return resourceType; }
    public List<AbilityData> getAbilities() { return abilities; }
}
