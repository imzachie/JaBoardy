package com.example.jaboard.core.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun ExpressiveCard(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(26.dp)
  if (onClick != null) {
    ElevatedCard(
      onClick = onClick,
      modifier = modifier,
      shape = shape,
      colors =
        CardDefaults.elevatedCardColors(
          containerColor = containerColor,
          contentColor = contentColor,
        ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
      content = content,
    )
  } else {
    Surface(
      modifier = modifier,
      shape = shape,
      color = containerColor,
      contentColor = contentColor,
      tonalElevation = 1.dp,
    ) {
      Column(content = content)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JaBoardUtilityTopBar(
  title: String,
  subtitle: String? = null,
  onBack: () -> Unit,
  actions: @Composable () -> Unit = {},
  scrollBehavior: TopAppBarScrollBehavior? = null,
) {
  TopAppBar(
    title = {
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = onBack, modifier = Modifier.testTag("top_bar_back_button")) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
        )
      }
    },
    actions = { actions() },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    scrollBehavior = scrollBehavior,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JaBoardLargeTopBar(
  title: String,
  subtitle: String? = null,
  actions: @Composable () -> Unit = {},
  scrollBehavior: TopAppBarScrollBehavior? = null,
) {
  LargeTopAppBar(
    title = {
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    actions = { actions() },
    colors =
      TopAppBarDefaults.largeTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    scrollBehavior = scrollBehavior,
  )
}

@Composable
fun MetricProgressRing(
  progress: Float,
  modifier: Modifier = Modifier,
  size: Dp = 88.dp,
  strokeWidth: Dp = 9.dp,
  activeColor: Color = MaterialTheme.colorScheme.primary,
  trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
  centerContent: @Composable () -> Unit = {},
) {
  val animatedProgress by
    animateFloatAsState(
      targetValue = progress.coerceIn(0f, 1f),
      animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
      label = "ring_progress",
    )

  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokePx = strokeWidth.toPx()
      drawArc(
        color = trackColor,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
      drawArc(
        color = activeColor,
        startAngle = 135f,
        sweepAngle = 270f * animatedProgress,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
    }
    centerContent()
  }
}

@Composable
fun PermissionPromptCard(
  icon: ImageVector,
  title: String,
  description: String,
  actionLabel: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
  secondaryLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  actionTestTag: String = "permission_grant_button",
) {
  ExpressiveCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(10.dp).size(24.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(onClick = onAction, modifier = Modifier.testTag(actionTestTag)) {
          Text(actionLabel)
        }
        if (secondaryLabel != null && onSecondaryAction != null) {
          FilledTonalButton(onClick = onSecondaryAction) { Text(secondaryLabel) }
        }
      }
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = MaterialTheme.colorScheme.primary,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.padding(18.dp).size(36.dp),
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(modifier = Modifier.height(18.dp))
      FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
  }
}

@Composable
fun KeyValueRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  monoValue: Boolean = false,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style =
        if (monoValue) JaBoardMonoStyles.SpecValue
        else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.End,
    )
  }
}
