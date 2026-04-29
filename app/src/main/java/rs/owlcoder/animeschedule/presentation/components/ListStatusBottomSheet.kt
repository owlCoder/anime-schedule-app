package rs.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.MalListUpdate
import rs.owlcoder.animeschedule.domain.model.WatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListStatusBottomSheet(
    animeId: Int,
    currentEntry: MalListEntry?,
    onDismiss: () -> Unit,
    onConfirm: (Int, MalListUpdate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedStatus by remember { mutableStateOf(currentEntry?.status ?: WatchStatus.PLAN_TO_WATCH) }
    var episodesText by remember { mutableStateOf(currentEntry?.episodesWatched?.toString() ?: "0") }
    var score by remember { mutableFloatStateOf(currentEntry?.score?.toFloat() ?: 0f) }

    val statuses = WatchStatus.entries.filter { it != WatchStatus.NOT_IN_LIST }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Status", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            statuses.forEach { status ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedStatus == status, onClick = { selectedStatus = status })
                    Spacer(Modifier.width(8.dp))
                    Text(status.displayName)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = episodesText,
                onValueChange = { if (it.all { c -> c.isDigit() }) episodesText = it },
                label = { Text("Epizode") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Ocena: ${score.toInt()}")
            Slider(value = score, onValueChange = { score = it }, valueRange = 0f..10f, steps = 9)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Otkaži") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onConfirm(
                            animeId, MalListUpdate(
                                status = selectedStatus,
                                episodesWatched = episodesText.toIntOrNull(),
                                score = score.toInt().takeIf { it > 0 }
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Sačuvaj") }
            }
        }
    }
}
