package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.usecase.AddWatchSourceUseCase
import com.owlcoder.animeschedule.domain.usecase.DeleteWatchSourceUseCase
import com.owlcoder.animeschedule.domain.usecase.GetWatchSourcesUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateWatchSourceUseCase
import javax.inject.Inject

@HiltViewModel
class WatchSourcesViewModel @Inject constructor(
    getWatchSourcesUseCase: GetWatchSourcesUseCase,
    private val addWatchSourceUseCase: AddWatchSourceUseCase,
    private val deleteWatchSourceUseCase: DeleteWatchSourceUseCase,
    private val updateWatchSourceUseCase: UpdateWatchSourceUseCase
) : ViewModel() {

    val sources: StateFlow<List<WatchSource>> = getWatchSourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSource(name: String, urlTemplate: String, openExternally: Boolean) {
        viewModelScope.launch {
            // urlTemplate is like "https://host/path?q={query}" — {} isn't valid URI syntax,
            // so java.net.URI would throw. Pull the host out with a regex instead.
            val domain = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#]+)").find(urlTemplate)?.groupValues?.get(1)
            val faviconUrl = domain?.let { "https://www.google.com/s2/favicons?domain=$it&sz=64" }
            addWatchSourceUseCase(name, urlTemplate, faviconUrl, openExternally)
        }
    }

    fun setOpenExternally(source: WatchSource, openExternally: Boolean) {
        viewModelScope.launch { updateWatchSourceUseCase(source.copy(openExternally = openExternally)) }
    }

    fun deleteSource(source: WatchSource) {
        viewModelScope.launch { deleteWatchSourceUseCase(source) }
    }
}
