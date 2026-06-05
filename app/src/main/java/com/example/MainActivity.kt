package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: YoYoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgColor
                ) { innerPadding ->
                    val state by viewModel.uiState.collectAsState()
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val isWideScreen = maxWidth > 600.dp
                        
                        if (isWideScreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    DashboardPanel(
                                        state = state,
                                        onStartPause = viewModel::togglePlayPause,
                                        onReset = viewModel::reset
                                    )
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    ParticipantsPanel(
                                        participants = state.participants,
                                        onParticipantClick = viewModel::onParticipantClick
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                DashboardPanel(
                                    state = state,
                                    onStartPause = viewModel::togglePlayPause,
                                    onReset = viewModel::reset
                                )
                                ParticipantsPanel(
                                    participants = state.participants,
                                    onParticipantClick = viewModel::onParticipantClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardPanel(
    state: TrackerState,
    onStartPause: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PanelBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val MAX_DISTANCE = 3500f
        val MAX_SPEED = 20f
        
        val currentShuttle = state.currentShuttle
        val speedFraction = (currentShuttle.speed.toFloat() / MAX_SPEED).coerceIn(0f, 1f)
        val distFraction = (state.globalLiveDistance.toFloat() / MAX_DISTANCE).coerceIn(0f, 1f)
        val paceFraction = if (state.phaseDuration > 0) {
            (state.timeLeft.toFloat() / state.phaseDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        val animatedPace by animateFloatAsState(targetValue = paceFraction, label = "pace")
        val animatedSpeed by animateFloatAsState(targetValue = speedFraction, label = "speed")
        val animatedDist by animateFloatAsState(targetValue = distFraction, label = "dist")
        
        val statusText = when (state.phase) {
            Phase.IDLE -> "READY"
            Phase.RUN -> "RUN"
            Phase.REST -> "REST"
            Phase.DONE -> "DONE"
        }
        val statusColor = when (state.phase) {
            Phase.IDLE -> TextMuted
            Phase.RUN -> AccentRun
            Phase.REST -> AccentRest
            Phase.DONE -> AccentDist
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val size = size.minDimension
                val center = Offset(size / 2, size / 2)
                
                val ringBgColor = Color(0xFF334155)
                
                // Outer Ring: Distance
                val rDist = size / 2 * 0.95f
                val strokeDist = size * 0.045f
                drawCircle(color = ringBgColor, radius = rDist, center = center, style = Stroke(width = strokeDist))
                val distSweep = 360f * animatedDist
                drawArc(
                    brush = Brush.linearGradient(listOf(Color(0xFF10b981), Color(0xFF34d399))),
                    startAngle = -90f, sweepAngle = distSweep, useCenter = false,
                    style = Stroke(width = strokeDist, cap = StrokeCap.Round),
                    size = Size(rDist * 2, rDist * 2), topLeft = Offset(center.x - rDist, center.y - rDist)
                )

                // Middle Ring: Pace
                val rPace = size / 2 * 0.8f
                val strokePace = size * 0.07f
                drawCircle(color = ringBgColor, radius = rPace, center = center, style = Stroke(width = strokePace))
                val paceSweep = 360f * (1f - animatedPace)
                drawArc(
                    color = if (state.phase == Phase.REST) AccentRest else AccentRun,
                    startAngle = -90f, sweepAngle = paceSweep, useCenter = false,
                    style = Stroke(width = strokePace, cap = StrokeCap.Round),
                    size = Size(rPace * 2, rPace * 2), topLeft = Offset(center.x - rPace, center.y - rPace)
                )

                // Inner Arc: Speed
                val rSpeed = size / 2 * 0.65f
                val strokeSpeed = size * 0.055f
                val startAngleSpeed = 135f
                val sweepAngleSpeedMax = 270f
                drawArc(
                    color = ringBgColor, startAngle = startAngleSpeed, sweepAngle = sweepAngleSpeedMax,
                    useCenter = false, style = Stroke(width = strokeSpeed, cap = StrokeCap.Round),
                    size = Size(rSpeed * 2, rSpeed * 2), topLeft = Offset(center.x - rSpeed, center.y - rSpeed)
                )
                val speedSweep = sweepAngleSpeedMax * animatedSpeed
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFFfb923c), Color(0xFFf43f5e))),
                    startAngle = startAngleSpeed, sweepAngle = speedSweep, useCenter = false,
                    style = Stroke(width = strokeSpeed, cap = StrokeCap.Round),
                    size = Size(rSpeed * 2, rSpeed * 2), topLeft = Offset(center.x - rSpeed, center.y - rSpeed)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = statusText, color = statusColor,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(state.timeLeft), color = TextMain,
                        fontWeight = FontWeight.ExtraBold, fontSize = 56.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(text = "s", color = TextMuted, fontSize = 24.sp, modifier = Modifier.padding(start = 4.dp).alignByBaseline())
                }
                Text(
                    text = "Level ${currentShuttle.level} - Shuttle ${currentShuttle.shuttleNum}",
                    color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(currentShuttle.speed), color = AccentSpeed,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(text = " km/h", color = TextMuted, fontSize = 12.sp, modifier = Modifier.alignByBaseline())
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.globalLiveDistance.toInt().toString(), color = AccentDist,
                        fontWeight = FontWeight.Bold, fontSize = 24.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(text = " m", color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp).alignByBaseline())
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val startText = when {
                state.phase == Phase.DONE -> "FINISHED"
                state.isPlaying -> "PAUSE"
                state.phase != Phase.IDLE -> "RESUME"
                else -> "START"
            }
            val startBg = if (state.isPlaying || state.phase == Phase.DONE) AccentRest else AccentRun
            val startContent = if (state.isPlaying || state.phase == Phase.DONE) Color.Black else Color.Black
            
            Button(
                onClick = onStartPause,
                enabled = state.phase != Phase.DONE,
                colors = ButtonDefaults.buttonColors(containerColor = startBg, contentColor = startContent),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(startText, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RESET", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParticipantsPanel(
    participants: List<Participant>,
    onParticipantClick: (Participant) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PanelBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Runner Tracking",
            color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Sorting logic purely for UI rendering, without changing state IDs
        val sortedList = participants.sortedWith(compareBy<Participant> { 
            if (it.state == ParticipantState.SAVED) 1 else 0 
        }.thenByDescending { 
            it.savedDistance ?: 0 
        }.thenBy { it.id })

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = Int.MAX_VALUE
        ) {
            sortedList.forEach { participant ->
                ParticipantCard(
                    participant = participant,
                    onClick = { onParticipantClick(participant) },
                    modifier = Modifier.weight(1f, fill = false).defaultMinSize(minWidth = 120.dp)
                )
            }
        }
    }
}

@Composable
fun ParticipantCard(
    participant: Participant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (participant.state) {
        ParticipantState.ACTIVE -> Color(0xFF334155)
        ParticipantState.WARNED -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        ParticipantState.SAVED -> Color(0xFF1E293B)
    }
    
    val borderColor = when (participant.state) {
        ParticipantState.WARNED -> AccentRest
        ParticipantState.SAVED -> Color(0xFF334155)
        else -> Color.Transparent
    }
    
    val alpha = if (participant.state == ParticipantState.SAVED) 0.6f else 1f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = participant.state != ParticipantState.SAVED, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = participant.name, color = TextMain.copy(alpha = alpha),
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            maxLines = 1, modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        when (participant.state) {
            ParticipantState.WARNED -> {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentRest).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("1st Warn", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            ParticipantState.SAVED -> {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentDist).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${participant.savedDistance}m", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {}
        }
    }
}
