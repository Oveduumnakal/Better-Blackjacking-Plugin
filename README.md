<p align="center">
  <img src="banner.png" alt="Better Blackjacking banner: a thief with a blackjack pickpocketing a knocked-out bandit in a desert town">
</p>
<h1 align="center">Better Blackjacking</h1>

Better Blackjacking is a RuneLite plugin for blackjacking bandits and Menaphite thugs in Pollnivneach. With a blackjack equipped, it outlines your target's click box in a colour that tells you what to do next — pickpocket, get ready, knock out, or break combat — shows a smooth countdown to the next knock-out, times the two guaranteed pickpockets with a pair of pips, and can play a get-up animation when the target wakes instead of letting it snap to its feet. It only draws on screen: it never clicks, swaps menus or changes your input.

## Features

### When it's active

- **Only in Pollnivneach, only with a blackjack**

  The plugin switches itself on when you're in Pollnivneach with a blackjack in your weapon slot. Any of the nine blackjacks counts: oak, willow or maple, plain, offensive or defensive. Walk out of town or take the blackjack off and everything disappears, and the plugin forgets the knock-out it was timing.

- **Only targets you can thieve from**

  A target is drawn only if your Thieving level is high enough to pickpocket it. The plugin checks your boosted level, so a Thieving boost counts.

  | Target | Where | Thieving level |
  |---|---|---|
  | Bandit (bearded) | Northern Pollnivneach | 45 |
  | Bandit (clean-shaven) | Northern Pollnivneach | 55 |
  | Menaphite Thug | Southern Pollnivneach | 65 |

  Villagers are left out on purpose: they give no experience once you've finished The Feud. The plugin doesn't check your quest progress, so you still need to be far enough into The Feud to blackjack.

### Colour-coded click boxes

- **The colour tells you what to do next**

  Each target's click box is outlined, and lightly filled, in the colour of its state. There's no need to count ticks: follow the colour.

  | Colour | State | What to do |
  |---|---|---|
  | Green | Safe to pickpocket | The target is knocked out and another pickpocket will land. Pickpocket now. |
  | Yellow | Waking up | The target is still down, but a pickpocket now won't land before it wakes. Stop pickpocketing and get ready to knock it out. |
  | Orange | Knock out | The target is awake. Knock it out. |
  | Red | Attacking | You're being attacked and can't knock out. Break combat first, for example by swapping weapons, then knock out. |

  A target turns red when your knock-out glances off, when it says "I'll kill you for that!", when it hits you, or when the game tells you that you can't knock out or pickpocket during combat. It goes back to orange once it has stopped attacking you for a couple of ticks. All four colours, the outline width and the fill can be changed in the settings.

  <!-- GIF: docs/img/01-states.gif -->

### Knock-out timer

- **A countdown to the next knock-out**

  While your target is knocked out, a timer on its tile counts down to the moment it wakes, which is when your next knock-out can go in. Choose a pie that drains from full to empty, the seconds left (for example `2.4`), or no timer at all. The timer is drawn in the target's state colour, so it turns from green to yellow when it's time to stop pickpocketing.

- **Smooth, and sized to the target**

  The countdown moves smoothly between game ticks instead of jumping once every 0.6 seconds. It's sized from the target's tile on screen, so it shrinks as you zoom out and grows as you zoom in, but it never gets too small to read or too big to see past.

  <!-- GIF: docs/img/02-timer.gif -->

### Pickpocket pips

- **Two pickpockets per knock-out, timed for you**

  Two pips under the timer show how many guaranteed pickpockets you can still fit in before the target wakes: ●● means two, ●○ means one. When there's no time for another, both pips go hollow and the word STOP appears in the waking colour. The pips are sized from the target's tile, like the timer, and can be turned off.

  <!-- GIF: docs/img/03-pips.gif -->

### Wake-up animation

- **A get-up instead of a snap**

  When a knocked-out target wakes, the game snaps it straight from lying on the ground to standing. The plugin plays Max's get-up animation in its place, so you see the target climb to its feet. It's purely visual and only shown on your screen: the target is awake the moment the animation starts, and its outline turns orange straight away. Set the Wake-up animation to Off to keep the game's own snap.

  <!-- GIF: docs/img/04-wake-animation.gif -->

### Settings

- **Every setting and its default**

  | Section | Setting | Default | What it does |
  |---|---|---|---|
  | Outline | Outline width | 2 | The width of the outline around the click box, from 1 to 6 pixels |
  | Outline | Fill opacity | 20 | How solid the fill inside the click box is, from 0 (none) to 255 (solid) |
  | Colours | Safe to pickpocket | Green | The colour when another pickpocket will land |
  | Colours | Waking up | Yellow | The colour when no more pickpockets will land; STOP uses it too |
  | Colours | Knock out | Orange | The colour when the target is awake and can be knocked out |
  | Colours | Attacking | Red | The colour when you're being attacked and can't knock out |
  | Timer | Timer style | Pie | Pie, Seconds or Off |
  | Pickpockets | Show pickpocket pips | On | Shows the pips, and STOP when no more pickpockets will land |
  | Animation | Wake-up animation | Max get up | Max get up, or Off for the game's snap to standing |
  | Advanced | Knock-out duration | 5 ticks | How many game ticks a knocked-out target stays down, from 3 to 8. Only change this if the game does. |
  | Advanced | Debug logging | Off | Writes knock-out, pickpocket and wake-up events to the client log |

  The Advanced section is closed by default.

### Debug logging

- **Help with bug reports**

  If the colours or the timer ever get out of step with the game, turn on Debug logging in the Advanced section, play until it happens again, and attach the lines tagged `[BetterBlackjacking]` from your RuneLite client log (`.runelite/logs/client.log`) to a [bug report](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/new?template=bug_report.yml). Each line records the game tick and how many ticks have passed since the last knock-out, so the problem can be replayed and fixed. Leave it off the rest of the time.

### Visual only

- **It never plays for you**

  Better Blackjacking only draws on your screen. It never clicks, never swaps or reorders menu options, and never changes your input: every knock-out and pickpocket is still yours to click. The wake-up animation changes what you see, not what the game does.

## Development

- **Run the client:** `./gradlew run` starts RuneLite with Better Blackjacking and a dev-only preview recorder loaded. The recorder is never shipped with the plugin.
- **Record the preview frames:** a few ticks after you log in, the recorder records any wake-up animation clips that aren't on disk yet, into `.runelite/better-blackjacking-previews/`. Type `::bbpreview` in the chat box to record them all again. Stand still and leave the camera alone while it runs.
- **Make the GIFs:** `python scripts/make-preview-gifs.py OUTPUT_DIR` turns the recorded frames into labelled GIFs. It needs Pillow (`pip install Pillow`).

## Links

- [Report a bug](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/Oveduumnakal/Better-Blackjacking-Plugin/issues/new?template=feature_request.yml)

## Support

If Better Blackjacking saves you a few misclicks, you can [buy me a coffee](https://buymeacoffee.com/oveduumnakal).
