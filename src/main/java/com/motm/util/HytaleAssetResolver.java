package com.motm.util;

import com.motm.model.AbilityActionAssets;
import com.motm.model.AbilityData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps MOTM abilities onto real built-in Hytale assets so the future runtime
 * layer has concrete animation / particle / model targets to use.
 */
public final class HytaleAssetResolver {

    private static final String ANIM_DEFAULT_INTERACT = "Common/Characters/Animations/Default/Interact.blockyanim";
    private static final String ANIM_DASH_FORWARD = "Common/Characters/Animations/Dash/Dash_Forward.blockyanim";
    private static final String ANIM_FLY_FAST = "Common/Characters/Animations/Fly/Fly_Fast.blockyanim";
    private static final String ANIM_AXE_GUARD = "Common/Characters/Animations/Items/Dual_Handed/Axes/Attacks/Guard/Guard.blockyanim";
    private static final String ANIM_BATTLEAXE_SWEEP = "Common/Characters/Animations/Items/Dual_Handed/Battleaxe/Attacks/Sweep/Sweep.blockyanim";
    private static final String ANIM_BATTLEAXE_DOWNSTRIKE = "Common/Characters/Animations/Items/Dual_Handed/Battleaxe/Attacks/Downstrike_Charged/Downstrike_Charged.blockyanim";
    private static final String ANIM_BATTLEAXE_WHIRLWIND = "Common/Characters/Animations/Items/Dual_Handed/Battleaxe/Attacks/Whirlwind/Whirlwind.blockyanim";
    private static final String ANIM_BOW_SHOOT = "Common/Characters/Animations/Items/Dual_Handed/Bow/Attacks/Shoot/Shoot.blockyanim";
    private static final String ANIM_DAGGER_LUNGE = "Common/Characters/Animations/Items/Dual_Handed/Daggers/Attacks/Lunge_Double/Lunge_Double.blockyanim";
    private static final String ANIM_DAGGER_POUNCE = "Common/Characters/Animations/Items/Dual_Handed/Daggers/Attacks/Pounce/Pounce.blockyanim";
    private static final String ANIM_DAGGER_RAZORSTRIKE = "Common/Characters/Animations/Items/Dual_Handed/Daggers/Attacks/Razorstrike/Razorstrike_Slash.blockyanim";
    private static final String ANIM_VOID_SUMMON = "Common/NPC/Void/Necromancer_Void/Animations/Attacks/Summon.blockyanim";
    private static final String ANIM_VOID_SHOOT = "Common/NPC/Void/Eye_Void/Animations/Attacks/Shoot.blockyanim";
    private static final String ANIM_SPARK_ATTACK = "Common/NPC/Beast/Spark_Living/Animations/Attacks/Spit.blockyanim";
    private static final String ANIM_FROST_BOLT = "Common/NPC/Elemental/Dragon_Frost/Animations/Attacks/Frost_Bolt.blockyanim";

