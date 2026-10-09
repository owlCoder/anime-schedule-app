package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.datastore.PersonalBackupStore
import com.owlcoder.animeschedule.domain.model.PersonalBackup
import com.owlcoder.animeschedule.presentation.components.*
import java.time.LocalDate
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeLengthSheet(minutes: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.episode_length)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.episode_length_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledTonalIconButton({ onChange(minutes - 1) }, enabled = minutes > 1, modifier = Modifier.testTag("duration-decrease")) { Icon(Icons.Default.Remove, stringResource(R.string.duration_decrease)) }
                    Text(stringResource(R.string.episode_length_value, minutes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FilledTonalIconButton({ onChange(minutes + 1) }, enabled = minutes < 180, modifier = Modifier.testTag("duration-increase")) { Icon(Icons.Default.Add, stringResource(R.string.duration_increase)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12, 24, 45).forEach { value -> AppButton(stringResource(R.string.episode_length_value, value), { onChange(value) }, Modifier.weight(1f), variant = AppButtonVariant.Secondary, icon = Icons.Default.Timer) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalBackupSheet(store: PersonalBackupStore, username: String, onDismiss: () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<PersonalBackup?>(null) }
    val successExport = stringResource(R.string.backup_exported)
    val successRestore = stringResource(R.string.backup_restored)
    val failure = stringResource(R.string.backup_failed)
    val invalid = stringResource(R.string.backup_invalid)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val content = store.export()
                    val stream = resolver.openOutputStream(uri) ?: error("No output stream")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
                }
                feedback = successExport; feedbackError = false
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { feedback = failure; feedbackError = true }
            finally { busy = false }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                preview = withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: error("No input stream")
                    val bytes = stream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - output.size()))
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            require(output.size() <= 2_000_000)
                        }
                        output.toByteArray()
                    }
                    require(bytes.size <= 2_000_000)
                    PersonalBackup.decode(bytes.toString(Charsets.UTF_8))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { preview = null; feedback = invalid; feedbackError = true }
            finally { busy = false }
        }
    }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.personal_backup)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppButton(stringResource(R.string.backup_export), { export.launch("anime-personal-${LocalDate.now()}.json") }, Modifier.fillMaxWidth().testTag("backup-export"), enabled = !busy, icon = Icons.Default.FileDownload)
            AppButton(stringResource(R.string.backup_select), { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.fillMaxWidth().testTag("backup-select"), variant = AppButtonVariant.Secondary, enabled = !busy, icon = Icons.Default.FileUpload)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            feedback?.let { message ->
                Surface(shape = MaterialTheme.shapes.large, color = if (feedbackError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (feedbackError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null)
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            preview?.let { backup ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.backup_preview, backup.tools.favorites.size, backup.tools.notes.size, backup.tools.tags.size, backup.tools.activity.size), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.backup_replace_hint, username.ifBlank { stringResource(R.string.backup_guest) }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppButton(stringResource(R.string.backup_restore), {
                            scope.launch {
                                busy = true
                                try { store.restore(backup); preview = null; feedback = successRestore; feedbackError = false }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { feedback = failure; feedbackError = true }
                                finally { busy = false }
                            }
                        }, Modifier.fillMaxWidth().testTag("backup-restore"), enabled = !busy, icon = Icons.Default.Restore)
                        AppButton(stringResource(R.string.common_cancel), { preview = null }, Modifier.fillMaxWidth(), variant = AppButtonVariant.Plain, enabled = !busy, icon = Icons.Default.Close)
                    }
                }
            }
        }
    }
}
