package com.example.jaboard.feature.compass

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.core.common.Formatters
import com.example.jaboard.core.design.ExpressiveCard
import com.example.jaboard.core.design.JaBoardLargeTopBar
import com.example.jaboard.core.design.JaBoardMonoStyles
import com.example.jaboard.core.design.JaBoardStatusColors
import com.example.jaboard.core.design.MetricProgressRing
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompassScreen(viewModel: CompassViewModel, onBack: () -> Unit) {
  val heading by viewModel.heading.collectAsStateWithLifecycle()
  val altitude by viewModel.altitude.collectAsStateWithLifecycle()
  val accuracy by viewModel.accuracy.collectAsStateWithLifecycle()
  val speed by viewModel.speed.collectAsStateWithLifecycle()
  val latitude by viewModel.latitude.collectAsStateWithLifecycle()
  val longitude by viewModel.longitude.collectAsStateWithLifecycle()
  val gpsStatus by viewModel.gpsStatus.collectAsStateWithLifecycle()
  val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()

  val animatedHeading by animateFloatAsState(
    targetValue = heading,
    animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
    label = "compass_heading",
  )

  val context = LocalContext.current
  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) viewModel.refreshPermission()
  }

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      JaBoardLargeTopBar(
        title = "Compass",
        subtitle = gpsStatus,
        scrollBehavior = scrollBehavior,
        actions = {
          if (hasPermission) {
            IconButton(
              onClick = { /* Could add calibration or settings */ },
              modifier = Modifier.testTag("compass_settings_button"),
            ) {
              Icon(Icons.Rounded.Explore, contentDescription = "Compass options")
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (!hasPermission) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(80.dp),
          ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.padding(20.dp).size(40.dp))
          }
          Spacer(modifier = Modifier.height(24.dp))
          Text("Location Permission Required", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            "Compass needs approximate location access to show heading, altitude, and GPS data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
          )
          Spacer(modifier = Modifier.height(24.dp))
          FilledTonalButton(
            onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            modifier = Modifier.testTag("compass_permission_button"),
          ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Permission")
          }
        }
      } else {
        // Main Compass Rose - Simple version using rotating Box
        ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(320.dp)
              .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            // Outer compass ring
            Box(
              modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
              contentAlignment = Alignment.Center,
            ) {
              // Rotating needle
              Box(
                modifier = Modifier
                  .size(280.dp)
                  .graphicsLayer {
                    rotationZ = -animatedHeading
                  },
                contentAlignment = Alignment.Center,
              ) {
                // North needle (red triangle pointing up)
                Box(
                  modifier = Modifier
                    .width(6.dp)
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.error)
                    .clip(RoundedCornerShape(3.dp)),
                )
                // Center pivot
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                )
                // South needle (white triangle pointing down)
                Box(
                  modifier = Modifier
                    .width(4.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(RoundedCornerShape(2.dp))
                    .padding(top = 130.dp),
                )
              }

              // Cardinal direction labels (fixed, not rotating)
              Box(modifier = Modifier.size(280.dp)) {
                // N
                Text(
                  text = "N",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                )
                // E
                Text(
                  text = "E",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                )
                // S
                Text(
                  text = "S",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                )
                // W
                Text(
                  text = "W",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                )
              }

              // Center heading display
              Column(
                modifier = Modifier
                  .size(120.dp)
                  .graphicsLayer {
                    rotationZ = animatedHeading
                  },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = viewModel.getCardinalDirection(heading),
                  style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${heading.roundToInt()}°",
                  style = JaBoardMonoStyles.MetricReadout.copy(fontSize = 18.sp),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          // Heading & Cardinal Direction Card
          ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = viewModel.getCardinalName(heading),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  )
                  Text(
                    text = "Magnetic Heading",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = "${heading.roundToInt()}°",
                  style = JaBoardMonoStyles.HeroNumeric.copy(fontSize = 36.sp),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          // Data Grid
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Altitude & Accuracy
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CompassDataTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocationOn,
                title = "Altitude",
                value = String.format("%.0f m", altitude),
                subtitle = "MSL",
                accentColor = MaterialTheme.colorScheme.secondary,
              )
              CompassDataTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Navigation,
                title = "Accuracy",
                value = String.format("±%.0f m", accuracy),
                subtitle = "GPS precision",
                accentColor = JaBoardStatusColors.WarningAmber,
              )
            }

            // Row 2: Speed & GPS Status
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CompassDataTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Speed,
                title = "Speed",
                value = String.format("%.1f km/h", speed * 3.6),
                subtitle = if (speed * 3.6 > 5) "Moving" else "Stationary",
                accentColor = MaterialTheme.colorScheme.tertiary,
              )
              CompassDataTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.MyLocation,
                title = "GPS Status",
                value = if (gpsStatus == "GPS Active") "Active" else "Searching",
                subtitle = gpsStatus,
                accentColor = if (gpsStatus == "GPS Active") JaBoardStatusColors.HealthyGreen else JaBoardStatusColors.WarningAmber,
              )
            }

            // Row 3: Coordinates
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                  ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.padding(8.dp).size(24.dp))
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Coordinates", style = MaterialTheme.typography.titleSmall)
                    Text(
                      text = "Lat: ${String.format("%.6f", latitude)}, Lon: ${String.format("%.6f", longitude)}",
                      style = JaBoardMonoStyles.SpecValue,
                      maxLines = 1,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompassDataTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  ExpressiveCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = value,
        style = JaBoardMonoStyles.MetricReadout.copy(fontWeight = FontWeight.Bold),
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}