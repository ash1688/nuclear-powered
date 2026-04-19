# Nuclear Powered

_Minecraft Mod — Design Document v3_
_Real-World Reactor Progression · Steam Age to Endgame_
_Target: Minecraft 1.20.1 (NeoForge primary)_
_v3 · Blueprints & Tiered Deployers_

# 1. Overview & Design Philosophy

Nuclear Powered is a nuclear power mod for Minecraft 1.20.1, grounded in real-world reactor history. Players progress from primitive graphite piles through commercial reactors to endgame Super Massive Reactors (SMRs) via a realistic fuel cycle, isotope economy, and research system. The mod prioritises accessibility (NuclearCraft-level simplicity, not GregTech-level simulation) while drawing heavily on real nuclear engineering for flavour, progression logic, and reactor identity.

## Mod Identity

- Name: Nuclear Powered
- Target MC version: 1.20.1 (with NeoForge as primary loader; Forge compat optional)
- Primary target pack: SkyGreg (GTCEu-based skyblock), with tag-based compatibility for ATM/FTB-style packs and standalone play
- Release strategy: Incremental — ship a complete, self-sustaining Tier 1 first, expand via updates

## Core Design Principles

- Real-world reactor types drive the tech tree. Each tier represents a genuine historical stage: piles, Magnox/RBMK, commercial PWR/BWR/AGR/CANDU, fast breeders and molten salt reactors, then the mod's endgame Super Massive Reactors.
- Byproduct economy closes the tech tree. Reprocessing output from lower-tier reactors provides the materials to build higher-tier reactors. No tier can be skipped.
- Everything eventually decays to lead. Radioactive materials in storage slowly become inert over time, creating ongoing pressure to keep the fuel cycle running rather than hoarding.
- Simple operations, rich consequences. Three core reactor stats — heat, fuel, coolant — with a clear three-stage emergency ladder. Failure is dramatic but contained.
- Reactor identity through mechanics, not stats. RBMKs differ from PWRs because they behave differently (positive void coefficient, graphite fire on meltdown), not because their numbers are tuned higher.
- Self-contained but compatible. The mod provides its own complete chain from ore to reactor. Tag-based material definitions allow graceful interop with GregTech, Mekanism, Create, and others.
- Steam-age accessible. The tier 1 pile can be built, operated, and used for power generation without requiring pre-existing high-voltage electrical infrastructure.

## What This Mod Is Not

- Not a physics simulator. No xenon poisoning, no thermal-hydraulic modelling, no neutron transport equations.
- Not a griefing mod. Meltdowns are contained — no world damage, no persistent radiation zones.
- Not a weapons mod. Weapons-grade enrichment (>20%) is deliberately out of scope.

| *Creative license note: in this mod, SMR stands for Super Massive Reactor — a knowing riff on the real-world acronym (Small Modular Reactor). The mod's SMR multiblock is the largest in the tech tree at 13×13×13 minimum, and represents the endgame rather than a miniaturisation.* |
|---|

# 2. Release Strategy & Scope

The mod will be developed and released incrementally. Each release milestone is independently playable and valuable — a player starting with Release 1 can fully enjoy Tier 1 nuclear power without ever needing the later releases.

## Release Milestones

| **Release** | **Scope** | **Player Experience** | **Est. Effort** |
|---|---|---|---|
| R1 — Foundations | Tier 1 complete: pile, basic processing, basic reprocessing, steam-age power conversion, self-sustaining fuel cycle, first research | First nuclear reactor, replaces coal power, begins isotope stockpile | Weeks to months of part-time work |
| R2 — Commercial | Tier 2 & 3 reactors (Magnox, RBMK, PWR, BWR, AGR, CANDU), tiered enrichment, intermediate reprocessing, full research system, reactor commitment mechanic, blueprint & tiered deployer system | Grown-up reactor choices, branching tech, diverse base, scalable reactor fleet | 6–12 months part-time |
| R3 — Advanced | Tier 4 (fast breeder, MSR, LFTR), advanced reprocessing, Am/Cm separation, thorium chain | Closed fuel cycle, exotic reactors | Several months after R2 |
| R4 — Super Massive | Tier 5 Super Massive Reactor, full THORP, Pu-238 production, endgame polish | Endgame reactor, completionist content | Final polish release |

## Release 1 — The MVP

The first release is designed to be genuinely satisfying to play and complete in its own right. Players should be able to install R1, build a pile, generate nuclear power, escape coal dependency, and feel the progression loop working — without any awareness that R2+ is coming.

- Must include a full tier 1 fuel cycle: uranium mining, ore processing, pile construction, operation, spent fuel reprocessing, Pu-239 and Cs-137 extraction, decay to lead.
- Must include steam-age power conversion: heat exchangers, thermocouples, basic boiler, simple steam engine.
- Must include the first chunk of the research system: enough to make tier 1 choices feel meaningful, without requiring the full commitment/diversification mechanic planned for R2.
- Must include enough quest book content to guide a new player through the tier 1 loop.
- Must be genuinely self-contained — no hanging references to 'coming later' tech.

## Deferred to Later Releases

The following are designed in this document but deliberately not targeted for R1:

- All tier 2+ reactors and their infrastructure
- The reactor commitment / second-reactor-unlock mechanic (becomes relevant when there are multiple reactors per tier)
- Tiered enrichment (tier 1 uses natural uranium only)
- Advanced reprocessing outputs (Tc-99, Am-241, etc. — tier 1 reprocessing only extracts Pu-239 and Cs-137)
- The full research laboratory (R1 uses milestone-based research; the laboratory and sample system come in R2)
- Most side-path content (CANDU heavy water chain, LFTR thorium chain, fast breeder sodium handling)
- The blueprint and tiered deployer system (R2 feature — tier 1 pile is always hand-built in R1, which matches the 'primitive first reactor' character anyway)

# 3. Tier Progression & Tech Tree

Five tiers of reactor technology, each unlocking a new class of reactors and enrichment capability. Progression is gated by reprocessing output from the previous tier — you physically cannot build tier N+1 without running tier N and reprocessing its spent fuel.

