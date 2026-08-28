package com.motm.runtime.ability.field;

import com.motm.model.AbilityData;

import java.util.List;
import java.util.Set;

public final class FieldRuntimeSpecs {

    public static final double DEFAULT_AREA_RADIUS = 3.5;
    public static final double DEFAULT_FIELD_THICKNESS = 1.35;
    public static final double DEFAULT_FIELD_DAMAGE_RATIO = 0.28;
    public static final long FIELD_PULSE_INTERVAL_MS = 900L;
    public static final long FIELD_VISUAL_REFRESH_MS = 900L;

    private static final Set<String> PERSISTENT_FIELD_CAST_TYPES = Set.of(
            "ground_zone", "support_zone", "barrier", "ground_target");

    private FieldRuntimeSpecs() {
    }

    public static boolean isPersistentField(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String castType = lower(ability.getCastType());
        if (PERSISTENT_FIELD_CAST_TYPES.contains(castType)) {
            return true;
        }

        if ("self_buff".equals(castType)
                && terrainSpec(ability).kind() != FieldTerrainRuntimeKind.NONE) {
            return true;
        }

        if (!"ground_target".equals(castType)) {
            return false;
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        String abilityId = lower(ability.getId());
        return ability.getDurationSeconds() > 0.0
                && (ability.getDelaySeconds() > 0.0
                || terrainEffect.contains("sinkhole")
                || terrainEffect.contains("hazard")
                || "sinkhole".equals(abilityId));
    }

    public static boolean isIronWall(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        return "iron_wall".equals(lower(ability.getId()))
                || lower(ability.getTerrainEffect()).contains("iron_wall");
    }

    public static boolean isCasterCentered(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        return "lava_pool".equals(abilityId)
                || "ice_cap".equals(abilityId)
                || "snowstorm".equals(abilityId)
                || "piercing_rain".equals(abilityId)
                || "rainbow".equals(abilityId)
                || "tide_pool".equals(abilityId)
                || "oil_spill".equals(abilityId)
                || terrainEffect.contains("ice_cap_tube")
                || terrainEffect.contains("snowstorm")
                || terrainEffect.contains("piercing_rain")
                || terrainEffect.contains("healing_rainbow")
                || terrainEffect.contains("tide_pool")
                || terrainEffect.contains("oil_spill");
    }

    public static boolean shouldFollowOwner(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        return "snowstorm".equals(abilityId)
                || "piercing_rain".equals(abilityId)
                || "rainbow".equals(abilityId)
                || terrainEffect.contains("snowstorm")
                || terrainEffect.contains("piercing_rain")
                || terrainEffect.contains("healing_rainbow");
    }

    public static boolean shouldUseFieldVisualProxy(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        return !"lava_pool".equals(abilityId) && !terrainEffect.contains("lava_pool")
                && !"tide_pool".equals(abilityId) && !terrainEffect.contains("tide_pool")
                && !"oil_spill".equals(abilityId) && !terrainEffect.contains("oil_spill");
    }

    public static boolean shouldApplyRepeatingTargetTokens(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        return !"lava_pool".equals(abilityId) && !terrainEffect.contains("lava_pool");
    }

    public static FieldTerrainRuntimeSpec terrainSpec(AbilityData ability) {
        if (ability == null) {
            return FieldTerrainRuntimeSpec.none();
        }
        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("iron_wall")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.IRON_WALL,
                    "iron_wall",
                    false,
                    false,
                    false,
                    0,
                    List.of("Metal_Iron"),
                    List.of("Metal_Iron")
            );
        }
        if (terrainEffect.contains("lava_pool")) {
            return blockDisc("lava_pool",
                    "Rock_Volcanic_Cracked_Incandescent", "Rock_Volcanic_Cracked_Lava", "Rock_Volcanic");
        }
        if (terrainEffect.contains("mudpit")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.MUDPIT,
                    "mudpit",
                    false,
                    true,
                    true,
                    0,
                    List.of("Fluid_Water", "Water", "water"),
                    List.of()
            );
        }
        if (terrainEffect.contains("stone_pillar")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.STONE_PILLAR,
                    "stone_pillar",
                    false,
                    false,
                    false,
                    4,
                    List.of("Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick"),
                    List.of()
            );
        }
        if (terrainEffect.contains("tide_pool")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.TIDE_POOL,
                    "tide_pool",
                    false,
                    true,
                    false,
                    0,
                    List.of("Fluid_Water", "Water", "water"),
                    List.of()
            );
        }
        if (terrainEffect.contains("oil_spill")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.OIL_SPILL,
                    "oil_spill",
                    false,
                    true,
                    false,
                    0,
                    List.of("Fluid_Tar", "Tar", "tar", "Fluid_Water", "Water", "water"),
                    List.of()
            );
        }
        if (terrainEffect.contains("ice_cap_tube")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.ICE_CAP_TUBE,
                    "ice_cap_tube",
                    false,
                    false,
                    false,
                    3,
                    List.of("Rock_Stone_Brick_Pillar_Middle", "Rock_Calcite", "Ice_Block", "Fluid_Ice", "Water_Ice"),
                    List.of()
            );
        }
        if (terrainEffect.contains("glacier")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.GLACIER_WALL,
                    "glacier",
                    false,
                    false,
                    false,
                    0,
                    List.of("Rock_Stone_Brick_Pillar_Middle", "Rock_Calcite", "Ice_Block", "Fluid_Ice", "Water_Ice"),
                    List.of()
            );
        }
        if (terrainEffect.contains("ice_shelf")) {
            return new FieldTerrainRuntimeSpec(
                    FieldTerrainRuntimeKind.ICE_SHELF_WALL,
                    "ice_shelf",
                    true,
                    false,
                    false,
                    0,
                    List.of("Rock_Stone_Brick_Pillar_Middle", "Rock_Calcite", "Ice_Block", "Fluid_Ice", "Water_Ice"),
                    List.of()
            );
        }
        if (terrainEffect.contains("infernal_ground")) {
            return blockDisc("infernal_ground",
                    "Rock_Volcanic_Cracked_Incandescent", "Rock_Volcanic_Cracked_Lava", "Rock_Volcanic");
        }
        if (terrainEffect.contains("void_rift") || terrainEffect.contains("rift")) {
            // Ore_Onyxium_Basalt has no client texture (renders missing-texture checkerboard);
            // Rock_Slate is client-proven dark and reads as a void scar.
            return blockDisc("void_rift", "Rock_Slate", "Rock_Basalt", "Rock_Stone");
        }
        if (terrainEffect.contains("sanctuary")) {
            return blockDisc("sanctuary", "Rock_Marble", "Rock_Chalk", "Rock_Calcite");
        }
        if (terrainEffect.contains("snowstorm")) {
            return blockDisc("snowstorm", "Soil_Snow", "Soil_Snow_Half", "Rock_Aqua");
        }
        if (terrainEffect.contains("healing_rainbow") || terrainEffect.contains("rainbow")) {
            return blockDisc("healing_rainbow", "Rock_Marble", "Rock_Quartzite", "Rock_Calcite");
        }
        if (terrainEffect.contains("lingering_tremor") || terrainEffect.contains("seismic")) {
            return blockDisc("lingering_tremor", "Rock_Slate", "Rock_Shale", "Rock_Stone");
        }
        if (terrainEffect.contains("sinkhole")) {
            return blockDisc("sinkhole", "Soil_Dirt_Dry", "Soil_Dirt", "Rock_Slate");
        }
        if (terrainEffect.contains("crystal")) {
            // Green crystal (same client-proven blocks as the floating gem cube) so the gem field
            // reads as a green gem AoE, not a tan stone patch. Ore_Onyxium/Cobalt checkerboarded.
            return blockDisc("crystal_gem", "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Rock_Crystal_Green_Block");
        }
        if (terrainEffect.contains("acid_rain")) {
            return blockDisc("acid_rain", "Soil_Dirt_Poisoned", "Soil_Grass_Burnt", "Soil_Dirt_Dry");
        }
        if (terrainEffect.contains("smog")) {
            return blockDisc("smog_cloud", "Soil_Dirt_Poisoned", "Soil_Dirt_Dry", "Soil_Dirt");
        }
        if (terrainEffect.contains("smoke_bomb")) {
            return blockDisc("smoke_bomb", "Soil_Dirt_Burnt", "Soil_Dirt", "Rock_Slate");
        }
        if (terrainEffect.contains("funnel_cloud") || terrainEffect.contains("tempest")) {
            return blockDisc("wind_scour", "Soil_Dirt_Dry", "Soil_Sand", "Soil_Dirt");
        }
        if (terrainEffect.contains("piercing_rain")) {
            return fluidDisc("piercing_rain", "Fluid_Water");
        }
        return FieldTerrainRuntimeSpec.none();
    }

    private static FieldTerrainRuntimeSpec blockDisc(String reason, String... blocks) {
        return new FieldTerrainRuntimeSpec(
                FieldTerrainRuntimeKind.GROUNDED_BLOCK_DISC,
                reason, true, false, false, 0, List.of(blocks), List.of());
    }

    private static FieldTerrainRuntimeSpec fluidDisc(String reason, String... fluids) {
        return new FieldTerrainRuntimeSpec(
                FieldTerrainRuntimeKind.GROUNDED_FLUID_DISC,
                reason, true, true, false, 0, List.of(fluids), List.of());
    }

    public static List<String> terrainRestoreReasons(AbilityData ability) {
        FieldTerrainRuntimeSpec spec = terrainSpec(ability);
        return spec.kind() == FieldTerrainRuntimeKind.NONE || spec.reason().isBlank()
                ? List.of()
                : List.of(spec.reason());
    }

    public static double radius(AbilityData ability) {
        return ability != null && ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
    }

    public static double halfWidth(AbilityData ability, double radius) {
        return ability != null && ability.getWidth() > 0 ? ability.getWidth() / 2.0 : Math.max(radius, 3.0);
    }

    public static double thickness(AbilityData ability, double radius) {
        return ability != null && "barrier".equalsIgnoreCase(ability.getCastType())
                ? DEFAULT_FIELD_THICKNESS
                : Math.max(1.25, radius);
    }

    public static long delayMillis(AbilityData ability) {
        return (long) (Math.max(0.0, ability != null ? ability.getDelaySeconds() : 0.0) * 1000);
    }

    public static long durationMillis(AbilityData ability) {
        double durationSeconds = ability != null && ability.getDurationSeconds() > 0
                ? ability.getDurationSeconds()
                : 4.0;
        return (long) (Math.max(1.5, durationSeconds) * 1000);
    }

    public static double pulseDamage(double baseDamage, AbilityData ability) {
        if (baseDamage <= 0.0 || ability == null) {
            return 0.0;
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        return switch (lower(ability.getCastType())) {
            case "support_zone" -> 0.0;
            case "barrier" -> baseDamage * 0.18;
            default -> baseDamage * pulseDamageRatio(terrainEffect);
        };
    }

    public static double pullLift(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }
        String travelType = lower(ability.getTravelType());
        String terrainEffect = lower(ability.getTerrainEffect());
        String abilityId = lower(ability.getId());
        if (travelType.contains("funnel")
                || travelType.contains("twister")
                || terrainEffect.contains("funnel")
                || terrainEffect.contains("tempest")
                || abilityId.contains("tempest")) {
            return 0.35;
        }
        return 0.0;
    }

    private static double pulseDamageRatio(String terrainEffect) {
        if (terrainEffect.contains("sinkhole")) {
            return 0.34;
        }
        if (terrainEffect.contains("falling_rocks")) {
            return 0.36;
        }
        if (terrainEffect.contains("acid")) {
            return 0.30;
        }
        if (terrainEffect.contains("smog")) {
            return 0.22;
        }
        return DEFAULT_FIELD_DAMAGE_RATIO;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
