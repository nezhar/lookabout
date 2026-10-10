# Lookabout

Lookabout is an Android app for photo walks. It sets a challenge of 3 to 6 pictures to find on a walk, and checks each photo on the phone with Gemma 4 E2B, offline.

An entry for the Hacktoberfest Open-Source AI Challenge, Week 1: Touch Grass.

## Build

Needs JDK 17 or newer and the Android SDK (platform 37), with `ANDROID_HOME` set.

```sh
./gradlew assembleDebug test
```

The debug APK lands in `app/build/outputs/apk/debug/`. Every push and pull request is also built on GitHub Actions, which keeps the APK as a download on the run.

## Licence

[Apache License 2.0](LICENSE)
