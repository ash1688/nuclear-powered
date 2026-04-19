# Nuclear Powered

A nuclear power mod for **Minecraft 1.20.1 (NeoForge)**, grounded in real-world reactor history. Players progress from primitive graphite piles through commercial reactors to endgame Super Massive Reactors via a realistic fuel cycle, isotope economy, and research system.

> **Status:** Pre-alpha. Design phase. No playable build yet.

## Design Philosophy

- **Real reactor types drive the tech tree.** Piles → Magnox/RBMK → PWR/BWR/AGR/CANDU → fast breeders and MSRs → endgame SMR.
- **Byproduct economy closes the tree.** Reprocessing output from lower tiers is required to build the next tier. No skipping.
- **Everything eventually decays to lead.** Stockpiling radioactives is not a strategy; the fuel cycle has to keep running.
- **Simple operations, rich consequences.** Three reactor stats (heat, fuel, coolant); a three-stage emergency ladder; contained failures.
- **Reactor identity through mechanics, not stats.** An RBMK differs from a PWR because it *behaves* differently, not because its numbers are bigger.
- **Steam-age accessible.** The tier 1 pile is buildable and operable without pre-existing high-voltage infrastructure.

### What this mod is not

- Not a physics simulator (no xenon poisoning, no neutron transport).
- Not a griefing mod (meltdowns are contained — no world damage, no persistent radiation zones).
- Not a weapons mod (weapons-grade enrichment is deliberately out of scope).

## Tier Progression

| Tier | Era | Reactors | Fuel |
|---|---|---|---|
| 1 | Pioneer | Graphite pile (air / water cooled) | Natural U |
| 2 | First Generation | Magnox, RBMK | Natural U / very low LEU |
| 3 | Commercial | PWR, BWR, AGR, CANDU | 3–5% LEU (CANDU: natural U + D₂O) |
| 4 | Advanced | Fast breeder, MSR, LFTR | Pu/MOX, Thorium, HALEU |
| 5 | Super Massive | SMR (Super Massive Reactor) | HALEU / multi-fuel |

## Release Roadmap

| Release | Scope |
|---|---|
| **R1 — Foundations** (MVP) | Complete tier 1 fuel cycle, steam-age power conversion, basic reprocessing, milestone-based research. Self-contained and genuinely playable on its own. |
| **R2 — Commercial** | Tiers 2 & 3, tiered enrichment, intermediate reprocessing, full research system, reactor commitment mechanic, blueprint & tiered deployer system. |
| **R3 — Advanced** | Tier 4 (fast breeder, MSR, LFTR), advanced reprocessing, Am/Cm separation, thorium chain. |
| **R4 — Super Massive** | Tier 5 Super Massive Reactor, full THORP, Pu-238 production, endgame polish. |

## Target Environments

- **Primary:** SkyGreg (GTCEu-based skyblock).
- **Also supported:** ATM / FTB-style kitchen-sink packs, and standalone play.
- **Compatibility:** Tag-based material definitions for graceful interop with GregTech, Mekanism, Create, and others.

## Design Document

The full design lives in [NuclearPowered_DesignDoc_v3.md](NuclearPowered_DesignDoc_v3.md) — reactor mechanics, PUREX processing line, power conversion chain, research system, blueprint/deployer system, R1 success criteria, and more. The original `.docx` source is also in the repo.

## License

See [LICENSE](LICENSE).
