package com.example.jaboard.feature.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardUtilityTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel, onBack: () -> Unit) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      JaBoardUtilityTopBar(
        title = "Calculator",
        subtitle = if (state.isScientificExpanded) "Scientific mode" else "Standard mode",
        onBack = onBack,
        actions = {
          IconButton(
            onClick = viewModel::toggleScientific,
            modifier = Modifier.testTag("calc_scientific_toggle"),
          ) {
            Icon(Icons.Rounded.Functions, contentDescription = "Toggle scientific functions")
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.history.isNotEmpty()) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp),
        ) {
          items(state.history) { entry ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.clickable { viewModel.reuseHistoryResult(entry.result) },
            ) {
              Text(
                text = "${entry.expression} = ${entry.result}",
                style = JaBoardMonoStyles.SpecValue,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      ExpressiveCard(
        modifier = Modifier.fillMaxWidth().weight(1f),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(22.dp),
          verticalArrangement = Arrangement.Bottom,
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            text = state.expression.ifBlank { "0" },
            style = JaBoardMonoStyles.HeroNumeric.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().testTag("calc_display"),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = state.errorMessage ?: state.livePreview,
            style = JaBoardMonoStyles.MetricReadout,
            color =
              if (state.errorMessage != null) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().testTag("calc_preview"),
          )
        }
      }

      AnimatedVisibility(visible = state.isScientificExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf("sin(" to "sin", "cos(" to "cos", "tan(" to "tan", "√(" to "√").forEach {
              (token, label) ->
              CalcKeyButton(
                label = label,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).height(48.dp),
                onClick = { viewModel.appendToken(token) },
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf("ln(" to "ln", "log(" to "log", "^" to "^", "π" to "π").forEach {
              (token, label) ->
              CalcKeyButton(
                label = label,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).height(48.dp),
                onClick = { viewModel.appendToken(token) },
              )
            }
          }
        }
      }

      val keypadRows =
        listOf(
          listOf("AC", "(", ")", "÷"),
          listOf("7", "8", "9", "×"),
          listOf("4", "5", "6", "−"),
          listOf("1", "2", "3", "+"),
          listOf("0", ".", "%", "="),
        )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = viewModel::backspace,
            modifier = Modifier.testTag("calc_backspace"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
              )
              Text("DEL", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        keypadRows.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            row.forEach { key ->
              val isOperator = key in listOf("÷", "×", "−", "+", "(", ")")
              val isEquals = key == "="
              val isClear = key == "AC"

              val bg =
                when {
                  isEquals -> MaterialTheme.colorScheme.primary
                  isClear -> MaterialTheme.colorScheme.errorContainer
                  isOperator -> MaterialTheme.colorScheme.secondaryContainer
                  else -> MaterialTheme.colorScheme.surfaceContainerLow
                }
              val fg =
                when {
                  isEquals -> MaterialTheme.colorScheme.onPrimary
                  isClear -> MaterialTheme.colorScheme.onErrorContainer
                  isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
                  else -> MaterialTheme.colorScheme.onSurface
                }

              CalcKeyButton(
                label = key,
                containerColor = bg,
                contentColor = fg,
                modifier = Modifier.weight(1f).height(62.dp).testTag("calc_key_$key"),
                onClick = {
                  when (key) {
                    "AC" -> viewModel.clear()
                    "=" -> viewModel.evaluateEquals()
                    else -> viewModel.appendToken(key)
                  }
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalcKeyButton(
  label: String,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(22.dp),
    color = containerColor,
    contentColor = contentColor,
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
      )
    }
  }
}
