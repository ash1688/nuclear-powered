# Nuclear Powered

A nuclear power mod for **Minecraft 1.20.1 (Forge)**, grounded in real-world reactor history. Players progress from primitive graphite piles through commercial reactors to endgame Super Massive Reactors via a realistic fuel cycle, isotope economy, and research system.

> **Status:** R1 (Foundations) — playable alpha. Full tier 1 fuel cycle implemented end-to-end. Placeholder art on a few items; balance and polish still in flight.

## What's in R1

The full Tier 1 loop from ore to reprocessed fuel, plus steam-age power conversion:

- **Mining & processing.** Uranium and thorium ores (overworld / deepslate / nether / end). Crush → wash → smelt pipeline through the Crusher, Ore Washer, and Electric Furnace. Every processing machine consumes FE.
- **Graphite Pile.** The tier 1 reactor. A 3×3×3 casing-shell multiblock with a pile at the core. Burns uranium fuel rods, produces heat. Heat climbs on a thermal-throttle curve to a sweet spot near 3000 °C, then the burn rate slows — pile won't melt down, but a hot pile is a slow pile. Add graphite casings to raise max heat.
- **Heater.** Optional startup assist block — right-click to toggle. Pumps heat into an adjacent pile up to 2500 °C then tapers off.
- **Power conversion.** Two paths:
  - **Thermocouples** attached to pile casings — direct heat → FE. More thermos = more power and more cooling; a thermo sitting full of FE does nothing, so you need downstream FE demand (batteries, machines) to keep the pile cooling.
  - **Coal Boiler → Steam Engine** — pre-reactor bootstrap path; coal burns water into steam, steam drives an engine that outputs FE.
- **FE transport.** Energy Cables form a network — BFS-based conduit model, no per-cable buffer, no oscillation. Steam pipes do the same for fluids. Batteries buffer surpluses.
- **Fuel fabrication.** 3 uranium ingots + 4 iron → 1 uranium fuel rod via the Fuel Fabricator.
- **Reprocessing (PUREX chain).** Five machines in sequence, each consuming FE:
  1. **Shearer** — depleted rod → chopped fuel + cladding scrap (50 FE/tick)
  2. **Dissolver** — chopped fuel + nitric acid → dissolved fuel + reactor sludge (150 FE/tick)
  3. **Extraction Column** — dissolved fuel + extraction solvent → Pu-239 + reclaimed U + fission product stream (250 FE/tick)
  4. **Cs Column** — fission product stream + ion exchange resin → Cs-137 + residual waste (350 FE/tick)
  5. **Vitrifier** — residual waste + glass frit → vitrified waste (400 FE/tick)

  The chain pulls **1200 FE/tick combined**, above a single battery's 1024 FE/tick output — so sustained throughput needs two piles or multiple batteries on the network. That's the deliberate tier 1 "power hungry" design.
- **Closed fuel cycle.** Reclaimed uranium smelts back into an ingot. The **Cladding Recycler** compacts 9 cladding scraps into reusable fuel rod cladding, which the Fuel Fabricator accepts instead of 4 fresh iron. Run ~3 rods of reprocessing and you've recovered enough material for a new rod.
- **Progression tracking.** 12 vanilla advancements covering the tier 1 spine (Nuclear Dawn → Criticality → Self-Sustaining). A bundled FTB Quests chapter at `data/ftbquests/quests/chapters/tier1.snbt` mirrors the same tree for modpacks using FTB Quests.
- **Tier 2 stockpile.** Pu-239 and Cs-137 are collected but not yet consumed — they're the gate materials for R2 reactors (MOX fuel, startup irradiation).

## Tier 1 Walkthrough

1. Mine uranium ore. Crush → wash → smelt a few yellowcake into uranium ingots.
2. Craft the Fuel Fabricator (electric furnace-style) and fabricate your first fuel rod.
3. Build a Graphite Pile in the centre of a 3×3×3 shell of graphite casing. The pile auto-detects the structure; glowing particles = valid shell.
4. Drop a rod in the pile. It'll start burning. Toggle on a Heater next to it to skip the cold-start phase.
5. Surround the pile with Thermocouples. Connect them to Energy Cables and a Battery. Now you have FE.
6. Set up the processing line (Crusher, Washer, Electric Furnace, Fuel Fabricator) — these run off your FE budget.
7. When a fuel rod depletes, auto-output sends it along pipes/hoppers to the Shearer. Follow the PUREX chain: Shearer → Dissolver → Extraction Column → Cs Column → Vitrifier.
8. Reclaimed uranium smelts back to ingots. Cladding scrap (×9) compacts into recycled cladding. Feed both back into the Fabricator — loop closed.
9. Stockpile Pu-239 and Cs-137. R2 needs them.

