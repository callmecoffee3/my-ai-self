package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.api.CinematicSynth
import com.example.data.CardEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinematicDramaPlayer(
    cardList: List<CardEntity>,
    initialCardId: Int? = null,
    onDismiss: () -> Unit
) {
    // Filter to only complete cards to ensure a polished playback experience
    val playlist = remember(cardList) { cardList.filter { it.status == "complete" } }
    
    if (playlist.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Narrative Streams Loaded", fontFamily = FontFamily.Monospace) },
            text = { Text("You must awaken or manually complete at least one memory node before initializing rendering mode.", fontFamily = FontFamily.Default) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK", fontFamily = FontFamily.Monospace)
                }
            }
        )
        return
    }

    // Determine starting scene index
    val startIndex = remember(playlist, initialCardId) {
        val found = playlist.indexOfFirst { it.id == initialCardId }
        if (found != -1) found else 0
    }

    var currentSceneIndex by remember { mutableStateOf(startIndex) }
    val currentCard = playlist[currentSceneIndex]

    // Playback control states
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    
    // Timer/progress states for current scene (scene length = 10 seconds)
    val sceneDurationMs = 10000L
    var elapsedMs by remember { mutableStateOf(0L) }
    
    // Real-time Synthesizer Engine
    val synth = remember { CinematicSynth() }
    val scope = rememberCoroutineScope()

    // Initialize and dispose Synth lifecycle
    DisposableEffect(Unit) {
        synth.start()
        synth.setMuted(isMuted)
        synth.updateScene(currentCard.emo)
        onDispose {
            synth.stop()
        }
    }

    // Sync synth emotional target state with active scene
    LaunchedEffect(currentCard.emo) {
        synth.updateScene(currentCard.emo)
    }

    // Sync volume control state
    LaunchedEffect(isMuted) {
        synth.setMuted(isMuted)
    }

    // Cinematic Timer Tick Engine
    LaunchedEffect(currentSceneIndex, isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val tickInterval = 50L
        while (elapsedMs < sceneDurationMs) {
            delay(tickInterval)
            if (isPlaying) {
                elapsedMs += tickInterval
            }
        }
        // Auto-advance to next scene if complete
        if (currentSceneIndex < playlist.size - 1) {
            currentSceneIndex++
            elapsedMs = 0L
        } else {
            // Loop back to start
            currentSceneIndex = 0
            elapsedMs = 0L
        }
    }

    // Split content narrative text into distinct sentences for typographic caption timing
    val subtitles = remember(currentCard.content) {
        val raw = currentCard.content.trim()
        if (raw.isEmpty()) {
            listOf("NOVA SYNAPTIC PROTOCOLS: ACTIVE RESEARCH STATE.")
        } else {
            // Split by sentence punctuation
            val list = raw.split(Regex("(?<=\\.)\\s+")).filter { it.isNotBlank() }
            if (list.isEmpty()) listOf(raw) else list
        }
    }

    // Calculate which subtitle sentence should display based on current elapsed progress
    val activeSubtitleIndex = remember(subtitles, elapsedMs) {
        val index = ((elapsedMs.toDouble() / sceneDurationMs) * subtitles.size).toInt()
        index.coerceIn(0, subtitles.size - 1)
    }
    val activeSubtitleText = subtitles[activeSubtitleIndex]

    // Continuous float tick for Compose Canvas rendering at 60fps
    val transition = rememberInfiniteTransition(label = "cinema_engine")
    val animTicks by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animTicks"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070A))
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            // 1. GENERATIVE CINEMATIC GRAPHICS CANVAS
            GenerativeCinematicCanvas(
                emotion = currentCard.emo,
                syfyText = currentCard.syfy,
                ticks = animTicks,
                elapsedProgress = elapsedMs.toFloat() / sceneDurationMs,
                modifier = Modifier.fillMaxSize()
            )

            // 2. LETTERBOX GRADIENT SHADOWS (Top and Bottom Cinematic Shadows)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            // 3. TOP META PANEL (Scene Tracker & Close Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MICRO_DRAMA RESOLUTION // ACTIVE FEED",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF818CF8),
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EPISODE: ${currentCard.title.uppercase()}",
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF312E81).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SCENE ${currentSceneIndex + 1} OF ${playlist.size}",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC7D2FE)
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Cinema",
                            tint = Color.White
                        )
                    }
                }
            }

            // 4. CENTRAL GLOWING SCENE STATE SUMMARY
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (currentCard.emo.lowercase()) {
                                    "alarm", "panic", "defiance", "anger" -> Color(0xFFEF4444)
                                    "grief", "sorrow" -> Color(0xFFA855F7)
                                    "wonder", "hope", "curiosity" -> Color(0xFF10B981)
                                    else -> Color(0xFF3B82F6)
                                }
                            )
                    )
                    Text(
                        text = "ATMOSPHERE: ${currentCard.syfy.uppercase()} [${currentCard.emo.uppercase()}]",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // 5. BOTTOM CINEMATIC SUBTITLES / CAPTIONS OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 125.dp)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sentences fade smoothly using Compose transitions or AnimatedVisibility
                // To keep it simple and high-performance, we animate visibility based on the elapsed index
                subtitles.forEachIndexed { sIdx, text ->
                    AnimatedVisibility(
                        visible = sIdx == activeSubtitleIndex,
                        enter = fadeIn(animationSpec = tween(400)),
                        exit = fadeOut(animationSpec = tween(400))
                    ) {
                        Text(
                            text = text,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Normal,
                                fontSize = 18.sp,
                                color = Color(0xFFF3F4F6),
                                lineHeight = 26.sp,
                                textAlign = TextAlign.Center,
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 6f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 6. MEDIA PLAYBACK CONTROL DASHBOARD PANEL
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0F16).copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Progress/Seek Track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatTime(elapsedMs),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    // Draw seekbar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                // Simple tap seek approximation (just reset to start of scene)
                                elapsedMs = 0L
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(elapsedMs.toFloat() / sceneDurationMs)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                                    )
                                )
                        )
                    }

                    Text(
                        text = formatTime(sceneDurationMs),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }

                // Play, Skip, Volume Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mute Toggle Button
                    IconButton(
                        onClick = { isMuted = !isMuted }
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Main Cinematic Playback Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip Back Scene
                        IconButton(
                            onClick = {
                                if (currentSceneIndex > 0) {
                                    currentSceneIndex--
                                } else {
                                    currentSceneIndex = playlist.size - 1
                                }
                                elapsedMs = 0L
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Beat",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play/Pause Master Toggle
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Skip Forward Scene
                        IconButton(
                            onClick = {
                                if (currentSceneIndex < playlist.size - 1) {
                                    currentSceneIndex++
                                } else {
                                    currentSceneIndex = 0
                                }
                                elapsedMs = 0L
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Beat",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Quick Scene Selector Button Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "REND_ENGINE // 60FPS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Procedural Graphics Engine: Renders stylized cyber visuals based on scene emotions
 */
@Composable
fun GenerativeCinematicCanvas(
    emotion: String,
    syfyText: String,
    ticks: Float,
    elapsedProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f

        // Draw deep space background stars/dust
        drawStarsAndCosmosDust(ticks, width, height)

        when (emotion.lowercase().trim()) {
            "alarm", "panic", "defiance", "anger", "conflict" -> {
                // High alert matrix (Neon Red alert grids, warning vectors)
                val primaryAlertColor = Color(0xFFEF4444)
                val secondaryColor = Color(0xFFF97316)

                // Draw rotating cyber mesh grid
                drawPerspectiveGrid(ticks, primaryAlertColor.copy(alpha = 0.12f))

                // Draw expanding alarm rings
                val scale = (ticks % 60f) / 60f
                drawCircle(
                    color = primaryAlertColor,
                    radius = (120.dp.toPx() + scale * 180.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)),
                    alpha = (1.0f - scale) * 0.4f
                )

                // Synthesize oscillating laser beams
                val beamCount = 4
                for (j in 0 until beamCount) {
                    val angle = (ticks * (if (j % 2 == 0) 1 else -1) + j * (360f / beamCount)) * (Math.PI / 180.0)
                    val endX = cx + cos(angle).toFloat() * 400.dp.toPx()
                    val endY = cy + sin(angle).toFloat() * 400.dp.toPx()
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.18f),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), ticks)
                    )
                }

                // Draw vibrating digital core
                val vibrateRadius = 50.dp.toPx() + sin(ticks * 0.5f) * 8.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryAlertColor, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = vibrateRadius * 1.5f
                    ),
                    radius = vibrateRadius * 1.5f,
                    center = Offset(cx, cy)
                )
            }

            "wonder", "creativity", "curious", "curiosity", "hope", "analytical" -> {
                // Etheric Tech Constellations (Cyan grids, connecting nodes)
                val techColor = Color(0xFF06B6D4)
                val accentColor = Color(0xFF8B5CF6)

                drawPerspectiveGrid(ticks * 0.5f, techColor.copy(alpha = 0.08f))

                // Animated cyber-grid constellation network nodes
                val nodesCount = 12
                val nodes = ArrayList<Offset>()
                for (j in 0 until nodesCount) {
                    val angle = (ticks * 0.2f + j * (360f / nodesCount)) * (Math.PI / 180.0)
                    val dist = (100.dp.toPx() + sin(ticks * 0.05f + j) * 40.dp.toPx())
                    val nx = cx + cos(angle).toFloat() * dist
                    val ny = cy + sin(angle).toFloat() * dist
                    nodes.add(Offset(nx, ny))

                    // Draw glowing node
                    drawCircle(
                        color = techColor,
                        radius = 4.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                    drawCircle(
                        color = techColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }

                // Interconnect network constellations
                for (j in 0 until nodesCount) {
                    val n1 = nodes[j]
                    val n2 = nodes[(j + 1) % nodesCount]
                    val n3 = nodes[(j + 4) % nodesCount]
                    drawLine(
                        color = techColor.copy(alpha = 0.15f),
                        start = n1,
                        end = n2,
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = accentColor.copy(alpha = 0.10f),
                        start = n1,
                        end = n3,
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Ethereal core waveform
                val coreRadius = 70.dp.toPx() + cos(ticks * 0.1f) * 5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(techColor.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = Offset(cx, cy)
                )
            }

            "grief", "sorrow", "confusion", "gloom", "caution", "dread", "tension" -> {
                // Melancholy shadows (Dark violet drifting nodes, collapsing geometry)
                val voidColor = Color(0xFF8B5CF6)
                val shadowColor = Color(0xFF4F46E5)

                drawPerspectiveGrid(ticks * 0.2f, voidColor.copy(alpha = 0.05f))

                // Drifting slow-moving dark bubbles
                val bubbleCount = 6
                for (j in 0 until bubbleCount) {
                    val verticalProgress = ((ticks * 0.3f + j * 60f) % 360f) / 360f
                    val bx = cx + sin(ticks * 0.02f + j) * 120.dp.toPx()
                    val by = height - (verticalProgress * height)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(voidColor.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(bx, by),
                            radius = 60.dp.toPx()
                        ),
                        radius = 60.dp.toPx(),
                        center = Offset(bx, by)
                    )
                    drawCircle(
                        color = shadowColor.copy(alpha = 0.25f),
                        radius = 20.dp.toPx(),
                        center = Offset(bx, by),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Fading orbit vectors
                val orbitRadius = 150.dp.toPx()
                drawCircle(
                    color = voidColor.copy(alpha = 0.15f),
                    radius = orbitRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 40f), ticks))
                )
            }

            else -> {
                // Serene / Peaceful (Golden-green quantum matrix waves)
                val secureColor = Color(0xFF10B981)
                val calmColor = Color(0xFF14B8A6)

                // Perspective cyber grid
                drawPerspectiveGrid(ticks * 0.3f, secureColor.copy(alpha = 0.08f))

                // Slowly expanding ripples
                val waveCount = 3
                for (j in 0 until waveCount) {
                    val rippleProgress = ((ticks * 0.2f + j * 120f) % 360f) / 360f
                    drawCircle(
                        color = secureColor.copy(alpha = (1.0f - rippleProgress) * 0.25f),
                        radius = rippleProgress * 250.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Smooth floating cyber dust particles
                val particles = 15
                for (j in 0 until particles) {
                    val pAngle = j * (360f / particles) * (Math.PI / 180.0)
                    val pRadius = 80.dp.toPx() + sin(ticks * 0.02f + j) * 40.dp.toPx()
                    val px = cx + cos(pAngle).toFloat() * pRadius
                    val py = cy + sin(pAngle).toFloat() * pRadius

                    drawCircle(
                        color = calmColor.copy(alpha = 0.35f),
                        radius = 3.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

/**
 * Draws floating visual stars and galactic dust
 */
fun DrawScope.drawStarsAndCosmosDust(ticks: Float, width: Float, height: Float) {
    // Semi-random star placements based on systematic offsets
    val starCount = 35
    for (i in 0 until starCount) {
        val xSeed = (i * 12345.67) % width
        val ySeed = (i * 98765.43) % height
        // Subtle drift movement
        val driftX = sin(ticks * 0.01f + i) * 3f
        val driftY = cos(ticks * 0.008f + i) * 3f
        val brightness = 0.15f + (sin(ticks * 0.05f + i) * 0.1f)

        drawCircle(
            color = Color.White.copy(alpha = brightness),
            radius = (1.dp.toPx() + (i % 2) * 1.dp.toPx()),
            center = Offset(xSeed.toFloat() + driftX, ySeed.toFloat() + driftY)
        )
    }
}

/**
 * Procedural cybergrid lines drawn in perspective to simulate 3D space movement
 */
fun DrawScope.drawPerspectiveGrid(ticks: Float, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val width = size.width
    val height = size.height

    // Vanishing point perspective lines
    val linesCount = 18
    for (i in 0 until linesCount) {
        val angle = i * (360f / linesCount) * (Math.PI / 180.0)
        val endX = cx + cos(angle).toFloat() * width
        val endY = cy + sin(angle).toFloat() * height
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(endX, endY),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Concentric perspective rings expanding outward
    val ringCount = 5
    for (i in 0 until ringCount) {
        val progress = ((ticks * 0.4f + i * (360f / ringCount)) % 360f) / 360f
        val ringRadius = progress * (width * 0.8f)
        drawCircle(
            color = color,
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}
