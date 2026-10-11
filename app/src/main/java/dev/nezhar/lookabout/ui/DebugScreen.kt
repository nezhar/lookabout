package dev.nezhar.lookabout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nezhar.lookabout.ModelChoice
import dev.nezhar.lookabout.ui.theme.LookaboutTheme

/**
 * Model test and the photo check, for development. Opened by a long press on the Setup title.
 * On phones with the memory for it ([canChooseModel]) it also offers the choice of model.
 */
@Composable
fun DebugScreen(
    onBack: () -> Unit,
    canChooseModel: Boolean,
    modelChoice: ModelChoice,
    modelChoiceEnabled: Boolean,
    modelChoiceMessage: String?,
    onChooseModel: (ModelChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }
        Text("Lookabout debug", style = MaterialTheme.typography.headlineMedium)
        if (canChooseModel) {
            ModelChoiceOptions(
                choice = modelChoice,
                enabled = modelChoiceEnabled,
                message = modelChoiceMessage,
                onChoose = onChooseModel,
            )
            HorizontalDivider()
        }
        ModelTest()
        HorizontalDivider()
        CheckScreen()
    }
}

/** The two models as radio options; choosing one reloads the model. */
@Composable
private fun ModelChoiceOptions(
    choice: ModelChoice,
    enabled: Boolean,
    message: String?,
    onChoose: (ModelChoice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Model", style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.selectableGroup()) {
            ModelChoice.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == choice,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onChoose(option) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == choice, onClick = null, enabled = enabled)
                    Text(
                        if (option == ModelChoice.E2B) "${option.label} (default)" else option.label,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelChoiceOptionsPreview() {
    LookaboutTheme {
        ModelChoiceOptions(
            choice = ModelChoice.E2B,
            enabled = true,
            message = "Gemma 4 E4B is not on the phone: push gemma-4-E4B-it.litertlm to /data/local/tmp/llm/. Staying on Gemma 4 E2B.",
            onChoose = {},
        )
    }
}
