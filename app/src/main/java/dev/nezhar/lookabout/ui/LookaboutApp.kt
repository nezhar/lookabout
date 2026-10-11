package dev.nezhar.lookabout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nezhar.lookabout.Screen
import dev.nezhar.lookabout.backTarget

/** The app shell: the ViewModel's screen value decides which screen shows. */
@Composable
fun LookaboutApp(viewModel: LookaboutViewModel = viewModel()) {
    val screen = viewModel.screen
    // On Setup Back is not handled, so it leaves the app; during a walk it is consumed and ignored.
    BackHandler(enabled = screen.backTarget != null) { viewModel.back() }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding()
        val list = viewModel.list
        val walk = viewModel.walk
        when {
            screen == Screen.List && list != null -> ListScreen(
                list = list,
                onStartWalk = viewModel::startWalk,
                onNewList = viewModel::newList,
                modifier = modifier,
            )
            screen == Screen.Walk && walk != null -> WalkScreen(
                walk = walk,
                onWalkChange = { updated -> viewModel.updateWalk { updated } },
                onFinish = viewModel::finishWalk,
                modifier = modifier,
            )
            screen == Screen.Summary && walk != null -> SummaryScreen(
                walk = walk,
                onNewWalk = viewModel::showSetup,
                modifier = modifier,
            )
            screen == Screen.Debug -> DebugScreen(
                onBack = viewModel::showSetup,
                modifier = modifier,
            )
            else -> SetupScreen(
                choices = viewModel.setup,
                model = viewModel.model,
                onChoicesChange = viewModel::updateSetup,
                onMakeList = viewModel::makeList,
                onTryAgain = viewModel::loadModel,
                onOpenDebug = viewModel::openDebug,
                modifier = modifier,
            )
        }
    }
}
