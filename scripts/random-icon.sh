#!/usr/bin/env bash
# Download a random Oxygen Mod icon from the Imgur set.
# Run before building to get a random icon in the JAR.
# Usage: ./gradlew randomIcon build   OR   ./scripts/random-icon.sh && ./gradlew build
# Note: The mod list icon is baked into the JAR—it can't change every time you open the game.
#       Each time you run this and build, the JAR gets a new random icon from the set.

IMAGES=(
  "https://i.imgur.com/QNALeuX.png"
  "https://i.imgur.com/BrKZibT.png"
  "https://i.imgur.com/UcbFvS4.png"
  "https://i.imgur.com/sJgl3LB.png"
  "https://i.imgur.com/TMh1x3A.png"
  "https://i.imgur.com/pnN8A0c.png"
  "https://i.imgur.com/XAGf75D.png"
)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ICON="$ROOT/src/main/resources/assets/sign-replacer-addon/icon.png"

mkdir -p "$(dirname "$ICON")"
idx=$(( RANDOM % ${#IMAGES[@]} ))
url="${IMAGES[$idx]}"
echo "Using icon $((idx+1))/${#IMAGES[@]}: $url"
curl -sL -o "$ICON" "$url"
echo "Saved to $ICON"
