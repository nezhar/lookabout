package dev.nezhar.lookabout.ui

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.nezhar.lookabout.ArchivedWalks
import dev.nezhar.lookabout.ChallengeList
import dev.nezhar.lookabout.DOWNLOAD_POLL_MS
import dev.nezhar.lookabout.DownloadStep
import dev.nezhar.lookabout.Gemma
import dev.nezhar.lookabout.ModelChoice
import dev.nezhar.lookabout.ModelDownloader
import dev.nezhar.lookabout.ModelStart
import dev.nezhar.lookabout.PausedCheck
import dev.nezhar.lookabout.Screen
import dev.nezhar.lookabout.SetupChoices
import dev.nezhar.lookabout.WALK_FILE_NAME
import dev.nezhar.lookabout.WalkNotifier
import dev.nezhar.lookabout.WalkStore
import dev.nezhar.lookabout.answersToRecord
import dev.nezhar.lookabout.backTarget
import dev.nezhar.lookabout.canStartEndCheck
import dev.nezhar.lookabout.checkPhoto
import dev.nezhar.lookabout.checksAtEnd
import dev.nezhar.lookabout.copyToGallery
import dev.nezhar.lookabout.downloadStep
import dev.nezhar.lookabout.droppedPhotos
import dev.nezhar.lookabout.isAppPhoto
import dev.nezhar.lookabout.photosSafeToDelete
import dev.nezhar.lookabout.walkPhotoPaths
import dev.nezhar.lookabout.listPrompt
import dev.nezhar.lookabout.missingModelText
import dev.nezhar.lookabout.modelFile
import dev.nezhar.lookabout.modelPath
import dev.nezhar.lookabout.modelStart
import dev.nezhar.lookabout.monthName
import dev.nezhar.lookabout.offersCheckAgain
import dev.nezhar.lookabout.offersShowResultsNow
import dev.nezhar.lookabout.retriesModelLoad
import dev.nezhar.lookabout.sunHere
import dev.nezhar.lookabout.offersModelChoice
import dev.nezhar.lookabout.saveModelChoice
import dev.nezhar.lookabout.savedModelChoice
import dev.nezhar.lookabout.timeOfDay
import dev.nezhar.lookabout.usableModelChoice
import dev.nezhar.lookabout.waitsForPhoto
import dev.nezhar.lookabout.walkNotice
import dev.nezhar.lookabout.walk.ArchivedWalkSummary
import dev.nezhar.lookabout.walk.SavedWalk
import dev.nezhar.lookabout.walk.WALK_ARCHIVE_DIR
import dev.nezhar.lookabout.walk.Walk
import dev.nezhar.lookabout.walk.archiveId
import dev.nezhar.lookabout.walk.isAccepted
import dev.nezhar.lookabout.writeList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/** Where loading the model stands. */
sealed interface ModelState {
    data object Loading : ModelState
    data object Ready : ModelState
    data class Failed(val message: String) : ModelState

    /** No model file and no download: Setup offers the download. */
    data object Missing : ModelState

    /**
     * The model downloads; [percent] is null while unknown, [pausedReason] is the system's reason
     * code while the download is paused and null while it runs.
     */
    data class Downloading(val percent: Int?, val pausedReason: Int?) : ModelState

    /** The download failed or was stopped: Setup shows [message] and Try again. */
    data class DownloadFailed(val message: String) : ModelState
}

