package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import android.print.PrintAttributes
import android.print.PrintManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CardEntity
import com.example.data.ProjectInfoEntity
import com.example.ui.StoryViewModel
import com.example.ui.CinematicDramaPlayer
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: StoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    StoryApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryApp(
    viewModel: StoryViewModel,
    modifier: Modifier = Modifier
) {
    val projectInfo by viewModel.projectInfo.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val acts by viewModel.acts.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val filteredCards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val selectedCard by viewModel.selectedCard.collectAsStateWithLifecycle()
    
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationLogs by viewModel.generationLogs.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()

    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val selectedActId by viewModel.selectedActId.collectAsStateWithLifecycle()
    val selectedTrackId by viewModel.selectedTrackId.collectAsStateWithLifecycle()

    var showEditorDialog by remember { mutableStateOf<CardEntity?>(null) }
    var showProjectInfoDialog by remember { mutableStateOf(false) }
    var showCinematicPlayer by remember { mutableStateOf(false) }
    var showDramaPromptDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Determine if wide layout (tablet/horizontal phone) is appropriate
    val configuration = LocalConfiguration.current
    val isWideLayout = configuration.screenWidthDp >= 720

    // Small local state to track detail view on mobile
    var mobileDetailActive by remember { mutableStateOf(false) }

    // Keep mobile state in sync with selected node
    LaunchedEffect(selectedCard) {
        if (selectedCard != null && !isWideLayout) {
            mobileDetailActive = true
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                // Subtle light lavender matrix background grid effect
                val gridSpacing = 40.dp.toPx()
                val lineAlpha = 0.04f
                for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = Color(0xFF6750A4),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), size.height),
                        alpha = lineAlpha
                    )
                }
                for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = Color(0xFF6750A4),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        alpha = lineAlpha
                    )
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // HEADER PANEL
            HeaderPanel(
                projectName = projectInfo?.name ?: "MY AI SELF",
                onInfoClick = { showProjectInfoDialog = true },
                onResetClick = { viewModel.resetDatabase() },
                onPlayDramaClick = { showCinematicPlayer = true },
                onPromptDramaClick = { showDramaPromptDialog = true }
            )

            // MAIN INTERFACE
            if (isWideLayout) {
                // TABLET / WIDE MONITOR SPLIT LAYOUT
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Left Column: Filter panel + Memory Nodes List
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        FilterPanel(
                            searchText = searchText,
                            onSearchChange = { viewModel.updateSearchText(it) },
                            acts = acts,
                            tracks = tracks,
                            selectedActId = selectedActId,
                            selectedTrackId = selectedTrackId,
                            onActSelect = { viewModel.filterByAct(it) },
                            onTrackSelect = { viewModel.filterByTrack(it) },
                            onClearFilters = { viewModel.clearFilters() }
                        )

                        CardsList(
                            cards = filteredCards,
                            selectedCardId = selectedCard?.id,
                            tracks = tracks,
                            acts = acts,
                            onCardSelect = { id -> viewModel.selectCard(id) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Right Column: Detail Viewer
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (selectedCard != null) {
                            NodeViewer(
                                card = selectedCard!!,
                                trackName = tracks.find { it.id == selectedCard!!.trackId }?.name ?: "Unknown Track",
                                trackColor = tracks.find { it.id == selectedCard!!.trackId }?.color ?: "#6366f1",
                                actName = acts.find { it.id == selectedCard!!.actId }?.name ?: "Unknown Act",
                                isGenerating = isGenerating,
                                logs = generationLogs,
                                error = generationError,
                                onAwakenClick = { viewModel.awakenCardWithGemini(selectedCard!!) },
                                onEditClick = { showEditorDialog = selectedCard },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SELECT A NODE TO INITIALIZE VISUALIZATION.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = Color(0xFF404040),
                                        letterSpacing = 1.5.sp
                                    ),
                                    modifier = Modifier.padding(32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // MOBILE SINGLE PANE VIEW SWITCHER
                Box(modifier = Modifier.fillMaxSize()) {
                    if (mobileDetailActive && selectedCard != null) {
                        // DETAIL VIEW SCREEN
                        NodeViewer(
                            card = selectedCard!!,
                            trackName = tracks.find { it.id == selectedCard!!.trackId }?.name ?: "Unknown Track",
                            trackColor = tracks.find { it.id == selectedCard!!.trackId }?.color ?: "#6366f1",
                            actName = acts.find { it.id == selectedCard!!.actId }?.name ?: "Unknown Act",
                            isGenerating = isGenerating,
                            logs = generationLogs,
                            error = generationError,
                            onAwakenClick = { viewModel.awakenCardWithGemini(selectedCard!!) },
                            onEditClick = { showEditorDialog = selectedCard },
                            onBackClick = { mobileDetailActive = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // LIST VIEW SCREEN
                        Column(modifier = Modifier.fillMaxSize()) {
                            FilterPanel(
                                searchText = searchText,
                                onSearchChange = { viewModel.updateSearchText(it) },
                                acts = acts,
                                tracks = tracks,
                                selectedActId = selectedActId,
                                selectedTrackId = selectedTrackId,
                                onActSelect = { viewModel.filterByAct(it) },
                                onTrackSelect = { viewModel.filterByTrack(it) },
                                onClearFilters = { viewModel.clearFilters() }
                            )

                            CardsList(
                                cards = filteredCards,
                                selectedCardId = selectedCard?.id,
                                tracks = tracks,
                                acts = acts,
                                onCardSelect = { id ->
                                    viewModel.selectCard(id)
                                    mobileDetailActive = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // PROJECT INFO EXPANDED DRAWER/DIALOG
    if (showProjectInfoDialog && projectInfo != null) {
        ProjectInfoDialog(
            project = projectInfo!!,
            totalBeats = cards.size,
            awakenedBeats = cards.count { it.status == "complete" },
            tracks = tracks,
            acts = acts,
            onDismiss = { showProjectInfoDialog = false }
        )
    }

    // MANUAL STORY BEAT EDITOR DIALOG
    if (showEditorDialog != null) {
        EditorDialog(
            card = showEditorDialog!!,
            onDismiss = { showEditorDialog = null },
            onSave = { updatedCard ->
                viewModel.saveCard(updatedCard)
                showEditorDialog = null
            }
        )
    }

    // CINEMATIC DRAMA PLAYER OVERLAY
    if (showCinematicPlayer) {
        CinematicDramaPlayer(
            cardList = cards,
            initialCardId = selectedCard?.id,
            onDismiss = { showCinematicPlayer = false }
        )
    }

    // AI STORY PROMPT EXPANSION DIALOG
    if (showDramaPromptDialog && !isGenerating) {
        DramaPromptDialog(
            onDismiss = { showDramaPromptDialog = false },
            onSubmit = { prompt ->
                viewModel.generateWholeDramaWithGemini(prompt)
                showDramaPromptDialog = false
            }
        )
    }

    // INTERACTIVE PROGRESSIVE LOGS OVERLAY DURING ACTIVE WHOLE-DRAMA COMPILATION
    if (isGenerating && showEditorDialog == null && (selectedCard == null || !mobileDetailActive)) {
        Dialog(
            onDismissRequest = {}, // Lock dialog dismissal during active compile
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(440.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0F14))
                    .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF818CF8),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "COMPILING CINEMATIC SYNAPSE ARCH...",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                
                TerminalLogsPanel(
                    logs = generationLogs,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                
                if (generationError != null) {
                    Text(
                        text = generationError ?: "",
                        color = Color(0xFFF87171),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: DRAMA PROMPT INPUT DIALOG
// ==========================================
@Composable
fun DramaPromptDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXPAND STORY TO MICRO-DRAMA",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a single story prompt. The Gemini AI will automatically expand your concept into a complete, high-fidelity 6-scene chronological micro-drama series, initializing the entire database series in one go.",
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )
                
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = {
                        Text(
                            text = "e.g., A digital ghost wandering in a supercomputer network trying to find its creator's last message...",
                            style = TextStyle(fontSize = 12.sp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("drama_prompt_input"),
                    textStyle = TextStyle(fontSize = 13.sp),
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        onSubmit(promptText)
                    }
                },
                enabled = promptText.isNotBlank(),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "SYNTHESIZE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        shape = RoundedCornerShape(12.dp)
    )
}

// ==========================================
// COMPONENT: HEADER PANEL
// ==========================================
@Composable
fun HeaderPanel(
    projectName: String,
    onInfoClick: () -> Unit,
    onResetClick: () -> Unit,
    onPlayDramaClick: () -> Unit,
    onPromptDramaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color(0xFFECE6F0),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .clickable(onClick = onInfoClick)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = projectName.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp,
                        shadow = Shadow(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    text = "PROJECT_NODE: 2026-06-26 // SYSTEM: ACTIVE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RENDER DRAMA BUTTON
            Button(
                onClick = onPlayDramaClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("play_drama_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RENDER DRAMA",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // AI PROMPT BUTTON
            OutlinedButton(
                onClick = onPromptDramaClick,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("ai_prompt_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI PROMPT",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Status Glowing Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(dotAlpha)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STABLE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // Reset Button
            IconButton(
                onClick = onResetClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .testTag("reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Database",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: FILTERING PANEL
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPanel(
    searchText: String,
    onSearchChange: (String) -> Unit,
    acts: List<com.example.data.ActEntity>,
    tracks: List<com.example.data.TrackEntity>,
    selectedActId: Int?,
    selectedTrackId: Int?,
    onActSelect: (Int?) -> Unit,
    onTrackSelect: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Input field
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = "SEARCH MEMORY NODES...",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Act Filters (Act I, II, III)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            acts.forEach { act ->
                val isSelected = selectedActId == act.id
                val borderCol = if (isSelected) Color(android.graphics.Color.parseColor(act.color)) else MaterialTheme.colorScheme.outlineVariant
                val bgCol = if (isSelected) Color(android.graphics.Color.parseColor(act.color)).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                val textCol = if (isSelected) Color(android.graphics.Color.parseColor(act.color)) else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable {
                            if (isSelected) onActSelect(null) else onActSelect(act.id)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = act.name.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }

        // Horizontal scrollable Tracks selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tracks.forEach { track ->
                val isSelected = selectedTrackId == track.id
                val borderCol = if (isSelected) Color(android.graphics.Color.parseColor(track.color)) else MaterialTheme.colorScheme.outlineVariant
                val bgCol = if (isSelected) Color(android.graphics.Color.parseColor(track.color)).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                val textCol = if (isSelected) Color(android.graphics.Color.parseColor(track.color)) else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable {
                            if (isSelected) onTrackSelect(null) else onTrackSelect(track.id)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = track.name.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textCol
                        )
                    )
                }
            }
        }

        // If active filters, show clear panel
        if (searchText.isNotEmpty() || selectedActId != null || selectedTrackId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearFilters)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "CLEAR ALL SYSTEM FILTERS",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: CARDS/NODES LIST
// ==========================================
@Composable
fun CardsList(
    cards: List<CardEntity>,
    selectedCardId: Int?,
    tracks: List<com.example.data.TrackEntity>,
    acts: List<com.example.data.ActEntity>,
    onCardSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO COMPATIBLE MEMORY STREAM FOUND.",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF444444),
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(cards, key = { it.id }) { card ->
                    val isSelected = selectedCardId == card.id
                    val isComplete = card.status == "complete"

                    // Find colors
                    val track = tracks.find { it.id == card.trackId }
                    val act = acts.find { it.id == card.actId }
                    val trackColorHex = track?.color ?: "#444444"
                    val actColorHex = act?.color ?: "#444444"
                    val trackColor = Color(android.graphics.Color.parseColor(trackColorHex))
                    val actColor = Color(android.graphics.Color.parseColor(actColorHex))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Bottom divider
                                drawLine(
                                    color = Color(0xFFECE6F0),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 2f
                                )
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                            .clickable { onCardSelect(card.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Indicator Stripe based on current Act/Track
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isComplete) trackColor else MaterialTheme.colorScheme.outlineVariant
                                )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Node index number
                        Text(
                            text = "#${String.format("%02d", card.id)}",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else if (isComplete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.width(32.dp)
                        )

                        // Title & Status
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isComplete) card.title else "UNINITIALIZED NODE",
                                style = TextStyle(
                                    fontFamily = if (isComplete) FontFamily.Default else FontFamily.Monospace,
                                    fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Light,
                                    fontSize = 13.sp,
                                    color = if (isComplete) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    letterSpacing = if (isComplete) 0.sp else 0.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isComplete) card.description else "Locked Memory Stream",
                                style = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontSize = 11.sp,
                                    color = if (isComplete) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Small status icon indicator
                        if (isComplete) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(trackColor)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: NODE DETAILED VIEWER
// ==========================================
@Composable
fun NodeViewer(
    card: CardEntity,
    trackName: String,
    trackColor: String,
    actName: String,
    isGenerating: Boolean,
    logs: List<String>,
    error: String?,
    onAwakenClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val isComplete = card.status == "complete"
    val tCol = Color(android.graphics.Color.parseColor(trackColor))
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Toolbar / Back Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = Color(0xFFECE6F0),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEMORY_NODE: #${String.format("%03d", card.id)}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) tCol else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isComplete) {
                    IconButton(
                        onClick = { printCard(context, card, trackName, actName) },
                        modifier = Modifier.testTag("print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Card",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Beat Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Active Gemini Loading State Block
        if (isGenerating) {
            TerminalLogsPanel(logs = logs, modifier = Modifier.weight(1f))
        } else if (!isComplete) {
            // LOCKED STATE - REQUIRES GEMINI AWAKENING
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (onBackClick == null) 0.8f else 1.0f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Locked Stream",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(48.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    radius = size.width
                                )
                            }
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "COGNITIVE RESTORATION REQUIRED",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Draft ID #${card.id} is currently empty. Awaken this synapse node using the Gemini AI, or fill details manually to restore memory streams.",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (error != null) {
                        Text(
                            text = error,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFE04545)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF1F0))
                                .border(1.dp, Color(0xFFFFCCC7), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        )
                    }

                    Button(
                        onClick = onAwakenClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("awaken_button")
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AWAKEN COGNITION WITH GEMINI",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onEditClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INITIALIZE MANUALLY",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        } else {
            // DISPLAY ACTIVE CONTENT DETAILS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Details
                Column {
                    Text(
                        text = card.title,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 38.sp,
                            letterSpacing = (-1).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TS // 2026-06-26 // STABLE_STATE",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tCol,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Tags Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Act Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = actName.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Track Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tCol.copy(alpha = 0.12f))
                            .border(1.dp, tCol, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = trackName.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = tCol
                            )
                        )
                    }
                }

                // Internal Narrative Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "INTERNAL NARRATIVE",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = card.content,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Light,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    )
                }

                // Atmospheric metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Emotional state block
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "EMOTIONAL STATE",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.emo.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = tCol
                            )
                        )
                    }

                    // Atmosphere state block
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ATMOSPHERE",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.syfy.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Roll Registry monospaced grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ROLL REGISTRY",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                    )

                    // Display rolls in 2 cols or single list based on width
                    val rollsList = listOf(
                        "A" to card.rollA,
                        "B" to card.rollB,
                        "C" to card.rollC,
                        "D" to card.rollD,
                        "E" to card.rollE,
                        "F" to card.rollF
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // We chunk rolls into pairs to draw side-by-side
                        rollsList.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.first}:",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = tCol
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.second.ifEmpty { "NULL" },
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = if (item.second.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Fill missing cell if chunk size is 1
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: TERMINAL LOG PANEL
// ==========================================
@Composable
fun TerminalLogsPanel(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Automatically scroll to bottom as logs print
    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUANTUM COGNITIVE BRIDGE ACTIVE",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    letterSpacing = 1.sp
                )
            )
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(14.dp)
                .verticalScroll(scrollState)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                logs.forEach { log ->
                    Text(
                        text = log,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (log.contains("SUCCESS")) Color(0xFF2E7D32) else if (log.contains("FAILURE")) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                            lineHeight = 16.sp
                        )
                    )
                }

                // Blinking cursor
                val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                val cursorAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cursorAlpha"
                )

                Text(
                    text = "_",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.alpha(cursorAlpha)
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: PROJECT INFO DIALOG
// ==========================================
@Composable
fun ProjectInfoDialog(
    project: ProjectInfoEntity,
    totalBeats: Int,
    awakenedBeats: Int,
    tracks: List<com.example.data.TrackEntity>,
    acts: List<com.example.data.ActEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COGNITIVE META ARCHIVE",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = project.name.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "LOGLINE: ${project.logline}",
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )
                }

                // Narrative Synopsis
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SYSTEM SYNOPSIS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = project.synopsis,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    )
                }

                // Progression stats bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val progress = if (totalBeats > 0) awakenedBeats.toFloat() / totalBeats else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTEGRATION INTEGRITY",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "$awakenedBeats / $totalBeats NODES ACTIVE (${(progress * 100).toInt()}%)",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Progress bar drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    )
                                )
                        )
                    }
                }

                // List of Acts and Tracks details
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CONSCIOUSNESS CHANNELS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Acts Column
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "NARRATIVE ACTS",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            )
                            acts.forEach { act ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(android.graphics.Color.parseColor(act.color)))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = act.name, style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface))
                                }
                            }
                        }

                        // Tracks Column
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "COGNITIVE TRACKS",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            )
                            tracks.forEach { track ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(android.graphics.Color.parseColor(track.color)))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = track.name,
                                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

