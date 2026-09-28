<!--
Better Blackjacking changelog. Newest release first. Each release is a top-level heading
"# <version> - <written-out date>" followed by a Quick Overview, a Detailed
Breakdown (features grouped by area, each with the issues that make it up), and
Bug Fixes. The build fails if the top entry's version does not match
runelite-plugin.properties (see ChangelogGuardTest), so a version bump forces a
new entry before anything can ship. Order features within a section by user impact.
Bug Fixes lists only bugs that shipped in a previous release; bugs introduced and
fixed within the same release cycle are omitted, since users never saw them.
-->

# 1.0 - September 27 2026

## Quick Overview

Better Blackjacking 1.0 is the first release. Equip a blackjack in Pollnivneach and each bandit or Menaphite thug you have the Thieving level for gets its click box outlined in a colour that says what to do next: green to pickpocket, yellow to stop and get ready, orange to knock out, and red when you're being attacked and have to break combat first. While your target is knocked out, a smooth timer on its tile counts down to the moment it wakes, and two pips underneath time the two guaranteed pickpockets, turning to STOP when no more will land. When the target wakes, it climbs to its feet with a get-up animation instead of snapping upright. Everything is sized to the target's tile, every colour and style can be changed, and the plugin only draws on your screen: it never clicks, swaps menus or changes your input.

## Detailed Breakdown

### Colour-coded Click Boxes

#### Outlines that tell you what to do next
Each target's click box is outlined and lightly filled in the colour of its state: green while another pickpocket will land, yellow once none will, orange when the target is awake, and red when it is attacking you. The plugin follows the knock-out from the game's own messages, overhead text and animations, so the colour changes on the right tick.
[#9](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/9), [#5](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/5), [#8](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/8)

#### Red means break combat first
A target turns red when your knock-out glances off, when it hits you, or when the game says you can't knock out or pickpocket during combat, and goes back to orange once it has stopped attacking you.
[#5](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/5)

#### Only where it helps
The plugin is active only in Pollnivneach with one of the nine blackjacks equipped, and it draws only the bearded bandits (Thieving 45), clean-shaven bandits (55) and Menaphite Thugs (65) your boosted Thieving level can pickpocket.
[#3](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/3), [#4](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/4)

### Knock-out Timer

#### A smooth countdown to the next knock-out
While the target is knocked out, a pie or the seconds left count down on its tile to the moment it wakes, moving smoothly between game ticks rather than jumping once a tick. It is drawn in the target's state colour and sized from its tile, so it scales with your zoom and stays readable. It can also be turned off.
[#10](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/10), [#6](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/6)

### Pickpocket Pips

#### Two guaranteed pickpockets, timed
Two pips under the timer show how many guaranteed pickpockets you can still fit in before the target wakes (●● or ●○). When there's no time for another, both go hollow and STOP appears in the waking colour.
[#11](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/11), [#5](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/5)

### Wake-up Animation

#### A get-up instead of a snap
When a knocked-out target wakes, it plays Max's get-up animation instead of snapping from the ground to standing. It is only on your screen and changes nothing in the game; set Wake-up animation to Off to keep the snap.
[#13](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/13)

### Settings

#### Make it your own
Change the outline width and fill, all four state colours, the timer style (Pie, Seconds or Off), whether the pips show, and the wake-up animation. An Advanced section, closed by default, holds the knock-out duration for if the game ever changes it.
[#2](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/2)

#### Debug logging for bug reports
Turn on Debug logging in the Advanced section to write each knock-out, pickpocket and wake-up to the client log, tagged `[BetterBlackjacking]`, so a problem can be replayed exactly from a bug report.
[#12](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/12)

### Look and Feel

#### A logo and banner
Better Blackjacking has its own icon in the plugin list: two playing cards with a wooden blackjack across them. Its GitHub page opens with a matching banner.
[#7](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/7), [#29](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/29)