    private static final String FX_EARTH_CAST = "Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner";
    private static final String FX_EARTH_IMPACT = "Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner";
    private static final String FX_TERRA_QUAKE_CAST = "MOTM_Terra_Quake_Cast";
    private static final String FX_TERRA_QUAKE_IMPACT = "MOTM_Terra_Quake_Impact";
    private static final String FX_TERRA_QUAKE_LOOP = "MOTM_Terra_Quake_Loop";
    private static final String FX_PROOF_METAL_COAT = "MOTM_Proof_Coating_Metal";
    private static final String FX_PROOF_ALLOY_ENHANCEMENT = "MOTM_Proof_Alloy_Enhancement";
    private static final String FX_PROOF_ALLOY_IMPACT = "MOTM_Proof_Alloy_Impact";
    private static final String FX_PROOF_MAGMA_SLING_TRAVEL = "MOTM_Proof_Magma_Sling_Travel";
    private static final String FX_PROOF_LAVA_POOL_FIELD = "MOTM_Proof_Lava_Pool_Field";
    private static final String FX_PROOF_OBSIDIAN_COAT = "MOTM_Proof_Coating_Obsidian";
    private static final String FX_PROOF_STONE_COAT = "MOTM_Proof_Coating_Stone";
    private static final String FX_PROOF_POISON_COAT = "MOTM_Proof_Coating_Poison";
    private static final String FX_PROOF_SAND_CLOUD = "MOTM_Proof_Sand_Cloud";
    private static final String FX_PROOF_DEBRIS_WAVE = "MOTM_Proof_Debris_Wave";
    private static final String FX_PROOF_GEM_GREEN = "MOTM_Proof_Gem_Green";
    private static final String FX_STONE_DUST = "Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner";
    private static final String FX_METAL_SPARKS = "Server/Particles/Block/Metal/Spawners/Block_Break_Metal_Sparks.particlespawner";
    private static final String FX_CRYSTAL_SPARKS = "Server/Particles/Block/Crystal/Spawners/Block_Break_Crystal_Sparks.particlespawner";
    private static final String FX_SAND_DUST = "Server/Particles/Block/Sand/Spawners/Block_Break_Sand_Dust.particlespawner";
    private static final String FX_WATER_CAST = "Server/Particles/Block/Water/Spawners/Bubbles.particlespawner";
    private static final String FX_WATER_TRAVEL = "Server/Particles/Block/Water/Spawners/Water_Bubble_Stream_Alpha.particlespawner";
    private static final String FX_WATER_IMPACT = "Server/Particles/Block/Water/Spawners/Water_Small_Burst.particlespawner";
    private static final String FX_ICE_IMPACT = "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner";
    private static final String FX_HEAL_LOOP = "Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner";
    private static final String FX_HEAL_SMOKE = "Server/Particles/Deployables/Healing_Totem/Totem_Heal_SmokeFlat_Constant.particlespawner";
    private static final String FX_SLOW_LOOP = "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner";
    private static final String FX_WIND_CAST = "Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner";
    private static final String FX_WIND_TRAVEL = "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner";
    private static final String FX_WIND_IMPACT = "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner";
    private static final String FX_WIND_LOOP = "Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem";
    private static final String FX_LIGHTNING = "Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner";
    private static final String FX_LIGHTNING_SPARKS = "Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner";
    private static final String FX_SMOKE_CAST = "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner";
    private static final String FX_SMOKE_END = "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner";
    private static final String FX_VOID_CAST = "Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner";
    private static final String FX_VOID_IMPACT = "Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem";
    private static final String FX_VOID_SMOKE = "Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner";
    private static final String FX_VOID_SPLASH = "Server/Particles/Combat/Impact/Misc/Void/VoidSplash.particlespawner";
    private static final String FX_POISON_IMPACT = "Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem";
    private static final String FX_ACID_SPARKS = "Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner";
    private static final String FX_FIRE_CAST = "Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner";
    private static final String FX_FIRE_IMPACT = "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner";
    private static final String FX_FIRE_SMOKE = "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner";
    private static final String FX_FIRE_AOE = "Server/Particles/Combat/Fire_Stick/Fire_Trap/Fire_AoE_Grow.particlesystem";

