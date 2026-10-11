package dev.nezhar.lookabout.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nezhar.lookabout.ChallengeList
import dev.nezhar.lookabout.Gemma
import dev.nezhar.lookabout.PausedCheck
import dev.nezhar.lookabout.Screen
import dev.nezhar.lookabout.SetupChoices
import dev.nezhar.lookabout.WALK_FILE_NAME
import dev.nezhar.lookabout.WalkNotifier
import dev.nezhar.lookabout.WalkStore
import dev.nezhar.lookabout.answersToRecord
import dev.nezhar.lookabout.backTarget
import dev.nezhar.lookabout.checkPhoto
import dev.nezhar.lookabout.listPrompt
import dev.nezhar.lookabout.modelPath
import dev.nezhar.lookabout.monthName
import dev.nezhar.lookabout.timeOfDay
import dev.nezhar.lookabout.waitsForPhoto
import dev.nezhar.lookabout.walkNotice
import dev.nezhar.lookabout.walk.SavedWalk
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.isAccepted
import dev.nezhar.lookabout.writeList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
 *
 * The walk is saved to `filesDir` after every change and loaded again when the ViewModel is
 * created, so it survives Android closing the app while the camera is open.
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

    private val walkStore = WalkStore(File(application.filesDir, WALK_FILE_NAME))

    /** True once the walker has moved on to Summary for the current walk; saved with the walk. */
    private var onSummary = false

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
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (t: Throwable) {
                        // An empty reply has no pictures, so the built-in list fills the gap.
                        Log.w(Gemma.TAG, "Could not write the list", t)
                        ""
                    }
                }, isActive = { isActive })
            }
            list = ChallengeList(topic, sky, pictures)
            writingCount = null
        }
    }

    /** Leaving List: stops any list Gemma is still writing, so the model is free again. */
    private fun stopWriting() {
        writeJob?.cancel()
        writeJob = null
        writingCount = null
    }

    /** Start walk: a new walk from the list, shown on Walk. */
    fun startWalk() {
        val current = list ?: return
        walk = current.startWalk(System.currentTimeMillis())
        onSummary = false
        saveWalk()
        screen = Screen.Walk
    }

    /** Replaces the walk in progress with an updated one. Returns true when [change] changed it. */
    fun updateWalk(change: (Walk) -> Walk): Boolean {
        val current = walk ?: return false
        val updated = change(current)
        walk = updated
        saveWalk()
        return updated != current
    }

    /** Finish walk (or Skip on the last picture): shows Summary. */
    fun finishWalk() {
        if (walk == null) return
        onSummary = true
        saveWalk()
        screen = Screen.Summary
    }

    /** New walk on Summary: the finished walk is dropped, with its saved copy, and Setup shows. */
    fun newWalk() {
        walk = null
        onSummary = false
        saveWalk()
        screen = Screen.Setup
    }

    /** Writes the walk, or deletes the saved one when there is no walk, in the background. */
    private fun saveWalk() {
        walkStore.save(walk?.let { SavedWalk(it, onSummary) })
    }

    /**
     * Loads the saved walk, if any, and opens where the walker left off: Summary when they had
     * moved on to it, else Walk, whose screens follow from the current shot. A photo the camera
     * wrote while the app was closed is checked; an empty or missing one is dropped. A photo
     * whose check had not come in is checked again.
     */
    private fun restoreWalk() {
        val saved = walkStore.load() ?: return
        walk = saved.walk
        onSummary = saved.onSummary
        screen = if (saved.onSummary) Screen.Summary else Screen.Walk
        val pending = saved.walk.pendingPhotoPath
        val shot = saved.walk.current
        when {
            pending != null -> {
                val file = File(pending)
                if (!saved.onSummary && shot != null && file.length() > 0) {
                    photoTaken(file)
                } else {
                    if (file.isFile && file.length() == 0L) file.delete()
                    cameraCancelled()
                }
            }
            !saved.onSummary && shot != null && shot.hasPhoto && !shot.isChecked -> checkCurrentPhoto()
        }
    }

    /** Back to Setup, keeping the choices. */
    fun showSetup() {
        screen = Screen.Setup
    }

    fun openDebug() {
        screen = Screen.Debug
    }

    /**
     * System Back: List, Summary and Debug return to Setup; a walk ignores it. Leaving List stops
     * the list being written; leaving Summary is New walk, so the finished walk is not reopened.
     */
    fun back() {
        val target = screen.backTarget ?: return
        when (screen) {
            Screen.List -> stopWriting()
            Screen.Summary -> {
                newWalk()
                return
            }
            else -> {}
        }
        screen = target
    }

    /** True while the current photo is being checked: Walk shows Checking and offers no actions. */
    var checking by mutableStateOf(false)
        private set

    /** The last failed check, if any; it shows as Check paused while its shot still holds that photo. */
    private var pausedCheck by mutableStateOf<PausedCheck?>(null)

    /** True when the current photo's check failed: Walk shows Check paused with Try again and Skip. */
    val checkPaused: Boolean
        get() {
            val current = walk ?: return false
            return pausedCheck?.appliesTo(current.currentIndex, current.current) == true
        }

    /** True when Shoot or Retake may open the camera: on a shot, with no check running. */
    val canTakePhoto: Boolean
        get() = !checking && walk?.current != null

    /** The camera is about to open for Shoot or Retake: it writes the photo to [file]. */
    fun photoRequested(file: File) {
        if (!canTakePhoto) return
        updateWalk { it.photoRequested(file.path) }
    }

    /** The camera was cancelled or could not open: the pending photo is dropped, nothing else changes. */
    fun cameraCancelled() {
        updateWalk { it.cameraCancelled() }
    }

    /** The camera wrote [file]: it becomes the current shot's photo, replacing an older one, and is checked. */
    fun photoTaken(file: File) {
        if (!canTakePhoto) return
        // Already the current photo: the saved walk was restored and checked it before the camera result came in.
        val current = walk ?: return
        if (current.pendingPhotoPath != file.path && current.current?.photoPath == file.path) return
        updateWalk { w ->
            val pending = if (w.pendingPhotoPath == file.path) w else w.photoRequested(file.path)
            pending.photoTaken()
        }
        checkCurrentPhoto()
    }

    /** Try again on Check paused: checks the same photo again. */
    fun tryAgain() {
        if (checking || !checkPaused) return
        checkCurrentPhoto()
    }

    /** Skip: moves on without an accepted photo; on the last picture it opens Summary. */
    fun skip() {
        val current = walk ?: return
        if (checking || current.current == null) return
        pausedCheck = null
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

    /**
     * Checks the current shot's photo with Gemma off the main thread (the model is loaded first if
     * needed) and records the answers and the model time through [updateWalk], only if the shot
     * still holds that photo. A failed check records nothing and pauses the check instead.
     */
    private fun checkCurrentPhoto() {
        val current = walk ?: return
        val index = current.currentIndex
        val shot = current.current ?: return
        val path = shot.photoPath ?: return
        val app = getApplication<Application>()
        pausedCheck = null
        checking = true
        viewModelScope.launch {
            try {
                val result = checkPhoto(app, Uri.fromFile(File(path)), shot.questions)
                val answers = answersToRecord(result)
                if (answers == null) {
                    pausedCheck = PausedCheck(index, path)
                } else {
                    val recorded = updateWalk { w ->
                        if (w.shots.getOrNull(index)?.photoPath == path) w.recordCheck(answers, result.modelMs, index) else w
                    }
                    if (recorded) buzzVerdict(app, isAccepted(answers))
                }
            } finally {
                checking = false
            }
        }
    }

    /**
     * Tap on a criterion on Accepted or Not yet: flips the answer to [questionIndex] (0 to 3) on the
     * current shot, or restores Gemma's answer if it was already changed. Ignored while a check runs.
     */
    fun flip(questionIndex: Int) {
        if (checking) return
        updateWalk { it.flip(questionIndex) }
    }

    private val walkNotifier = WalkNotifier(application)

    /** True when the current picture on Walk waits for its photo: a tap on the walk notice opens the camera. */
    val waitsForPhoto: Boolean
        get() = screen == Screen.Walk && !checking && walk?.waitsForPhoto() == true

    /** Posts, updates or removes the walk notice for the current walk and screen, e.g. once notifications are allowed. */
    fun refreshWalkNotice() {
        walkNotifier.show(walkNotice(walk, screen))
    }

    // The walk notice follows the current picture and is removed on Summary and New walk. Dispatched
    // after the ViewModel is created, so it reads the state once the restored walk is in.
    init {
        viewModelScope.launch(Dispatchers.Main) {
            snapshotFlow { walkNotice(walk, screen) }.collect { walkNotifier.show(it) }
        }
    }

    // Last in the class: restoring may start a check, which needs every property above.
    init {
        restoreWalk()
    }
}
