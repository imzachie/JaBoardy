package com.example.jaboard.feature.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.database.TodoEntity
import com.example.jaboard.core.database.TodoPriority
import com.example.jaboard.core.design.EmptyStateView
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.JaBoardUtilityTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel, onBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showAddDialog by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "To-Do Lists",
        subtitle = "${uiState.activeCount} active • ${uiState.completedCount} done",
        onBack = onBack,
        actions = {
          if (uiState.completedCount > 0) {
            IconButton(
              onClick = viewModel::clearCompleted,
              modifier = Modifier.testTag("todo_clear_completed_button"),
            ) {
              Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear completed tasks")
            }
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        modifier = Modifier.testTag("todo_add_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add task")
      }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TodoFilter.entries.forEach { filter ->
          FilterChip(
            selected = uiState.filter == filter,
            onClick = { viewModel.setFilter(filter) },
            label = { Text(filter.label) },
            modifier = Modifier.testTag("todo_filter_${filter.name.lowercase()}"),
          )
        }
      }

      if (uiState.items.isEmpty()) {
        EmptyStateView(
          icon = Icons.Rounded.CheckCircleOutline,
          title =
            when (uiState.filter) {
              TodoFilter.ALL -> "No tasks yet"
              TodoFilter.ACTIVE -> "Everything completed"
              TodoFilter.COMPLETED -> "No completed tasks"
            },
          message = "Organize your daily priorities locally with offline Room persistence.",
          actionLabel = if (uiState.filter != TodoFilter.COMPLETED) "Add Task" else null,
          onAction = if (uiState.filter != TodoFilter.COMPLETED) ({ showAddDialog = true }) else null,
          modifier = Modifier.weight(1f),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(uiState.items, key = { it.id }) { todo ->
            TodoRowCard(
              todo = todo,
              onToggle = { viewModel.toggleCompleted(todo) },
              onDelete = { viewModel.deleteTodo(todo.id) },
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddTodoDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { title, notes, priority, category ->
        viewModel.addTodo(title, notes, priority, category)
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun TodoRowCard(
  todo: TodoEntity,
  onToggle: () -> Unit,
  onDelete: () -> Unit,
) {
  val priority = runCatching { TodoPriority.valueOf(todo.priority) }.getOrDefault(TodoPriority.MEDIUM)
  val priorityColor =
    when (priority) {
      TodoPriority.HIGH -> JaBoardStatusColors.CriticalRed
      TodoPriority.MEDIUM -> MaterialTheme.colorScheme.primary
      TodoPriority.LOW -> MaterialTheme.colorScheme.tertiary
    }

  ExpressiveCard(
    modifier = Modifier.fillMaxWidth().testTag("todo_row_${todo.id}"),
    onClick = onToggle,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = todo.isCompleted,
        onCheckedChange = { onToggle() },
        modifier = Modifier.testTag("todo_checkbox_${todo.id}"),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = todo.title,
          style = MaterialTheme.typography.titleMedium,
          textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
          color =
            if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
        if (todo.notes.isNotBlank()) {
          Text(
            text = todo.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = priorityColor.copy(alpha = 0.14f),
            contentColor = priorityColor,
          ) {
            Text(
              priority.label,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Text(
              todo.category,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete task")
      }
    }
  }
}

@Composable
private fun AddTodoDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, TodoPriority, String) -> Unit,
) {
  var title by rememberSaveable { mutableStateOf("") }
  var notes by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("General") }
  var priority by rememberSaveable { mutableStateOf(TodoPriority.MEDIUM) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Task") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Task title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("todo_title_input"),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (optional)") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Priority", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TodoPriority.entries.forEach { p ->
            FilterChip(
              selected = priority == p,
              onClick = { priority = p },
              label = { Text(p.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title, notes, priority, category) },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("todo_save_button"),
      ) {
        Text("Add")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
