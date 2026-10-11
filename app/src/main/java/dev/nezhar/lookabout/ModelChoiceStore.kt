package dev.nezhar.lookabout

import android.app.ActivityManager
import android.content.Context
import java.io.File

private const val MODEL_PREFS = "model_choice"
private const val MODEL_CHOICE_KEY = "choice"

/** The phone's memory as Android reports it (`MemoryInfo.totalMem`), in bytes. */
fun totalMemoryBytes(context: Context): Long {
    val manager = context.getSystemService(ActivityManager::class.java) ?: return 0
    val info = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(info)
    return info.totalMem
}

/** True when this phone has the memory for E4B, so the debug screen offers the choice. */
fun offersModelChoice(context: Context): Boolean = offersModelChoice(totalMemoryBytes(context))

/** The model chosen on the debug screen; E2B on a phone that does not offer the choice. */
fun savedModelChoice(context: Context): ModelChoice {
    if (!offersModelChoice(context)) return ModelChoice.E2B
    val prefs = context.getSharedPreferences(MODEL_PREFS, Context.MODE_PRIVATE)
    return modelChoiceFromKey(prefs.getString(MODEL_CHOICE_KEY, null))
}

/** Remembers [choice] across restarts. Writes to disk: call it off the main thread. */
fun saveModelChoice(context: Context, choice: ModelChoice) {
    context.getSharedPreferences(MODEL_PREFS, Context.MODE_PRIVATE)
        .edit().putString(MODEL_CHOICE_KEY, choice.name).commit()
}

/** [choice]'s file: the app's own files folder if it is there, otherwise [DEV_MODEL_DIR]. */
fun modelFile(context: Context, choice: ModelChoice): File = modelFile(choice, context.filesDir)

/** The model to use now: the saved choice if its file is on the phone, else E2B. */
fun usableModelChoice(context: Context): ModelChoice =
    usableModelChoice(savedModelChoice(context), offersModelChoice(context)) { choice ->
        modelFile(context, choice).exists()
    }
