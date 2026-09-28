# Better Blackjacking — JavaDoc Reference

<!-- GENERATED FILE — DO NOT EDIT BY HAND.
     Run `./gradlew generateJavaDocs` and commit the result. -->

## Contents

- [com.oveduumnakal.betterblackjacking.BetterBlackjackingPlugin](#comoveduumnakalbetterblackjackingbetterblackjackingplugin)
- [com.oveduumnakal.betterblackjacking.Changelog](#comoveduumnakalbetterblackjackingchangelog)
- [com.oveduumnakal.betterblackjacking.Changelog.Release](#comoveduumnakalbetterblackjackingchangelogrelease)

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
