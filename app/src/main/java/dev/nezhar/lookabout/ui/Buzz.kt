package dev.nezhar.lookabout.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/** Buzzes a verdict: one 400 ms pulse for Accepted, two 120 ms pulses 100 ms apart for Not yet. */
fun buzzVerdict(context: Context, accepted: Boolean) {
    val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
    if (!vibrator.hasVibrator()) return
    val effect = if (accepted) {
        VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
    } else {
        VibrationEffect.createWaveform(longArrayOf(0, 120, 100, 120), -1)
    }
    vibrator.vibrate(effect)
}
