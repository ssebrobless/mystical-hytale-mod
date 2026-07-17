# Capability Gate Results - 2026-07-16 (live-verified)

Executed in-world ("MOTM Creative Test", Hytale 0.5.6, build with proof runners
`gate-r6-*`..`gate-r11-*`, MotmProofCatalog CAPABILITY_GATE kind). Author confirmed each
visual live; observability lines in the run's JSONL evidence.

| Gate | Capability | Verdict | Consequence |
|---|---|---|---|
| R6 | `ParticleUtil.spawnParticleEffect(String, Vector3d, ComponentAccessor)` world-space burst, no entity | **PASS** | Proxy-free tethers/field pulses/impact accents GO: U4 particle-chain renderer primary, A3 chain hops, A7 trails, T3 vines, C9 life_drain, E-P6 field simplification |
| R7 | `EntityScaleComponent(float)` scale up/down on NPC proxy, visible | **PASS** | A5 pressure_burst growth, sinkhole shrink, summon sizing GO |
| R8 | Pack-shipped custom `.particlesystem` (`MOTM_Proof_Pink_Halo` + `_Ring` spawner, Ring_Green texture) resolves + renders | **PASS** | Custom particle authoring GO: G1 pink halo, G4 projectile blue fire, C8 switch flash, H6 rainbow arc (if needed), the ~160-effect per-style program |
| R9 | `PersistentDynamicLight(ColorLight)` on renderless proxy | **FAIL** (not perceptible in test conditions, 2 runs) | Use the PROVEN fullbright light-BLOCK trick (`Build/Dev_Lightsource_*`, BlockType Light{Color}) for all glow use cases (T9 refraction, field glows). Do not schedule dynamic-light work. |
| R10 | `Intangible` component: player passes through proxy | **PASS** | G5 smoke_form move-through-enemies GO (A6). Note: proof proxy retains idle walk animation - expected. |
| R11 | Player-model cloning onto NPC (`setAppearance` with player's model asset id / ModelComponent copy) | **PARTIAL** | Base Human model clones (NPC appeared as default unclothed character); player customization/gear does NOT transfer (client-side). shadow_step clone design UPGRADED from Mannequin to dark-tinted default-human silhouette + void smoke (supersedes the G11 interim; a true appearance clone is out of reach and no longer pursued). |

Confidence census after gates: **every proposal in
`docs/improvement-proposals-2026-07-16.md` now rests on a live-proven or
vanilla/mod-precedented mechanism.** Zero speculative items remain.