## Tier Summary

| **Tier** | **Era** | **Reactors** | **Fuel** | **Base Size** |
|---|---|---|---|---|
| 1 | Pioneer | Graphite pile (air or water cooled) | Natural U | 3×3×3 – 6×3×3 |
| 2 | First Generation | Magnox, RBMK | Natural U / very low LEU | 5×5×5 (RBMK larger) |
| 3 | Commercial | PWR, BWR, AGR, CANDU | 3–5% LEU (CANDU: natural U + D₂O) | 7×7×7 |
| 4 | Advanced | Fast breeder, MSR, LFTR | Pu/MOX, Thorium, HALEU | 9×9×9 |
| 5 | Super Massive | SMR (Super Massive Reactor) | HALEU / multi-fuel | 13×13×13 |

## Reactor Commitment Mechanic (R2+)

From tier 2 onwards, a player reaching a new tier selects ONE reactor to unlock first. Alternate reactors at the same tier remain locked but visible in JEI with a clear requirement to unlock. Later, using research points, players can unlock additional reactors at the same tier and run multiple reactor types simultaneously.

- First reactor at each tier: unlocked via milestone / direct progression when the player crosses the tier boundary.
- Second reactor at same tier: ~50 research points (placeholder).
- Third reactor at same tier: ~40 research points (diminishing cost — you've done this before).
- Fourth reactor: ~30 research points.
- Cross-tier choices are independent. A tier 2 Magnox player is not locked out of any tier 3 reactor; however, research costs may be slightly lower for thematically-related tech (a Magnox / AGR lineage, for example).

## Why This Matters

- Player commitment. Choosing Magnox over RBMK at tier 2 is a statement about playstyle, not a tick-box exercise.
- Replayability. Different reactor choices across playthroughs produce genuinely different bases and bottlenecks.
- Multi-reactor gameplay. Players who unlock both reactors at a tier can operate them simultaneously, specialising each for different roles (Magnox for plutonium breeding, PWR for power output, etc.).
- Research economy. Gives the research point system a clear and satisfying use — diversifying your reactor fleet.

# 4. The Mod Name Acronym

In this mod, SMR stands for Super Massive Reactor. This is a deliberate creative licence choice — in real life, SMR stands for Small Modular Reactor and refers to sub-300MW designs emphasising safety and manufacturability. This mod's SMR is the opposite: the endgame reactor, the culmination of every tech tree branch, built only after a player has mastered every preceding reactor type.

The naming reveal is part of the quest book progression. Earlier quest entries reference real-world SMRs in the expected sense, then tier 5 flips the acronym. This is a one-time joke that rewards players paying attention.

## What Makes the Super Massive Reactor Special

The tier 5 SMR is the 'everything reactor' — flexibility and scale are its defining features:

- Multi-fuel: accepts natural U, LEU at any enrichment, HALEU, MOX, thorium salt, and recycled minor actinides. Runs on whatever the player has.
- Multi-output: simultaneously generates power, breeds fresh fuel, and burns waste actinides.
- Massive scale: 13×13×13 minimum. Can be overbuilt further.
- Endgame: construction requires materials and isotopes from every prior tier. No shortcuts.

# 5. Fuel Cycle & Materials Economy

## The Three Cycles

The mod's economy is built on three interlocking cycles, none of which can be ignored at scale:

- **Mining cycle —** uranium ore is finite per-chunk and requires exploration. Thorium ore exists as a parallel resource for the MSR/LFTR branch (R3+).
- **Reprocessing cycle —** spent fuel from any reactor goes through PUREX-style reprocessing to extract usable isotopes and reclaimed LEU. Mandatory for tier progression.
- **Decay cycle —** all radioactive items in storage slowly decay toward stable lead over in-game days to weeks. Lead becomes structural shielding. Stockpiling indefinitely is not an option.

## Enrichment Tiers (R2+)

| **Facility** | **Input** | **Output** | **Unlocks** |
|---|---|---|---|
| Tier 1 (basic centrifuge) | Natural U | Up to 3% LEU | Tier 2 reactors |
| Tier 2 (cascade centrifuge) | 3% LEU or natural U | Up to 5% LEU | Tier 3 reactors |
| Tier 3 (advanced / laser) | 5% LEU | HALEU 10–20% | Tier 4 & 5 reactors |

Every enrichment step produces depleted uranium (DU) tails as a byproduct. DU accumulates in storage until fed to a fast breeder (converts to Pu-239 via neutron capture on a blanket) or reprocessed through full THORP (reclaims usable LEU from fission-product mix).

## Fission Products & Their Uses

Reprocessing separates spent fuel into distinct isotopes. Most are construction and infrastructure materials, not fuels. Scope column indicates earliest release in which each isotope is extracted.

| **Isotope** | **Role** | **Notes** | **Scope** |
|---|---|---|---|
| Pu-239 | MOX fuel / breeder startup | High energy density, closes fuel cycle | R1 |
| Cs-137 | Startup block irradiation (thermal reactors) | Abundant, reliable thermal gamma source | R1 |
| Sr-90 | RTG fuel (passive power) | Set-and-forget, works without reactor | R2 |
| Tc-99 | Control rod material (tier 3+) | Strong neutron absorber | R2 |
| Am-241 | Startup block irradiation (fast reactors) | Fast-spectrum source | R3 |
| Np-237 | Precursor for Pu-238 production | Gateway to premium RTG fuel | R3 |
| Am-243 / Cm | Fast reactor / MSR exotic fuels | Late-game specialist fuel | R3 |
| Pu-238 (derived) | Premium RTG fuel | Much higher output than Sr-90 | R4 |
| Vitrified waste | Final disposal output | Stable glass form; must be stored | R1 |

# 6. Processing Line (PUREX-Based)

Real-world spent fuel reprocessing uses the PUREX (Plutonium Uranium Reduction EXtraction) process — a liquid-liquid chemistry chain that has been used at Sellafield, La Hague, and Hanford since the 1950s. The mod's processing line follows this sequence, giving players a genuine nuclear chemistry experience rather than a generic 'waste in, fuel out' machine.

## Distinction: Enrichment vs Reprocessing

Two distinct processes, often confused:

- **Enrichment (pre-reactor) —** Natural U is converted to UF₆ gas, spun through centrifuge cascades to concentrate U-235, then converted back to solid UO₂ fuel. Used to produce the enriched fuel for tiers 2+. Gas-based.
- **Reprocessing (post-reactor) —** Spent fuel is chopped, dissolved in nitric acid, and separated via liquid-liquid solvent extraction. Used to recover U and Pu from spent rods and to separate fission products. Liquid-based, no gas phase.

## The PUREX Processing Chain

From spent fuel rods to separated products, the full chain is:

### Step 1 — Decladding

Spent fuel is encased in metal cladding (Magnox alloy, aluminium, zircaloy). This must be removed before dissolution.

- Shearing Machine: mechanically chops fuel rods into short segments. Outputs chopped fuel + separated cladding scrap (recyclable).
- Alternative (Magnox fuel only): chemical decladding in dilute caustic solution.

### Step 2 — Dissolution

Chopped fuel is dropped into boiling concentrated nitric acid. UO₂ dissolves as uranyl nitrate. Plutonium and most fission products dissolve alongside.

- Dissolver Tank multiblock: input chopped fuel + nitric acid, output dissolved fuel solution (fluid) + insoluble sludge (small byproduct).
- Consumes: nitric acid (fluid input)

### Step 3 — Solvent Extraction (PUREX core)

The dissolved fuel solution is mixed with an organic solvent (TBP dissolved in kerosene). TBP selectively binds uranium and plutonium nitrates while ignoring fission products. The two phases are separated.

- Solvent Extraction Column: a tall multiblock where aqueous and organic fluids meet, mix, and separate. Input: dissolved fuel solution + TBP/kerosene. Output: U+Pu organic stream + fission product aqueous stream.

### Step 4 — Partitioning (U from Pu)

The U+Pu organic stream is treated with a reducing agent, which converts Pu(IV) to Pu(III). Pu(III) no longer binds to TBP and washes out into a fresh aqueous phase. Uranium remains in the organic.

- Partitioning Chamber: input U+Pu organic + reducing agent, output separated U nitrate + Pu nitrate streams.

### Step 5 — Conversion to Oxides

Each nitrate stream is evaporated and heated to produce solid oxide powders.

- Denitration Furnace: converts uranyl nitrate → UO₃ → UO₂ (with hydrogen reduction).
- Plutonium Calciner: precipitates Pu as oxalate, calcines to PuO₂ powder.
- Consumes: hydrogen (for UO₃ → UO₂ reduction)

### Step 6 — Fission Product Separation (R1 basic, expands in R2+)

The aqueous fission product stream is pumped through selective ion exchange columns, each tuned to a specific isotope or isotope group. The product chain expands as the player tiers up their reprocessing infrastructure.

- R1 basic: Cs-137 column only. Output: Cs-137, residual waste stream.
- R2 intermediate: + Sr-90, Tc-99 columns.
- R3 advanced: + Am-241, Np-237 columns (DIAMEX-equivalent).
- R4 full THORP: + Am-243, Cm columns (SANEX-equivalent).

### Step 7 — Vitrification

Residual waste solution is mixed with borosilicate glass frit and heated to produce molten glass, poured into canisters to solidify as stable glass logs for long-term disposal.

- Vitrification Furnace multiblock: input residual waste + glass frit, output vitrified waste blocks.

## Supporting Chemistry

The processing line consumes several chemical inputs. These are produced by the mod's own chemistry infrastructure OR, via tags, by compatible mods (GregTech, Mekanism, etc.).

| **Reagent** | **Used In** | **Production Path** |
|---|---|---|
| Nitric acid | Dissolution | Ostwald process: ammonia + air → NO → NO₂ → HNO₃. Tag: forge:fluids/nitric_acid |
| Sulfuric acid | Nitric acid production, general chemistry | Contact process from sulfur. Tag: forge:fluids/sulfuric_acid |
| TBP / extraction solvent | Solvent extraction | Nuclear-specific. Crafted from phosphate + solvents. Tag: nuclearpowered:fluids/extraction_solvent |
| Reducing agent | Partitioning | Iron sulfamate or hydroxylamine, abstracted. Tag: nuclearpowered:fluids/reducer |
| Glass frit | Vitrification | Sand + borax + lithium. Item. |
| Hydrogen | UO₃ reduction | Electrolysis of water. Tag: forge:gases/hydrogen |
| Ion exchange resin | Fission product separation | Consumable, periodically replaced. Item. |

## Reprocessing Tiers

| **Stage** | **Outputs** | **Machines Added** | **Unlocks** |
|---|---|---|---|
| Basic (R1) | Pu-239, Cs-137, reclaimed U, waste | Shearer, Dissolver, basic Extraction Column, Cs column, small Vitrifier | Tier 2 construction |
| Intermediate (R2) | + Sr-90, Tc-99 | Partitioning Chamber, Denitration Furnace, Sr & Tc columns | Tier 3 construction |
| Advanced (R3) | + Am-241, Np-237 | DIAMEX-equivalent columns, minor actinide handling | Tier 4 construction |
| Full THORP (R4) | + Am-243, Cm, Pu-238 production | SANEX columns, Pu-238 irradiation loop | Tier 5 (SMR) |

# 7. Power Conversion Chain

Reactors generate heat, not electricity directly. The mod's power conversion chain is the bridge between reactor heat and usable FE output. It's explicitly decoupled from reactor operation so players can upgrade power generation independently of reactor upgrades.

## The Three-Zone Reactor Architecture

Every reactor has three functional zones:

- **Core —** the multiblock proper (channels for piles, fuel chambers for tier 2+). Generates heat.
- **Active cooling —** fans, pumps, gas circulation, sodium loops, molten salt — whatever the reactor type requires. Keeps the core at safe operating temperature.
- **Heat capture —** heat exchangers wrapped around the exterior, which tap the heat being rejected and convert it to usable output (FE directly or via steam).

This separation means the reactor itself is unchanged as the player upgrades their power generation — they just swap out the downstream conversion equipment.

## Conversion Options

| **Tech** | **Efficiency** | **Requires** | **Available From** |
|---|---|---|---|
| Thermocouples (direct heat → FE) | Low (~40%) | Thermocouple blocks adjacent to reactor heat exchangers | R1 day one |
| Steam engine (heat → water → steam → piston) | Medium (~60%) | External water loop, boiler block, steam engine block | R1 mid-tier |
| Steam turbine (heat → water → steam → turbine) | High (~80%) | External water loop, boiler, turbine multiblock | R2+ |

## R1 Self-Sustaining Target

A minimum-size tier 1 pile with basic thermocouple conversion must produce enough FE to run the mod's own basic processing chain, plus a small surplus for a modest base. This is the defining success criterion for R1.

## R1 Power Budget (Rough Targets)

| **Item** | **FE/tick** |
|---|---|
| Basic crusher (ore → crushed ore) | ~20 |
| Ore washer (purification) | ~20 |
| Electric furnace (smelting) | ~30 |
| Fuel fabricator (assembly) | ~40 |
| Pile cooling (if electric) | ~10–20 |
| Monitoring / control | ~5 |
| Total draw (everything running) | ~130 |
| Realistic average draw (batched operation) | ~50–70 |

## R1 Pile Output Target

| **Configuration** | **Thermocouple FE/tick** | **Notes** |
|---|---|---|
| 3×3×3 minimum pile, basic thermocouples | ~120 | Small margin above basic processing. Self-sustaining. |
| 6×3×3 max pile, basic thermocouples | ~200 | Comfortable surplus. Small base viable. |
| 6×3×3 max pile + steam engine | ~300 | Upgrade path visible. Water loop investment rewarded. |
| 6×3×3 max pile + steam turbine (R2) | ~400 | Pile outscaled by Magnox but still viable as auxiliary. |

| *These numbers are illustrative — expect to tune during testing. What matters is the ratio: a minimum pile must comfortably power the basic processing chain, and upgrading conversion must feel like a meaningful jump.* |
|---|

## Thermocouples Stay Relevant

Thermocouples are not a throwaway tier 1 block. The same block technology is used for RTGs (Sr-90 and Pu-238 variants) in R2+, providing passive remote power for satellite bases and specialised machinery. Players learn thermocouples at tier 1 and apply them across the mod's lifespan.

# 8. Reactor Designs

## Tier 1 — Graphite Pile (R1)

The player's first reactor. A graphite stack with channels drilled through it, loaded with natural uranium fuel slugs. Minimal automation, dangerous through radiation rather than explosion, produces small but critical amounts of Pu-239 and Cs-137 for tier 2 construction.

### Structure

- Size: 3×3×3 minimum to 6×3×3 maximum. The larger Hanford-style layout has more channels.
- Channels: 9 at minimum (3×3 grid in top view), 15 at maximum (5×3 grid).
- Rods per channel: 3 (stacked vertically within the 3-block-tall core).
- Total rod capacity: 27 at min, 45 at max.
- Control rod channels separate from fuel channels (exact layout TBD).

### Cooling Options

- **Air cooling —** Giant industrial fans (Windscale / Brookhaven style). Can be steam-driven (no electricity required). Cheap, lower cooling capacity, restricts burn rate. Failure mode: graphite fire on overheat.
- **Water cooling —** Large pumps pulling through water source blocks (Hanford B Reactor style). Higher infrastructure cost, higher cooling capacity. Failure mode: loss-of-coolant meltdown.

### Operation & Outputs

- Startup block: not required. Piles go critical on natural criticality — deliberate gentle introduction before tier 2 raises complexity.
- Primary output: heat (to be converted via thermocouples, engine, or turbine).
- Byproduct output: Pu-239 (from neutron capture on U-238), Cs-137, spent natural U for reprocessing.
- Steam-age accessible: all tier 1 machines can run on steam power, no electricity required to start.

## Tier 2 — First Generation (R2)

Commercial-scale reactor engineering begins. Two reactors with distinct operational characters, both graphite-moderated, both fed by tier 1 enrichment (up to 3% LEU).

### Magnox

- Size: 5×5×5 standard; overbuildable to 7×7×7 at a fuel-efficiency penalty.
- Coolant: CO₂ gas.
- Fuel: Natural uranium in Magnox alloy cladding, or up to 3% LEU.
- Character: steady workhorse. Hard to break, modest output, forgiving.
- Failure mode: graphite fire (contained, local, no explosion).

### RBMK

- Size: approximately 10×10×7 — double Magnox footprint, flatter profile reflecting historical design.
- Coolant: light water.
- Fuel: 3% LEU.
- **Positive void coefficient —** reactor power rises when coolant boils or is lost. Coolant management is critical.
- Character: high output, temperamental, rewards careful operation.
- Failure mode: runaway excursion + steam explosion + graphite fire. Dramatic but contained.
- Online refuelling (shared with CANDU): fuel channels can be swapped while running; no scram required for fuel changes.

## Tier 3 — Commercial Reactors (R2/R3)

The mainstream of civilian nuclear power. Four reactor types with distinct characters; all at 7×7×7 base size.

### PWR (Pressurised Water Reactor)

- Coolant/moderator: pressurised light water. Fuel: 3–5% LEU assemblies.
- Character: well-balanced workhorse of the tier.

### BWR (Boiling Water Reactor)

- Coolant/moderator: boiling light water (single loop). Fuel: 3–5% LEU.
- Simpler than PWR but coolant is radioactive during operation.

### AGR (Advanced Gas-Cooled Reactor)

- Coolant: CO₂. Moderator: graphite. Fuel: 2–3% LEU.
- High thermal efficiency; more FE per unit fuel.

### CANDU

- Coolant/moderator: heavy water (D₂O). Fuel: natural uranium (no enrichment required).
- Online refuelling supported.
- Parallel-path reactor: skips enrichment entirely but requires heavy water infrastructure.

## Tier 4 — Advanced & Exotic (R3)

### Fast Breeder (sodium-cooled)

- Sodium coolant, no moderator (fast spectrum), Pu/MOX fuel with DU blanket.
- Breeds Pu-239 from DU. Net fissile gain over operation.
- Sodium fire failure mode.

### MSR / LFTR (Molten Salt)

- Molten fluoride salt coolant + dissolved fuel. Thorium bred to U-233 (LFTR) or U/Pu dissolved (MSR).
- Freeze plug safety: drains core to passive dump tanks on overheat. Very difficult to meltdown.

## Tier 5 — Super Massive Reactor (R4)

The endgame reactor. 13×13×13 minimum, multi-fuel, multi-output. Construction requires materials and isotopes from every preceding tier. The Super Massive Reactor is the reward for completing the mod's tech tree.

- Multi-fuel: accepts any fuel type the player has available.
- Multi-output: generates power, breeds fuel, transmutes actinides simultaneously.
- Massive scale: larger than any prior reactor; can be overbuilt even further.
- Quest book naming reveal: 'SMR' flips from real-world meaning to Super Massive Reactor.

## Universal Sizing Rules (Tier 2+)

Every tier-2+ reactor has a base size and can be overbuilt within reason. Size determines interior volume, which determines how many internal components (fuel chambers, cooling, control rods, moderator) fit inside.

- Building bigger = more capacity, not a different reactor tier.
- Oversized lower-tier reactors are always less efficient than standard higher-tier reactors.
- Size fixed at commissioning; decommission to resize.

## Internal Component Placement (Tier 2+)

Tier 2+ reactors use NuclearCraft-style placed internal components. The player builds the multiblock shell, then places fuel chambers, cooling components, control rod blocks, and moderator blocks inside. The controller GUI calculates capacity from what's placed.

- Fuel chamber count and total rod capacity
- Cooling capacity (sum of components)
- Control rod coverage
- Required cooling at full load
- Ready-to-commission status

Layout matters. Chambers adjacent to cooling run cooler; clustered chambers overheat. Reactor-internals design is a meaningful puzzle within each multiblock.

# 9. Blueprints & Tiered Deployers (R2+)

From tier 2 onwards, reactor construction has two modes: the first reactor of each type is built by hand, and all subsequent reactors of that type can be constructed from a recorded blueprint via a tiered deployer machine. This mechanic turns reactor construction into a first-time-ritual followed by industrial repetition — which mirrors how real engineering progresses and gives the mod a distinctive mechanic you don't see in other reactor mods.

## Design Intent

- The first reactor of any type must be built by hand, block by block. This forces the player to learn the structure — where fuel chambers go, where cooling sits, where control rods insert.
- On successful commissioning of a first-built reactor, a Reactor Blueprint item is produced, recording that specific reactor's layout including the player's chosen internal component placement.
- Subsequent reactors of the same type can be built via a Deployer using the blueprint, which fast-tracks construction once the player has proven they understand the design.
- The deployer itself is tiered. Each deployer tier can only build reactors of its tier and below. Upgrading the deployer in place is a separate progression track that parallels reactor unlocks.

## Blueprint Mechanics

- Reactor-type specific. A Magnox blueprint only builds Magnox reactors. You must hand-build each reactor type at least once, ever.
- Produced only on full successful commissioning — you cannot get a blueprint by placing blocks and breaking them down.
- Records the player's actual layout, not a generic template. If you optimised your first Magnox's internal placement, the blueprint preserves it. Players who tune layouts are rewarded permanently.
- Reusable, not consumable. Once recorded, a blueprint can be used as many times as the player wants.
- Improvable. A player who later hand-builds a better Magnox layout can record a new blueprint that replaces the old one. Each reactor type has a single 'current blueprint' the deployer uses.
- Overbuilt reactors: blueprints cover the structural shell and chosen internal layout; if you overbuilt your first reactor, the blueprint reproduces that scale. A separate hand-built pass is required if the player wants to record a blueprint for a different size.

## The Tiered Deployer

The deployer is a multiblock placed somewhere in the player's base. It accepts blueprints, material inputs, and FE, and constructs reactors over time at a location the player specifies. Each deployer tier raises the maximum reactor tier it can build.

| **Deployer Tier** | **Builds** | **Unlock Trigger** | **Upgrade Cost** |
|---|---|---|---|
| Tier 1 | Tier 1 (pile) + Tier 2 (Magnox, RBMK) | After first Magnox or RBMK commissioned | Initial build — basic steel, motors, lead shielding, simple control circuits |
| Tier 2 | Above + Tier 3 (PWR, BWR, AGR, CANDU) | After first tier 3 reactor commissioned + research unlock | Upgrade kit: Tc-99 reinforced framing, improved motors, reactor-grade control chips |
| Tier 3 | Above + Tier 4 (fast breeder, MSR, LFTR) | After first tier 4 reactor commissioned + research unlock | Upgrade kit: Am-241 calibration modules, high-temp materials, exotic seals |
| Tier 4 | Above + Tier 5 (Super Massive Reactor) | After first SMR commissioned + research unlock | Upgrade kit: HALEU-grade fabrication components, minor actinide materials |

## Deployer Operation

- Materials still required. The deployer saves labour, not resources. Building a second Magnox costs the same materials as the first; the deployer just places them.
- Build speed is proportional to reactor size/tier. Tier 2 reactors complete in minutes; a tier 5 Super Massive Reactor construction is a multi-tens-of-minutes event.
- FE consumption during build scales with target reactor tier. Higher tiers consume more power per tick during construction.
- Higher-tier deployers build lower-tier reactors faster. A tier 4 deployer building a Magnox is much faster than a tier 1 deployer building the same thing — the infrastructure has improved even when the target hasn't.
- Only one deployer per base is needed. Upgrades happen in-place: apply an upgrade kit to the existing multiblock, the tier counter increases, no rebuild required.

## Visual & Base Identity

The deployer is a growing multiblock. Each upgrade extends the physical structure with additional blocks — robotic arms, material handling belts, hazard-striped platforms, steam-venting stacks. Visitors to a player's base can tell at a glance what tier the deployer has reached just by looking at it.

## Quest Book Integration

- 'First Criticality': build and commission the first pile. Blueprint recorded (pile blueprint).
- 'Industrial Assembly': build the first Magnox or RBMK. Unlocks Tier 1 Deployer recipe; Magnox/RBMK blueprint recorded.
- 'Scaling Up': deploy a second reactor using the deployer. Validates the player has used the mechanic.
- Research unlock 'Advanced Fabrication I': after completing a tier 3 reactor, spend research points to unlock the Tier 1 → Tier 2 deployer upgrade recipe. Similar unlocks at each deployer tier.

## Design Notes

- The pile is eligible for deployer construction. Players who want to build multiple piles (for heavy Pu-239 breeding from natural U) should not have to hand-place each one. The first pile is hand-built; subsequent piles can deploy.
- The first reactor of each type always requires hand-building. There is no way to skip the learning experience. Sharing a friend's late-game blueprint does not let a new player leap ahead, because they still need the deployer tier to use it.
- The deployer mechanic does not apply in R1 — tier 1 piles in R1 are hand-built, which matches the 'primitive first reactor' character of the pile anyway. Blueprints and deployers arrive with R2.

# 10. Radiation & Shielding System

One radiation model governs irradiation chambers, operating reactors, spent fuel transport, RTGs, and waste storage: source strength vs shielding thickness, with effects decreasing by distance.

## Shielding Materials

| **Material** | **Gamma** | **Neutron** | **Notes** |
|---|---|---|---|
| Lead block | High | Low | Produced from decay; primary gamma shielding |
| Concrete block | Moderate | Moderate | Bulk shielding |
| Stone | Low | Low | Minimal use; early-game fallback only |
| Water block | Low | High | Essential for neutron shielding |
| Iron/steel | Moderate | Low | Structural component with partial shielding |

## Radiation Effects

- Correctly shielded sources emit zero leakage. Player stands nearby safely.
- Missing a single block of shielding causes directional leakage (radiation cone through the gap).
- Unshielded sources affect the player within a source-strength-dependent radius.
- Hazmat suit (R2+): full set grants substantial radiation resistance.

## Irradiation Chamber Sizing (R2+)

| **Chamber** | **Exterior** | **Interior** | **Wall Requirement** | **Used For** |
|---|---|---|---|---|
| Thermal (Cs-137) | 3×3×3 | 1×1×1 | 1-block lead | Tier 2/3 startup blocks |
| Fast (Am-241) | 5×5×5 | 3×3×3 | 2-block composite (lead + water/concrete) | Tier 4/5 startup blocks |

# 11. Operations & Failure Mechanics

## Core Reactor Stats

- **Heat —** rises with power output, falls with cooling. Exceeding threshold triggers meltdown.
- **Fuel —** rods deplete over time based on reactor type and size. Spent rods must be removed for reprocessing.
- **Coolant —** varies by reactor type. Loss of coolant triggers rapid heat rise (and on RBMKs, runaway power increase).

## The Emergency Response Ladder

**Stage 1 — Coolant to max.** Fans/pumps go full. Early catch resolves most incidents with minor FE cost.

**Stage 1.5 — Insert control rods.** Reactor drops to standby. No startup block consumed. Rods can be withdrawn later to restart.

**Stage 2 — Scram (AZ-5).** Emergency shutdown. Startup block consumed. Requires new irradiated block to restart. Behaviour identical across all reactor types — scram is always reliable.

**Stage 3 — Meltdown.** Heat exceeded critical threshold. See below.

## Meltdown Behaviour (NuclearCraft-Style)

All meltdowns are contained — no world damage outside the reactor footprint, no persistent radiation zones.

- Partial meltdown: fuel chambers melt. Fuel lost. Walls survive.
- Full meltdown: fuel + walls melt. Full rebuild required.
- Catastrophic failure: as full meltdown + cosmetic damage in reactor room (burn marks, broken blocks). Does not spread.

## Reactor-Specific Failure Flavour

| **Reactor** | **Primary Failure Mode** | **Character** |
|---|---|---|
| Pile (air) | Graphite fire | Smoke, no explosion |
| Pile (water) | Core melt | Radioactive puddle |
| Magnox | Graphite fire | Slow, forgiving |
| RBMK | Steam explosion + graphite fire | Dramatic, punishes lateness |
| PWR / BWR | Steam explosion + core melt | Fast, pressure event |
| AGR | Mild graphite fire | Gas coolant limits steam risk |
| CANDU | Core melt, tritium release | Heavy water loss accelerates failure |
| Fast Breeder | Sodium fire + core melt | Sodium burns on air/water contact |
| MSR / LFTR | Rarely fails; freeze plug drains core | Graceful; fuel recoverable |
| SMR (Super Massive) | Scaled to fuel loaded | Largest footprint event in game |

# 12. Startup Block Commissioning (R2+)

Every tier-2+ reactor requires a charged startup block to be installed before commissioning. The mod's signature ritual — a deliberate pre-operation step that makes reactor startup feel like an event.

## Commissioning Flow

- Craft uncharged startup assembly (lead shielding, metals, reactor-grade components).
- Place in irradiation chamber sized for required source type.
- Insert required isotope source (Cs-137 for thermal, Am-241 for fast, Pu-based mix for MSR).
- Wait for irradiation. Source depletes.
- Install charged block in reactor socket.
- Reactor is now commissionable.

## Source Requirements by Reactor

| **Reactor** | **Source** | **Chamber** |
|---|---|---|
| Tier 1 pile | None required | — |
| Magnox, RBMK, PWR, BWR, AGR, CANDU | Cs-137 | Thermal (3×3×3) |
| Fast breeder | Am-241 | Fast (5×5×5) |
| MSR / LFTR | Pu-based mixed source | Fast (5×5×5) |
| SMR (thermal config) | Cs-137 | Thermal (3×3×3) |
| SMR (fast config) | Am-241 | Fast (5×5×5) |

## Startup Block Lifecycle

- Installed / running: intact. Reactor operates indefinitely on fuel + coolant.
- Standby (rods in): intact. Restart without new block.
- Scram: consumed. New block required.
- Reactor destroyed: lost.

# 13. Research & Quest System

Progression is driven by a hybrid system: milestone-based direct unlocks along the critical path, plus a research point currency for optional specialisations and reactor diversification. FTB Quests is the implementation platform.

## Research Categories

Points are earned and spent within categories, preventing a single currency from unlocking everything.

- Reactor Engineering — reactor upgrades, new reactor unlocks
- Chemistry — reprocessing improvements, new isotope separations
- Materials Science — better alloys, shielding, fuel cladding
- Safety — improved scram, containment, decontamination

## Earning Research Points

- Milestones (auto-completion): quest book entries granting points when conditions met. Covers the spine of progression — players cannot get stuck.
- Research Laboratory (R2+): placed multiblock accepting samples and producing points over time. Rewards active research engagement. Samples include ore, spent fuel, isotopes, meltdown residue, reactor telemetry.
- Documents / operation logs: rare drops or craftable from reactor operation, grant points when read.

## Spending Research Points

Points are spent in the quest book to unlock:

- Alternate reactors at the current tier (reactor diversification)
- Efficiency upgrades for existing reactors (improved pile design, advanced thermocouples, etc.)
- Safety upgrades (better scram, redundant cooling)
- Processing optimisations (improved PUREX yield, faster centrifuges)
- Side-branch unlocks (RTG construction, thorium chain, heavy water plant)

## Recipe Visibility

Unreleased recipes are visible in JEI with a lock icon and visible requirement text. True hiding is avoided — players benefit from knowing what they're working toward. 'Visible but locked' is the standard for modern modpacks.

## Scope by Release

- R1: milestone-based unlocks only. Simple, direct. No research lab, no currency economy yet.
- R2: full research system introduced. Research Lab multiblock, reactor commitment mechanic, research categories active.
- R3+: additional research trees for advanced content.

## Quest Book Structure

- Main chapter: the tier spine. Auto-completing milestones.
- Reactor choice chapters (R2+): one per tier, the 'pick your reactor' decision points.
- Research chapter (R2+): points spending UI and tree.
- Side chapters: RTG construction, exotic fuel production, repository design, niche infrastructure.

# 14. Ores, Materials & Compatibility

The mod ships a complete self-contained ore and material set for standalone play, designed to coexist gracefully with other mods via Forge/NeoForge tags. In pack environments with GregTech, Mekanism, or similar, the mod's recipes accept either source transparently.

## Compatibility Strategy

- All recipes use tag-based inputs. Any mod satisfying the tag works.
- The mod's own ores and machines exist as fallback for standalone play.
- In GregTech packs: GT's materials satisfy the tags; players use GT's superior processing chain.
- In Mekanism packs: Mekanism's chemistry satisfies acid/gas tags.
- Optional config: pack authors can disable the mod's ore generation if another mod provides the equivalents.

## Core Ores (Mod-Provided, Tagged)

| **Ore** | **Tag** | **Primary Use** |
|---|---|---|
| Uranium | forge:ores/uranium | Fuel |
| Thorium | forge:ores/thorium | MSR/LFTR fuel (R3+) |
| Lead | forge:ores/lead | Shielding baseline (before decay provides bulk) |
| Zircon | forge:ores/zircon | Fuel cladding |
| Lithium (spodumene) | forge:ores/lithium | Tritium targets, FLiBe salt |
| Boron / borax | forge:ores/boron | Control rods, glass frit |
| Beryllium (beryl) | forge:ores/beryllium | Reflectors, AmBe sources |
| Fluorite | forge:ores/fluorite | Molten salt fluorine source (R3+) |
| Saltpeter | forge:ores/saltpeter | Nitric acid production |
| Sulfur | forge:ores/sulfur | Sulfuric acid production |
| Salt / halite | forge:ores/salt | Sodium metal for fast breeders (R3+) |

## Graphite

Not a dedicated ore. Crafted from coal (raw graphite) with an optional purification step (nuclear-grade graphite) for reactor use. Real reactor graphite requires extreme purity — particularly low boron content — which is modelled as the purification step.

## Structural Metals

Iron, steel, chromium, nickel, titanium — sourced via tags. Mod-provided only as fallback. In GT/Mekanism packs the host mod supplies these with better processing.

## Nuclear-Specific Materials

These exist only in Nuclear Powered and are not satisfied by other mods:

- Startup assembly blocks (uncharged + charged variants per source type)
- Fuel chamber blocks (variants per reactor type)
- Reactor casings (tier-specific)
- Ion exchange resins
- TBP/extraction solvent
- Reducing agent
- Vitrified waste blocks

## Standalone Processing Machines (R1)

For players without GT or Mekanism, the mod provides a minimum viable processing chain. In GT packs, players will likely use GT's versions — which is fine, since recipes are tag-based.

- Ore Crusher (steam-powered or low-FE)
- Ore Washer (purification for reactor-grade materials)
- Electric Furnace (smelting / reduction)
- Chemical Reactor (generic chemistry recipes including nitric acid, TBP)
- Centrifuge (enrichment R2+; general separation R1)
- Electrolyser (hydrogen, heavy water)
- Fuel Fabricator (crafting fuel slugs from UO₂ + cladding)

# 15. Balance Reference Tables

Rough starting values for implementation. All subject to playtesting.

## Tier Scaling Targets

| **Tier** | **Base Size** | **Rod Capacity (rough)** | **Relative Power** |
|---|---|---|---|
| 1 (pile) | 3×3×3 – 6×3×3 | 27 – 45 | 1× |
| 2 (Magnox) | 5×5×5 | ~50–80 | 3–5× |
| 2 (RBMK) | 10×10×7 | ~200+ | 8–12× |
| 3 | 7×7×7 | ~150–280 | 10–20× |
| 4 | 9×9×9 | ~400–600 | 25–50× |
| 5 (SMR) | 13×13×13 | ~1000–1700 | 80–150× |

## Fuel Burn Rate (FE/tick per rod, illustrative)

| **Reactor** | **FE/tick per rod** | **Rod Lifetime** | **Notes** |
|---|---|---|---|
| Pile (natural U) | ~40 | Long | Teaching reactor |
| Magnox (natural U) | ~80 | Long | Workhorse |
| Magnox (3% LEU) | ~120 | Shorter | Overclocked |
| RBMK (3% LEU) | ~180 | Medium | Coolant-demanding |
| PWR / BWR (5% LEU) | ~250 | Medium | Commercial baseline |
| AGR (2–3% LEU) | ~280 | Medium | High-temp outlet |
| CANDU (natural U) | ~200 | Medium | No enrichment cost |
| Fast breeder (MOX) | ~320 | Long + breeding gain | Pu output |
| MSR / LFTR | ~300 | Very long (online refuel) | Hard to melt |
| SMR (HALEU) | ~400 | Long | Best ratio |

## Isotope Decay Half-Lives (In-Game)

| **Isotope** | **In-Game Half-Life** | **Decay Product** |
|---|---|---|
| Cs-137 / Sr-90 | ~3 days | Lead |
| Am-241 | ~10 days | Lead |
| Tc-99 / Np-237 | ~14 days | Lead |
| Minor actinides (Am-243, Cm) | ~21 days | Lead |
| Pu-239 | ~30 days (slow — usually used before decay matters) | Lead |

# 16. Implementation Roadmap

## Platform & Libraries

- Minecraft 1.20.1
- NeoForge (primary); optional Forge compatibility
- Multiblock library: Multiblocked (fast start) or custom (more control) — TBD during implementation
- Fluid handling: NeoForge fluid capabilities
- Quest system: FTB Quests
- Recipe gating / hiding: FTB Quests reward locks or JEI integration
- Power: FE via NeoForge energy capability (accepts/outputs FE to any compatible mod)

## R1 Implementation Order

### Phase 1 — Foundations

- Project skeleton, NeoForge mod setup, tag definitions
- Uranium ore + world generation (with optional disable config)
- Natural uranium item, fuel slug item
- Supporting ores: lead, lithium, boron, saltpeter, sulfur, graphite source (coal derivation)

### Phase 2 — Basic Processing

- Ore Crusher (accepts steam or FE)
- Electric Furnace
- Fuel Fabricator
- Chemical Reactor (nitric acid recipe, TBP recipe)

### Phase 3 — Tier 1 Pile

- Graphite blocks (raw, nuclear-grade)
- Pile multiblock controller + detection logic
- Channel + fuel loading mechanic
- Heat generation model
- Cooling infrastructure: fans (air), pumps (water)

### Phase 4 — Power Conversion

- Heat exchanger blocks
- Thermocouple blocks (direct heat → FE)
- Boiler + water loop + steam fluid
- Simple steam engine (steam → FE)

### Phase 5 — Reprocessing (Basic)

- Shearing Machine
- Dissolver Tank + nitric acid fluid handling
- Basic Solvent Extraction Column
- Cs-137 ion exchange column
- Small Vitrification Furnace
- Vitrified waste blocks

### Phase 6 — Radiation & Shielding

- Radiation source tracking on relevant blocks/items
- Shielding attenuation model
- Player damage from exposure
- Lead block and lead-lined storage

### Phase 7 — Decay System

- Half-life tracking on radioactive items in storage
- Decay to lead in storage blocks (paused in player inventory)

### Phase 8 — Quest Book & Polish

- FTB Quests R1 chapter
- Milestone-based unlocks for R1 content
- JEI integration and recipe visibility
- Balance pass: confirm self-sustaining target met
- Meltdown mechanic (simple version — fuel + walls melt, no spread)
- In-game manual / quick-start guide

## Success Criteria for R1 Release

- A player can start fresh, build a pile, power their own processing chain from its output, stockpile Pu-239 and Cs-137, and reprocess spent fuel into vitrified waste — all without coal or external power beyond the starter.
- Steam-age accessibility confirmed: pile is buildable without prerequisite electric infrastructure.
- Self-sustaining target met: minimum pile + thermocouples produces enough FE to run basic processing chain plus small surplus.
- Quest book guides a new player through the full loop without external documentation.
- Works in standalone and in SkyGreg (tested).

# 17. Open Questions & Future Decisions

Design choices not yet locked down — to be resolved during implementation or deferred:

- Exact channel layout for the 6×3×3 pile (5×3 fuel grid + reserved row for control rods, or another geometry)
- Tier 3 channel density vs water-moderated packing (graphite-moderated vs water-moderated internal placement rules)
- Does the MSR freeze plug recover fuel automatically, or does the player have to pump it back from dump tanks?
- Radiation meter UI — persistent bar, HUD indicator only when radiation present, or geiger-counter item?
- Reprocessing chemistry granularity — named reagents (nitric, TBP, reducer) vs more abstraction?
- Whether to ship starter-kit commands or creative-tab items for pack makers bootstrapping mid-tree
- Multiblock library choice (Multiblocked vs custom) — decide in early implementation
- Whether the Super Massive Reactor has subtypes (thermal/fast/salt) or is a single configurable reactor
- Exact FE output tuning to hit self-sustaining target — requires playtest
- Forge compatibility scope — full parity with NeoForge, or NeoForge-only for launch?
- Deployer build speed tuning — exact minutes per reactor tier, and whether speed difference between deployer tiers should be significant or modest
- Blueprint storage and sharing — should blueprints be transferable between players (single-item gift), or should each player have to record their own? Current design assumes transferable but requires deployer tier to use.
- Deployer failure modes — what happens if the deployer runs out of materials mid-build or loses power? Pause and resume, or fail with partial structure?
