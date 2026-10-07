package com.owlcoder.animeschedule.presentation.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalResources
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.UndoListChange
import com.owlcoder.animeschedule.domain.repository.MalRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
fun ListUndoMessages(repository: MalRepository, toast: ToastController) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    fun offer(change: UndoListChange, failed: Boolean = false) {
        toast.show(if (failed) resources.getString(R.string.undo_failed) else resources.getString(R.string.undo_list_change, change.title),
            if (failed) ToastTone.Error else ToastTone.Success,
            ToastAction(change.id, resources.getString(R.string.undo_action), {
                scope.launch {
                    val result = repository.undoListChange(change.id)
                    if (result is AppResult.Success) toast.success(resources.getString(R.string.undo_done))
                    else if (repository.undoChange.first()?.id == change.id) offer(change, failed = true)
                    else toast.error(resources.getString(R.string.undo_unavailable))
                }
            }, { repository.dismissUndo(change.id) }))
    }
    LaunchedEffect(repository, resources) {
        var lastKey: Long? = null
        repository.undoChange.collect { change ->
            if (change != null) offer(change) else lastKey?.let(toast::clearAction)
            lastKey = change?.id
        }
    }
}
