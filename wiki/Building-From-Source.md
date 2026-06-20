# Building From Source

Standard **Fabric + Gradle (Loom)** project — no separate Gradle install needed, the
wrapper handles it.

## Prerequisites

- **JDK 25** (Loom auto-provisions it via the Foojay resolver in `settings.gradle`;
  locally you can point `org.gradle.java.installations.paths` in
  `~/.gradle/gradle.properties` at a JDK 25).
- **Git**
- Internet access for the first build.

## Build

```bash
git clone https://github.com/MilkdromedaStudios/Nexus-Minecraft-AI.git
cd Nexus-Minecraft-AI
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows
```

Output: `build/libs/blockpal-<version>.jar`

## Dev tasks

```bash
./gradlew runClient   # dev client with the mod loaded
./gradlew runServer   # dev server
./gradlew clean       # wipe build/ for a fresh rebuild
```

Always run a real `./gradlew clean build` before committing a jar.

## Where versions live

| File | Holds |
|------|-------|
| `gradle.properties` | Minecraft, Fabric Loader, Fabric API, Loom, `mod_version` |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle itself |

## Build artifacts → `builds/`

Tested jars are copied into the repo's `builds/` folder so they're available without
compiling. History is kept — every released `mod_version` keeps its own
`builds/blockpal-<version>.jar`; old builds are never deleted. (`builds/` is not
gitignored; only `build/` is.)

## Releasing to Modrinth

The **Release to Modrinth** workflow (`.github/workflows/release.yml`) runs on
**every pull request**, on a `v*` tag push, and on a manual dispatch. It builds
the mod, renames the jar to
`Blockpal-<mod_version>-<minecraft_version>.jar` (e.g. `Blockpal-3.1.0-26.2.jar`)
and uploads it to Modrinth via `Kir-Antipov/mc-publish`.

It is **idempotent**: before uploading it asks Modrinth whether
`<mod_version>+mc<minecraft_version>` already exists and, if so, skips the
publish. So running on every PR only ever uploads a given version once — bump
`mod_version` in `gradle.properties` to ship a new one. (Publishing is also
skipped automatically on fork PRs, where the secrets aren't available.)

One-time setup (repo **Settings ▸ Secrets and variables ▸ Actions**):

| Kind | Name | Value |
|------|------|-------|
| Secret | `MODRINTH_TOKEN` | a Modrinth PAT with the *Create versions* scope |
| Variable | `MODRINTH_PROJECT_ID` | the Modrinth project's ID or slug |

```bash
git tag v3.1.0
git push origin v3.1.0   # triggers the release
```
</content>