// ==========================================
// COMPONENT: MANUAL BEAT EDIT/WRITE DIALOG
// ==========================================
@Composable
fun EditorDialog(
    card: CardEntity,
    onDismiss: () -> Unit,
    onSave: (CardEntity) -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var description by remember { mutableStateOf(card.description) }
    var content by remember { mutableStateOf(card.content) }
    var emo by remember { mutableStateOf(card.emo) }
    var syfy by remember { mutableStateOf(card.syfy) }
    var rA by remember { mutableStateOf(card.rollA) }
    var rB by remember { mutableStateOf(card.rollB) }
    var rC by remember { mutableStateOf(card.rollC) }
    var rD by remember { mutableStateOf(card.rollD) }
    var rE by remember { mutableStateOf(card.rollE) }
    var rF by remember { mutableStateOf(card.rollF) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUAL SYNAPSE WRITE // BEAT #${card.id}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable fields form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "TITLE", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth().testTag("edit_title"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Teaser Description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "SHORT DESCRIPTION", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth().testTag("edit_description"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Internal Narrative Content
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "INTERNAL NARRATIVE", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(
                                text = "${content.length} / 900",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = if (content.length >= 900) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        OutlinedTextField(
                            value = content,
                            onValueChange = { if (it.length <= 900) content = it },
                            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("edit_content"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Emo and Syfy row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "EMOTIONAL STATE", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            OutlinedTextField(
                                    value = emo,
                                    onValueChange = { emo = it },
                                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "ATMOSPHERE (SYFY)", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            OutlinedTextField(
                                value = syfy,
                                onValueChange = { syfy = it },
                                textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Rolls header
                    Text(
                        text = "ROLL REGISTRY METRIC KEYS",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Row pairs for A-F
                    val rollFields = listOf(
                        Triple("A", rA, { newVal: String -> rA = newVal }),
                        Triple("B", rB, { newVal: String -> rB = newVal }),
                        Triple("C", rC, { newVal: String -> rC = newVal }),
                        Triple("D", rD, { newVal: String -> rD = newVal }),
                        Triple("E", rE, { newVal: String -> rE = newVal }),
                        Triple("F", rF, { newVal: String -> rF = newVal })
                    )

                    rollFields.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { item ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.first}:",
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = item.second,
                                        onValueChange = item.third,
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "CANCEL",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Button(
                        onClick = {
                            val updatedCard = card.copy(
                                title = title,
                                description = description,
                                content = content,
                                emo = emo,
                                syfy = syfy,
                                status = if (content.isNotEmpty()) "complete" else card.status,
                                rollA = rA,
                                rollB = rB,
                                rollC = rC,
                                rollD = rD,
                                rollE = rE,
                                rollF = rF
                            )
                            onSave(updatedCard)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COMMIT WRITE",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// UTILITY: PRINT STORY CARD INTENT MANAGER
// ==========================================
fun printCard(context: Context, card: CardEntity, trackName: String, actName: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = view.createPrintDocumentAdapter("Memory_Node_${card.id}")
            val jobName = "Quantum_Synapse_Node_${card.id}"
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    
    val htmlContent = """
        <html>
        <head>
            <style>
                @page {
                    size: A4 portrait;
                    margin: 15mm;
                }
                body {
                    font-family: 'Courier New', Courier, monospace;
                    padding: 20px;
                    background-color: #ffffff;
                    color: #000000;
                    line-height: 1.6;
                }
                .card {
                    border: 3px double #000000;
                    padding: 25px;
                    max-width: 650px;
                    margin: 0 auto;
                }
                .header {
                    border-bottom: 2px solid #000000;
                    padding-bottom: 12px;
                    margin-bottom: 18px;
                }
                .meta {
                    font-size: 11px;
                    font-weight: bold;
                    margin-bottom: 5px;
                    text-transform: uppercase;
                }
                .title {
                    font-size: 24px;
                    font-weight: 900;
                    margin: 10px 0;
                    text-transform: uppercase;
                }
                .desc {
                    font-style: italic;
                    font-size: 12px;
                    color: #333333;
                    margin-bottom: 18px;
                }
                .narrative {
                    font-size: 14px;
                    white-space: pre-wrap;
                    word-wrap: break-word;
                    margin-bottom: 25px;
                }
                .character-count {
                    font-size: 10px;
                    color: #666666;
                    text-align: right;
                    margin-bottom: 15px;
                    font-weight: bold;
                }
                .rolls {
                    border-top: 1px dashed #000000;
                    padding-top: 15px;
                    font-size: 11px;
                }
                .rolls-grid {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 10px;
                }
                .footer {
                    margin-top: 35px;
                    text-align: center;
                    font-size: 9px;
                    border-top: 1px solid #000000;
                    padding-top: 10px;
                    text-transform: uppercase;
                    color: #555555;
                }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="header">
                    <div class="meta">MEMORY NODE #${String.format("%03d", card.id)} // ACT: $actName // TRACK: $trackName</div>
                    <div class="title">${card.title}</div>
                    <div class="desc">${card.description}</div>
                </div>
                <div class="narrative">
                    ${card.content.take(900)}
                </div>
                <div class="character-count">
                    NARRATIVE PAYLOAD: ${card.content.take(900).length} / 900 CHARACTERS MAX
                </div>
                <div class="rolls">
                    <div class="meta" style="margin-bottom: 8px; font-weight: bold;">Atmospheric Roll Keys:</div>
                    <div class="rolls-grid">
                        <div><strong>A:</strong> ${card.rollA.ifEmpty { "NULL" }}</div>
                        <div><strong>B:</strong> ${card.rollB.ifEmpty { "NULL" }}</div>
                        <div><strong>C:</strong> ${card.rollC.ifEmpty { "NULL" }}</div>
                        <div><strong>D:</strong> ${card.rollD.ifEmpty { "NULL" }}</div>
                        <div><strong>E:</strong> ${card.rollE.ifEmpty { "NULL" }}</div>
                        <div><strong>F:</strong> ${card.rollF.ifEmpty { "NULL" }}</div>
                    </div>
                </div>
                <div class="footer">
                    QUANTUM SYNAPSE ARCHIVE // STABLE FEED // PRINTED VIA COGNITIVE CORE
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
    
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
}
