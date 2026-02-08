package com.lbs.dr.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lbs.dr.domain.model.ActivityType
import com.lbs.dr.domain.model.FloorTransition
import com.lbs.dr.domain.model.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trail by viewModel.positionTrail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LBS/DR Indoor Positioning") },
                actions = {
                    IconButton(onClick = { viewModel.resetEngine() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.isTracking) viewModel.stopTracking()
                    else viewModel.startTracking()
                },
                containerColor = if (uiState.isTracking) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    if (uiState.isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isTracking) "Stop" else "Start"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Position display card
            PositionCard(uiState)

            // Floor info card
            FloorCard(uiState)

            // Sensor status card
            SensorStatusCard(uiState)

            // Position trail visualization
            TrailCanvas(
                trail = trail,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun PositionCard(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Position",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            state.currentPosition?.let { pos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Lat: ${"%.6f".format(pos.latitude)}", fontSize = 14.sp)
                        Text("Lon: ${"%.6f".format(pos.longitude)}", fontSize = 14.sp)
                        Text("Alt: ${"%.1f".format(pos.altitude)} m", fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Accuracy: ${"%.1f".format(pos.accuracy)} m", fontSize = 14.sp)
                        Text("Source: ${pos.source.name}", fontSize = 14.sp)
                        state.motionState?.let { motion ->
                            val activityIcon = when (motion.activity) {
                                ActivityType.WALKING -> "Walking"
                                ActivityType.RUNNING -> "Running"
                                ActivityType.CLIMBING_STAIRS -> "Stairs Up"
                                ActivityType.DESCENDING_STAIRS -> "Stairs Down"
                                ActivityType.ELEVATOR -> "Elevator"
                                else -> "Stationary"
                            }
                            Text(activityIcon, fontSize = 14.sp)
                        }
                    }
                }
            } ?: Text("Waiting for position...", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun FloorCard(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Floor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                state.floorInfo?.let { floor ->
                    val transitionText = when (floor.transition) {
                        FloorTransition.ASCENDING -> " (ascending)"
                        FloorTransition.DESCENDING -> " (descending)"
                        FloorTransition.NONE -> ""
                    }
                    Text(
                        "Level ${floor.currentFloor}$transitionText",
                        fontSize = 14.sp
                    )
                    Text(
                        "Confidence: ${"%.0f".format(floor.confidence * 100)}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } ?: Text("Calibrating...", fontSize = 14.sp, color = Color.Gray)
            }

            // Large floor number display
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${state.floorInfo?.currentFloor ?: "?"}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SensorStatusCard(state: MainUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Sensors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorChip(
                    label = "Steps",
                    value = "${state.stepCount}"
                )
                SensorChip(
                    label = "WiFi",
                    value = "${state.wifiApCount} AP"
                )
                SensorChip(
                    label = "RTT",
                    value = "${state.rttApCount} AP"
                )
                SensorChip(
                    label = "BLE",
                    value = "${state.beaconCount}"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                state.pressure?.let {
                    SensorChip(label = "Pressure", value = "${"%.1f".format(it)} hPa")
                }
                state.temperature?.let {
                    SensorChip(label = "Temp", value = "${"%.1f".format(it)} C")
                }
                state.motionState?.let {
                    SensorChip(label = "Speed", value = "${"%.1f".format(it.speed)} m/s")
                }
            }
        }
    }
}

@Composable
private fun SensorChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

/**
 * Simple canvas showing the position trail as a line plot.
 */
@Composable
private fun TrailCanvas(trail: List<Position>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (trail.size < 2) {
                Text(
                    "Position trail will appear here...",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val minX = trail.minOf { it.longitude }
                    val maxX = trail.maxOf { it.longitude }
                    val minY = trail.minOf { it.latitude }
                    val maxY = trail.maxOf { it.latitude }
                    val rangeX = (maxX - minX).coerceAtLeast(0.00001)
                    val rangeY = (maxY - minY).coerceAtLeast(0.00001)

                    val padding = 20f

                    // Draw trail line
                    for (i in 1 until trail.size) {
                        val x1 = padding + ((trail[i - 1].longitude - minX) / rangeX * (size.width - 2 * padding)).toFloat()
                        val y1 = size.height - padding - ((trail[i - 1].latitude - minY) / rangeY * (size.height - 2 * padding)).toFloat()
                        val x2 = padding + ((trail[i].longitude - minX) / rangeX * (size.width - 2 * padding)).toFloat()
                        val y2 = size.height - padding - ((trail[i].latitude - minY) / rangeY * (size.height - 2 * padding)).toFloat()

                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 3f
                        )
                    }

                    // Draw current position
                    val last = trail.last()
                    val cx = padding + ((last.longitude - minX) / rangeX * (size.width - 2 * padding)).toFloat()
                    val cy = size.height - padding - ((last.latitude - minY) / rangeY * (size.height - 2 * padding)).toFloat()

                    drawCircle(
                        color = primaryColor,
                        radius = 8f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.3f),
                        radius = 20f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
