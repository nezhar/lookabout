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
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import java.io.File

/** Owns the model for the app's lifetime. Call from a worker thread. */
object Gemma {
    private var engine: Engine? = null

    @Volatile
    var backendName: String? = null
        private set

    @Synchronized
    fun load(modelPath: String, cacheDir: String) {
        checkWorkerThread()
        if (engine != null) return
        engine = try {
            start(modelPath, cacheDir, Backend.GPU())
        } catch (t: Throwable) {
            Log.w("Gemma", "The GPU backend did not start, trying the CPU", t)
            start(modelPath, cacheDir, Backend.CPU())
        }
    }

    private fun start(modelPath: String, cacheDir: String, backend: Backend): Engine {
        val candidate = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = backend,
                visionBackend = backend,
                cacheDir = cacheDir,
            )
        )
        try {
            candidate.initialize()
        } catch (t: Throwable) {
            try {
                candidate.close()
            } catch (closeFailure: Throwable) {
                t.addSuppressed(closeFailure)
            }
            throw t
        }
        backendName = backend.name
        return candidate
    }

    @Synchronized
    fun ask(
        prompt: String,
        imagePath: String? = null,
        temperature: Double = 0.2,
        maxTokens: Int = 64,
        seed: Int = 0,
    ): String {
        checkWorkerThread()
        val loaded = engine ?: error("Model not loaded")
        val config = ConversationConfig(
            samplerConfig = SamplerConfig(
                topK = 10, topP = 0.95, temperature = temperature, seed = seed,
            ),
            thinkingConfig = ThinkingConfig(enableThinking = false),
            maxOutputToken = maxTokens,
        )
        return loaded.createConversation(config).use { conversation ->
            val reply = if (imagePath == null) {
                conversation.sendMessage(prompt)
            } else {
                conversation.sendMessage(Contents.of(Content.ImageFile(imagePath), Content.Text(prompt)))
            }
            reply.toString()
        }
    }

    private fun checkWorkerThread() {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Model calls must run off the main thread"
        }
    }
}

fun modelPath(context: Context): String {
    val own = File(context.filesDir, "gemma-4-E2B-it.litertlm")
    return if (own.exists()) own.path else "/data/local/tmp/llm/gemma-4-E2B-it.litertlm"
}
