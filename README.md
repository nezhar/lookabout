# Lookabout

Lookabout is an Android app for photo walks. It sets a challenge of 3 to 6 pictures to find on a walk, and checks each photo on the phone with Gemma 4 E2B, offline. It is for anyone who wants a reason to go outside and look around more closely.

An entry for the Hacktoberfest Open-Source AI Challenge, Week 1: Touch Grass.

## How it works

1. **Setup**: pick the number of pictures, a topic and what the sky is doing.
2. **List**: the pictures to find, all shown up front. Draw a new list or start the walk.
3. **Walk**: one picture at a time. Take a photo with the camera app, and Gemma answers four yes/no questions about it. Two or more YES answers and the photo is accepted; otherwise retake it or skip.
4. **Summary**: how the walk went.

Everything runs on the phone. The app has no INTERNET permission.

## Build

Needs JDK 17 or newer and the Android SDK (platform 37), with `ANDROID_HOME` set. Open the project in Android Studio, or build from the command line:

```sh
./gradlew assembleDebug test
```

The debug APK lands in `app/build/outputs/apk/debug/`. Every push and pull request is also built on GitHub Actions, which keeps the APK as a download on the run (`lookabout-debug-apk`).

The app needs a real phone on Android 12 or later (minimum SDK 31).

## Put the model on the phone

Download the single file `gemma-4-E2B-it.litertlm` (2.58 GB) from <https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm>, then, with the phone connected over USB debugging, run:

```text
adb shell mkdir -p /data/local/tmp/llm
adb push gemma-4-E2B-it.litertlm /data/local/tmp/llm/
```

The app first looks for `gemma-4-E2B-it.litertlm` in its own files folder, then in `/data/local/tmp/llm/`. Never commit the model file to the repository.

## Debug screens

A long press on the Lookabout title on Setup opens the debug screen, with a model test and the photo check.

## Licence

[Apache License 2.0](LICENSE)

## Credits

- [Gemma 4](https://ai.google.dev/gemma) by Google, the model that checks the photos, under Apache-2.0.
- [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) by Google, which runs the model on the phone, under Apache-2.0.
- [Coil](https://github.com/coil-kt/coil), for showing photos, under Apache-2.0.
- [Jetpack Compose and AndroidX](https://developer.android.com/jetpack), under Apache-2.0.

## Changes after the deadline
