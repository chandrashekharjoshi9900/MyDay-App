package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.model.JournalEntryItem
import com.myday.app.domain.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class JournalUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val entries: List<JournalEntryItem> = emptyList(),
    val totalCount: Int = 0
)

class JournalViewModel(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<JournalUiState> = combine(
        journalRepository.observeAllEntries(),
        searchQuery
    ) { allEntries, query ->
        val trimmed = query.trim().lowercase()
        val filtered = if (trimmed.isBlank()) {
            allEntries
        } else {
            allEntries.filter { entry ->
                entry.title.lowercase().contains(trimmed) ||
                    entry.content.lowercase().contains(trimmed) ||
                    entry.achievements.lowercase().contains(trimmed) ||
                    entry.reflection.lowercase().contains(trimmed)
            }
        }
        JournalUiState(
            isLoading = false,
            searchQuery = query,
            entries = filtered,
            totalCount = allEntries.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JournalUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun saveEntry(
        existing: JournalEntryItem?,
        epochDay: Long,
        title: String,
        content: String,
        achievements: String,
        reflection: String,
        moodLevel: Int
    ) {
        if (title.isBlank() && content.isBlank() && achievements.isBlank() && reflection.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entry = JournalEntryItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                epochDay = epochDay,
                title = title.trim().ifBlank { "Daily Note" },
                content = content.trim(),
                achievements = achievements.trim(),
                reflection = reflection.trim(),
                moodLevel = moodLevel.coerceIn(1, 5),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            journalRepository.upsertEntry(entry)
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            journalRepository.deleteEntry(entryId)
        }
    }
}
