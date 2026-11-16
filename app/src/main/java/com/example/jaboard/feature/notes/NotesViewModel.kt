package com.example.jaboard.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.database.NoteEntity
import com.example.jaboard.core.database.NotesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteEditorState(
  val id: Long = 0L,
  val title: String = "",
  val content: String = "",
  val isPinned: Boolean = false,
  val colorTag: String = "DEFAULT",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isLoaded: Boolean = false,
)

class NotesViewModel(private val notesRepository: NotesRepository) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  val notes: StateFlow<List<NoteEntity>> =
    combine(notesRepository.allNotes, _searchQuery) { list, query ->
        val q = query.trim()
        if (q.isEmpty()) list
        else
          list.filter {
            it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

  private val _editorState = MutableStateFlow(NoteEditorState())
  val editorState: StateFlow<NoteEditorState> = _editorState

  private var autoSaveJob: Job? = null

  fun onSearchChanged(query: String) {
    _searchQuery.value = query
  }

  fun togglePin(note: NoteEntity) {
    viewModelScope.launch { notesRepository.togglePin(note) }
  }

  fun deleteNote(id: Long) {
    viewModelScope.launch { notesRepository.deleteById(id) }
  }

  fun loadNoteForEditing(noteId: Long) {
    autoSaveJob?.cancel()
    if (noteId <= 0L) {
      val now = System.currentTimeMillis()
      _editorState.value =
        NoteEditorState(id = 0L, createdAt = now, updatedAt = now, isLoaded = true)
    } else {
      viewModelScope.launch {
        val existing = notesRepository.getById(noteId)
        if (existing != null) {
          _editorState.value =
            NoteEditorState(
              id = existing.id,
              title = existing.title,
              content = existing.content,
              isPinned = existing.isPinned,
              colorTag = existing.colorTag,
              createdAt = existing.createdAt,
              updatedAt = existing.updatedAt,
              isLoaded = true,
            )
        } else {
          _editorState.value = NoteEditorState(isLoaded = true)
        }
      }
    }
  }

  fun updateEditorTitle(title: String) {
    _editorState.value =
      _editorState.value.copy(title = title, updatedAt = System.currentTimeMillis())
    scheduleAutoSave()
  }

  fun updateEditorContent(content: String) {
    _editorState.value =
      _editorState.value.copy(content = content, updatedAt = System.currentTimeMillis())
    scheduleAutoSave()
  }

  fun updateEditorColorTag(tag: String) {
    _editorState.value =
      _editorState.value.copy(colorTag = tag, updatedAt = System.currentTimeMillis())
    scheduleAutoSave()
  }

  fun toggleEditorPin() {
    _editorState.value =
      _editorState.value.copy(
        isPinned = !_editorState.value.isPinned,
        updatedAt = System.currentTimeMillis(),
      )
    scheduleAutoSave(immediate = true)
  }

  fun flushAutoSave() {
    scheduleAutoSave(immediate = true)
  }

  private fun scheduleAutoSave(immediate: Boolean = false) {
    autoSaveJob?.cancel()
    autoSaveJob =
      viewModelScope.launch {
        if (!immediate) delay(300L)
        val state = _editorState.value
        if (state.title.isBlank() && state.content.isBlank()) {
          if (state.id > 0L) {
            notesRepository.deleteById(state.id)
            _editorState.value = state.copy(id = 0L)
          }
          return@launch
        }
        val savedId =
          notesRepository.upsert(
            NoteEntity(
              id = state.id,
              title = state.title.trim(),
              content = state.content,
              isPinned = state.isPinned,
              colorTag = state.colorTag,
              createdAt = state.createdAt,
              updatedAt = System.currentTimeMillis(),
            )
          )
        if (state.id == 0L && savedId > 0L) {
          _editorState.value = _editorState.value.copy(id = savedId)
        }
      }
  }
}