    private static final String MODEL_ROOT_SPIRIT = "Common/NPC/Elemental/Spirit_Root/Models/Model.blockymodel";
    private static final String MODEL_FROST_SPIRIT = "Common/NPC/Elemental/Spirit_Frost/Models/Model.blockymodel";
    private static final String MODEL_THUNDER_SPIRIT = "Common/NPC/Elemental/Spirit_Thunder/Models/Model.blockymodel";
    private static final String MODEL_FROST_GOLEM = "Common/NPC/Elemental/Golem_Crystal/Models/Model.blockymodel";
    private static final String MODEL_FIRE_GOLEM = "Common/NPC/Elemental/Golem_Firesteel/Models/Model.blockymodel";
    private static final String MODEL_SCARAK = "Common/NPC/Beast/Scarak_Fighter/Models/Model.blockymodel";
    private static final String MODEL_SCARAK_BROODMOTHER = "Common/NPC/Beast/Scarak_Broodmother/Models/Model.blockymodel";
    private static final String MODEL_SPARK_LIVING = "Common/NPC/Beast/Spark_Living/Models/Model.blockymodel";
    private static final String MODEL_PTERODACTYL = "Common/NPC/Flying_Beast/Pterodactyl/Models/Model.blockymodel";
    private static final String MODEL_BAT = "Common/NPC/Flying_Critter/Bat/Models/Model.blockymodel";
    private static final String MODEL_SHADOW_KNIGHT = "Common/NPC/Undead/Shadow_Knight/Models/Model.blockymodel";
    private static final String MODEL_TOAD_RHINO = "Common/NPC/Beast/Toad_Rhino/Models/Model.blockymodel";
    private static final String MODEL_REX_CAVE = "Common/NPC/Beast/Rex_Cave/Models/Model.blockymodel";
    private static final String MODEL_VOID_SPAWN = "Common/NPC/Void/Spawn_Void/Models/Model.blockymodel";
    private static final String MODEL_VOID_EYE = "Common/NPC/Void/Eye_Void/Models/Model.blockymodel";
    private static final String MODEL_FROG = "Common/NPC/Critter/Frog/Models/Model.blockymodel";
    private static final String MODEL_FIREBALL_PROJECTILE = "Common/Items/Projectiles/Fireball.blockymodel";
    private static final String MODEL_RUBBLE_PROJECTILE = "Server/Models/Projectiles/Items/Rubble/Rubble_Stone.json";
    private static final String ROLE_EMPTY = "Empty_Role";
    private static final String ROLE_SLUG_MAGMA = "Slug_Magma";
    private static final String ROLE_SPARK_LIVING = "Spark_Living";

    private HytaleAssetResolver() {
    }

    public static AbilityActionAssets resolve(String classId, String styleId, AbilityData ability) {
        if (ability == null) {
            return AbilityActionAssets.empty();
        }

        return new AbilityActionAssets(
                resolveAnimation(classId, styleId, ability),
                resolveCastEffect(classId, styleId, ability),
                resolveTravelEffect(classId, styleId, ability),
                resolveImpactEffect(classId, styleId, ability),
                resolveLoopEffect(classId, styleId, ability),
                resolveModel(classId, styleId, ability)
        );
    }

    public static String buildCompactSummary(String classId, String styleId, AbilityData ability) {
        AbilityActionAssets assets = resolve(classId, styleId, ability);
        List<String> parts = new ArrayList<>();
        append(parts, "Anim ", assets.getAnimationAsset());
        append(parts, "FX ", firstNonBlank(
                assets.getCastEffectAsset(),
                assets.getTravelEffectAsset(),
                assets.getImpactEffectAsset(),
                assets.getLoopEffectAsset()
        ));
        append(parts, "Model ", assets.getModelAsset());
        return String.join(" | ", parts);
    }

    public static String buildDetailedSummary(String classId, String styleId, AbilityData ability) {
        AbilityActionAssets assets = resolve(classId, styleId, ability);
        List<String> parts = new ArrayList<>();
        append(parts, "Anim ", assets.getAnimationAsset());
        append(parts, "Cast FX ", assets.getCastEffectAsset());
        append(parts, "Travel FX ", assets.getTravelEffectAsset());
        append(parts, "Impact FX ", assets.getImpactEffectAsset());
        append(parts, "Loop FX ", assets.getLoopEffectAsset());
        append(parts, "Model ", assets.getModelAsset());
        return String.join(" | ", parts);
    }

    public static String resolveModelId(String classId, String styleId, AbilityData ability) {
        AbilityActionAssets assets = resolve(classId, styleId, ability);
        return extractModelId(assets.getModelAsset());
    }

    public static String resolveProjectileRoleId(String classId, String styleId, AbilityData ability) {
        String abilityId = ability == null ? "" : lower(ability.getId());
        String style = lower(styleId);

        if ("terra".equals(lower(classId)) && "magma".equals(style) && "magma_sling".equals(abilityId)) {
            return ROLE_EMPTY;
        }
        if ("terra".equals(lower(classId))
                && (("stone".equals(style) && "rubble_rouser".equals(abilityId))
                || ("arbor".equals(style) && ("sapling".equals(abilityId) || "vines".equals(abilityId)))
                || ("bloom".equals(style) && "nightshade".equals(abilityId)))) {
            return ROLE_EMPTY;
        }

        String modelId = resolveModelId(classId, styleId, ability);
        if (modelId != null && !modelId.isBlank()) {
            return modelId;
        }

        return ROLE_SPARK_LIVING;
    }

