package com.example.jaboard.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jaboard.core.database.TodoEntity
import com.example.jaboard.core.database.TodoPriority
import com.example.jaboard.core.database.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TodoFilter(val label: String) {
  ALL("All"),
  ACTIVE("Active"),
  COMPLETED("Completed"),
}

data class TodoUiState(
  val items: List<TodoEntity> = emptyList(),
  val filter: TodoFilter = TodoFilter.ALL,
  val activeCount: Int = 0,
  val completedCount: Int = 0,
)

class TodoViewModel(private val todoRepository: TodoRepository) : ViewModel() {

  private val _filter = MutableStateFlow(TodoFilter.ALL)

  val uiState: StateFlow<TodoUiState> =
    combine(todoRepository.allTodos, _filter) { all, filter ->
        val filtered =
          when (filter) {
            TodoFilter.ALL -> all
            TodoFilter.ACTIVE -> all.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> all.filter { it.isCompleted }
          }
        TodoUiState(
          items = filtered,
          filter = filter,
          activeCount = all.count { !it.isCompleted },
          completedCount = all.count { it.isCompleted },
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoUiState(),
      )

  fun setFilter(filter: TodoFilter) {
    _filter.value = filter
  }

  fun addTodo(title: String, notes: String, priority: TodoPriority, category: String) {
    if (title.isBlank()) return
    viewModelScope.launch { todoRepository.addTodo(title, notes, priority, category) }
  }

  fun toggleCompleted(todo: TodoEntity) {
    viewModelScope.launch { todoRepository.setCompleted(todo.id, !todo.isCompleted) }
  }

  fun deleteTodo(id: Long) {
    viewModelScope.launch { todoRepository.deleteById(id) }
  }

  fun clearCompleted() {
    viewModelScope.launch { todoRepository.clearCompleted() }
  }
}
