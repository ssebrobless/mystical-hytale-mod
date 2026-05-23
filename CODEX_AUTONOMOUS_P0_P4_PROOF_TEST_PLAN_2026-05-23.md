# Autonomous P0-P4 Proof Test Plan - 2026-05-23

## Purpose

This plan turns the P0-P4 research into an autonomous test loop that can run in
Hytale without pretending that a screenshot or log is stronger than it is.

The proof goal is not "Terra is done." The proof goal is:

```text
Can this Hytale build safely express the primitive?
  +-- yes: use it in Terra abilities
  +-- no: document fallback before ability implementation
```

## Test Shape

```text
world entry
+-- screenshot before typing
+-- detect death/void/menu risk
+-- recover or stop before sending chat commands

camera setup
+-- press V for third person
+-- screenshot proof of third-person setup

arena setup
+-- verify flatlands
+-- move out of spawn/portal zone into a clear lane
+-- count tracked/nearby test mobs
+-- reuse tracked mobs when safe
+-- clear only when too many are nearby
+-- spawn one stationary dummy for target-dependent proofs

proof loop
+-- run /motm dev proof <proofId>
+-- perform required action if movement-based
+-- capture screenshot
+-- parse server log for PASS/FAIL and residuals
+-- write report.md + server.log + screen evidence
```

## User Concerns Covered

| Concern | Harness response |
| --- | --- |
| Do not assume world entry is normal | `scripts/check-world-entry-state.ps1` captures first and flags dark/death/void risk. |
| Do not type while dead/in void | `scripts/run-runtime-proofs.ps1` captures `entry-before-typing.png` before commands and records the classifier report. |
| Confirm third person | Runner presses `V`, captures `third-person-confirmation.png`, and records it before visual proofs. |
| Do not overspawn enemies | Runner sends `/motm dev test mobs count` before target-dependent proofs and reuses targets when safe. |
| Targets run away | New `/motm dev test mobs stationary` spawns only one `Test_Dummy_Stationary` grounded target. |
| Test the correct action | Proof IDs are split by primitive; movement proofs capture before/after screenshots and log start/destination. |
| Avoid world grief | Temporary selections track the prior `BlockSelection` and restore after the proof lifetime. |

## Commands Added

```text
/motm dev test mobs stationary
  +-- clears previously tracked test mobs
  +-- spawns one grounded Test_Dummy_Stationary
  +-- avoids the floating Bat used by older broad ability audits

/motm dev proof <proofId>
  +-- queues proof work onto the server tick
  +-- writes a single [MOTM] Proof <proofId> PASS|FAIL log line
```

## Proof IDs

```text
P0 coating
  +-- coating-metal
  +-- coating-obsidian
  +-- coating-stone
  +-- coating-poison-target

P1 temporary block
  +-- tempblock-metal-wall
  +-- tempblock-stone-pillar
  +-- tempblock-flower
  +-- tempblock-sapling

P2 temporary fluid
  +-- tempfluid-lava-ring
  +-- tempfluid-water-field

P3 model/proxy
  +-- proxy-magma-blob
  +-- proxy-cactus-projectile
  +-- proxy-gem
  +-- proxy-glass-shards

P4 movement safety
  +-- movement-burrow
  +-- movement-tunnel
  +-- movement-dust-devil
```

## Runner

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-runtime-proofs.ps1
```

Optional cold launch:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-runtime-proofs.ps1 -ColdLaunch
```

Output:

```text
audits/proofs/p0-p4/<timestamp>/
  +-- report.md
  +-- server.log
  +-- entry-before-typing.png
  +-- third-person-confirmation.png
  +-- <proof-id>.png
  +-- <movement-proof>-before.png
```

## Acceptance

```text
PASS per proof
  +-- /motm dev proof <proofId> logs PASS
  +-- screenshot exists
  +-- no missing asset, nonexistent role, classload, exception, or crash line
  +-- temporary block/fluid cleanup log appears after lifetime

FAIL per proof
  +-- no proof result line
  +-- result line logs FAIL
  +-- residual scan finds a blocking runtime error
  +-- screenshot shows wrong state on manual review
```

## After This Pass

```text
If P0 passes
  +-- implement Metal Coat, Obsidian/Gargoyle/Glare coatings with confidence

If P1 passes
  +-- implement Iron Wall, Pillar Strike, flowers, saplings with real blocks

If P2 passes
  +-- implement Lava Pool/Mudpit as real fluid fields plus style overlays

If P3 passes
  +-- implement Magma Sling, Cacti Cluster, Lapidary, Vitrification proxies

If P4 passes
  +-- implement Burrow, Tunnel safety, Dust Devil, Rockslide movement
```

Any failed proof becomes a concrete residual and fallback decision before Terra
ability implementation continues.