## Design Philosophy

- **Real reactor types drive the tech tree.** Piles → Magnox/RBMK → PWR/BWR/AGR/CANDU → fast breeders and MSRs → endgame SMR.
- **Byproduct economy closes the tree.** Reprocessing output from each tier gates the next. No skipping.
- **Everything eventually decays to lead.** Stockpiling radioactives is not a strategy; the cycle has to keep running.
- **Simple operations, rich consequences.** Three reactor stats (heat, fuel, coolant); thermal throttling replaces explosions; failures stay contained.
- **Reactor identity through mechanics, not stats.** An RBMK behaves differently from a PWR, not just has bigger numbers.
- **Steam-age accessible.** The tier 1 pile is buildable and operable without pre-existing high-voltage infrastructure.

### What this mod is not

- Not a physics simulator (no xenon poisoning, no neutron transport).
- Not a griefing mod (meltdowns are contained — no world damage, no persistent radiation zones).
- Not a weapons mod (weapons-grade enrichment is deliberately out of scope).

## Tier Progression

| Tier | Era | Reactors | Fuel | Release |
|---|---|---|---|---|
| 1 | Pioneer | Graphite pile (air / water cooled) | Natural U | **R1 (shipped)** |
| 2 | First Generation | Magnox, RBMK | Natural U / very low LEU | R2 |
| 3 | Commercial | PWR, BWR, AGR, CANDU | 3–5% LEU (CANDU: natural U + D₂O) | R2 |
| 4 | Advanced | Fast breeder, MSR, LFTR | Pu/MOX, thorium, HALEU | R3 |
| 5 | Super Massive | SMR (Super Massive Reactor) | HALEU / multi-fuel | R4 |

## Release Roadmap

| Release | Scope | Status |
|---|---|---|
| **R1 — Foundations** | Tier 1 pile, processing, reprocessing (PUREX), steam-age power, milestone research, closed fuel cycle. | Alpha — playable end-to-end. |
| **R2 — Commercial** | Tiers 2 & 3, tiered enrichment, intermediate reprocessing, full research-point system, reactor commitment mechanic, blueprint & tiered deployer. | Planned. |
| **R3 — Advanced** | Tier 4 reactors, advanced reprocessing (DIAMEX / SANEX), Am/Cm separation, thorium chain. | Planned. |
| **R4 — Super Massive** | Tier 5 SMR, full THORP, Pu-238 production, endgame polish. | Planned. |

## Installing

Requires Forge 47.1.3+ for Minecraft 1.20.1.

```bash
git clone https://github.com/ash1688/nuclear-powered
cd nuclear-powered
./gradlew build
# JAR lands in build/libs/
```

Drop the JAR in your Minecraft instance's `mods/` folder. JEI (optional) is auto-detected and registers recipe categories for Crushing, Washing, and Fabricating. FTB Quests (optional) auto-loads the shipped Tier 1 quest chapter.

## Target Environments

- **Primary:** SkyGreg (GTCEu-based skyblock).
- **Also supported:** ATM / FTB-style kitchen-sink packs, and standalone play.
- **Compatibility:** Tag-based material definitions where possible, for graceful interop with GregTech, Mekanism, Create, and others.

## Known Gaps in R1

- Some R1 items still use vanilla placeholder textures (cladding_scrap, dissolved_fuel, reactor_sludge, fission_product_stream, residual_waste, reclaimed_uranium, plutonium_239, vitrified_waste, fuel_rod_cladding, fuel_rod_cladding from the recycler — these are queued for custom art).
- Cladding Recycler block still uses the anvil placeholder texture.
- Reprocessing machines share placeholder GUIs borrowed from the crusher/washer panels.
- Pu-239 and Cs-137 have no in-game use yet in R1; they wait for R2.
- All block models are `cube_all` (same iso-render texture on all 6 faces). Proper per-face textures require splitting the iso renders — not yet done.

## Design Document

The full design lives in [NuclearPowered_DesignDoc_v3.md](NuclearPowered_DesignDoc_v3.md) — reactor mechanics, PUREX processing line, power conversion chain, research system, blueprint/deployer system, R1 success criteria, and more. The original `.docx` source is also in the repo.

## License

See [LICENSE](LICENSE).
