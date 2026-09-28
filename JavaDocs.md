# Better Blackjacking — JavaDoc Reference

<!-- GENERATED FILE — DO NOT EDIT BY HAND.
     Run `./gradlew generateJavaDocs` and commit the result. -->

## Contents

- [com.oveduumnakal.betterblackjacking.BetterBlackjackingColors](#comoveduumnakalbetterblackjackingbetterblackjackingcolors)
- [com.oveduumnakal.betterblackjacking.BetterBlackjackingConfig](#comoveduumnakalbetterblackjackingbetterblackjackingconfig)
- [com.oveduumnakal.betterblackjacking.BetterBlackjackingPlugin](#comoveduumnakalbetterblackjackingbetterblackjackingplugin)
- [com.oveduumnakal.betterblackjacking.Changelog](#comoveduumnakalbetterblackjackingchangelog)
- [com.oveduumnakal.betterblackjacking.Changelog.Release](#comoveduumnakalbetterblackjackingchangelogrelease)
- [com.oveduumnakal.betterblackjacking.TargetState](#comoveduumnakalbetterblackjackingtargetstate)
- [com.oveduumnakal.betterblackjacking.TargetStateView](#comoveduumnakalbetterblackjackingtargetstateview)
- [com.oveduumnakal.betterblackjacking.TimerStyle](#comoveduumnakalbetterblackjackingtimerstyle)
- [com.oveduumnakal.betterblackjacking.WakeAnimation](#comoveduumnakalbetterblackjackingwakeanimation)

---

## com.oveduumnakal.betterblackjacking.BetterBlackjackingColors

_class_

`final class BetterBlackjackingColors`

The plugin's shared colour palette, defined once so the config defaults and every overlay stay
consistent and a colour change is a one-line edit.

<p>The four state colours are only defaults: the overlays draw with the colours the player has
configured, read through `TargetStateView#colorOf(TargetState)`.

### Field Summary

| Modifier and Type | Field | Description |
|---|---|---|
| `static final Color` | `ATTACKING` | Default colour of `TargetState#ATTACKING`: red `#FF0000`. |
| `static final Color` | `KNOCK_OUT` | Default colour of `TargetState#KNOCK_OUT`: orange `#FFA500`. |
| `static final Color` | `SAFE` | Default colour of `TargetState#SAFE`: green `#00FF00`. |
| `static final Color` | `WAKING` | Default colour of `TargetState#WAKING`: yellow `#FFFF00`. |

### Constructor Summary

| Constructor | Description |
|---|---|
| `BetterBlackjackingColors()` |  |

### Field Detail

#### ATTACKING

`static final Color ATTACKING`

Default colour of `TargetState#ATTACKING`: red `#FF0000`.

#### KNOCK_OUT

`static final Color KNOCK_OUT`

Default colour of `TargetState#KNOCK_OUT`: orange `#FFA500`.

#### SAFE

`static final Color SAFE`

Default colour of `TargetState#SAFE`: green `#00FF00`.

#### WAKING

`static final Color WAKING`

Default colour of `TargetState#WAKING`: yellow `#FFFF00`.

### Constructor Detail

#### BetterBlackjackingColors

`private BetterBlackjackingColors()`

---

## com.oveduumnakal.betterblackjacking.BetterBlackjackingConfig

_interface_

`public interface BetterBlackjackingConfig`

RuneLite configuration for the Better Blackjacking plugin.

<p>Every user-facing setting is a defaulted `@ConfigItem` accessor, grouped into
`@ConfigSection`s in this order: outline, colours, timer, pickpockets, animation, and a
closed-by-default advanced section. The `KEY_*` constants are the persisted setting keys
and `#GROUP` names the config group.

<p>RuneLite saves a setting's default into the user's profile the first time it is read, so
changing a default later never reaches existing users. Changing a default therefore means
renaming its key, and the rename gets a changelog note.

### Field Summary

| Modifier and Type | Field | Description |
|---|---|---|
| `String` | `GROUP` | RuneLite config group name (`"betterblackjacking"`). |
| `String` | `KEY_ATTACKING_COLOR` | Persisted config key `"attackingColor"`. |
| `String` | `KEY_DEBUG_LOGGING` | Persisted config key `"debugLogging"`. |
| `String` | `KEY_FILL_OPACITY` | Persisted config key `"fillOpacity"`. |
| `String` | `KEY_KNOCK_OUT_COLOR` | Persisted config key `"knockOutColor"`. |
| `String` | `KEY_KNOCK_OUT_TICKS` | Persisted config key `"knockOutTicks"`. |
| `String` | `KEY_OUTLINE_WIDTH` | Persisted config key `"outlineWidth"`. |
| `String` | `KEY_PICKPOCKET_PIPS` | Persisted config key `"pickpocketPips"`. |
| `String` | `KEY_SAFE_COLOR` | Persisted config key `"safeColor"`. |
| `String` | `KEY_TIMER_STYLE` | Persisted config key `"timerStyle"`. |
| `String` | `KEY_WAKE_ANIMATION` | Persisted config key `"wakeAnimation"`. |
| `String` | `KEY_WAKING_COLOR` | Persisted config key `"wakingColor"`. |
| `String` | `advancedSection` | Timing overrides and diagnostics most players never need; closed by default. |
| `String` | `animationSection` | The get-up animation played when the target wakes. |
| `String` | `coloursSection` | The colour for each target state, shared by the outline, timer and pips. |
| `String` | `outlineSection` | The click-box outline's stroke and fill. |
| `String` | `pickpocketsSection` | The pips that time the two guaranteed pickpockets. |
| `String` | `timerSection` | The knock-out timer drawn on the target's tile. |

### Method Summary

| Modifier and Type | Method | Description |
|---|---|---|
| `default Color` | `attackingColor()` | Colour of `TargetState#ATTACKING`: the target is in combat with the player, so it can't be knocked out until combat is broken. |
| `default boolean` | `debugLogging()` | Whether to write knock-out, pickpocket and wake-up events to the client log. |
| `default int` | `fillOpacity()` | Opacity of the click-box fill, from 0 (no fill) to 255 (solid). |
| `default Color` | `knockOutColor()` | Colour of `TargetState#KNOCK_OUT`: the target is awake and can be knocked out. |
| `default int` | `knockOutTicks()` | How many game ticks a knocked-out target stays down, from 3 to 8. |
| `default int` | `outlineWidth()` | Stroke width of the click-box outline, from 1 to 6 pixels. |
| `default boolean` | `pickpocketPips()` | Whether to draw the pips that show how many guaranteed pickpockets are left. |
| `default Color` | `safeColor()` | Colour of `TargetState#SAFE`: the target is knocked out and another pickpocket will land. |
| `default TimerStyle` | `timerStyle()` | How the countdown to the next knock-out is drawn. |
| `default WakeAnimation` | `wakeAnimation()` | The get-up animation the target plays, on this player's screen only, when it wakes. |
| `default Color` | `wakingColor()` | Colour of `TargetState#WAKING`: the target is knocked out but no more pickpockets will land. |

### Field Detail

#### GROUP

`String GROUP`

RuneLite config group name (`"betterblackjacking"`).

#### KEY_ATTACKING_COLOR

`String KEY_ATTACKING_COLOR`

Persisted config key `"attackingColor"`.

#### KEY_DEBUG_LOGGING

`String KEY_DEBUG_LOGGING`

Persisted config key `"debugLogging"`.

#### KEY_FILL_OPACITY

`String KEY_FILL_OPACITY`

Persisted config key `"fillOpacity"`.

#### KEY_KNOCK_OUT_COLOR

`String KEY_KNOCK_OUT_COLOR`

Persisted config key `"knockOutColor"`.

#### KEY_KNOCK_OUT_TICKS

`String KEY_KNOCK_OUT_TICKS`

Persisted config key `"knockOutTicks"`.

#### KEY_OUTLINE_WIDTH

`String KEY_OUTLINE_WIDTH`

Persisted config key `"outlineWidth"`.

#### KEY_PICKPOCKET_PIPS

`String KEY_PICKPOCKET_PIPS`

Persisted config key `"pickpocketPips"`.

#### KEY_SAFE_COLOR

`String KEY_SAFE_COLOR`

Persisted config key `"safeColor"`.

#### KEY_TIMER_STYLE

`String KEY_TIMER_STYLE`

Persisted config key `"timerStyle"`.

#### KEY_WAKE_ANIMATION

`String KEY_WAKE_ANIMATION`

Persisted config key `"wakeAnimation"`.

#### KEY_WAKING_COLOR

`String KEY_WAKING_COLOR`

Persisted config key `"wakingColor"`.

#### advancedSection

`String advancedSection`

Timing overrides and diagnostics most players never need; closed by default.

#### animationSection

`String animationSection`

The get-up animation played when the target wakes.

#### coloursSection

`String coloursSection`

The colour for each target state, shared by the outline, timer and pips.

#### outlineSection

`String outlineSection`

The click-box outline's stroke and fill.

#### pickpocketsSection

`String pickpocketsSection`

The pips that time the two guaranteed pickpockets.

#### timerSection

`String timerSection`

The knock-out timer drawn on the target's tile.

### Method Detail

#### attackingColor

`default Color attackingColor()`

Colour of `TargetState#ATTACKING`: the target is in combat with the player, so it can't be
knocked out until combat is broken.

- **Returns:** the attacking colour (default red)

#### debugLogging

`default boolean debugLogging()`

Whether to write knock-out, pickpocket and wake-up events to the client log.

- **Returns:** whether debug logging is on (default false)

#### fillOpacity

`default int fillOpacity()`

Opacity of the click-box fill, from 0 (no fill) to 255 (solid).

- **Returns:** the fill alpha, 0 to 255 (default 20)

#### knockOutColor

`default Color knockOutColor()`

Colour of `TargetState#KNOCK_OUT`: the target is awake and can be knocked out.

- **Returns:** the knock-out colour (default orange)

#### knockOutTicks

`default int knockOutTicks()`

How many game ticks a knocked-out target stays down, from 3 to 8.

- **Returns:** the knock-out duration in ticks (default 5)

#### outlineWidth

`default int outlineWidth()`

Stroke width of the click-box outline, from 1 to 6 pixels.

- **Returns:** the outline stroke width in pixels (default 2)

#### pickpocketPips

`default boolean pickpocketPips()`

Whether to draw the pips that show how many guaranteed pickpockets are left.

- **Returns:** whether the pickpocket pips are shown (default true)

#### safeColor

`default Color safeColor()`

Colour of `TargetState#SAFE`: the target is knocked out and another pickpocket will land.

- **Returns:** the safe colour (default green)

#### timerStyle

`default TimerStyle timerStyle()`

How the countdown to the next knock-out is drawn.

- **Returns:** the timer style (default `TimerStyle#PIE`)

#### wakeAnimation

`default WakeAnimation wakeAnimation()`

The get-up animation the target plays, on this player's screen only, when it wakes.

- **Returns:** the wake-up animation (default `WakeAnimation#OFF`)

#### wakingColor

`default Color wakingColor()`

Colour of `TargetState#WAKING`: the target is knocked out but no more pickpockets will land.

- **Returns:** the waking colour (default yellow)

---

## com.oveduumnakal.betterblackjacking.BetterBlackjackingPlugin

_class_

`public class BetterBlackjackingPlugin`

Entry point of the Better Blackjacking plugin, which helps players blackjack in Pollnivneach.

<p>It outlines the target's click box in a colour that says what to do next, counts down to the
next knock-out, times the two guaranteed pickpockets, and smooths the target's wake-up animation.
This scaffold only registers the plugin with the client; the event wiring and overlays arrive in
later changes.

---

## com.oveduumnakal.betterblackjacking.Changelog

_class_

`public final class Changelog`

Parses the bundled `changelog.md` resource into an ordered list of
releases (newest first). Each release starts with a top-level `# <version> - <date>`
heading; everything up to the next such heading is that release's markdown body.
The parser is offline and deterministic; the newest entry's version is treated as the
plugin's current version at runtime, and `ChangelogGuardTest` enforces that it
matches `runelite-plugin.properties`.

### Nested Type Summary

| Type | Description |
|---|---|
| _class_ [`Release`](#comoveduumnakalbetterblackjackingchangelogrelease) | One release: its version, written-out date, and the raw markdown body beneath its heading. |

### Field Summary

| Modifier and Type | Field | Description |
|---|---|---|
| `private static final Pattern` | `HEADING` | A release heading: `# 1.0 - September 27 2026`. |
| `static final String` | `RESOURCE` | Resource path of the bundled changelog, relative to this class's package (`com/oveduumnakal/betterblackjacking`). |
| `private final List<Release>` | `releases` |  |

### Constructor Summary

| Constructor | Description |
|---|---|
| `Changelog(List<Release> releases)` |  |

### Method Summary

| Modifier and Type | Method | Description |
|---|---|---|
| `public String` | `currentVersion()` |  |
| `public boolean` | `hasVersion(String version)` |  |
| `public static Changelog` | `load()` | Loads and parses the bundled changelog resource. |
| `public static Changelog` | `parse(String markdown)` | Parses changelog markdown into releases in document order (expected newest first). |
| `private static String` | `read(InputStream in) throws IOException` |  |
| `public List<Release>` | `releases()` |  |

### Field Detail

#### HEADING

`private static final Pattern HEADING`

A release heading: `# 1.0 - September 27 2026`. The `(?!#)` keeps it to a single
`#` so the body's `##`/`###`/`####` headings aren't release boundaries.

#### RESOURCE

`static final String RESOURCE`

Resource path of the bundled changelog, relative to this class's package
(`com/oveduumnakal/betterblackjacking`).

<p>Deliberately package-relative, like `icon.png`: every plugin-hub plugin shares one
classloader, so a root-level `/changelog.md` is not namespaced to Better Blackjacking and
another plugin's root changelog could resolve in its place, showing the wrong release notes.

#### releases

`private final List<Release> releases`

### Constructor Detail

#### Changelog

`private Changelog(List<Release> releases)`

### Method Detail

#### currentVersion

`public String currentVersion()`

- **Returns:** the newest release's version, or `null` when the changelog is empty.

#### hasVersion

`public boolean hasVersion(String version)`

- **Returns:** whether a release with exactly `version` exists.

#### load

`public static Changelog load()`

Loads and parses the bundled changelog resource.

#### parse

`public static Changelog parse(String markdown)`

Parses changelog markdown into releases in document order (expected newest first).

#### read

`private static String read(InputStream in) throws IOException`

#### releases

`public List<Release> releases()`

- **Returns:** the releases, newest first (document order).

---

## com.oveduumnakal.betterblackjacking.Changelog.Release

_class_

`public static class Release`

One release: its version, written-out date, and the raw markdown body beneath its heading.

### Field Summary

| Modifier and Type | Field | Description |
|---|---|---|
| `String` | `body` |  |
| `String` | `date` |  |
| `String` | `version` |  |

### Field Detail

#### body

`String body`

#### date

`String date`

#### version

`String version`

---

## com.oveduumnakal.betterblackjacking.TargetState

_enum_

`public enum TargetState`

What the player should do next with a blackjack target, which decides the colour it is drawn in.

<p>Targets that aren't being tracked (neither the player's current knock-out target nor in combat
with the player) are `#KNOCK_OUT`.

### Enum Constant Summary

| Enum Constant | Description |
|---|---|
| `ATTACKING` | The target is in combat with the player, so it can't be knocked out. |
| `KNOCK_OUT` | Knock the target out: it is awake and not in combat with the player. |
| `SAFE` | Pickpocket now: the target is knocked out and at least one more pickpocket will land. |
| `WAKING` | Stop pickpocketing and get ready: the target is knocked out, but no more pickpockets will land. |

### Method Summary

| Modifier and Type | Method | Description |
|---|---|---|
| `public boolean` | `isKnockedOut()` | Whether the target is knocked out, which is when the timer and pickpocket pips are drawn. |

### Enum Constant Detail

#### ATTACKING

`ATTACKING`

The target is in combat with the player, so it can't be knocked out. The player has to break
combat by any means first, e.g. swap weapon, then knock out.

#### KNOCK_OUT

`KNOCK_OUT`

Knock the target out: it is awake and not in combat with the player.

#### SAFE

`SAFE`

Pickpocket now: the target is knocked out and at least one more pickpocket will land.

#### WAKING

`WAKING`

Stop pickpocketing and get ready: the target is knocked out, but no more pickpockets will land.

### Method Detail

#### isKnockedOut

`public boolean isKnockedOut()`

Whether the target is knocked out, which is when the timer and pickpocket pips are drawn.

- **Returns:** `true` for `#SAFE` and `#WAKING`

---

## com.oveduumnakal.betterblackjacking.TargetStateView

_interface_

`public interface TargetStateView`

The read-only view of the plugin's state that the overlays draw from.

<p>The plugin implements it by combining the knock-out tracker, the activation and level gates,
the sub-tick clock and the config, so overlays depend only on this interface and can be written
and tested without the plugin. At most one target is knocked out at a time; the timing methods
(`#pickpocketsLeft()`, `#remainingMillis()`) describe that target.

<p>Every method is called on the client thread, usually once per rendered frame, so each one must
be cheap, must not block, and must never return `null`.

### Method Summary

| Modifier and Type | Method | Description |
|---|---|---|
| `Color` | `colorOf(TargetState state)` | The colour the player has configured for `state`. |
| `long` | `durationMillis()` | The full length of a knock-out in milliseconds (the configured knock-out duration in ticks times 600 ms). |
| `Collection<NPC>` | `eligibleTargets()` | The NPCs to draw: every blackjack target in the scene that the player has the Thieving level for. |
| `boolean` | `isActive()` | Whether the activation gate is open: the player is in Pollnivneach with a blackjack equipped. |
| `int` | `pickpocketsLeft()` | How many more guaranteed pickpockets will land on the knocked-out target before it wakes, if the player clicks as soon as possible. |
| `long` | `remainingMillis()` | Milliseconds until the knocked-out target wakes, interpolated between game ticks so a countdown drawn every frame moves smoothly. |
| `TargetState` | `stateOf(NPC npc)` | The state of `npc`, which decides its colour. |

### Method Detail

#### colorOf

`Color colorOf(TargetState state)`

The colour the player has configured for `state`.

- **Parameter** `state` — a target state
- **Returns:** that state's colour, never `null`

#### durationMillis

`long durationMillis()`

The full length of a knock-out in milliseconds (the configured knock-out duration in ticks times
600 ms). A pie timer's filled fraction is `remainingMillis() / (double) durationMillis()`.

- **Returns:** the knock-out duration in milliseconds, always positive

#### eligibleTargets

`Collection<NPC> eligibleTargets()`

The NPCs to draw: every blackjack target in the scene that the player has the Thieving level
for. Empty, never `null`, while the plugin is inactive. Callers must not modify the
returned collection.

- **Returns:** the eligible targets currently in the scene

#### isActive

`boolean isActive()`

Whether the activation gate is open: the player is in Pollnivneach with a blackjack equipped.
Overlays draw nothing while this is `false`.

- **Returns:** whether the plugin is active

#### pickpocketsLeft

`int pickpocketsLeft()`

How many more guaranteed pickpockets will land on the knocked-out target before it wakes, if the
player clicks as soon as possible. This is between 0 and 2 while a target is knocked out, and 0
when none is.

- **Returns:** the pickpockets left, from 0 to 2

#### remainingMillis

`long remainingMillis()`

Milliseconds until the knocked-out target wakes, interpolated between game ticks so a countdown
drawn every frame moves smoothly. It is never negative, and 0 when no target is knocked out.

- **Returns:** the time left until the target wakes, in milliseconds

#### stateOf

`TargetState stateOf(NPC npc)`

The state of `npc`, which decides its colour. A target that isn't tracked (not the current
knock-out target and not in combat with the player) is `TargetState#KNOCK_OUT`.

- **Parameter** `npc` — a target, usually one from `#eligibleTargets()`
- **Returns:** the target's state, never `null`

---

## com.oveduumnakal.betterblackjacking.TimerStyle

_enum_

`public enum TimerStyle`

How the knock-out timer is drawn on the target's tile while it is knocked out.

<p>The display name is what the config panel's dropdown shows.

### Enum Constant Summary

| Enum Constant | Description |
|---|---|
| `OFF` | No timer. |
| `PIE` | A pie that drains from full at the knock-out to empty when the target wakes. |
| `SECONDS` | The seconds left, with one decimal, e.g. |

### Field Summary

| Modifier and Type | Field | Description |
|---|---|---|
| `private final String` | `label` |  |

### Constructor Summary

| Constructor | Description |
|---|---|
| `TimerStyle(String label)` |  |

### Method Summary

| Modifier and Type | Method | Description |
|---|---|---|
| `public String` | `toString()` | Returns the display name shown in the config panel. |

### Enum Constant Detail

#### OFF

`OFF`

No timer.

#### PIE

`PIE`

A pie that drains from full at the knock-out to empty when the target wakes.

#### SECONDS

`SECONDS`

The seconds left, with one decimal, e.g. `2.4`.

### Field Detail

#### label

`private final String label`

### Constructor Detail

#### TimerStyle

`TimerStyle(String label)`

### Method Detail

#### toString

`public String toString()`

Returns the display name shown in the config panel.

- **Returns:** the display name

---

## com.oveduumnakal.betterblackjacking.WakeAnimation

_enum_

`public enum WakeAnimation`

The get-up animation played on the knocked-out target when it wakes, in place of the game's snap
from lying down to standing (`AnimationID#HUMAN_READY`).

<p>Each option carries the animation IDs to play in order; a two-animation option plays the second
once the first has finished. The animation is cosmetic and only shows on this player's screen. The
lengths in each option's Javadoc were measured in the cache viewer.

### Enum Constant Summary

| Enum Constant | Description |
|---|---|
| `CYRISUS_SIT_UP` | Cyrisus's sit up, 1.2 s: unconscious to sitting, then a snap to standing. |
| `CYRISUS_SIT_UP_THEN_STAND` | Cyrisus's sit up followed by his stand up, 3.2 s. |
| `CYRISUS_STAND_UP` | Cyrisus's stand up, 2.0 s: crouching to standing. |
| `MAX_GET_UP` | Max's get up, 1.6 s. |
| `OFF` | No replacement: the game's own snap to standing. |
| `SITUPS_GET_UP` | Situps get up, 0.5 s. |
| `SIT_UP_SHORT` | Sit-up short, 1.9 s. |
| `SIT_UP_THEN_GET_UP` | A sit-up followed by the situps get-up, 1.1 s. |

### Field Summary

| Modifier and Type | Field | Description |
|---|---|---|
| `private final int[]` | `animationIds` |  |
| `private final String` | `label` |  |

### Constructor Summary

| Constructor | Description |
|---|---|
| `WakeAnimation(String label, int... animationIds)` |  |

### Method Summary

| Modifier and Type | Method | Description |
|---|---|---|
| `public int[]` | `animationIds()` | Returns the animation IDs to play, in order. |
| `public String` | `toString()` | Returns the display name shown in the config panel. |

### Enum Constant Detail

#### CYRISUS_SIT_UP

`CYRISUS_SIT_UP`

Cyrisus's sit up, 1.2 s: unconscious to sitting, then a snap to standing.

#### CYRISUS_SIT_UP_THEN_STAND

`CYRISUS_SIT_UP_THEN_STAND`

Cyrisus's sit up followed by his stand up, 3.2 s.

#### CYRISUS_STAND_UP

`CYRISUS_STAND_UP`

Cyrisus's stand up, 2.0 s: crouching to standing.

#### MAX_GET_UP

`MAX_GET_UP`

Max's get up, 1.6 s.

#### OFF

`OFF`

No replacement: the game's own snap to standing.

#### SITUPS_GET_UP

`SITUPS_GET_UP`

Situps get up, 0.5 s.

#### SIT_UP_SHORT

`SIT_UP_SHORT`

Sit-up short, 1.9 s.

#### SIT_UP_THEN_GET_UP

`SIT_UP_THEN_GET_UP`

A sit-up followed by the situps get-up, 1.1 s.

### Field Detail

#### animationIds

`private final int[] animationIds`

#### label

`private final String label`

### Constructor Detail

#### WakeAnimation

`WakeAnimation(String label, int... animationIds)`

### Method Detail

#### animationIds

`public int[] animationIds()`

Returns the animation IDs to play, in order. The array is a copy, so callers may keep or change it.

- **Returns:** the animation IDs in play order; empty for `#OFF`

#### toString

`public String toString()`

Returns the display name shown in the config panel.

- **Returns:** the display name
