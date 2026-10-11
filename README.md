# Lookabout

Lookabout is an Android app for photo walks. It sets a challenge of 3 to 6 pictures to find on a walk, and checks each photo on the phone with Gemma 4 E2B, offline. It is for anyone who wants a reason to go outside and look around more closely.

An entry for the Hacktoberfest Open-Source AI Challenge, Week 1: Touch Grass.

## How it works

1. **Setup**: pick the number of pictures, a topic and what the sky is doing. On first start, Setup offers to download the model (2.6 GB, Wi-Fi only, once); the progress shows there and the download can be cancelled. Under the sky choice, **Use my location for the light** asks once for your rough location. Setup also has a **Check photos at the end** switch (see below).
2. **List**: Gemma writes the pictures to find from the topic, the sky, the time of day and the month, each with four yes/no questions. If you allowed location, the prompt also gets a line saying where the sun is, such as "low in the south-west", worked out on the phone from the last known coarse location; the location is not stored. Any gap is filled from a built-in list, which is also used alone when the model is not loaded. All pictures are shown up front. Ask for a new list or start the walk.
3. **Walk**: one picture at a time. Take a photo with the camera app, and Gemma answers the four questions about it. Two or more YES answers and the photo is accepted; otherwise retake it or skip. The phone buzzes with the verdict: one long pulse for Accepted, two short ones for Not yet. Tap a question to change Gemma's answer if it got it wrong (tap again to restore it); the verdict follows. Each photo is also copied to the phone's gallery, in `Pictures/Lookabout`, so you can edit and share it from there.
4. **Summary**: the topic, the date and how many pictures were accepted, then each picture with its photo, its coverage (the share of YES answers) and how long the check took. A line says how often you and Gemma agreed, counting the answers you left unchanged.

With **Check photos at the end** on, the walk does not wait for Gemma: take a photo, go to the next picture, and at **Finish walk** every photo is checked one after another ("Checking 2 of 5"). If the model is not ready yet, the check waits for it ("Getting the model ready") and starts by itself once it is. A model that failed to load is tried once more; if it is not on the phone or still fails, **Show results now** opens Summary without the check. A photo whose check fails shows **Not checked** on Summary, where **Check again** checks those photos once more.

During a walk, a silent notice, also shown on the lock screen, shows the current picture and its position, such as "2 of 5". Tapping it opens the walk, and the camera when the picture still needs its photo. Start walk asks once for permission to post notifications (Android 13 and later); without it there is no notice.

The walk is saved on the phone after every change, so it survives Android closing the app while the camera is open: the app opens where you left off and checks a photo taken in the meantime. With **Check photos at the end** on, that photo is checked at **Finish walk** with the others instead, and a walk closed during the check at the end picks it up again.

Everything runs on the phone. Downloading the model is the only time the app goes online; the lists, the photos and the photo checks never leave the phone.

## Permissions

- **Internet** (`INTERNET`): to download the model, once, over Wi-Fi.
- **Vibration** (`VIBRATE`): to buzz the verdict when a photo is checked during the walk.
- **Approximate location** (`ACCESS_COARSE_LOCATION`, optional, asked from Setup): to work out where the sun is for the list. Without it the list is written without the sun line.
- **Notifications** (`POST_NOTIFICATIONS`, optional, asked once at Start walk on Android 13 and later): for the lock-screen notice during a walk.

The camera app takes the photos, so Lookabout needs no camera permission, and copying photos to `Pictures/Lookabout` needs no storage permission.

## Build

Needs JDK 17 or newer and the Android SDK (platform 37), with `ANDROID_HOME` set. Open the project in Android Studio, or build from the command line:

```sh
./gradlew assembleDebug test
```

The debug APK lands in `app/build/outputs/apk/debug/`. Every push to `main` and every pull request is also built on GitHub Actions, which keeps the APK as a download on the run (`lookabout-debug-apk`).

The app needs a real phone on Android 12 or later (minimum SDK 31).

## Put the model on the phone

The app downloads the model itself: without it, Setup offers **Download the model (2.6 GB)**. The download runs through Android's download manager on Wi-Fi only, shows a notification and keeps going if the app is closed. When it is done, the file moves into the app's own files folder and the model loads.

During development you can push it instead. Download the single file `gemma-4-E2B-it.litertlm` (2.58 GB) from <https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm>, then, with the phone connected over USB debugging, run:

```text
adb shell mkdir -p /data/local/tmp/llm
adb push gemma-4-E2B-it.litertlm /data/local/tmp/llm/
```

The app first looks for `gemma-4-E2B-it.litertlm` in its own files folder, then in `/data/local/tmp/llm/`. Never commit the model file to the repository.

On phones with 12 GB of memory or more, the debug screen also offers the larger Gemma 4 E4B: push `gemma-4-E4B-it.litertlm` (3.66 GB) from <https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm> with the same commands (`adb push gemma-4-E4B-it.litertlm /data/local/tmp/llm/`). The in-app download is E2B only.

## Debug screens

A long press on the Lookabout title on Setup opens the debug screen, with a model test and the photo check. The model loads on the GPU with speculative decoding (multi-token prediction) when it can, else on the GPU without it, else on the CPU; the model test shows the backend and whether speculative decoding is on. On phones with 12 GB of memory or more (Android reports at least 11 GiB) it also lets you choose between Gemma 4 E2B (the default) and Gemma 4 E4B; the choice is remembered, and if the E4B file is missing the app says so and stays on E2B.

## Licence

[Apache License 2.0](LICENSE)

## Credits

- [Gemma 4](https://ai.google.dev/gemma) by Google, the model that checks the photos, under Apache-2.0.
- [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) by Google, which runs the model on the phone, under Apache-2.0.
- [Coil](https://github.com/coil-kt/coil), for showing photos, under Apache-2.0.
- [Jetpack Compose and AndroidX](https://developer.android.com/jetpack), under Apache-2.0.

## Changes after the deadline
