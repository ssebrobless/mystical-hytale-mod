package com.motm.proof;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MotmProofCatalog {

    private static final List<ProofDefinition> DEFINITIONS = List.of(
            new ProofDefinition("coating-metal", ProofKind.EFFECT, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("coating-obsidian", ProofKind.EFFECT, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("coating-stone", ProofKind.EFFECT, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("coating-poison-target", ProofKind.EFFECT, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("tempblock-metal-wall", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempblock-stone-pillar", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempblock-flower", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempblock-sapling", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempblock-gem-cluster", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempblock-cactus", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempblock-roots", ProofKind.TEMPORARY_BLOCK, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempfluid-lava-ring", ProofKind.TEMPORARY_FLUID, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempfluid-water-field", ProofKind.TEMPORARY_FLUID, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("tempfluid-mud-field", ProofKind.TEMPORARY_FLUID, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("proxy-magma-blob", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("proxy-cactus-projectile", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("proxy-gem", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("proxy-gem-aura", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("proxy-glass-shards", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("proxy-sand-cloud", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("proxy-debris-wave", ProofKind.PROXY, EnumSet.of(EvidenceStream.CLIENT_INTENT)),
            new ProofDefinition("movement-burrow", ProofKind.MOVEMENT, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("movement-tunnel", ProofKind.MOVEMENT, EnumSet.of(EvidenceStream.SERVER_TRUTH)),
            new ProofDefinition("movement-dust-devil", ProofKind.MOVEMENT, EnumSet.of(EvidenceStream.SERVER_TRUTH))
    );
    private static final List<String> PROOF_IDS =
            DEFINITIONS.stream().map(ProofDefinition::id).toList();

    private MotmProofCatalog() {
    }

    public static List<String> ids() {
        return PROOF_IDS;
    }

    public static List<ProofDefinition> definitions() {
        return DEFINITIONS;
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

    public record ProofDefinition(
            String id,
            ProofKind kind,
            Set<EvidenceStream> evidenceStreams
    ) {
    }

    public enum ProofKind {
        EFFECT,
        TEMPORARY_BLOCK,
        TEMPORARY_FLUID,
        PROXY,
        MOVEMENT
    }

    public enum EvidenceStream {
        SERVER_TRUTH,
        CLIENT_INTENT,
        CAUSALITY
    }
}
