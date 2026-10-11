package dev.nezhar.lookabout

import java.io.File

/** The models the debug screen offers, with their file names. E2B is the default. */
enum class ModelChoice(val label: String, val fileName: String) {
    E2B("Gemma 4 E2B", MODEL_FILE_NAME),
    E4B("Gemma 4 E4B", "gemma-4-E4B-it.litertlm"),
}

/** The folder a model file is pushed to during development. */
const val DEV_MODEL_DIR = "/data/local/tmp/llm"

/**
 * The least `ActivityManager.MemoryInfo.totalMem` for which E4B is offered: 11 GiB. The Gallery
 * asks for 12 GB, but `totalMem` leaves out memory the kernel keeps for itself, so a 12 GB phone
 * reports roughly 11.2 to 11.6 GiB; an 8 GB phone stays well below 11 GiB.
 */
const val E4B_MIN_TOTAL_MEMORY_BYTES = 11L * 1024 * 1024 * 1024

/** True when a phone with [totalMemBytes] of memory may choose E4B. */
fun offersModelChoice(totalMemBytes: Long): Boolean = totalMemBytes >= E4B_MIN_TOTAL_MEMORY_BYTES

/** The choice saved under [key]; anything unknown, or a missing key, is E2B. */
fun modelChoiceFromKey(key: String?): ModelChoice =
    ModelChoice.entries.firstOrNull { it.name == key } ?: ModelChoice.E2B

/**
 * The model to use: the [saved] choice on a phone that [offersChoice], E2B otherwise. A choice
 * other than E2B whose file is not [found] falls back to E2B too.
 */
fun usableModelChoice(
    saved: ModelChoice,
    offersChoice: Boolean,
    found: (ModelChoice) -> Boolean,
): ModelChoice = when {
    !offersChoice || saved == ModelChoice.E2B -> ModelChoice.E2B
    found(saved) -> saved
    else -> ModelChoice.E2B
}

/** [choice]'s file: in [filesDir] when it is there, else in [devDir], where it is pushed with adb. */
fun modelFile(choice: ModelChoice, filesDir: File, devDir: File = File(DEV_MODEL_DIR)): File {
    val own = File(filesDir, choice.fileName)
    return if (own.exists()) own else File(devDir, choice.fileName)
}

/** The model's name for the model file at [path]: the choice's label, or the file name. */
fun modelLabel(path: String): String {
    val name = File(path).name
    return ModelChoice.entries.firstOrNull { it.fileName == name }?.label ?: name
}

/** What the debug screen says when [choice]'s file is not on the phone. */
fun missingModelText(choice: ModelChoice): String =
    "${choice.label} is not on the phone: push ${choice.fileName} to $DEV_MODEL_DIR/. " +
        "Staying on ${ModelChoice.E2B.label}."
