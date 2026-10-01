package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.repository.WatchSourceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WatchSourcesViewModel @Inject constructor(
    private val repository: WatchSourceRepository,
) : ViewModel() {

    val sources: StateFlow<List<WatchSource>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSource(name: String, urlTemplate: String, openExternally: Boolean) {
        viewModelScope.launch {
            repository.add(
                name,
                urlTemplate,
                faviconUrl(urlTemplate),
                openExternally,
            )
        }
    }

    fun updateSource(
        source: WatchSource,
        name: String,
        urlTemplate: String,
        openExternally: Boolean,
    ) {
        viewModelScope.launch {
            repository.update(
                source.copy(
                    name = name,
                    urlTemplate = urlTemplate,
                    faviconUrl = faviconUrl(urlTemplate),
                    openExternally = openExternally,
                ),
            )
        }
    }

    fun setOpenExternally(source: WatchSource, openExternally: Boolean) {
        viewModelScope.launch {
            repository.update(source.copy(openExternally = openExternally))
        }
    }

    fun deleteSource(source: WatchSource) {
        viewModelScope.launch { repository.delete(source) }
    }

    private fun faviconUrl(urlTemplate: String): String? {
        val domain = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#]+)")
            .find(urlTemplate)
            ?.groupValues
            ?.get(1)
        return domain?.let { "https://www.google.com/s2/favicons?domain=$it&sz=64" }
    }
}