/**
 * The app's state: the screen, the Setup choices, the current list and walk, and the model's
 * loading state. Starts loading the model as soon as it is created.
 *
 * The walk is saved to `filesDir` after every change and loaded again when the ViewModel is
 * created, so it survives Android closing the app while the camera is open. A walk that reaches
 * Summary is also kept in the walk archive, and saved there again whenever its results change.
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

    /** Every walk that reached Summary, with its results. */
    val archivedWalks = ArchivedWalks.at(File(application.filesDir, WALK_ARCHIVE_DIR))

    // Held while a photo is copied to the gallery or dropped photos are deleted, so a photo
    // skipped right after it was taken is not deleted before its gallery copy is written.
    private val photoFiles = Mutex()

    private val downloader = ModelDownloader(application)

    /** True once the walker has moved on to Summary for the current walk; saved with the walk. */
    private var onSummary = false

    /** The model chosen on Debug: E2B unless E4B was chosen on a phone with the memory for it. */
    var modelChoice by mutableStateOf(ModelChoice.E2B)
        private set

    /** True on phones with the memory for E4B: Debug offers the choice of model. */
    val canChooseModel: Boolean = offersModelChoice(application)

    /** Debug's line about the last change of model, e.g. a missing E4B file; null for none. */
    var modelChoiceMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadModel()
    }

    /**
     * Loads the model off the main thread when its file is there; otherwise follows a download
     * started earlier, or offers the download. Also Try again after a failed load.
     */
    fun loadModel() {
        if (loadJob?.isActive == true || model == ModelState.Ready) return
        model = ModelState.Loading
        launchModelJob { startModel() }
    }

    /**
     * The choice of model on Debug: closes the loaded model and loads [choice] the usual way.
     * A missing file keeps the current model and says so. Ignored while the model loads or
     * downloads, and on phones that do not offer the choice.
     */
    fun chooseModel(choice: ModelChoice) {
        if (!canChooseModel || choice == modelChoice) return
        if (loadJob?.isActive == true) {
            modelChoiceMessage = "Wait until the model has loaded."
            return
        }
        val app = getApplication<Application>()
        modelChoiceMessage = null
        launchModelJob {
            // The in-app download is for E2B only, so only another model needs its file now.
            val found = choice == ModelChoice.E2B ||
                withContext(Dispatchers.IO) { modelFile(app, choice).exists() }
            if (!found) {
                modelChoiceMessage = missingModelText(choice)
                return@launchModelJob
            }
            model = ModelState.Loading
            withContext(Dispatchers.IO) { saveModelChoice(app, choice) }
            modelChoice = choice
            withContext(Dispatchers.Default) { Gemma.close() }
            startModel()
        }
    }

    /**
     * Loads the chosen model when its file is there, follows a download started earlier, or
     * offers the download. A chosen E4B whose file has gone falls back to E2B, which is saved.
     */
    private suspend fun startModel() {
        val app = getApplication<Application>()
        val (saved, usable) = withContext(Dispatchers.IO) {
            savedModelChoice(app) to usableModelChoice(app)
        }
        if (usable != saved) {
            withContext(Dispatchers.IO) { saveModelChoice(app, usable) }
            modelChoiceMessage = missingModelText(saved)
        }
        modelChoice = usable
        val (found, id) = withContext(Dispatchers.IO) {
            File(modelPath(app)).exists() to downloader.savedId()
        }
        when (modelStart(found, id)) {
            ModelStart.Load -> {
                // A remembered download is not needed any more.
                if (id != null) withContext(Dispatchers.IO) { downloader.discard(id) }
                loadFromFile()
            }
            ModelStart.FollowDownload -> if (id != null) followDownload(id)
            ModelStart.OfferDownload -> model = ModelState.Missing
        }
    }

    /** Download the model on Setup, and Try again after a failed download. */
    fun downloadModel() {
        if (loadJob?.isActive == true) return
        if (model != ModelState.Missing && model !is ModelState.DownloadFailed) return
        model = ModelState.Downloading(percent = null, pausedReason = null)
        launchModelJob {
            val id = try {
                withContext(Dispatchers.IO) { downloader.start() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e(Gemma.TAG, "Could not start the model download", t)
                model = ModelState.DownloadFailed("The download did not start: ${t.message ?: t.javaClass.simpleName}")
                return@launchModelJob
            }
            followDownload(id)
        }
    }

    /** Cancel while the model downloads: stops the download, deletes its file, offers it again. */
    fun cancelDownload() {
        if (model !is ModelState.Downloading) return
        val running = loadJob
        model = ModelState.Missing
        launchModelJob {
            // Waits for a download that is just being started, so its id is saved before it goes.
            running?.cancelAndJoin()
            withContext(Dispatchers.IO) { downloader.savedId()?.let { downloader.discard(it) } }
        }
    }

    /** Runs [block] as the model job; an unexpected error shows as a failed load with Try again. */
    private fun launchModelJob(block: suspend () -> Unit) {
        loadJob = viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e(Gemma.TAG, "Could not get the model ready", t)
                model = ModelState.Failed(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private suspend fun loadFromFile() {
        val app = getApplication<Application>()
        model = ModelState.Loading
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

    /**
     * Shows the download's progress, asking the system every [DOWNLOAD_POLL_MS], until it ends.
     * A finished download is moved into `filesDir` and loaded as at app start.
     */
    private suspend fun followDownload(id: Long) {
        while (true) {
            val snapshot = withContext(Dispatchers.IO) { downloader.query(id) }
            when (val step = downloadStep(snapshot)) {
                is DownloadStep.Running -> model = ModelState.Downloading(step.percent, step.pausedReason)
                DownloadStep.Finished -> {
                    model = ModelState.Loading
                    val moved = try {
                        withContext(Dispatchers.IO) { downloader.install(snapshot?.localPath) }
                        true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (t: Throwable) {
                        Log.e(Gemma.TAG, "Could not move the downloaded model", t)
                        model = ModelState.DownloadFailed(
                            "The model could not be saved: ${t.message ?: t.javaClass.simpleName}"
                        )
                        false
                    }
                    withContext(Dispatchers.IO) { downloader.discard(id) }
                    if (moved) loadFromFile()
                    return
                }
                is DownloadStep.Failed -> {
                    withContext(Dispatchers.IO) { downloader.discard(id) }
                    model = ModelState.DownloadFailed(step.message)
                    return
                }
            }
            delay(DOWNLOAD_POLL_MS)
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
        val modelReady = model == ModelState.Ready
        val app = getApplication<Application>()
        writeJob = viewModelScope.launch {
            val pictures = withContext(Dispatchers.Default) {
                val sun = sunHere(app, System.currentTimeMillis())
                val prompt = listPrompt(count, topic, sky, timeOfDay(now.hour), monthName(now.monthValue), sun)
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
        walk = current.startWalk(System.currentTimeMillis(), setup.checkAtEnd)
        onSummary = false
        saveWalk()
        screen = Screen.Walk
    }

    /**
     * Replaces the walk in progress with an updated one. Returns true when [change] changed it. A
     * photo the update dropped (replaced by Retake, or skipped before its check) is deleted, see
     * [deleteDroppedPhotos].
     */
    fun updateWalk(change: (Walk) -> Walk): Boolean {
        val current = walk ?: return false
        val updated = change(current)
        walk = updated
        val written = saveWalk()
        deleteDroppedPhotos(current, updated, written)
        return updated != current
    }

    /**
     * Finish walk (or Skip on the last picture): shows Summary. When the photos are checked at the
     * end, the check at the end runs first and opens Summary when it is done.
     */
    fun finishWalk() {
        val current = walk ?: return
        if (checksAtEnd(current, onSummary) && current.shotsToCheck.isNotEmpty()) {
            checkAllAtEnd()
            return
        }
        showSummary()
    }

    /** Shows Summary for the walk, without checking anything. */
    private fun showSummary() {
        if (walk == null) return
        onSummary = true
        saveWalk()
        screen = Screen.Summary
    }

    /**
     * New walk on Summary: the finished walk is dropped, with its saved copy, and Setup shows. Its
     * archived copy and its photos stay.
     */
    fun newWalk() {
        endCheckJob?.cancel()
        walk = null
        onSummary = false
        saveWalk()
        screen = Screen.Setup
    }

    /**
     * Writes the walk, or deletes the saved one when there is no walk, in the background. A walk on
     * Summary is also written to the archive, so it always holds the latest results. The result
     * completes with true once the walk file holds this save or a newer one (see [WalkStore.save]).
     */
    private fun saveWalk(): Deferred<Boolean> {
        val current = walk
        val written = walkStore.save(current?.let { SavedWalk(it, onSummary) })
        if (current != null && onSummary) archivedWalks.save(current)
        return written
    }

    /**
     * Loads the saved walk, if any, and opens where the walker left off: Summary when they had
     * moved on to it, else Walk, whose screens follow from the current shot. A photo the camera
     * wrote while the app was closed is checked; an empty or missing one is dropped. A photo
     * whose check had not come in is checked again. When the photos are checked at the end, a
     * photo is only checked there: a walk past its last picture resumes the check at the end, which
     * waits for the model first.
     */
    private fun restoreWalk() {
        val saved = walkStore.load() ?: return
        walk = saved.walk
        onSummary = saved.onSummary
        // A walk that reached Summary before the archive existed, or just before the app closed.
        if (saved.onSummary) archivedWalks.save(saved.walk)
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
            checksAtEnd(saved.walk, saved.onSummary) -> finishWalk()
            !saved.walk.checkAtEnd && !saved.onSummary && shot != null && shot.hasPhoto && !shot.isChecked ->
                checkCurrentPhoto()
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
     * System Back: List, Summary, Debug and Past walks return to Setup; a walk ignores it. Leaving
     * List stops the list being written; leaving Summary is New walk, so the finished walk is not
     * reopened. While Check again runs from Summary, Back stops it and Summary shows again. A past
     * walk's Summary returns to Past walks.
     */
    fun back() {
        val target = screen.backTarget ?: return
        when (screen) {
            Screen.List -> stopWriting()
            Screen.Summary -> {
                if (endCheckJob?.isActive == true) endCheckJob?.cancel() else newWalk()
                return
            }
            Screen.PastSummary -> {
                closePastWalk()
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

    /**
     * The camera wrote [file]: it becomes the current shot's photo, replacing an older one (whose
     * file is deleted), and is checked, unless the walk checks its photos at the end.
     */
    fun photoTaken(file: File) {
        if (!canTakePhoto) return
        // Already the current photo: the saved walk was restored and checked it before the camera result came in.
        val current = walk ?: return
        if (current.pendingPhotoPath != file.path && current.current?.photoPath == file.path) return
        updateWalk { w ->
            val pending = if (w.pendingPhotoPath == file.path) w else w.photoRequested(file.path)
            pending.photoTaken()
        }
        // Undispatched, so the copy holds the photo lock before any later delete asks for it.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            photoFiles.withLock {
                withContext(Dispatchers.IO) { copyToGallery(getApplication<Application>(), file) }
            }
        }
        if (!current.checkAtEnd) checkCurrentPhoto()
    }

    /**
     * Deletes, in the background, the photos [before] referenced and [after] no longer does
     * ([droppedPhotos]): only the app's own files in its Pictures folder, never a gallery copy.
     * It first waits for the walk file write of this update ([written]) and keeps every photo when
     * that write failed or the walk file cannot be read; then it deletes only photos that neither
     * the walk in progress, the saved walk file nor any archived walk references.
     */
    private fun deleteDroppedPhotos(before: Walk, after: Walk, written: Deferred<Boolean>) {
        val candidates = droppedPhotos(before, after)
        if (candidates.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val saved = written.await()
                val inUse = walk?.let(::walkPhotoPaths).orEmpty()
                val stillDropped = candidates.filter { it !in inUse }
                if (!saved || stillDropped.isEmpty()) return@launch
                photoFiles.withLock {
                    withContext<Unit>(Dispatchers.IO) {
                        val photoDir = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            ?: return@withContext
                        val own = stillDropped.filter { isAppPhoto(it, photoDir.path) }
                        val deletable = photosSafeToDelete(own, saved, walkStore.photoPaths())
                        if (deletable.isNotEmpty()) archivedWalks.deleteUnarchivedPhotos(deletable)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.w(Gemma.TAG, "Could not delete a dropped photo", t)
            }
        }
    }

    /** Try again on Check paused: checks the same photo again. */
    fun tryAgain() {
        if (checking || !checkPaused) return
        checkCurrentPhoto()
    }

    /**
     * Skip: moves on without an accepted photo; on the last picture it opens Summary. A photo that
     * was not checked is dropped from the shot and its file deleted.
     */
    fun skip() {
        val current = walk ?: return
        if (checking || current.current == null) return
        pausedCheck = null
        updateWalk { it.skip() }
        if (current.isLast) finishWalk()
    }

    /**
     * Next picture after Accepted, or after Photo taken when the photos are checked at the end; on
     * the last picture this is Finish walk.
     */
    fun nextPicture() {
        val current = walk ?: return
        if (checking || !current.canMoveOn) return
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
     * The shot whose photo the check at the end is checking, null while it does not run. Walk shows
     * Checking with the photo's position then, and keeps the screen on.
     */
    var endCheckIndex by mutableStateOf<Int?>(null)
        private set

    /**
     * True while the check at the end waits for the model to be ready: its screen says "Getting
     * the model ready" instead of the photo's position.
     */
    var endCheckWaiting by mutableStateOf(false)
        private set

    /** True when the model is not on the phone or failed to load: the waiting end check says so. */
    val modelUnavailable: Boolean
        get() = when (model) {
            ModelState.Missing, is ModelState.Failed, is ModelState.DownloadFailed -> true
            ModelState.Loading, ModelState.Ready, is ModelState.Downloading -> false
        }

    /**
     * True when the model is not on the phone (never downloaded, or its download failed): it cannot
     * get ready during a walk, so the waiting end check points to Show results now.
     */
    val modelMissing: Boolean
        get() = model == ModelState.Missing || model is ModelState.DownloadFailed

    private var endCheckJob: Job? = null

    /** True when Summary offers Check again: a photo has no result and no check runs. */
    val canCheckAgain: Boolean
        get() {
            val current = walk ?: return false
            return offersCheckAgain(current, endCheckRunning = endCheckIndex != null)
        }

    /** True when the waiting end check offers Show results now: the model may never be ready. */
    val canShowResultsNow: Boolean
        get() = endCheckIndex != null && offersShowResultsNow(endCheckWaiting, modelUnavailable)

    /**
     * Show results now on the waiting end check: stops it and opens Summary, where Check again
     * checks the photos once the model is ready.
     */
    fun showResultsNow() {
        if (!canShowResultsNow) return
        endCheckJob?.cancel()
        showSummary()
    }

    /**
     * Check again on Summary: runs the check at the end on the photos without a result; its
     * screen shows over Summary, which shows again when it is done.
     */
    fun checkAgain() {
        if (screen != Screen.Summary || !canCheckAgain) return
        checkAllAtEnd()
    }

    /**
     * The check at the end: waits until the model is ready (its screen says so meanwhile, and a
     * failed load is retried once through [loadModel]; Show results now leaves the wait), then
     * checks every photo without a check, one after another, with the same check as during a walk,
     * and records each result through [updateWalk] (so it is saved) if the shot still holds that
     * photo. A failed check leaves its shot unchecked and moves on. Then Summary opens. There is
     * no buzz per result: the walker is watching the screen, and a run of buzzes would only blur
     * the verdicts, which Summary lists together. Also Check again on Summary.
     */
    private fun checkAllAtEnd() {
        if (endCheckJob?.isActive == true) return
        val first = walk?.shotsToCheck?.firstOrNull() ?: return
        val app = getApplication<Application>()
        endCheckIndex = first
        endCheckWaiting = model != ModelState.Ready
        endCheckJob = viewModelScope.launch {
            try {
                // A check before the model is ready would fail every photo at once. Nothing left
                // to check needs no model: the walk goes straight on to Summary.
                var retried = false
                snapshotFlow {
                    val current = walk
                    val ready = current == null || current.shotsToCheck.isEmpty() ||
                        canStartEndCheck(current, model == ModelState.Ready)
                    ready to (model is ModelState.Failed)
                }.first { (ready, failed) ->
                    if (!ready && retriesModelLoad(failed, retried)) {
                        retried = true
                        loadModel()
                    }
                    ready
                }
                endCheckWaiting = false
                val queue = walk?.shotsToCheck.orEmpty()
                for (index in queue) {
                    val shot = walk?.shots?.getOrNull(index) ?: continue
                    val path = shot.photoPath ?: continue
                    if (shot.isChecked) continue
                    endCheckIndex = index
                    val result = checkPhoto(app, Uri.fromFile(File(path)), shot.questions)
                    val answers = answersToRecord(result) ?: continue
                    updateWalk { w ->
                        if (w.shots.getOrNull(index)?.photoPath == path) w.recordCheck(answers, result.modelMs, index) else w
                    }
                }
                showSummary()
            } finally {
                endCheckIndex = null
                endCheckWaiting = false
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

    /**
     * The archived walks, newest first, for Past walks and for Setup, which offers Past walks when
     * there is one. Null until the archive has been read; read again whenever Setup or Past walks
     * shows. An unreadable archive reads as empty.
     */
    var pastWalks by mutableStateOf<List<ArchivedWalkSummary>?>(null)
        private set

    /** The archived walk shown read-only on [Screen.PastSummary], null on every other screen. */
    var pastWalk by mutableStateOf<Walk?>(null)
        private set

    /** Past walks on Setup. */
    fun showPastWalks() {
        if (screen != Screen.Setup) return
        screen = Screen.PastWalks
    }

    /**
     * A tap on a row on Past walks: loads that walk off the main thread and shows it read-only on
     * Summary. A walk that can no longer be read is dropped from the list instead.
     */
    fun openPastWalk(id: Long) {
        if (screen != Screen.PastWalks) return
        viewModelScope.launch {
            val loaded = try {
                archivedWalks.load(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.w(Gemma.TAG, "Could not read the archived walk", t)
                null
            }
            if (screen != Screen.PastWalks) return@launch
            if (loaded == null) {
                readPastWalks()
            } else {
                pastWalk = loaded
                screen = Screen.PastSummary
            }
        }
    }

    /** Back on a past walk's Summary: Past walks again. */
    fun closePastWalk() {
        if (screen != Screen.PastSummary) return
        pastWalk = null
        screen = Screen.PastWalks
    }

    /**
     * Delete on Past walks: removes the archived walk with [id] and the app's own copies of its
     * photos (the files in the app's Pictures folder); copies in the gallery stay. When it is
     * still the current walk (on Summary, or saved to reopen there), that walk is dropped too, so
     * no later save brings it back.
     */
    fun deletePastWalk(id: Long) {
        if (screen != Screen.PastWalks) return
        val current = walk
        if (current != null && archiveId(current) == id) {
            endCheckJob?.cancel()
            walk = null
            onSummary = false
            saveWalk()
        }
        pastWalks = pastWalks?.filterNot { it.id == id }
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val photoDir = withContext(Dispatchers.IO) { app.getExternalFilesDir(Environment.DIRECTORY_PICTURES) }
                if (!archivedWalks.deleteWithPhotos(id, photoDir)) Log.w(Gemma.TAG, "Could not delete the archived walk $id")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.w(Gemma.TAG, "Could not delete the archived walk $id", t)
            }
            readPastWalks()
        }
    }

    private suspend fun readPastWalks() {
        pastWalks = try {
            archivedWalks.list()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.w(Gemma.TAG, "Could not read the walk archive", t)
            emptyList()
        }
    }

    // Setup and Past walks read the archive each time they show, so a walk archived or deleted
    // meanwhile is counted.
    init {
        viewModelScope.launch {
            snapshotFlow { screen }.collectLatest { shown ->
                if (shown == Screen.Setup || shown == Screen.PastWalks) readPastWalks()
            }
        }
    }

    // Last in the class: restoring may start a check, which needs every property above.
    init {
        restoreWalk()
    }
}
