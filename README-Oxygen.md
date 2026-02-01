# Oxygen Mod – current features (Oxygen / 2b2t addon)

This file describes the **current** behavior of the mod. It is kept separate from the main README so it can be reverted or merged independently. The main README.md is unchanged.

---

## What this mod is

**Oxygen Mod** (Meteor Client addon) – Sign replacer, custom title-screen splashes, and credits for 2b2t / Void Supply.

- **Sign Replacer**: Place new signs with custom text (place-only mode by default), or optionally scan for existing signs, break them, and replace.
- **Title screen**: Custom yellow splash texts (e.g. "Oxygen is love, Oxygen is life", "discord.gg/shop2b2t"); Meteor Client splash replaced with shop link; QuiveringMudflap credit in gold in the mod list.

---

## Sign Replacer – current settings

| Setting | Description | Default |
|--------|-------------|---------|
| **range** | Scan/placement range (blocks) | 100 |
| **place-only** | Only place new signs (no breaking) | true |
| **place-pitch** | Look-down angle in place-only (degrees) | 70 |
| **placement-cooldown** | Ticks to wait after placing before next scan (reduces lag) | 10 |
| **scan-interval** | In place-only, run scan every N ticks | 2 |
| **give-up-ticks** | Skip current target after this many ticks | 3000 |
| **pickup-range** | Walk to dropped sign within this (blocks) | 2.0 |
| **delay** | Ticks between actions | 5 |
| **rotate** | Rotate towards sign when interacting | true |
| **only-different** | Only replace signs with different text | true |
| **auto-walk** | Walk towards out-of-reach signs | true |
| **Line 1–4** | Custom sign text | #1 FASTEST / DELIVERY ON 2B2T / KITS & GEAR / -> .gg/shop2b2t |
| **render** | Draw sign boxes in world | true |
| **shape-mode, colors** | How boxes are drawn | See in-game |

When **place-only** is off and **range** > 32, scanning uses a **layered scan** (one Y layer per tick) to avoid big FPS drops.

---

## Splash texts

- Custom Oxygen splashes (yellow with outline): e.g. VOID, Oxygen is love, discord.gg/shop2b2t, QuiveringMudflap based god, etc.
- **meteorclient.com** (and www.meteorclient.com) are replaced with **discord.gg/shop2b2t**.
- **MiniGame159 based god** is replaced with **QuiveringMudflap based god**.

---

## Build and deploy

From `sign-replacer-addon/`:

```bash
./gradlew build
```

The JAR is built and **automatically copied** to the Prism Launcher mods folder (see `build.gradle` – `deployToMods`).

---

## Reverting the “additions”

- **Icon**: Delete `src/main/resources/assets/sign-replacer-addon/icon.png` and remove the `icon` entry from `fabric.mod.json`.
- **Layered scan**: In `SignReplacer.java`, remove the `r > 32` branch in `scanForSigns()` and remove the methods `scanForSignsLayered()`, `scanForSignsFull()`, and `finishScanAndPickTarget()`; restore the original single-method full 3D scan in `scanForSigns()`.
- **This doc**: Delete `README-Oxygen.md`; the main README is unchanged.
