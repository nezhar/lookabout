package dev.nezhar.lookabout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nezhar.lookabout.Screen
import dev.nezhar.lookabout.backTarget

/**
 * The app shell: the ViewModel's screen value decides which screen shows. [noticeTaps] counts taps
 * on the walk notice since the activity was created; each new tap opens the camera when the
 * current picture waits for its photo.
 */
@Composable
fun LookaboutApp(noticeTaps: Int = 0, viewModel: LookaboutViewModel = viewModel()) {
    val screen = viewModel.screen
    // On Setup Back is not handled, so it leaves the app; during a walk it is consumed and ignored.
    BackHandler(enabled = screen.backTarget != null) { viewModel.back() }
    // Shoot and Retake on Walk hand off to the camera app; the result goes to the ViewModel.
    val openCamera = rememberCameraHandoff(
        onPhoto = viewModel::photoTaken,
        onCancelled = viewModel::cameraCancelled,
        onLaunch = viewModel::photoRequested,
    )
    val takePhoto: () -> Unit = { if (viewModel.canTakePhoto) openCamera() }
    // A tap on the walk notice shows the walk, and opens the camera when the picture waits for its photo.
    LaunchedEffect(noticeTaps) {
        if (noticeTaps > 0 && viewModel.waitsForPhoto) takePhoto()
    }
    // Start walk asks once for notifications (Android 13+); the notice appears as soon as they are allowed.
    val askNotifications = rememberNotificationPermissionAsk(onAnswered = viewModel::refreshWalkNotice)

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding()
        val list = viewModel.list
        val walk = viewModel.walk
        val writingCount = viewModel.writingCount
        when {
            screen == Screen.List && writingCount != null -> ListProgress(
                count = writingCount,
                modifier = modifier,
            )
            screen == Screen.List && list != null -> ListScreen(
                list = list,
                onStartWalk = {
                    viewModel.startWalk()
                    askNotifications()
                },
                onNewList = viewModel::newList,
                modifier = modifier,
            )
            screen == Screen.Walk && walk != null -> WalkScreen(
                walk = walk,
                checking = viewModel.checking,
                checkPaused = viewModel.checkPaused,
                onShoot = takePhoto,
                onSkip = viewModel::skip,
                onRetake = takePhoto,
                onTryAgain = viewModel::tryAgain,
                onNextPicture = viewModel::nextPicture,
                onFinish = viewModel::finishWalk,
                onFlip = viewModel::flip,
                modifier = modifier,
            )
            screen == Screen.Summary && walk != null -> SummaryScreen(
                walk = walk,
                onNewWalk = viewModel::newWalk,
                modifier = modifier,
            )
            screen == Screen.Debug -> DebugScreen(
                onBack = viewModel::showSetup,
                canChooseModel = viewModel.canChooseModel,
                modelChoice = viewModel.modelChoice,
                modelChoiceEnabled = viewModel.model !is ModelState.Loading &&
                    viewModel.model !is ModelState.Downloading,
                modelChoiceMessage = viewModel.modelChoiceMessage,
                onChooseModel = viewModel::chooseModel,
                modifier = modifier,
            )
            else -> SetupScreen(
                choices = viewModel.setup,
                model = viewModel.model,
                onChoicesChange = viewModel::updateSetup,
                onMakeList = viewModel::makeList,
                onTryAgain = viewModel::loadModel,
                onDownload = viewModel::downloadModel,
                onCancelDownload = viewModel::cancelDownload,
                onOpenDebug = viewModel::openDebug,
                modifier = modifier,
            )
        }
    }
}
