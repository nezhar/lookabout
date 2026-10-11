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
import dev.nezhar.lookabout.STAND_IN_CHECK_MILLIS
import dev.nezhar.lookabout.SetupChoices
import dev.nezhar.lookabout.backTarget
import dev.nezhar.lookabout.modelPath
import dev.nezhar.lookabout.standInAnswers
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.pickBuiltInPictures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    /** The walk in progress or just finished, null until Start walk. */
    var walk by mutableStateOf<Walk?>(null)
        private set

    var model by mutableStateOf<ModelState>(ModelState.Loading)
        private set

    private var loadJob: Job? = null

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

    /** Make my list: draws the pictures and shows List. */
    fun makeList() {
        val choices = setup
        val sky = choices.sky
        if (!choices.canMakeList(model == ModelState.Ready) || sky == null) return
        list = ChallengeList(choices.topicText, sky, pickBuiltInPictures(choices.count))
        screen = Screen.List
    }

    /** New list: draws the pictures again for the same topic, sky and count. */
    fun newList() {
        val current = list ?: return
        list = current.copy(pictures = pickBuiltInPictures(current.pictures.size))
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

    /** True while the current photo is being checked: Walk shows Checking and offers no actions. */
    var checking by mutableStateOf(false)
        private set

    /**
     * Shoot (and Retake): a new photo for the current picture, then its check. Ignored while a
     * check runs, so a result always belongs to the photo it was made for.
     *
     * Stand-in until PW-14: there is no camera yet, so the photo is a path without a file (Walk
     * shows a plain box for it) and it is taken at once.
     */
    fun shoot() {
        if (checking || walk?.current == null) return
        val path = File(getApplication<Application>().cacheDir, "stand-in-${System.currentTimeMillis()}.jpg").path
        updateWalk { it.photoRequested(path) }
        photoTaken()
    }

    /** Retake on Not yet: the same as Shoot. */
    fun retake() = shoot()

    /** The camera wrote the pending photo: it becomes the current shot's photo and is checked. */
    fun photoTaken() {
        if (checking || walk?.pendingPhotoPath == null) return
        updateWalk { it.photoTaken() }
        checkCurrentPhoto()
    }

    /** Skip: moves on without an accepted photo; on the last picture it opens Summary. */
    fun skip() {
        val current = walk ?: return
        if (checking || current.current == null) return
        updateWalk { it.skip() }
        if (current.isLast) finishWalk()
    }

    /** Next picture after Accepted; on the last picture this is Finish walk and opens Summary. */
    fun nextPicture() {
        val current = walk ?: return
        if (checking || current.current?.isChecked != true) return
        updateWalk { it.nextPicture() }
        if (current.isLast) finishWalk()
    }

    /** Checks the current shot's photo off the main thread and records the answers through [updateWalk]. */
    private fun checkCurrentPhoto() {
        val current = walk ?: return
        val index = current.currentIndex
        val shot = current.current ?: return
        val path = shot.photoPath ?: return
        checking = true
        viewModelScope.launch {
            try {
                val start = System.nanoTime()
                val answers = standInCheck(index)
                val checkMillis = (System.nanoTime() - start) / 1_000_000
                // Only for the photo it was made for, should the shot have changed meanwhile.
                updateWalk { w ->
                    if (w.shots.getOrNull(index)?.photoPath == path) w.recordCheck(answers, checkMillis, index) else w
                }
            } finally {
                checking = false
            }
        }
    }

    /** The stand-in check until PW-14 brings the model: waits two seconds, returns the sample answers. */
    private suspend fun standInCheck(shotIndex: Int): List<Boolean> {
        delay(STAND_IN_CHECK_MILLIS)
        return standInAnswers(shotIndex)
    }
}
