package com.motm.model;

/**
 * Resolved Hytale asset bindings for a style ability.
 *
 * These are presentation-only references for the future cast/runtime layer:
 * animation, cast VFX, travel VFX, impact VFX, loop VFX, and optional proxy
 * models for summons or transformations.
 */
public class AbilityActionAssets {

    private final String animationAsset;
    private final String castEffectAsset;
    private final String travelEffectAsset;
    private final String impactEffectAsset;
    private final String loopEffectAsset;
    private final String modelAsset;

    public AbilityActionAssets(String animationAsset,
                               String castEffectAsset,
                               String travelEffectAsset,
                               String impactEffectAsset,
                               String loopEffectAsset,
                               String modelAsset) {
        this.animationAsset = animationAsset;
        this.castEffectAsset = castEffectAsset;
        this.travelEffectAsset = travelEffectAsset;
        this.impactEffectAsset = impactEffectAsset;
        this.loopEffectAsset = loopEffectAsset;
        this.modelAsset = modelAsset;
    }

    public static AbilityActionAssets empty() {
        return new AbilityActionAssets(null, null, null, null, null, null);
    }

    public String getAnimationAsset() {
        return animationAsset;
    }

    public String getCastEffectAsset() {
        return castEffectAsset;
    }

    public String getTravelEffectAsset() {
        return travelEffectAsset;
    }

    public String getImpactEffectAsset() {
        return impactEffectAsset;
    }

    public String getLoopEffectAsset() {
        return loopEffectAsset;
    }

    public String getModelAsset() {
        return modelAsset;
    }
}
