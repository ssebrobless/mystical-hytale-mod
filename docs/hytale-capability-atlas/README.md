# Hytale Capability Atlas

Updated: 2026-05-24

This atlas is the working bridge between MOTM ability concepts and the current
Hytale build installed on this machine. Use it before implementing or approving
any class/style/ability visuals or mechanics.

```
╔════════════════════════════════════════════════════════════════════╗
║                    MOTM Implementation Truth Stack                ║
╠══════════════════════╦════════════════════════════╦═══════════════╣
║ Layer                ║ Use For                    ║ Trust         ║
╠══════════════════════╬════════════════════════════╬═══════════════╣
║ User concept notes   ║ What the ability must feel ║ Design truth  ║
║ Style JSON/plans     ║ Current data contract      ║ Repo truth    ║
║ Official docs/blogs  ║ Intended Hytale model      ║ Directional   ║
║ Community docs       ║ API examples and gaps      ║ Verify local  ║
║ Local Assets.zip     ║ Actual available assets    ║ Authoritative ║
║ Local server jar     ║ Actual class/method names  ║ Authoritative ║
║ Runtime proofs/logs  ║ What really works in game  ║ Final gate    ║
╚══════════════════════╩════════════════════════════╩═══════════════╝
```

The important conclusion from the research is not "Hytale cannot do this." It
is that Hytale Early Access requires a stricter workflow:

```
Concept
  └─▶ choose Hytale primitive
       └─▶ verify asset id in Assets.zip
            └─▶ verify API/method in local HytaleServer.jar
                 └─▶ run tiny runtime proof
                      └─▶ implement ability
                           └─▶ log structured evidence
                                └─▶ user visual PASS
```

## Current Local Evidence

Fresh probes were run on 2026-05-24:

- `audits/hytale-asset-library/2026-05-24-capability-atlas/report.md`
- `audits/hytale-runtime-capabilities/2026-05-24-capability-atlas/report.md`
- `audits/harness/assets/2026-05-24-capability-atlas/report.md`
- `scripts/audit-no-resource.ps1` verifies the current no-resource casting model
  across all 40 styles and 120 abilities.

Key counts from the installed game package:

| Surface | Count |
| --- | ---: |
| `Assets.zip` entries | 59,518 |
| `HytaleServer.jar` entries | 38,672 |
| Particles | 2,320 |
| Models | 2,815 |
| Animations | 6,717 |
| Block models | 1,151 |
| Block/item JSON | 4,101 |
| Prefabs | 7,823 |
| Entity effects | 140 |
| UI files | 100 |
| Relevant API class hits | 521-823 depending probe scope |

## Files

- `../ABILITY_REFERENCE.md` is the GitHub-facing reference for every class,
  style, and active ability. It reflects the current no-resource casting model.
- `source-index.md` records useful public and local sources.
- `proven-primitives.md` records what Hytale surfaces we can use, and how safe
  each is.
- `ability-translation-rules.md` turns concepts into implementation choices.
- `research-gates.md` lists the next concrete proof/research tasks before
  continuing wide ability implementation.
- `terra-class-implementation-readiness.md` maps every Terra style/ability to
  the Hytale primitive it should use, the remaining proof gates, and the
  resource-free design choices that still need user confirmation.
- `terra-visual-functional-success-map.md` defines what visual and functional
  success means for every Terra ability, including the ideal primitive, proof
  gate, and acceptance evidence.
- `terra-30-ability-implementation-map.md` expands all 30 Terra abilities into
  concrete implementation routes, fallback choices, proof gates, and shared
  dependencies.
- `terra-30-ability-full-scope-cross-audit.md` cross-checks those routes against
  user concept decisions, current Terra data, local Hytale capability research,
  and known in-game failure modes.
- `terra-30-ability-feasibility-risk-register.md` records per-ability feasibility,
  expected bugs, complications, proof gates, and plan adjustments before final
  Terra implementation.
