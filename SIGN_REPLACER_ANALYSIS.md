# Sign Replacer – Problem Analysis

## Minecraft mechanics (vanilla)

- **Breaking a sign** always drops **one sign item** of the same wood type (e.g. Birch Sign block → Birch Sign item). Standing vs wall only changes block state; the item is the same (`SignBlock` and `WallSignBlock` both use the same item, e.g. `Items.BIRCH_SIGN`).
- So after breaking a birch/pale oak sign, the only way to “place it back” correctly is to use that **exact** item from inventory (the one we just picked up, or the same type).

## What was going wrong

### 1. Missing wood type (PALE_OAK_SIGN) – **certain**

- `isSignItem()` and `findSign()` did not include `Items.PALE_OAK_SIGN`.
- So:
  - `findDroppedSignEntity()` ignored pale oak drops (filter uses `isSignItem`).
  - `findSign()` never considered a pale oak sign in inventory.
- Result: break pale oak → we never “see” the drop or count as having a sign → we never go to Placing → **“it just mines it”**. Adding `PALE_OAK_SIGN` was required for pale oak to work at all.

### 2. Wrong sign type when placing – **very likely**

- `InvUtils.find()` returns the **first** matching stack (slot order: hotbar then inventory).
- If you have **oak in slot 0** and break a **birch** sign, the birch drop goes to another slot. Without preferring type, `findSign()` can return **oak** first → we place **oak** instead of birch.
- So we “mine” birch but “place” oak → looks like “only placing some” or “birch just gets mined.” Storing `preferredSignItem` from the block we broke and preferring it in `findSign()` fixes this.

### 3. 2b2t lag – **possible extra cause**

- 2b2t often runs below 20 TPS; block breaks and **item entity spawns** can be delayed.
- In `tickPathingToDrop()`: when **dropEntity == null** (drop not visible yet) but **haveSign** is true (any sign in inventory, e.g. oak), we **immediately** go to Placing.
- So: we break birch → server is slow → we don’t see the birch drop yet → we already have oak in hotbar → we go to Placing and place **oak**. So we “only place some” (wrong type) or it feels like “it just mines” the birch.
- Fix: when we are **replacing** a sign (`preferredSignItem != null`), only go to Placing when we have **that** type (the one we broke), not any sign. So we wait for the actual drop (or timeout) instead of placing a different wood type.

## Summary

| Cause | Fix | Certainty |
|-------|-----|-----------|
| Pale oak not in sign list | Add `PALE_OAK_SIGN` to `isSignItem` / `findSign` | 100% |
| Placing first sign in inv (wrong type) | Prefer `preferredSignItem` in `findSign` | Very high |
| Going to Placing with “any” sign before picking drop (2b2t lag) | When `preferredSignItem != null`, only allow Placing if we have that type | High |

The code was updated for (1) and (2). (3) is fixed in the same change set by only allowing Placing when we have the preferred sign type when replacing.
