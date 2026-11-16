package com.example.jaboard.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.database.NoteEntity
import com.example.jaboard.core.design.EmptyStateView
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardUtilityTopBar

private val NoteColorTags = listOf("DEFAULT", "COBALT", "TEAL", "AMBER", "ROSE")

@Composable
private fun noteContainerColor(tag: String): Color =
  when (tag) {
    "COBALT" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    "TEAL" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    "AMBER" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    "ROSE" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    else -> MaterialTheme.colorScheme.surfaceContainerLow
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
  viewModel: NotesViewModel,
  onBack: () -> Unit,
  onOpenNote: (Long) -> Unit,
) {
  val notes by viewModel.notes.collectAsStateWithLifecycle()
  val query by viewModel.searchQuery.collectAsStateWithLifecycle()

  val pinned = notes.filter { it.isPinned }
  val unpinned = notes.filter { !it.isPinned }

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "Notes",
        subtitle = "${notes.size} local notes",
        onBack = onBack,
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { onOpenNote(-1L) },
        modifier = Modifier.testTag("notes_add_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Create note")
      }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      OutlinedTextField(
        value = query,
        onValueChange = viewModel::onSearchChanged,
        modifier =
          Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("notes_search_input"),
        placeholder = { Text("Search notes...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { viewModel.onSearchChanged("") }) {
              Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.cd_clear_search))
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
      )

      if (notes.isEmpty()) {
        EmptyStateView(
          icon = Icons.Rounded.Description,
          title = if (query.isBlank()) "No notes yet" else "No matching notes",
          message =
            if (query.isBlank())
              "Capture quick thoughts, checklists, or snippets. Notes are saved automatically on this device."
            else "Try a different search term.",
          actionLabel = if (query.isBlank()) "New Note" else null,
          onAction = if (query.isBlank()) ({ onOpenNote(-1L) }) else null,
          modifier = Modifier.weight(1f),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (pinned.isNotEmpty()) {
            item {
              Text(
                "Pinned",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            items(pinned, key = { it.id }) { note ->
              NoteRowCard(
                note = note,
                onClick = { onOpenNote(note.id) },
                onTogglePin = { viewModel.togglePin(note) },
                onDelete = { viewModel.deleteNote(note.id) },
              )
            }
          }
          if (unpinned.isNotEmpty()) {
            if (pinned.isNotEmpty()) {
              item {
                Text(
                  "Others",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 6.dp),
                )
              }
            }
            items(unpinned, key = { it.id }) { note ->
              NoteRowCard(
                note = note,
                onClick = { onOpenNote(note.id) },
                onTogglePin = { viewModel.togglePin(note) },
                onDelete = { viewModel.deleteNote(note.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NoteRowCard(
  note: NoteEntity,
  onClick: () -> Unit,
  onTogglePin: () -> Unit,
  onDelete: () -> Unit,
) {
  ExpressiveCard(
    modifier = Modifier.fillMaxWidth().testTag("note_card_${note.id}"),
    containerColor = noteContainerColor(note.colorTag),
    onClick = onClick,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = note.title.ifBlank { "Untitled note" },
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Row {
          IconButton(onClick = onTogglePin) {
            Icon(
              imageVector =
                if (note.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
              contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = "Delete note",
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
      if (note.content.isNotBlank()) {
        Text(
          text = note.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = "Edited ${Formatters.formatRelativeTimestamp(note.updatedAt)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
  viewModel: NotesViewModel,
  noteId: Long,
  onBack: () -> Unit,
) {
  val state by viewModel.editorState.collectAsStateWithLifecycle()

  LaunchedEffect(noteId) { viewModel.loadNoteForEditing(noteId) }

  DisposableEffect(Unit) {
    onDispose { viewModel.flushAutoSave() }
  }

  Scaffold(
    containerColor = noteContainerColor(state.colorTag),
    topBar = {
      JaBoardUtilityTopBar(
        title = if (state.id > 0L) "Edit Note" else "New Note",
        subtitle = "Auto-saved locally",
        onBack = {
          viewModel.flushAutoSave()
          onBack()
        },
        actions = {
          IconButton(onClick = viewModel::toggleEditorPin) {
            Icon(
              imageVector =
                if (state.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
              contentDescription = if (state.isPinned) "Unpin note" else "Pin note",
            )
          }
          if (state.id > 0L) {
            IconButton(
              onClick = {
                viewModel.deleteNote(state.id)
                onBack()
              }
            ) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete note")
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(NoteColorTags) { tag ->
          FilterChip(
            selected = state.colorTag == tag,
            onClick = { viewModel.updateEditorColorTag(tag) },
            label = { Text(tag.lowercase().replaceFirstChar { it.uppercase() }) },
          )
        }
      }

      OutlinedTextField(
        value = state.title,
        onValueChange = viewModel::updateEditorTitle,
        modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
        placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
        textStyle = MaterialTheme.typography.headlineSmall,
        singleLine = true,
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
          ),
      )

      OutlinedTextField(
        value = state.content,
        onValueChange = viewModel::updateEditorContent,
        modifier = Modifier.fillMaxWidth().weight(1f).testTag("note_content_input"),
        placeholder = { Text("Start writing...") },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
          ),
      )
    }
  }
}