    public static String resolveFieldRoleId(String classId, String styleId, AbilityData ability) {
        return ROLE_EMPTY;
    }

    private static String resolveAnimation(String classId, String styleId, AbilityData ability) {
        String castType = lower(ability.getCastType());
        String abilityId = lower(ability.getId());

        if (isFlightTransformation(abilityId, castType)) {
            return ANIM_FLY_FAST;
        }
        if ("summon".equals(castType) || "summon_buff".equals(castType)) {
            if ("corruptus".equals(lower(classId))) {
                return ANIM_VOID_SUMMON;
            }
            if ("hydro".equals(lower(classId))) {
                return ANIM_FROST_BOLT;
            }
            return ANIM_DEFAULT_INTERACT;
        }
        if ("teleport".equals(castType)) {
            return ANIM_DAGGER_LUNGE;
        }
        if ("dash".equals(castType) || "dash_strike".equals(castType) || "dash_buff".equals(castType)) {
            return "aero".equals(lower(classId)) ? ANIM_DASH_FORWARD : ANIM_DAGGER_LUNGE;
        }
        if ("leap".equals(castType) || "dive_strike".equals(castType)) {
            return ANIM_DAGGER_POUNCE;
        }
        if ("projectile".equals(castType)
                || "projectile_line".equals(castType)
                || "projectile_volley".equals(castType)
                || "projectile_burst".equals(castType)
                || "chain".equals(castType)
                || "gaze".equals(castType)
                || "curse".equals(castType)
                || "line_control".equals(castType)
                || "wave_line".equals(castType)) {
            if ("corruptus".equals(lower(classId))) {
                return ANIM_VOID_SHOOT;
            }
            if ("aero".equals(lower(classId)) && abilityId.contains("smite")) {
                return ANIM_SPARK_ATTACK;
            }
            if ("hydro".equals(lower(classId)) && (abilityId.contains("frost") || abilityId.contains("ice"))) {
                return ANIM_FROST_BOLT;
            }
            return ANIM_BOW_SHOOT;
        }
        if ("ground_zone".equals(castType)
                || "ground_burst".equals(castType)
                || "ground_target".equals(castType)
                || "ground_strike".equals(castType)
                || "self_burst".equals(castType)
                || "support_zone".equals(castType)
                || "barrier".equals(castType)
                || "execute".equals(castType)) {
            if ("aero".equals(lower(classId)) && "tornado".equals(lower(styleId))) {
                return ANIM_BATTLEAXE_WHIRLWIND;
            }
            return ANIM_BATTLEAXE_DOWNSTRIKE;
        }
        if ("cone".equals(castType)) {
            return ANIM_BATTLEAXE_SWEEP;
        }
        if ("self_buff".equals(castType) || "cleanse".equals(castType) || "channel".equals(castType) || "air_stall".equals(castType)) {
            return ANIM_AXE_GUARD;
        }
        if ("transformation".equals(castType)) {
            return abilityId.contains("shadow") ? ANIM_DAGGER_RAZORSTRIKE : ANIM_FLY_FAST;
        }
        return ANIM_DEFAULT_INTERACT;
    }

