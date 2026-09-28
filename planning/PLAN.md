# Better Blackjacking — Implementation Plan

A RuneLite Plugin Hub plugin, written from scratch, that helps players blackjack in Pollnivneach:
it outlines the target's click box in a colour that says what to do next, shows a smooth
countdown to the next knock-out, times the two guaranteed pickpockets, and smooths the target's
wake-up animation.

This document lives at `planning/PLAN.md` in the repository. It is written to be handed to an
**orchestrating agent** that spawns **sub-agents**
for the individual tasks in [§8](#8-work-breakdown). Every task lists the files it owns, what it
depends on, and how to tell it is done. Read §1–§7 before spawning anything; they are the spec
every task refers back to.

> **Status of inputs.** Facts marked **[wiki]** were checked against the OSRS Wiki on
> 2026-09-27. Facts marked **[measured]** come from debug logs of 19 real bandit knockouts,
> saved in `planning/fixtures/`. Facts marked **[unverified]** must be confirmed in-game (task
> T14) before release. Decisions are recorded in [§10](#10-decisions); only the wake-up animation is still open.

---

## 1. Scope

### In scope (v1.0)

1. **Click-box outline** on blackjackable NPCs the player has the Thieving level for, coloured by
   state (§4).
2. **Activation gate:** everything is active only while the player is **in Pollnivneach** with a
   **blackjack of any type equipped** (§3.2, §3.3).
3. **Knock-out timer**: a smooth countdown (refreshing at least every 0.1 s) to when the target
   should be knocked out again. Styles: **Pie** (default), **Seconds**, **Off**. Sized from the
   target's tile, not the camera zoom (§5.3).
4. **Pickpocket indicator** that times the two guaranteed pickpockets and then says stop, also
   sized from the tile (§5.4).
5. **Wake-up animation**: a get-up animation the plugin plays (on the player's screen only) when
   the target wakes, instead of it snapping from lying down to standing (§5.5). The choice of
   animation is pending the user's review of the GIFs in `planning/wake-animation-previews/`.
6. Repository, CI, style checks, changelog and docs to **Stockpile-Plugin standards** (§7).
7. Logo (§7.6). The banner is TBD and out of scope for v1.0.

### Out of scope / non-goals

- No automation of any kind: no menu swapping, no clicking and no input changes. This is an
  overlay plugin only, per Jagex's third-party client guidelines.
- No network access.
- No support for NPCs outside Pollnivneach.
- No banner image (TBD by the user).

---

## 2. Reference implementation and data

A throwaway prototype exists in the user's fork of the original plugin:
`C:\Users\Jake\IdeaProjects\blackjack-plugin` (package `com.blackjack`). It is **reference only**:
do not copy its structure or style. The useful parts:

| Prototype piece | What to take from it |
|---|---|
| `BlackjackPlugin#getPickpocketsLeft` | The pickpocket timing model (§4.3) |
| `BlackjackPlugin#onOverheadTextChanged` | Waking up is detected from the target saying "Arghh my head." |
| `BlackjackPlugin#playWakeAnimation` | Replacing the server's snap to standing (`808`) with a get-up animation |
| `BlackjackPlugin#debug` + handlers | The shape of the debug log that produced the fixtures |
| `src/test/.../BlackjackPreviewPlugin` | A dev-only recorder that captures animation GIFs from the client |

**Fixtures:** `planning/fixtures/session{1,2}-bandit.log` contain the prototype's debug lines,
one event per line: `tick=<n> ko+<ticks since knockout> <event>`. The test task (T4) turns these
into replayable test fixtures.

---

## 3. Game facts

### 3.1 Blackjack targets [wiki]

Thieving level requirement = the level needed to pickpocket that NPC.

| Target | NPC IDs | Combat | Thieving req. | Notes |
|---|---|---|---|---|
| Bandit (bearded) | 736, 737 | 41 | 45 | Northern Pollnivneach. The fixtures were recorded on ID 737. |
| Bandit (no beard) | 734, 735 | 56 | 55 | Northern Pollnivneach |
| Menaphite Thug | 3550 | 55 | 65 | Southern Pollnivneach. 3549 is the pre-quest version and can't be blackjacked. |

**Villagers (3552–3560) are deliberately excluded** (decided): they need Thieving 30 but give no
XP after The Feud.

Knock-out success chance depends only on Thieving level (31% at level 1, rising linearly to 94%
at 99). The NPC type and blackjack type don't affect it [wiki]. Blackjacking also needs partial
completion of The Feud; the plugin does not check quest state.

**Level gate:** compare the player's **boosted** Thieving level with the requirement (decided).
Re-check it on `StatChanged`.

### 3.2 Blackjacks [wiki + `net.runelite.api.gameval.ItemID`]

All nine equippable blackjacks: oak 4599, 6408 (o), 6410 (d); willow 4600, 6412 (o), 6414 (d);
maple 6416, 6418 (o), 6420 (d). Use the `ItemID` constants, not literals.
**Exclude** the makeshift blackjack (30944), which is a Final Dawn quest item and can't be
equipped [wiki].

"Equipped" means the weapon slot of the `EQUIPMENT` item container. Re-evaluate on
`ItemContainerChanged`.

### 3.3 Pollnivneach area [wiki coordinates, derived]

Every Pollnivneach NPC coordinate on the wiki (bandits x 3354–3371 / y 2984–3003, thugs
x 3333–3352 / y 2945–2961, villagers, merchants) falls in **map regions 13358** (x 3328–3391,
y 2944–3007) and **13357** (y 2880–2943, for the southern edge at y ≈ 2942). Gate on
`WorldPoint.getRegionID()` ∈ {13357, 13358}. [unverified] T14 confirms the town's edges don't
reach into 13102 or 13359.

### 3.4 Event signatures [measured unless noted]

| Event | Signal | Chat type / notes |
|---|---|---|
| Knock-out succeeded | `You smack the bandit over the head and render them unconscious.` | SPAM, on tick **T** |
| Knock-out overhead | Target's overhead text `Zzzzzz` | Tick T |
| Knock-out swing | Player animation 401 and target animation 838 (`HUMAN_UNCONSCIOUS`) | Tick T−1 |
| Knock-out failed | `Your blow only glances off the bandit's head.` then target overhead `I'll kill you for that!` | SPAM |
| Target attacks | Target animation 395, then a hitsplat on the player the next tick | – |
| In combat (can't knock out) | `You can't knock-out the bandit during combat.` | GAMEMESSAGE |
| In combat (can't pickpocket) | `You can't pickpocket during combat.` | GAMEMESSAGE |
| Knock out while unconscious | `I can't do that. They're unconscious.` | GAMEMESSAGE |
| Pickpocket processed | `You attempt to pick the bandit's pocket.` | SPAM, on tick A |
| Pickpocket landed | `You pick the bandit's pocket.`, plus player animation 827 when guaranteed | SPAM, tick A+1 |
| Target wakes | Target overhead `Arghh my head.` and target animation **808** (`HUMAN_READY`) | Tick **T+5** |

The target of a knock-out is the NPC the player was interacting with when the success message
arrived. This was right in 19 of 19 knockouts. The `Zzzzzz` overhead can confirm it.

**[unverified]** Only bandit messages were captured. **Match messages with a pattern whose NPC
noun is a capture group**, e.g. `You smack the (.+) over the head and render them unconscious\.`,
and confirm the thug wording in T14.

### 3.5 Timing model [measured]

- The target stays down for **5 ticks**: knocked out at T, wakes at T+5. This held for every
  knockout where nothing interfered (16 of 16).
- Pickpockets: the earliest can be processed at T+1, and each lands 1 tick after it's processed.
  Consecutive pickpockets are 2 ticks apart [wiki: "experience drops every two ticks"].
  A guaranteed pickpocket landed as late as T+4, so **the last safe processing tick is T+3**.
- Re-knockout: the earliest success seen was T+6. All 3 knock-outs that landed at T+5 failed,
  which is suggestive but not conclusive. T14 confirms it.
- The player's typical cycle was 6–7 ticks.

---

## 4. Target state model

### 4.1 States and default colours

| State | Default colour | Meaning | Condition |
|---|---|---|---|
| `SAFE` | Green `#00FF00` | Pickpocket now | Knocked out, and at least one more pickpocket will land |
| `WAKING` | Yellow `#FFFF00` | Stop pickpocketing and get ready | Knocked out, but no more pickpockets will land |
| `KNOCK_OUT` | Orange `#FFA500` | Knock them out | Awake and not in combat with the player |
| `ATTACKING` | Red `#FF0000` | You're being attacked and can't knock out. Break combat by any means first (e.g. swap weapon, then knock out). | The target is in combat with the player |

All four colours are configurable. Targets that aren't being tracked (not the player's current
target and not in combat with them) are drawn as `KNOCK_OUT`.

### 4.2 Transitions

- **→ SAFE:** a knock-out success message arrives. Record T and the target NPC.
- **SAFE → WAKING:** when `pickpocketsLeft == 0` (§4.3).
- **WAKING / SAFE → KNOCK_OUT:** at tick T+5, or earlier if the target says `Arghh my head.`
  (the game's own signal wins).
- **→ ATTACKING:** on a failed knock-out, an `I'll kill you for that!` overhead, either "during
  combat" message, or a hitsplat on the player while the target is interacting with them.
- **ATTACKING → KNOCK_OUT:** on a successful knock-out (goes to SAFE instead), or once the target
  hasn't been interacting with the player for **N = 2 ticks**. [unverified] T14 tunes N.
- Clear everything when the target despawns, when the player logs out or hops, and when the
  activation gate closes.

### 4.3 Pickpockets left

This is the prototype's model, which fits all the measured data:

```
lastSafeTick = T + 5 - 1 - 1              // must land by T+4, and landing is 1 tick after processing
firstTick    = max(nextPickpocketTick, currentTick + 1)   // a click this tick is processed next tick
pickpocketsLeft = firstTick > lastSafeTick ? 0 : min(2, (lastSafeTick - firstTick) / 2 + 1)
```

`nextPickpocketTick` starts at T+1 and becomes A+2 after each "You attempt to pick…" message.
The constants (duration 5, landing delay 1, interval 2, maximum 2) are named constants in one
place. The knock-out duration is also an advanced config value (§6).

### 4.4 Timer target

The timer counts down to **T+5**, the tick the target wakes (decided). A knock-out clicked then
lands at T+6, the earliest success observed.

---

## 5. Rendering

### 5.1 Common rules

- Draw only while the activation gate is open, and only on targets that pass the level gate.
- Every size comes from the **tile unit** `u`: the on-screen width of the target's tile, from
  `Perspective.getCanvasTilePoly(client, npc.getLocalLocation())` bounds. It is computed every
  frame, so the overlay scales with the target's tile (smaller when zoomed out) and never reads
  the camera zoom value (decided). Clamp each size to a sane pixel range so it stays readable at
  extreme zoom.
- **Anchor (decided): the target's tile.** The timer is centred on the tile's projected centre
  (`Perspective.localToCanvas` at height 0), and the pips sit just below it, still within the
  tile's bounds.
- Stroke widths, font sizes and fill opacity come from config where §6 says so.

### 5.2 Click-box outline

Outline `npc.getConvexHull()` (the click box) in the state colour. Configurable stroke width
(default 2) and fill opacity (default 20/255). No text.

### 5.3 Knock-out timer

- **Smooth clock:** on each `GameTick`, store `System.nanoTime()`. The time remaining is
  `(wakeTick − currentTick) × 600 ms − msSinceLastTick`, clamped at 0. The overlay redraws every
  frame, so both styles update far more often than every 0.1 s.
- **Pie (default):** a RuneLite `ProgressPieComponent` (the pie timer used across the client).
  It drains from full at T to empty at T+5, drawn in the state colour, with radius `0.35u`
  clamped to [6, 40] px, centred on the tile (§5.1).
- **Seconds:** text with one decimal, e.g. `2.4`, font size `0.45u` clamped to [10, 32] px,
  centred on the tile.
- **Off:** nothing.
- Shown only while the target is `SAFE` or `WAKING`.

### 5.4 Pickpocket indicator

- **Pips (decided; there's no text style).** Filled pips show the pickpockets still available
  (●● / ●○). At 0 both pips are hollow and the word `STOP` is drawn in the `WAKING` colour. Pip
  diameter is `0.15u` clamped to [4, 16] px, placed directly under the timer on the tile.
- A config toggle turns the pips on or off (default on).
- Shown only while the target is `SAFE` or `WAKING`.

### 5.5 Wake-up animation

When the tracked target's animation changes to `808` within 2 ticks of T+5, replace it with the
chosen get-up animation using `npc.setAnimation(id)` and `setAnimationFrame(0)`. This is
cosmetic and only on the player's screen; the state has already switched to `KNOCK_OUT`, and the
next knock-out's `838` replaces it straight away. Options (lengths from the cache [measured via
cache viewer]):

| Option | ID(s) | Length | Notes |
|---|---|---|---|
| Off | – | – | The game's snap to standing (default until decided) |
| Situps get up | 2760 `SITUPS_GETUP` | 0.5 s | |
| Sit-up, then get up | 2759 → 2760 | 1.1 s | |
| Sit-up short | 7190 `SITUP_SHORT` | 1.9 s | |
| Cyrisus sit up | 6283 `DREAM_CYRISUS_SIT_UP_TRANSITION` | 1.2 s | Unconscious to sitting, then snaps to standing |
| Cyrisus stand up | 6286 `DREAM_CYRISUS_STAND_UP_TRANSITION` | 2.0 s | Crouch to standing |
| Cyrisus sit up, then stand | 6283 → 6286 | 3.2 s | |
| Max get up | 7122 `MAX_GET_UP` | 1.6 s | |

The user rejected 534 (`HUMAN_GETUP`), 1409 and 3807, so they are not options. The choice among
these candidates is still open (§10 row 1).

---

## 6. Configuration

Config group `betterblackjacking`. Sections, in order:

| Section | Key | Name | Type | Default |
|---|---|---|---|---|
| Outline | `outlineWidth` | Outline width | int 1–6 | 2 |
| Outline | `fillOpacity` | Fill opacity | int 0–255 | 20 |
| Colours | `safeColor` | Safe to pickpocket | Color | green |
| Colours | `wakingColor` | Waking up | Color | yellow |
| Colours | `knockOutColor` | Knock out | Color | orange |
| Colours | `attackingColor` | Attacking | Color | red |
| Timer | `timerStyle` | Timer style | enum {PIE, SECONDS, OFF} | PIE |
| Pickpockets | `pickpocketPips` | Show pickpocket pips | boolean | true |
| Animation | `wakeAnimation` | Wake-up animation | enum (§5.5) | OFF (pending) |
| Advanced (closed) | `knockOutTicks` | Knock-out duration | int 3–8, ticks | 5 |
| Advanced (closed) | `debugLogging` | Debug logging | boolean | false |

**Lesson from the prototype:** RuneLite saves defaults into the user's profile, so changing a
default later doesn't reach existing users. Changing a default means renaming the key, which
gets a changelog note.

---

## 7. Repository standards (copy from Stockpile-Plugin)

Reference: `https://github.com/Oveduumnakal/Stockpile-Plugin` (read-only). Copy these exactly
unless a note says otherwise.

### 7.1 Layout

```
Better-Blackjacking-Plugin/
├── .github/
│   ├── FUNDING.yml, dependabot.yml
│   ├── ISSUE_TEMPLATE/{bug_report.yml, feature_request.yml, config.yml}   # change plugin name + links
│   └── workflows/{build.yml, javadocs.yml, pr-checks.yml, release.yml}   # copy unchanged
├── .gitattributes, .gitignore (includes /.claude and HANDOFF.md)
├── LICENSE                         # BSD 2-Clause, "Copyright (c) 2026, Oveduumnakal"
├── README.md                       # Stockpile's shape: centred banner (placeholder until TBD), feature sections
├── JavaDocs.md                     # generated
├── build.gradle, settings.gradle (rootProject.name = 'better-blackjacking')
├── gradle/wrapper (Gradle 8.10, pinned SHA), gradlew, gradlew.bat
├── icon.png                        # 48×48 (Plugin Hub limit)
├── icons/source/icon.svg           # logo source
├── javadoc-tools/                  # copy unchanged
├── runelite-plugin.properties      # displayName=Better Blackjacking, author=Oveduumnakal, version=1.0, build=standard
├── scripts/check-style.py          # copy; replace the StockpileColors note in the docstring with BetterBlackjackingColors
├── scripts/gen-icon.sh             # renders icons/source/icon.svg → icon.png + resources copy
├── planning/                       # this plan's fixtures and previews; delete before release
└── src/
    ├── main/java/com/oveduumnakal/betterblackjacking/...
    ├── main/resources/com/oveduumnakal/betterblackjacking/{changelog.md, icon.png}   # package-relative (Stockpile #351)
    └── test/
        ├── java/com/oveduumnakal/betterblackjacking/...
        └── resources/{logback-test.xml, fixtures/*.log}
```

### 7.2 Build

Stockpile's `build.gradle` with `pluginMainClass = 'com.oveduumnakal.betterblackjacking.BetterBlackjackingPluginTest'`:
Java release 11, `-Xlint:deprecation`, Lombok 1.18.30, JUnit 4.12, Mockito 5.12.0,
guice-testlib 4.1.0, `checkStyleRules` and `javadoc` wired into `check`, and the `run` and
`shadowJar` tasks. **No new dependencies.** The Plugin Hub builds under dependency verification.

### 7.3 Code style

`scripts/check-style.py` enforces 19 rules. The ones sub-agents trip on most:

- Tabs, Allman braces, lines of 120 columns or less.
- No `//` comments. Use Javadoc, which every class needs.
- No braces on a single-statement body.
- A blank line after every control block.
- Import groups: java/javax, then third-party, then net.runelite, then static. Alphabetical,
  with no wildcard imports.

Every file starts with the BSD license header. Colours used in overlays come from one
`BetterBlackjackingColors` class.

### 7.4 Changelog

`changelog.md` plus `Changelog.java` plus `ChangelogGuardTest`, in Stockpile's form. The top
entry `# 1.0 - <Month D YYYY>` must match `runelite-plugin.properties`. Only the release task
writes the entry.

### 7.5 Git and GitHub process (Stockpile conventions)

- One GitHub issue per task in §8, all in milestone **`Release 1.0`**.
- Branches follow `type/type-<issue#>-short-title` (e.g. `feature/feature-3-knockout-tracker`),
  which `pr-checks.yml` enforces.
- Every PR body contains `Closes #<issue>`, and every PR has the milestone set.
- Squash merge. Titles in the imperative ("Add the knock-out tracker").
- Releases are tagged `R-1.0`; `release.yml` creates the GitHub release and closes the milestone.

### 7.6 Logo

`icons/source/icon.svg` shows two playing cards fanned slightly, a blackjack weapon (a short
wooden club with a darker grip) laid sideways across both cards, and the letters **BB** readable
on the front card. It must still read clearly at 48×48, so use thick shapes, no fine detail and
at most 5 colours. Render it to `icon.png` (48×48) and to the resources copy. `rsvg-convert`
isn't installed on the user's machine; the script falls back to Python `cairosvg` or asks the
user to install librsvg. The banner is TBD, so the README uses a placeholder comment.

---

## 8. Work breakdown

### 8.1 Orchestration rules

1. **Each sub-agent works in its own git worktree** on its task branch, and opens one PR.
2. **File ownership is exclusive.** A task edits only the files it owns. If it needs something
   from another task's file, it asks the orchestrator rather than editing it.
3. **Shared hotspots have one owner each.** `JavaDocs.md`: every PR regenerates it for its own
   branch, since the JavaDocs CI check runs on PRs. When PRs conflict on it, the orchestrator
   regenerates it on the rebased branch rather than hand-merging. `changelog.md` is written only in
   T15. `BetterBlackjackingConfig.java` belongs to T1: later tasks read config and never add
   keys; if they need one, the orchestrator amends T1's file. `BetterBlackjackingPlugin.java`
   belongs to T7.
4. **Definition of done for every task:**
   - `./gradlew build` passes, which runs compile, tests, the style check and javadoc lint.
   - `./gradlew -p javadoc-tools checkJavaDocs` passes after regenerating.
   - The task's acceptance criteria are met and new logic has unit tests.
   - PR conventions (§7.5) are followed.
5. **Testing for coverage: both styles are allowed.** Use pure-logic tests (plain JUnit on
   classes with no RuneLite dependencies, driven by values or fixture logs) and/or Mockito
   tests (mocked `Client`, `NPC`, `Player`, `ItemContainer`, events and so on, as in
   Stockpile's `StockpilePluginHandlersTest` and `MockitoHarnessTest`). Pick whichever reaches
   the code most simply. Mix them in one task if that helps. Use Mockito for code that has to
   touch RuneLite types, such as event handlers, overlays and the gate wiring.
   As a design preference, keep game logic free of the RuneLite `Client` where that's natural,
   with the plugin class translating events into plain method calls. This makes pure-logic tests
   the easy path, but it's not a reason to leave code untested.
6. **Human gates:** the orchestrator stops and asks the user before (a) pushing a release
   tag, (b) submitting to the Plugin Hub, and (c) anything in §10 still open when a task needs it.
   The user approved (2026-09-27) creating the public GitHub repository and committing and
   merging plan work into it as needed.
7. **Branch flow:** T0's scaffold is the initial commit on `main`. Every later task goes through a
   PR to `main`, following §7.5. The orchestrator reviews each PR and squash-merges it once CI is
   green.

### 8.2 Waves

| Wave | Tasks | Parallel? |
|---|---|---|
| 0 | T0 scaffold, **then** T1 config and constants | Sequential, one agent |
| — | Create the GitHub repo (approved), push `main`, then create the milestone and one issue per task | Orchestrator |
| 1 | T2 domain tables, T3 gate logic, T4 knock-out tracker, T5 geometry and clock, T6 logo, T11 debug logging | Parallel |
| 2 | T7 plugin wiring, T8 outline overlay, T9 timer overlay, T10 pickpocket indicator | Parallel (T7 merges last) |
| 3 | T12 wake animation, T13 dev tooling | Parallel |
| 4 | T14 in-game verification (needs the user to play) | – |
| 5 | T15 docs, changelog, release prep | Sequential |

### 8.3 Tasks

**T0 Scaffold the repository** (wave 0)
Owns everything in §7.1 except `src/main/java/**` (other than a minimal plugin class that loads),
`icons/`, `icon.png` and `planning/`.
Produces a buildable, style-clean empty plugin: `BetterBlackjackingPlugin` (loads, does nothing),
`BetterBlackjackingPluginTest` (dev launcher), `Changelog` + `ChangelogGuardTest`, a placeholder
`changelog.md` entry `1.0`, CI workflows, templates and `JavaDocs.md`.
Done when `./gradlew build` and `checkJavaDocs` pass, and `./gradlew run` launches a client that
lists "Better Blackjacking" as a plugin.

**T1 Config and shared contracts** (wave 0, after T0)
Owns `BetterBlackjackingConfig.java`, `TimerStyle.java`, `WakeAnimation.java`,
`BetterBlackjackingColors.java`, `TargetState.java` and `TargetStateView.java`.
Implements §6 exactly, with sections, positions, ranges and units (`@Units(Units.TICKS)`).
`TargetState` is the enum from §4.1. `TargetStateView` is the read-only interface the overlays
draw from, so overlays (T8–T10) and wiring (T7) can be written in parallel:
`boolean isActive()`, `Collection<NPC> eligibleTargets()`, `TargetState stateOf(NPC)`,
`int pickpocketsLeft()`, `long remainingMillis()` (sub-tick, §5.3), `long durationMillis()` and
`Color colorOf(TargetState)`. `WakeAnimation` lists every §5.5 option with its animation-ID
sequence.
Done when a `ConfigDefaultsTest` asserts every default in §6.

**T2 Domain tables** (wave 1)
Owns `BlackjackTarget.java`, `Blackjacks.java` and `Pollnivneach.java`.
`BlackjackTarget` is an enum holding NPC IDs, the Thieving requirement and `forNpcId(int)`.
`Blackjacks.isBlackjack(itemId)` follows §3.2. `Pollnivneach.contains(WorldPoint)` uses regions
{13357, 13358}.
Done when tests cover every ID in §3.1–§3.2, the exclusion of villagers and of the makeshift
blackjack, and the region edges.

**T3 Activation and level gates** (wave 1)
Owns `ActivationGate.java`.
A pure function of (region, weapon item ID, boosted Thieving level): it returns whether the
plugin is active and, for each `BlackjackTarget`, whether it's eligible.
Done when table-driven tests pass.

**T4 Knock-out tracker and pickpocket model** (wave 1, the core of the plugin)
Owns `KnockoutTracker.java`, `PickpocketBudget.java`, `ChatSignals.java`
(regex patterns from §3.4) and `src/test/resources/fixtures/*`.
Implements §4 as plain methods, for example
`onTick(int)`, `onChat(type, message, interactingNpcIndex)`,
`onOverhead(npcIndex, text)`, `onNpcAnimation(npcIndex, animId)`,
`onPlayerHitsplat(sourceNpcIndex)`, `onNpcInteracting(npcIndex, boolean targetsPlayer)`,
`onDespawn(npcIndex)`, `reset()`, `stateOf(npcIndex)`, `pickpocketsLeft(int currentTick)`,
`knockedOutNpcIndex()` and `wakeTick()`. T7 exposes these through `TargetStateView` (T1).
Converts `planning/fixtures/*.log` into replayable fixtures, plus a small parser.
Done when replaying both sessions gives:
- a SAFE state at every `KNOCKOUT` line;
- KNOCK_OUT exactly at T+5 or at `Arghh my head.`, whichever comes first;
- ATTACKING after every `glances off`;
- `pickpocketsLeft` = 2 at T and 1 after a pickpocket processed at T+1.

**T5 Geometry and smooth clock** (wave 1)
Owns `TileScale.java` and `SubTickClock.java`.
`TileScale.unit(Polygon tilePoly)` and `clamp(scale × u, min, max)` as in §5.1.
`SubTickClock` works as in §5.3, with an injectable nanosecond source for tests.
Done when unit tests cover clamping and sub-tick interpolation to within 1 ms.

**T6 Logo** (wave 1)
Owns `icons/source/icon.svg`, `scripts/gen-icon.sh`, `icon.png` and the resources `icon.png`.
Done when the 48×48 PNG passes a visual check. The orchestrator sends it to the user with
`SendUserFile`. It may merge before the user replies; if they ask for changes, the orchestrator
opens a follow-up task.

**T7 Plugin wiring** (wave 2, integrates wave 1)
Owns `BetterBlackjackingPlugin.java` and `PluginStateView.java` (the `TargetStateView`
implementation, which combines the tracker, the gates, `SubTickClock` and config).
It registers the T8–T10 overlays. Because those are written in parallel, T7 merges last in the
wave: the orchestrator rebases it on the merged overlays.
Subscribes to `GameTick`, `ChatMessage`, `OverheadTextChanged`, `AnimationChanged`,
`HitsplatApplied`, `InteractingChanged`, `NpcDespawned`, `ItemContainerChanged`, `StatChanged`
and `GameStateChanged`. It feeds `KnockoutTracker` and `ActivationGate`, and adds and removes
overlays on startup and shutdown.
Done when Mockito handler tests (Stockpile's `StockpilePluginHandlersTest` pattern) show events
reaching the tracker, and the gate closes when the blackjack is unequipped or the player leaves
the area.

**T8 Outline overlay** (wave 2)
Owns `OutlineOverlay.java`. Implements §5.2, reading only `TargetStateView`, config and
`TileScale`.
Done when a test with a mocked NPC hull verifies the colour for each state.

**T9 Timer overlay** (wave 2)
Owns `TimerOverlay.java`. Implements §5.3 (pie, seconds, off), reading only `TargetStateView`,
config and `TileScale`.
Done when tests cover the pie fraction and the formatting of the seconds text (`2.4`, `0.0`),
using `SubTickClock` with a fake time source.

**T10 Pickpocket indicator overlay** (wave 2)
Owns `PickpocketIndicatorOverlay.java`. Implements §5.4, reading only `TargetStateView`, config
and `TileScale`.
Done when tests cover the pips for 2, 1 and 0 pickpockets left (`STOP` at 0).

**T11 Debug logging** (wave 1)
Owns `DebugLog.java`.
Behind the `debugLogging` config, it logs lines tagged `[BetterBlackjacking]` with
`tick=` and `ko+`, in the fixture format so new sessions can be added as fixtures directly. T7
calls it. It takes the config and a tick supplier, and has no event subscriptions of its own.

**T12 Wake animation** (wave 3)
Owns `WakeAnimationController.java`. Implements §5.5, including two-animation sequences (the
second starts when the first's frame lengths have elapsed, counted in client ticks). It ships
with every candidate as an option and defaults to Off until decision 1 in §10 is made.
Done when a test verifies the animation is replaced only for the tracked target and only within
the wake window. The user reviews it in-game.

**T13 Dev tooling** (wave 3)
Owns `src/test/.../dev/*`.
Ports the prototype's preview recorder (dev-only, loaded by the test launcher, never shipped),
used for the README GIFs.

**T14 In-game verification** (wave 4, needs the user)
Produces `planning/verification.md` and new fixtures.
The user plays with debug logging on, while the orchestrator watches the log. The checklist:
- thug chat wording (§3.4);
- the town's edges (§3.3);
- ATTACKING's exit rule, N (§4.2);
- whether a knock-out at T+5 can succeed (§3.5);
- that the two-pickpocket rhythm lands both guaranteed pickpockets.
Each result is added as a new fixture, and the spec is updated where the data disagrees.

**T15 Docs and release prep** (wave 5)
Owns `README.md`, `changelog.md`, `runelite-plugin.properties` (version) and removes
`planning/`.
The README follows Stockpile's layout: features with GIFs recorded using T13's tool.
Writes the changelog entry `1.0`. The Plugin Hub submission is a human gate.

---

## 9. Risks

| Risk | Mitigation |
|---|---|
| Thug messages differ from the bandit's | Regex with the NPC noun as a capture group; T14 confirms |
| The knock-out target isn't the interacting NPC (e.g. with several bandits around) | Cross-check with the `Zzzzzz` overhead; fall back to the nearest eligible NPC that said it |
| A Plugin Hub reviewer objects to setting NPC animations | It's cosmetic and off by default; say so in the PR to the Plugin Hub |
| RuneLite saves defaults into profiles | Rename the key whenever a default changes (§6) |
| Sub-agents conflict on shared files | Exclusive ownership and orchestrator-only hotspots (§8.1) |

---

## 10. Decisions

| # | Question | Decision |
|---|---|---|
| 1 | Wake-up animation | **Open.** The user rejected 534, 1409 and 3807. The sit-up candidates in §5.5 are under review (GIFs in `planning/wake-animation-previews/`). Default: Off. Also open: whether to start the animation a tick early, during `WAKING`, so the target is standing right at T+5. |
| 2 | Villagers | Excluded entirely |
| 3 | Level gate | Boosted Thieving level |
| 4 | Scale to tile | Sizes follow the target's on-screen tile (smaller when zoomed out), with min/max clamps |
| 5 | Timer end point | Counts down to the wake tick, T+5 |
| 6 | Pickpocket indicator | Pips only (●● / ●○ / STOP), with an on/off toggle |
| 7 | Anchor | On the target's tile |
| 8 | Red | "You're being attacked and can't knock out." Breaking combat by any means clears it. |
