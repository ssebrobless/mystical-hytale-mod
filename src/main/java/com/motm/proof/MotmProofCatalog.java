package com.motm.proof;

import java.util.List;
import java.util.Locale;

public final class MotmProofCatalog {

    private static final List<String> PROOF_IDS = List.of(
            "coating-metal",
            "coating-obsidian",
            "coating-stone",
            "coating-poison-target",
            "tempblock-metal-wall",
            "tempblock-stone-pillar",
            "tempblock-flower",
            "tempblock-sapling",
            "tempblock-gem-cluster",
            "tempblock-cactus",
            "tempblock-roots",
            "tempfluid-lava-ring",
            "tempfluid-water-field",
            "tempfluid-mud-field",
            "proxy-magma-blob",
            "proxy-cactus-projectile",
            "proxy-gem",
            "proxy-gem-aura",
            "proxy-glass-shards",
            "proxy-sand-cloud",
            "proxy-debris-wave",
            "movement-burrow",
            "movement-tunnel",
            "movement-dust-devil"
    );

    private MotmProofCatalog() {
    }

    public static List<String> ids() {
        return PROOF_IDS;
    }

    public static String normalize(String proofId) {
        return proofId == null ? "" : proofId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isKnown(String proofId) {
        return PROOF_IDS.contains(normalize(proofId));
    }

    public static String usage() {
        return "[MOTM] Usage: /motm dev proof <proofId>\n"
                + "Proofs: " + String.join(", ", PROOF_IDS);
    }
}