    private static String resolveCastEffect(String classId, String styleId, AbilityData ability) {
        String abilityId = lower(ability.getId());
        String castType = lower(ability.getCastType());
        String style = lower(styleId);

        if ("terra".equals(lower(classId)) && "quake".equals(style)
                && ("stomp".equals(abilityId) || "aftershock".equals(abilityId) || "sinkhole".equals(abilityId))) {
            return FX_TERRA_QUAKE_CAST;
        }
        if ("terra".equals(lower(classId)) && "magma".equals(style)) {
            if (abilityId.contains("lava") || abilityId.contains("magma")) {
                return FX_FIRE_CAST;
            }
            if (abilityId.contains("obsidian")) {
                return FX_PROOF_OBSIDIAN_COAT;
            }
        }
        if ("terra".equals(lower(classId))) {
            if ("metal".equals(style) && abilityId.contains("alloy")) {
                return FX_PROOF_ALLOY_ENHANCEMENT;
            }
            if ("metal".equals(style) && abilityId.contains("metal")) {
                return FX_PROOF_METAL_COAT;
            }
            if ("self_petrification".equals(style) && (abilityId.contains("gargoyle") || abilityId.contains("glare") || abilityId.contains("tunnel"))) {
                return FX_PROOF_STONE_COAT;
            }
            if ("soil".equals(style) && abilityId.contains("debris")) {
                return FX_PROOF_DEBRIS_WAVE;
            }
            if ("sand".equals(style) && (abilityId.contains("sandstorm") || abilityId.contains("dust_devil"))) {
                return FX_PROOF_SAND_CLOUD;
            }
            if ("gem".equals(style) || abilityId.contains("lapidary") || abilityId.contains("refraction") || abilityId.contains("fracture")) {
                return FX_PROOF_GEM_GREEN;
            }
        }

        return switch (lower(classId)) {
            case "terra" -> {
                if (abilityId.contains("gem") || abilityId.contains("crystal")) {
                    yield FX_CRYSTAL_SPARKS;
                }
                if (abilityId.contains("sand")) {
                    yield FX_SAND_DUST;
                }
                if (abilityId.contains("metal")) {
                    yield FX_METAL_SPARKS;
                }
                yield FX_EARTH_CAST;
            }
            case "hydro" -> {
                if (abilityId.contains("rainbow") || abilityId.contains("heal")) {
                    yield FX_HEAL_SMOKE;
                }
                if ("snow".equals(style) || abilityId.contains("snow") || abilityId.contains("frost")) {
                    yield FX_ICE_IMPACT;
                }
                yield FX_WATER_CAST;
            }
            case "aero" -> {
                if (abilityId.contains("smite") || abilityId.contains("lightning") || abilityId.contains("thunder")) {
                    yield FX_LIGHTNING;
                }
                if (abilityId.contains("smoke") || abilityId.contains("vanish")) {
                    yield FX_SMOKE_CAST;
                }
                if ("summon".equals(castType)) {
                    yield FX_LIGHTNING_SPARKS;
                }
                yield FX_WIND_CAST;
            }
            case "corruptus" -> {
                if (abilityId.contains("fire") || abilityId.contains("hell") || abilityId.contains("infernal") || abilityId.contains("ignite")) {
                    yield FX_FIRE_CAST;
                }
                yield FX_VOID_CAST;
            }
            default -> null;
        };
    }

    private static String resolveTravelEffect(String classId, String styleId, AbilityData ability) {
        String castType = lower(ability.getCastType());
        String travelType = lower(ability.getTravelType());
        String abilityId = lower(ability.getId());

        if ("terra".equals(lower(classId)) && "magma".equals(lower(styleId)) && "magma_sling".equals(abilityId)) {
            return FX_PROOF_MAGMA_SLING_TRAVEL;
        }
        if (travelType.contains("shadow") || travelType.contains("void")) {
            return FX_VOID_SMOKE;
        }
        if (travelType.contains("anchor")) {
            return FX_METAL_SPARKS;
        }
        if (travelType.contains("lightning") || abilityId.contains("smite") || abilityId.contains("lightning")) {
            return FX_LIGHTNING;
        }
        if (travelType.contains("wind") || travelType.contains("gale") || travelType.contains("twister") || travelType.contains("tornado")) {
            return FX_WIND_TRAVEL;
        }
        if (travelType.contains("water") || travelType.contains("surf") || travelType.contains("tidal")
                || travelType.contains("splash") || travelType.contains("mist") || travelType.contains("river")) {
            return FX_WATER_TRAVEL;
        }
        if (travelType.contains("ice") || travelType.contains("snow") || abilityId.contains("frost")
                || abilityId.contains("ice") || abilityId.contains("snow")) {
            return FX_ICE_IMPACT;
        }
        if (travelType.contains("fire") || travelType.contains("hellfire") || travelType.contains("boiling") || travelType.contains("heated")
                || abilityId.contains("magma") || abilityId.contains("lava")) {
            return FX_FIRE_SMOKE;
        }
        if (travelType.contains("sand")) {
            return FX_SAND_DUST;
        }
        if (travelType.contains("crystal") || travelType.contains("gem")) {
            return FX_CRYSTAL_SPARKS;
        }
        if (travelType.contains("metal")) {
            return FX_METAL_SPARKS;
        }
        if (travelType.contains("burrow") || travelType.contains("underground") || "dash".equals(castType) || "dash_strike".equals(castType)) {
            return "terra".equals(lower(classId)) ? FX_STONE_DUST : FX_WIND_TRAVEL;
        }

        return switch (lower(classId)) {
            case "terra" -> FX_STONE_DUST;
            case "hydro" -> FX_WATER_TRAVEL;
            case "aero" -> FX_WIND_TRAVEL;
            case "corruptus" -> FX_VOID_SMOKE;
            default -> null;
        };
    }

