# Build and repo workflow

- **After any code change:** run `./gradlew build` (builds JAR and deploys to Prism Launcher mods folder) and then push to the repo (e.g. `./gradlew pushRepo`), or just `./gradlew pushRepo` (it depends on build).
- **When making a full / notable update** (new features, bigger behavior changes): bump **mod_version** in `gradle.properties` (e.g. `1.1.0` → `1.2.0`) before building and pushing, and say in the commit or release that it’s a new version.

Current version is in `gradle.properties`: `mod_version=1.1.0`.
