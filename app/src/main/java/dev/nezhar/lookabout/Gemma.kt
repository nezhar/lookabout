package dev.nezhar.lookabout

import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import java.io.File

/**
 * Owns the model for the app's lifetime.
 *
 * [load] and [ask] block for seconds: call them from a background coroutine
 * (`Dispatchers.Default`), never the main thread. They run one at a time.
 */
object Gemma {
    /** Logcat tag for everything the model does. */
    const val TAG = "Gemma"

    private var engine: Engine? = null

    /** "GPU" or "CPU", once the model is loaded. */
    @Volatile
    var backendName: String? = null
        private set

    /** Whether speculative decoding (multi-token prediction) is on, once the model is loaded. */
    @Volatile
    var speculativeDecoding: Boolean = false
        private set

    /**
     * Loads the model once; later calls return at once. Tries the GPU with speculative decoding
     * first, then the GPU without it, then the CPU without it, for both the main and the vision
     * backend. Speculative decoding needs the MTP drafter in the model file.
     */
    @Synchronized
    fun load(modelPath: String, cacheDir: String) {
        checkNotMainThread()
        if (engine != null) return
        engine = try {
            start(modelPath, cacheDir, Backend.GPU(), speculative = true)
        } catch (withMtp: Throwable) {
            Log.w(
                TAG,
                "The GPU backend did not start with speculative decoding, trying without",
                withMtp,
            )
            try {
                start(modelPath, cacheDir, Backend.GPU(), speculative = false)
            } catch (gpu: Throwable) {
                Log.w(TAG, "The GPU backend did not start, trying the CPU", gpu)
                start(modelPath, cacheDir, Backend.CPU(), speculative = false)
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    private fun start(
        modelPath: String,
        cacheDir: String,
        backend: Backend,
        speculative: Boolean,
    ): Engine {
        val candidate = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = backend,
                visionBackend = backend,
                cacheDir = cacheDir,
            )
        )
        // initialize() reads this global flag. True makes it fail if the model has no MTP
        // drafter; null leaves the native setting unset, which in 0.17.1 means off.
        ExperimentalFlags.enableSpeculativeDecoding = if (speculative) true else null
        try {
            // A failed initialize() leaves no native engine behind, so there is nothing to close.
            candidate.initialize()
        } finally {
            ExperimentalFlags.enableSpeculativeDecoding = null
        }
        backendName = backend.name
        speculativeDecoding = speculative
        return candidate
    }

    /**
     * Asks one question in a fresh conversation, with thinking off, and returns the reply's text.
     * The image, if any, goes before the text.
     */
    @Synchronized
    fun ask(
        prompt: String,
        imagePath: String? = null,
        temperature: Double = 0.2,
        maxTokens: Int = 64,
        seed: Int = 0,
    ): String {
        checkNotMainThread()
        val loaded = engine ?: error("Model not loaded")
        val config = ConversationConfig(
            samplerConfig = SamplerConfig(
                topK = 10,
                topP = 0.95,
                temperature = temperature,
                seed = seed,
            ),
            // Thinking tokens would count towards maxOutputToken.
            maxOutputToken = maxTokens,
            thinkingConfig = ThinkingConfig(enableThinking = false),
        )
        return loaded.createConversation(config).use { conversation ->
            val reply = if (imagePath == null) {
                conversation.sendMessage(prompt)
            } else {
                conversation.sendMessage(
                    Contents.of(Content.ImageFile(imagePath), Content.Text(prompt))
                )
            }
            reply.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
        }
    }

    private fun checkNotMainThread() {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Model calls must run off the main thread"
        }
    }
}

/**
 * The model file: the app's own files folder if the file is there, otherwise the path used
 * during development.
 */
fun modelPath(context: Context): String {
    val own = File(context.filesDir, "gemma-4-E2B-it.litertlm")
    return if (own.exists()) own.path else "/data/local/tmp/llm/gemma-4-E2B-it.litertlm"
}