    private static String resolveImpactEffect(String classId, String styleId, AbilityData ability) {
        String abilityId = lower(ability.getId());
        String style = lower(styleId);

        if ("terra".equals(lower(classId)) && "quake".equals(style)
                && ("stomp".equals(abilityId) || "aftershock".equals(abilityId) || "sinkhole".equals(abilityId))) {
            return FX_TERRA_QUAKE_IMPACT;
        }
        if ("terra".equals(lower(classId)) && "magma".equals(style)) {
            if (abilityId.contains("lava") || abilityId.contains("magma")) {
                return FX_FIRE_IMPACT;
            }
            if (abilityId.contains("obsidian")) {
                return FX_PROOF_OBSIDIAN_COAT;
            }
        }
        if ("terra".equals(lower(classId)) && "gem".equals(style) && "fracture".equals(abilityId)) {
            return FX_PROOF_GEM_GREEN;
        }
        if ("terra".equals(lower(classId))) {
            if ("metal".equals(style) && "alloy_enhancement".equals(abilityId)) {
                return FX_PROOF_ALLOY_IMPACT;
            }
            if ("bloom".equals(style) && (abilityId.contains("nightshade") || abilityId.contains("cacti"))) {
                return FX_PROOF_POISON_COAT;
            }
            if ("self_petrification".equals(style) && abilityId.contains("glare")) {
                return FX_PROOF_STONE_COAT;
            }
            if ("soil".equals(style) && abilityId.contains("debris")) {
                return FX_PROOF_DEBRIS_WAVE;
            }
            if ("sand".equals(style) && (abilityId.contains("sandstorm") || abilityId.contains("dust_devil"))) {
                return FX_PROOF_SAND_CLOUD;
            }
            if ("gem".equals(style) || abilityId.contains("lapidary") || abilityId.contains("refraction") || abilityId.contains("fracture")) {
                return FX_PROOF_GEM_GREEN;
            }
        }

        return switch (lower(classId)) {
            case "terra" -> {
                if (abilityId.contains("sand")) {
                    yield FX_SAND_DUST;
                }
                if (abilityId.contains("gem") || abilityId.contains("crystal")) {
                    yield FX_CRYSTAL_SPARKS;
                }
                if (abilityId.contains("metal")) {
                    yield FX_METAL_SPARKS;
                }
                yield FX_EARTH_IMPACT;
            }
            case "hydro" -> {
                if (abilityId.contains("anchor")) {
                    yield FX_METAL_SPARKS;
                }
                if (abilityId.contains("ice") || abilityId.contains("frost") || abilityId.contains("snow")) {
                    yield FX_ICE_IMPACT;
                }
                if (abilityId.contains("rainbow") || abilityId.contains("heal")) {
                    yield FX_HEAL_LOOP;
                }
                yield FX_WATER_IMPACT;
            }
            case "aero" -> {
                if (abilityId.contains("smite") || abilityId.contains("lightning") || abilityId.contains("thunder")) {
                    yield FX_LIGHTNING_SPARKS;
                }
                if (abilityId.contains("acid") || abilityId.contains("toxic") || abilityId.contains("smog")) {
                    yield FX_ACID_SPARKS;
                }
                yield FX_WIND_IMPACT;
            }
            case "corruptus" -> {
                if (abilityId.contains("fire") || abilityId.contains("hell") || abilityId.contains("infernal") || abilityId.contains("ignite")) {
                    yield FX_FIRE_IMPACT;
                }
                if (abilityId.contains("poison") || abilityId.contains("acid") || abilityId.contains("toxic")) {
                    yield FX_POISON_IMPACT;
                }
                yield FX_VOID_IMPACT;
            }
            default -> null;
        };
    }

