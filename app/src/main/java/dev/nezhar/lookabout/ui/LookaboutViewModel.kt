package dev.nezhar.lookabout.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nezhar.lookabout.ChallengeList
import dev.nezhar.lookabout.Gemma
import dev.nezhar.lookabout.Screen
import dev.nezhar.lookabout.SetupChoices
import dev.nezhar.lookabout.backTarget
import dev.nezhar.lookabout.listPrompt
import dev.nezhar.lookabout.modelPath
import dev.nezhar.lookabout.monthName
import dev.nezhar.lookabout.timeOfDay
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.writeList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** Where loading the model stands. */
sealed interface ModelState {
    data object Loading : ModelState
    data object Ready : ModelState
    data class Failed(val message: String) : ModelState
}

/**
 * The app's state: the screen, the Setup choices, the current list and walk, and the model's
 * loading state. Starts loading the model as soon as it is created.
 */
class LookaboutViewModel(application: Application) : AndroidViewModel(application) {
    var screen by mutableStateOf(Screen.Setup)
        private set

    var setup by mutableStateOf(SetupChoices())
        private set

    /** The list shown on List, null until Make my list. */
    var list by mutableStateOf<ChallengeList?>(null)
        private set

    /** The number of pictures Gemma is writing for List, null while no list is being written. */
    var writingCount by mutableStateOf<Int?>(null)
        private set

    /** The walk in progress or just finished, null until Start walk. */
    var walk by mutableStateOf<Walk?>(null)
        private set

    var model by mutableStateOf<ModelState>(ModelState.Loading)
        private set

    private var loadJob: Job? = null

    private var writeJob: Job? = null

    init {
        loadModel()
    }

    /** Loads the model off the main thread. Also Try again after a failed load. */
    fun loadModel() {
        if (loadJob?.isActive == true || model == ModelState.Ready) return
        model = ModelState.Loading
        val app = getApplication<Application>()
        loadJob = viewModelScope.launch {
            model = try {
                withContext(Dispatchers.Default) {
                    Gemma.load(modelPath(app), app.cacheDir.path)
                }
                ModelState.Ready
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e(Gemma.TAG, "Could not load the model", t)
                ModelState.Failed(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun updateSetup(choices: SetupChoices) {
        setup = choices
    }

    /** Make my list: shows List, which shows progress while Gemma writes the pictures. */
    fun makeList() {
        val choices = setup
        val sky = choices.sky
        if (!choices.canMakeList(model == ModelState.Ready) || sky == null) return
        startWriting(choices.topicText, sky, choices.count)
        screen = Screen.List
    }

    /** New list: Gemma writes the pictures again for the same topic, sky and count. */
    fun newList() {
        val current = list ?: return
        startWriting(current.topic, current.sky, current.pictures.size)
    }

    /**
     * Has Gemma write [count] pictures off the main thread, filling any gap from the built-in
     * list, then puts them on List. A newer call cancels an older one, so a stale list never
     * lands. Without a loaded model the built-in list is used alone.
     */
    private fun startWriting(topic: String, sky: String, count: Int) {
        writeJob?.cancel()
        writingCount = count
        val now = LocalDateTime.now()
        val prompt = listPrompt(count, topic, sky, timeOfDay(now.hour), monthName(now.monthValue))
        val modelReady = model == ModelState.Ready
        writeJob = viewModelScope.launch {
            val pictures = withContext(Dispatchers.Default) {
                writeList(prompt, count, ask = { text, seed ->
                    try {
                        check(modelReady) { "Model not loaded" }
                        Gemma.ask(text, temperature = 0.8, maxTokens = 600, seed = seed)
                    } catch (e: Exception) {
                        Log.w(Gemma.TAG, "Could not write the list", e)
                        throw e
                    }
                })
            }
            list = ChallengeList(topic, sky, pictures)
            writingCount = null
        }
    }

    /** Start walk: a new walk from the list, shown on Walk. */
    fun startWalk() {
        val current = list ?: return
        walk = current.startWalk(System.currentTimeMillis())
        screen = Screen.Walk
    }

    /** Replaces the walk in progress with an updated one. */
    fun updateWalk(change: (Walk) -> Walk) {
        walk = walk?.let(change)
    }

    /** Finish walk (or Skip on the last picture): shows Summary. */
    fun finishWalk() {
        if (walk != null) screen = Screen.Summary
    }

    /** Back to Setup, keeping the choices. */
    fun showSetup() {
        screen = Screen.Setup
    }

    fun openDebug() {
        screen = Screen.Debug
    }

    /** System Back: List, Summary and Debug return to Setup; a walk ignores it. */
    fun back() {
        screen.backTarget?.let { screen = it }
    }
}
