# Terra Non-Ready Runtime Kinks - 2026-05-23

Scope: harden the Terra styles that were still partial after the concept review, without claiming full style completion until their real abilities are wired and visually audited in-game.

Hard rules still apply: protected JSON files are surgical-edit only, no invented SystemIds, no undocumented movement APIs, and every reusable primitive must be proven in a cold in-world audit before promotion.

## Readiness Shape

```text
╔══════════════════════╦════════════════════════════╦════════════════════════════╦════════════╗
║ Style                ║ User-facing visual target  ║ Runtime primitive to prove ║ Status     ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╬════════════╣
║ Metal                ║ 2x2 real metal barricade   ║ P1 temp blocks             ║ PROVEN     ║
║ Stone                ║ real stone pillar + coating║ P0 coating, P1 blocks      ║ PROVEN     ║
║ Arbor                ║ roots/vines, sapling block ║ P1 roots/sapling/flowers   ║ PROVEN     ║
║ Bloom                ║ flowers + cactus projectile║ P1 flowers/cactus, P3 tint ║ PROVEN     ║
║ Self Petrification   ║ tight stone coat + tunnel  ║ P0 coating, P4 movement    ║ PROVEN     ║
║ Soil                 ║ burrow, debris, mud field  ║ P2 water, P3 debris, P4    ║ PROVEN     ║
║ Sand                 ║ beige cloud + dash combo   ║ P3 cloud, P4 movement      ║ PROVEN     ║
║ Gem                  ║ floating green gem object  ║ P1 crystal, P3 green aura  ║ PROVEN     ║
╚══════════════════════╩════════════════════════════╩════════════════════════════╩════════════╝
```

## Local Asset Findings

```text
Terra physical assets
├── Gem
│   ├── Rock_Crystal_Green_Block / Large / Medium / Small
│   ├── Plant_Bush_Crystal / Plant_Leaves_Crystal / Plant_Sapling_Crystal
│   └── Crystal particle routes exist in HytaleAssetResolver
├── Bloom
│   ├── Plant_Flower_Common_* and Plant_Sapling_Oak already place
│   └── Plant_Cactus_1 / Prototype_Cactus_* / Plant_Cactus_Ball_1
├── Arbor
│   └── Plant_Roots_Leafy / Plant_Roots_Cave / Plant_Vine_Thick_Roots
├── Soil + Sand
│   ├── Water and Lava fluids are proven; Mud appears as block/particle assets, not a proven Fluid id
│   ├── Mud must initially be water-field mechanics + brown/debris visuals
│   └── Sand/debris clouds use verified MOTM EntityEffect SystemIds first
└── Movement
    ├── Burrow = short forward movement proof
    ├── Tunnel = surface-recovery movement proof
    └── Dust Devil = dash movement proof
```

## New Proof Set

Run with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-terra-readiness-proofs.ps1 -ColdLaunch
```

Latest PASS audit:

```text
audits/proofs/terra-readiness/2026-05-23T00-06-59/report.md
```

Proofs:

```text
P1 physical blocks
├── tempblock-gem-cluster   -> floating 2x2 green crystal/gem candidate
├── tempblock-cactus        -> cactus object candidate for Cacti Cluster
└── tempblock-roots         -> root/vine ground candidate for Arbor

P2 fluids
└── tempfluid-mud-field     -> water-field foundation for Mud Pit, radius 3

P3 visual proxies
├── proxy-gem-aura          -> green aura/tint for Lapidary/Refraction
├── proxy-sand-cloud        -> beige cloud using verified MOTM earth particles
└── proxy-debris-wave       -> brown debris cloud using verified MOTM earth particles

P4 movement
├── movement-burrow
├── movement-tunnel
└── movement-dust-devil
```

## Promotion Rule

A Terra partial style can move from KINKED to IMPLEMENTABLE only when its primitive row above has a PASS audit path. Full READY still requires the actual style abilities to be run through the concept-aware style test with screenshots and server-log proof.

Residuals after this pass:

```text
Mud Pit       ▶ water-field placement is proven; true brown-tinted water is not proven. Implement as water + brown proxy particles unless the API exposes tintable fluid.
Sand/Debris   ▶ beige/brown cloud route is proven with verified MOTM earth particles. Sand-specific EntityEffect SystemId still needs /showcase proof before substitution.
Gem Lapidary  ▶ green crystal block cluster and green aura are proven. HP bar/recall/control is not part of this primitive proof.
Tunnel        ▶ surface-recovery movement is proven. Long-form underground traversal still needs a dedicated safety pass after the style mechanic is wired.
Startup logs  ▶ current cold launches still emit unrelated Hytale Store-is-shutdown lines during world load and an existing Corruptus brood_surge data error; neither occurred during Terra proof execution.
```