    private static String resolveLoopEffect(String classId, String styleId, AbilityData ability) {
        String castType = lower(ability.getCastType());
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        String style = lower(styleId);

        if ("terra".equals(lower(classId)) && "quake".equals(style)
                && ("aftershock".equals(abilityId) || "sinkhole".equals(abilityId))) {
            return FX_TERRA_QUAKE_LOOP;
        }
        if ("terra".equals(lower(classId)) && "magma".equals(style)) {
            if ("lava_pool".equals(abilityId)) {
                return FX_PROOF_LAVA_POOL_FIELD;
            }
            if (abilityId.contains("lava") || abilityId.contains("magma")) {
                return FX_FIRE_AOE;
            }
            if (abilityId.contains("obsidian")) {
                return FX_PROOF_OBSIDIAN_COAT;
            }
        }
        if ("terra".equals(lower(classId))) {
            if ("soil".equals(style) && (abilityId.contains("mudpit") || abilityId.contains("debris"))) {
                return FX_PROOF_DEBRIS_WAVE;
            }
            if ("sand".equals(style) && (abilityId.contains("sandstorm") || abilityId.contains("dust_devil"))) {
                return FX_PROOF_SAND_CLOUD;
            }
            if ("gem".equals(style) || abilityId.contains("lapidary") || abilityId.contains("refraction")) {
                return FX_PROOF_GEM_GREEN;
            }
        }

        if ("ground_zone".equals(castType) || "support_zone".equals(castType) || "barrier".equals(castType)) {
            return switch (lower(classId)) {
                case "terra" -> {
                    yield abilityId.contains("sand") ? FX_SAND_DUST : FX_STONE_DUST;
                }
                case "hydro" -> abilityId.contains("rainbow") || abilityId.contains("heal")
                        ? FX_HEAL_LOOP
                        : "snow".equals(style) || abilityId.contains("snow") || abilityId.contains("frost")
                        ? FX_SLOW_LOOP
                        : FX_WATER_TRAVEL;
                case "aero" -> abilityId.contains("smoke") || terrainEffect.contains("smog")
                        ? FX_SMOKE_END
                        : abilityId.contains("acid") || abilityId.contains("toxic")
                        ? FX_SLOW_LOOP
                        : FX_WIND_LOOP;
                case "corruptus" -> terrainEffect.contains("infernal") || abilityId.contains("fire")
                        ? FX_FIRE_AOE
                        : abilityId.contains("sanctuary")
                        ? FX_HEAL_LOOP
                        : terrainEffect.contains("shadow")
                        ? FX_VOID_SMOKE
                        : abilityId.contains("acid") || abilityId.contains("toxic")
                        ? FX_SLOW_LOOP
                        : FX_VOID_SPLASH;
                default -> null;
            };
        }

        if ("self_buff".equals(castType) || "transformation".equals(castType)) {
            return switch (lower(classId)) {
                case "terra" -> FX_EARTH_CAST;
                case "hydro" -> "snow".equals(style) || abilityId.contains("snow") || abilityId.contains("frost")
                        ? FX_ICE_IMPACT
                        : FX_WATER_CAST;
                case "aero" -> abilityId.contains("smoke") ? FX_SMOKE_END : FX_WIND_LOOP;
                case "corruptus" -> abilityId.contains("sanctuary") ? FX_HEAL_LOOP : FX_VOID_SMOKE;
                default -> null;
            };
        }

        return null;
    }

