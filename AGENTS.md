# Working on Lookabout

Android app, Kotlin and Jetpack Compose, one module (`app`). Package and application ID: `dev.nezhar.lookabout`. minSdk 31, compileSdk 37 (Compose needs it), targetSdk 36.

## Building

- GitHub Actions runs `./gradlew assembleDebug test` on every push and pull request (`.github/workflows/build.yml`). That run is the check for a task's build criteria.
- The VibePod overlay (`.vibepod/overlay/`) gives agent containers a JDK and the Android SDK, so the same command works locally. The machine is short on memory: build one task at a time, and prefer `./gradlew :app:compileDebugKotlin` or `./gradlew test` for quick checks.
- The model file (`*.litertlm`) never goes into the repository.

## Layout

- `app/src/main/java/dev/nezhar/lookabout/`: app code. Keep UI under `ui/`, put logic that needs no Android classes in plain Kotlin files so it can be unit tested.
- `app/src/test/`: JVM unit tests, run by `./gradlew test`.
- `gradle/libs.versions.toml`: all dependency versions. Add a dependency there, with a fixed version, never `latest.release` or `+`.
- `design/`: the designer's hand-over (values, icon, screens), when it lands.

## Rules

- Stay inside the task. Don't upgrade AGP, Kotlin, Gradle or the Compose BOM unless the task says so; parallel tasks would conflict.
- Every call into the model runs off the main thread.
