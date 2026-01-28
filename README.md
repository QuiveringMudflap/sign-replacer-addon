# Sign Replacer Addon for Meteor Client

A Meteor Client addon that automatically scans for signs in your vicinity, mines them, and replaces them with custom text.

## Features

- **Sign Scanner**: Automatically detects all signs within configurable range
- **Auto Break & Replace**: Breaks existing signs and places new ones with your custom text
- **Edit Only Mode**: Option to only edit signs without breaking (for servers that allow sign editing)
- **Custom Text**: Set up to 4 lines of custom text for your signs
- **Visual Rendering**: See all detected signs highlighted in-game
- **Smart Detection**: Option to only replace signs that have different text

## Installation

### Method 1: Download Release
1. Download the latest `.jar` from the [Releases](../../releases) page
2. Place the `.jar` file in your `.minecraft/mods` folder
3. Make sure you have Meteor Client installed
4. Launch Minecraft with Fabric

### Method 2: Build from Source
```bash
git clone https://github.com/YOUR_USERNAME/sign-replacer-addon.git
cd sign-replacer-addon
./gradlew build
```
The built jar will be in `build/libs/`

## Usage

1. Open Meteor Client GUI (Right Shift by default)
2. Navigate to the "Sign Replacer" category
3. Find the "Sign Replacer" module
4. Configure your settings:
   - **Range**: How far to scan for signs (1-10 blocks)
   - **Delay**: Ticks between actions (higher = slower but safer)
   - **Mode**: 
     - `BreakAndPlace` - Breaks signs and places new ones
     - `EditOnly` - Tries to edit existing signs (server must allow)
   - **Line 1-4**: Your custom sign text
   - **Only Different**: Only replace signs that don't already have your text
5. Enable the module

## Settings

| Setting | Description | Default |
|---------|-------------|---------|
| Range | Scan radius in blocks | 5 |
| Delay | Ticks between actions | 5 |
| Rotate | Face signs when interacting | true |
| Only Different | Skip signs that already match | true |
| Mode | Break&Place or EditOnly | BreakAndPlace |
| Line 1-4 | Custom text for each line | (empty) |
| Render | Show sign highlights | true |

## Requirements

- Minecraft 1.21+
- Fabric Loader 0.15.0+
- Meteor Client 0.5.8+
- Java 21+

## Building

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/sign-replacer-addon.git

# Navigate to the project
cd sign-replacer-addon

# Build the project
./gradlew build

# The jar will be in build/libs/
```

## License

GPL-3.0 License - See [LICENSE](LICENSE) for details.

## Credits

- Built for [Meteor Client](https://meteorclient.com/)
- Developed for the Order community