    private static String resolveModel(String classId, String styleId, AbilityData ability) {
        String summonName = lower(ability.getSummonName());
        String abilityId = lower(ability.getId());

        if ("terra".equals(lower(classId)) && "magma".equals(lower(styleId)) && "magma_sling".equals(abilityId)) {
            return MODEL_FIREBALL_PROJECTILE;
        }
        if ("terra".equals(lower(classId)) && "stone".equals(lower(styleId)) && "rubble_rouser".equals(abilityId)) {
            return MODEL_RUBBLE_PROJECTILE;
        }

        if (!summonName.isBlank()) {
            return switch (summonName) {
                case "treant_sapling" -> MODEL_ROOT_SPIRIT;
                case "snow_imp" -> MODEL_FROST_SPIRIT;
                case "frosty_golem" -> MODEL_FROST_GOLEM;
                case "swamp_monster" -> MODEL_FROG;
                case "skeleton_minion", "shadow_clone" -> MODEL_SHADOW_KNIGHT;
                case "void_spawn" -> MODEL_VOID_SPAWN;
                case "scarak_egg" -> MODEL_SCARAK;
                case "locust_queen" -> MODEL_SCARAK_BROODMOTHER;
                default -> null;
            };
        }

        if (abilityId.contains("pterodactyl")) {
            return MODEL_PTERODACTYL;
        }
        if (abilityId.contains("smoke_form")) {
            return MODEL_BAT;
        }
        if (abilityId.contains("triceratops")) {
            return MODEL_TOAD_RHINO;
        }
        if (abilityId.contains("t_rex") || abilityId.contains("trex")) {
            return MODEL_REX_CAVE;
        }
        if (abilityId.contains("primordial")) {
            return MODEL_VOID_SPAWN;
        }
        if (abilityId.contains("shadow")) {
            return MODEL_SHADOW_KNIGHT;
        }
        if ("aero".equals(lower(classId)) && (abilityId.contains("smite") || abilityId.contains("thunder"))) {
            return MODEL_THUNDER_SPIRIT;
        }
        if ("hydro".equals(lower(classId)) && (abilityId.contains("frost") || abilityId.contains("ice"))) {
            return MODEL_FROST_SPIRIT;
        }
        if ("terra".equals(lower(classId)) && lower(styleId).contains("arbor")) {
            return MODEL_ROOT_SPIRIT;
        }
        if ("corruptus".equals(lower(classId)) && abilityId.contains("void")) {
            return MODEL_VOID_EYE;
        }
        if ("corruptus".equals(lower(classId)) && abilityId.contains("hell")) {
            return MODEL_FIRE_GOLEM;
        }
        if ("aero".equals(lower(classId)) && (abilityId.contains("gust") || abilityId.contains("tempest"))) {
            return MODEL_SPARK_LIVING;
        }
        return null;
    }

    private static String extractModelId(String assetPath) {
        if (assetPath == null || assetPath.isBlank()) {
            return null;
        }

        String normalized = assetPath.replace('\\', '/');
        String[] segments = normalized.split("/");
        if (segments.length >= 3 && "Models".equalsIgnoreCase(segments[segments.length - 2])) {
            return segments[segments.length - 3];
        }
        String fileName = segments[segments.length - 1];
        return fileName.replaceFirst("\\.[^.]+$", "");
    }

    private static void append(List<String> parts, String label, String assetPath) {
        if (assetPath != null && !assetPath.isBlank()) {
            parts.add(label + prettyAssetName(assetPath));
        }
    }

    private static String prettyAssetName(String assetPath) {
        String normalized = assetPath.replace('\\', '/');
        String[] segments = normalized.split("/");
        String fileName = segments[segments.length - 1];
        String base = fileName.replaceFirst("\\.[^.]+$", "");
        if ("Model".equalsIgnoreCase(base) && segments.length >= 3) {
            return humanize(segments[segments.length - 3]);
        }
        return humanize(base);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean isFlightTransformation(String abilityId, String castType) {
        if (!"transformation".equals(castType)) {
            return false;
        }
        return abilityId.contains("pterodactyl")
                || abilityId.contains("flight")
                || abilityId.contains("smoke_form");
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String humanize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        String[] parts = rawValue.trim().replace('-', '_').split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }
}
