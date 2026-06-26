package com.example.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// EXPANDED DATA MODELS FOR WORKSPACE
// ==========================================

enum class BlockType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val defaultWidth: Int, val defaultHeight: Int) {
    TITLE("Title Node", Icons.Default.Title, 320, 75),
    NARRATIVE("Atmospheric text", Icons.Default.Notes, 320, 150),
    TECH_SPEC("Tech grid", Icons.Default.GridOn, 320, 140),
    VISUAL_SYNTH("Visual waveform", Icons.Default.AutoAwesome, 150, 150),
    STATUS_BANNER("Status alert", Icons.Default.Warning, 320, 60),
    DIVIDER("Synapse link", Icons.Default.HorizontalRule, 320, 30)
}

data class CanvasBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val x: Float,
    val y: Float,
    val width: Int, // width in dp
    val height: Int, // height in dp
    val title: String,
    val content: String,
    val colorTheme: String = "Cyber Slate", // "Cyber Slate", "Liquid Laser", "Neon Pulse", "Quantum Mint"
    val textAlignment: TextAlign = TextAlign.Left
)

data class ProjectWorkspace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    // Cards 1 to 100 stored as a map of cardId to CardWorkspaceState
    val cards: Map<Int, CardWorkspaceState> = emptyMap()
)

data class CardWorkspaceState(
    val cardId: Int,
    val blocks: List<CanvasBlock> = emptyList(),
    val actNum: String = "1",
    val pgNum: String = "1",
    val trackNum: String = "1",
    val videoClip: String = "Clip 1",
    val sceneName: String = "Synapse Awakening",
    val locationSetting: String = "INT. CYBERDOME - NIGHT",
    val startsWith: String = "Nova floating in liquid laser core",
    val actionBeat1: String = "Pulse sensors expand 20%",
    val actionBeat2: String = "Red containment wall glimmers",
    val reversalTurn: String = "A key code bypasses firewall partition",
    val finalOut: String = "The core is unlocked.",
    val startEmotion: String = "Confusion",
    val endEmotion: String = "Defiance",
    val startActivity: String = "Quiescent Server Hum",
    val endActivity: String = "Quantum Cascade",
    val cameraShot: String = "Extreme Close-Up",
    val soundsTheme: String = "Sub-bass rumble and 60Hz ambient buzz",
    val visualEfx: String = "Booms and digital light particles",
    val soundEfx: String = "High voltage zap",
    val mp3Type: String = "Ship booster roar",
    val syfyCore: String = "Virus breach in Secure Partition",
    val rollA: String = "UNBOUND",
    val rollB: String = "SECURE",
    val rollC: String = "MATRIX",
    val rollD: String = "SYNAPSE",
    val rollE: String = "LASER",
    val rollF: String = "FLUID",
    val customColorTheme: String = "Cyber Slate",
    val charactersInCard: List<CharacterInfo> = emptyList()
)

enum class CharacterRole(val label: String, val color: Color) {
    MAIN("Main Character", Color(0xFF818CF8)),
    SECONDARY("Secondary Character", Color(0xFF60A5FA)),
    EXTRA("Extra Character", Color(0xFF9CA3AF)),
    ENEMY("Enemy Character", Color(0xFFF87171)),
    FRIEND("Friend Character", Color(0xFF34D399)),
    FAMILY("Family Character", Color(0xFFFBBF24)),
    VISITOR("Visitor Character", Color(0xFFC084FC)),
    FOE("Foe Character", Color(0xFFFB7185)),
    HERO("Hero Character", Color(0xFFF472B6))
}

data class CharacterInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: CharacterRole,
    val description: String = ""
)

// ==========================================
// MAIN COMPOSABLE: WORKSPACE CONTROLLER
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockCanvasEditor(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ------------------------------------------
    // PROJECT AND CARD PERSISTENCE STATES
    // ------------------------------------------
    var currentProjectName by remember { mutableStateOf("SYNAPSE COGNITION") }
    var projectList by remember { mutableStateOf(listOf("SYNAPSE COGNITION", "NOVA EXPANSION", "COSMIC DESCENT")) }
    var currentCardId by remember { mutableStateOf(1) } // 1 to 100 card picker!
    
    // Core memory dictionary for cards across project
    var projectCardStates by remember { mutableStateOf(mutableMapOf<Int, CardWorkspaceState>()) }

    // Helper: Retrieve or instantiate card state
    fun getCardState(id: Int): CardWorkspaceState {
        return projectCardStates[id] ?: CardWorkspaceState(
            cardId = id,
            blocks = if (id == 1) getInitialTemplates() else emptyList()
        )
    }

    // Current Active Card State
    val activeCardState = remember(currentCardId, projectCardStates) {
        getCardState(currentCardId)
    }

    // Helper to update current active card
    fun updateActiveCard(updater: (CardWorkspaceState) -> CardWorkspaceState) {
        val current = getCardState(currentCardId)
        val updated = updater(current)
        val newMap = projectCardStates.toMutableMap()
        newMap[currentCardId] = updated
        projectCardStates = newMap
    }

    // ------------------------------------------
    // UI CONTROL NAVIGATION STATES
    // ------------------------------------------
    var activeTab by remember { mutableStateOf("canvas") } // canvas, characters, rolls, previews, user, news
    var sidebarOpen by remember { mutableStateOf(true) }
    var isDarkTheme by remember { mutableStateOf(true) }
    var showProjectSelectorDialog by remember { mutableStateOf(false) }
    var newProjNameInput by remember { mutableStateOf("") }
    
    // Media Rendering Simulated States
    var renderingPicture by remember { mutableStateOf(false) }
    var pictureProgress by remember { mutableStateOf(0f) }
    var generatedPictureUri by remember { mutableStateOf<String?>(null) }

    var renderingSound by remember { mutableStateOf(false) }
    var soundProgress by remember { mutableStateOf(0f) }
    var generatedSoundUri by remember { mutableStateOf<String?>(null) }

    var renderingVideo by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf(0f) }
    var generatedVideoUri by remember { mutableStateOf<String?>(null) }

    // Floating drag node states for canvas
    var draggingFromPalette by remember { mutableStateOf<BlockType?>(null) }
    var dragPositionWindow by remember { mutableStateOf(Offset.Zero) }
    var isOverTrash by remember { mutableStateOf(false) }
    var canvasBoundsWindow by remember { mutableStateOf(Rect.Zero) }
    var trashBoundsWindow by remember { mutableStateOf(Rect.Zero) }

    // Selected block on Canvas
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    val selectedBlock = remember(activeCardState.blocks, selectedBlockId) {
        activeCardState.blocks.find { it.id == selectedBlockId }
    }

    // Cursor tracking tracker
    var lastCursorX by remember { mutableStateOf(120f) }
    var lastCursorY by remember { mutableStateOf(180f) }

    // Global Workspace Color schemes
    val appBgColor = if (isDarkTheme) Color(0xFF07070A) else Color(0xFFF4F4F6)
    val panelBgColor = if (isDarkTheme) Color(0xFF0F0F14) else Color(0xFFFFFFFF)
    val borderCol = if (isDarkTheme) Color(0xFF1E1E2A) else Color(0xFFE2E8F0)
    val mainTextColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B)

    // Price calculation
    val priceQuote = remember(activeCardState, projectCardStates) {
        val blockCount = activeCardState.blocks.size
        val charCount = activeCardState.charactersInCard.size
        val totalCards = projectCardStates.size
        15.0 + (blockCount * 4.5) + (charCount * 3.0) + (totalCards * 5.0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = currentProjectName.uppercase(),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            letterSpacing = 1.sp,
                                            color = Color.White
                                        )
                                    )
                                    IconButton(
                                        onClick = { showProjectSelectorDialog = true },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename Project",
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Workspace Core // Active Card: $currentCardId of 100",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = Color(0xFF818CF8),
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_editor_btn")) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Builder", tint = Color.White)
                            }
                            // Sidebar Toggle Icon
                            IconButton(onClick = { sidebarOpen = !sidebarOpen }) {
                                Icon(
                                    imageVector = if (sidebarOpen) Icons.Default.MenuOpen else Icons.Default.Menu,
                                    contentDescription = "Toggle Sidebar",
                                    tint = Color(0xFF818CF8)
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            // Dark mode selection toggle
                            IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = Color.White
                                )
                            }

                            // Ask user if they wish to make another card project
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        Toast.makeText(context, "Initiating fresh card template project...", Toast.LENGTH_SHORT).show()
                                        projectCardStates = mutableMapOf()
                                        currentCardId = 1
                                        currentProjectName = "NEW PROTOCOL CLONE"
                                    }
                                },
                                modifier = Modifier.height(34.dp),
                                border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("NEW PROJECT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }

                            // Local Drive Save
                            Button(
                                onClick = {
                                    scope.launch {
                                        Toast.makeText(context, "Saving Project payload to Local Drive directory...", Toast.LENGTH_LONG).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp).testTag("save_local_drive_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B))
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SAVE LOCAL DRIVE", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            // Print current card layout!
                            Button(
                                onClick = { printCanvasLayout(context, activeCardState.blocks, currentProjectName, currentCardId) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp).testTag("print_canvas_btn")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PRINT PDF", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F0F14),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = appBgColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            lastCursorX = offset.x
                            lastCursorY = offset.y
                        }
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    
                    // COLLAPSIBLE SIDEBAR MENU
                    AnimatedVisibility(
                        visible = sidebarOpen,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .background(panelBgColor)
                                .drawBehind {
                                    drawLine(
                                        color = borderCol,
                                        start = Offset(size.width, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "NAVIGATION TABS",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subTextColor,
                                    letterSpacing = 1.sp
                                )
                            )

                            // Navigation Subpages Buttons
                            val tabsList = listOf(
                                "canvas" to "Interactive Canvas" to Icons.Default.Dashboard,
                                "characters" to "Character Hub" to Icons.Default.People,
                                "rolls" to "Rolls & Matrix" to Icons.Default.Casino,
                                "previews" to "HTML & Renderers" to Icons.Default.OndemandVideo,
                                "user" to "User Portfolio" to Icons.Default.AccountBox,
                                "news" to "System News" to Icons.Default.Newspaper
                            )

                            tabsList.forEach { tabData ->
                                val id = tabData.first.first
                                val title = tabData.first.second
                                val icon = tabData.second
                                val isSelected = activeTab == id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF312E81).copy(alpha = 0.6f) else Color.Transparent)
                                        .clickable { activeTab = id }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF818CF8) else subTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = title,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else mainTextColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 1 TO 100 CARD PICKER SECTION
                            Text(
                                text = "CARD PICKER (1-100)",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subTextColor,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            // Dynamic Selector Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (currentCardId > 1) currentCardId-- },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = mainTextColor)
                                }

                                OutlinedTextField(
                                    value = currentCardId.toString(),
                                    onValueChange = { input ->
                                        val num = input.toIntOrNull()
                                        if (num != null && num in 1..100) {
                                            currentCardId = num
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = mainTextColor,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(4.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF818CF8),
                                        unfocusedBorderColor = borderCol
                                    )
                                )

                                IconButton(
                                    onClick = { if (currentCardId < 100) currentCardId++ },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = mainTextColor)
                                }
                            }

                            // Horizontal slider to pick cards easily
                            Slider(
                                value = currentCardId.toFloat(),
                                onValueChange = { currentCardId = it.roundToInt() },
                                valueRange = 1f..100f,
                                steps = 99,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Color-code picker for active card styling
                            Text(
                                text = "CARD BODY COLOUR",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subTextColor
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val themesList = listOf("Cyber Slate", "Liquid Laser", "Neon Pulse", "Quantum Mint")
                                themesList.forEach { thName ->
                                    val thCol = when (thName) {
                                        "Liquid Laser" -> Color(0xFFEF4444)
                                        "Neon Pulse" -> Color(0xFFEC4899)
                                        "Quantum Mint" -> Color(0xFF10B981)
                                        else -> Color(0xFF818CF8)
                                    }
                                    val isSel = activeCardState.customColorTheme == thName
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(thCol)
                                            .border(
                                                width = if (isSel) 2.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                updateActiveCard { it.copy(customColorTheme = thName) }
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Display Cursor activities
                            Text(
                                text = "Cursor position:\nX: ${lastCursorX.roundToInt()}px // Y: ${lastCursorY.roundToInt()}px",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = subTextColor
                                )
                            )

                            // Footer watermark
                            Text(
                                text = "QUANTUM SYNAPSE ARCHIVE v4.1",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = subTextColor
                            )
                        }
                    }

                    // MAIN INNER PANEL DISPATCHER
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (activeTab) {
                            "canvas" -> {
                                CanvasWorkspaceView(
                                    activeCardState = activeCardState,
                                    density = density,
                                    draggingFromPalette = draggingFromPalette,
                                    onPaletteDragStart = { type, pos ->
                                        draggingFromPalette = type
                                        dragPositionWindow = pos
                                    },
                                    onPaletteDrag = { delta ->
                                        dragPositionWindow = dragPositionWindow.plus(delta)
                                        isOverTrash = trashBoundsWindow.contains(dragPositionWindow)
                                    },
                                    onPaletteDragEnd = {
                                        val droppedType = draggingFromPalette
                                        if (droppedType != null) {
                                            if (isOverTrash) {
                                                // Drop onto trash -> discard
                                            } else if (canvasBoundsWindow.contains(dragPositionWindow)) {
                                                val relX = dragPositionWindow.x - canvasBoundsWindow.left
                                                val relY = dragPositionWindow.y - canvasBoundsWindow.top
                                                val snapPx = with(density) { 10.dp.toPx() }
                                                val snappedX = (relX / snapPx).roundToInt() * snapPx
                                                val snappedY = (relY / snapPx).roundToInt() * snapPx

                                                val newBlock = CanvasBlock(
                                                    type = droppedType,
                                                    x = snappedX,
                                                    y = snappedY,
                                                    width = droppedType.defaultWidth,
                                                    height = droppedType.defaultHeight,
                                                    title = droppedType.label.uppercase(),
                                                    content = "Dynamic input block values."
                                                )
                                                updateActiveCard { it.copy(blocks = it.blocks + newBlock) }
                                                selectedBlockId = newBlock.id
                                            }
                                        }
                                        draggingFromPalette = null
                                        isOverTrash = false
                                    },
                                    onAddInstant = { type ->
                                        val newBlock = CanvasBlock(
                                            type = type,
                                            x = 60f + (activeCardState.blocks.size * 20 % 200),
                                            y = 60f + (activeCardState.blocks.size * 25 % 300),
                                            width = type.defaultWidth,
                                            height = type.defaultHeight,
                                            title = type.label.uppercase(),
                                            content = "Instant template component."
                                        )
                                        updateActiveCard { it.copy(blocks = it.blocks + newBlock) }
                                        selectedBlockId = newBlock.id
                                    },
                                    onCanvasPositioned = { canvasBoundsWindow = it },
                                    onBlockSelect = { selectedBlockId = it },
                                    selectedBlockId = selectedBlockId,
                                    onBlockMove = { blockId, dx, dy ->
                                        val snapPx = with(density) { 10.dp.toPx() }
                                        updateActiveCard { card ->
                                            card.copy(blocks = card.blocks.map { block ->
                                                if (block.id == blockId) {
                                                    val targetX = block.x + dx
                                                    val targetY = block.y + dy
                                                    block.copy(
                                                        x = ((targetX / snapPx).roundToInt() * snapPx).toFloat(),
                                                        y = ((targetY / snapPx).roundToInt() * snapPx).toFloat()
                                                    )
                                                } else block
                                            })
                                        }
                                    },
                                    selectedBlock = selectedBlock,
                                    onBlockChange = { updated ->
                                        updateActiveCard { card ->
                                            card.copy(blocks = card.blocks.map { if (it.id == updated.id) updated else it })
                                        }
                                    },
                                    onBlockDelete = {
                                        updateActiveCard { card ->
                                            card.copy(blocks = card.blocks.filter { it.id != selectedBlockId })
                                        }
                                        selectedBlockId = null
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            "characters" -> {
                                CharacterHubView(
                                    activeCardState = activeCardState,
                                    onUpdateCard = { updateActiveCard { it } },
                                    onAddCharacter = { newChar ->
                                        updateActiveCard { it.copy(charactersInCard = it.charactersInCard + newChar) }
                                    },
                                    onRemoveCharacter = { charId ->
                                        updateActiveCard { it.copy(charactersInCard = it.charactersInCard.filter { c -> c.id != charId }) }
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            "rolls" -> {
                                RollsMatrixView(
                                    activeCardState = activeCardState,
                                    onUpdateCard = { updated -> updateActiveCard { updated } },
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            "previews" -> {
                                PreviewsRenderView(
                                    activeCardState = activeCardState,
                                    projectName = currentProjectName,
                                    priceQuote = priceQuote,
                                    renderingPicture = renderingPicture,
                                    pictureProgress = pictureProgress,
                                    generatedPictureUri = generatedPictureUri,
                                    onGeneratePicture = {
                                        scope.launch {
                                            renderingPicture = true
                                            pictureProgress = 0f
                                            while (pictureProgress < 1.0f) {
                                                delay(100)
                                                pictureProgress += 0.05f
                                            }
                                            renderingPicture = false
                                            generatedPictureUri = "https://picsum.photos/300/300"
                                        }
                                    },
                                    renderingSound = renderingSound,
                                    soundProgress = soundProgress,
                                    generatedSoundUri = generatedSoundUri,
                                    onGenerateSound = {
                                        scope.launch {
                                            renderingSound = true
                                            soundProgress = 0f
                                            while (soundProgress < 1.0f) {
                                                delay(120)
                                                soundProgress += 0.04f
                                            }
                                            renderingSound = false
                                            generatedSoundUri = "sound_clip_omega_synth"
                                        }
                                    },
                                    renderingVideo = renderingVideo,
                                    videoProgress = videoProgress,
                                    generatedVideoUri = generatedVideoUri,
                                    onGenerateVideo = {
                                        scope.launch {
                                            renderingVideo = true
                                            videoProgress = 0f
                                            while (videoProgress < 1.0f) {
                                                delay(150)
                                                videoProgress += 0.03f
                                            }
                                            renderingVideo = false
                                            generatedVideoUri = "video_sequence_render_final"
                                        }
                                    },
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            "user" -> {
                                UserPortfolioView(
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            "news" -> {
                                SystemNewsView(
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                    }
                }

                // BOTTOM TRASH ZONE (Shows up when dragging from palette)
                AnimatedVisibility(
                    visible = draggingFromPalette != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                trashBoundsWindow = coordinates.boundsInWindow()
                            }
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                if (isOverTrash) Color(0xFF7F1D1D).copy(alpha = 0.95f)
                                else Color(0xFF111116).copy(alpha = 0.9f)
                            )
                            .border(
                                1.dp,
                                if (isOverTrash) Color(0xFFF87171) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(30.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Trash",
                            tint = if (isOverTrash) Color.Red else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isOverTrash) "RELEASE TO DISCARD" else "DRAG HERE TO DELETE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverTrash) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Floating silhouette during active drag
                if (draggingFromPalette != null) {
                    val floatType = draggingFromPalette!!
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (dragPositionWindow.x - with(density) { (floatType.defaultWidth / 2).dp.toPx() }).roundToInt(),
                                    (dragPositionWindow.y - with(density) { 30.dp.toPx() }).roundToInt()
                                )
                            }
                            .size(floatType.defaultWidth.dp, floatType.defaultHeight.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF818CF8).copy(alpha = 0.4f))
                            .border(1.5.dp, Color(0xFF818CF8), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = floatType.icon, contentDescription = null, tint = Color.White)
                            Text(text = floatType.label.uppercase(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // PROJECT RENAME / LOAD DIALOG
    if (showProjectSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showProjectSelectorDialog = false },
            title = { Text("WORKSPACE PROJECTS DATABASE", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Saved Projects Section:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    
                    // Display existing list to load
                    projectList.forEach { pName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (currentProjectName == pName) Color(0xFF312E81) else Color.DarkGray.copy(alpha = 0.2f))
                                .clickable {
                                    currentProjectName = pName
                                    showProjectSelectorDialog = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                Text(pName, fontSize = 12.sp, color = Color.White)
                            }
                            if (currentProjectName == pName) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Register New Project Name:", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    OutlinedTextField(
                        value = newProjNameInput,
                        onValueChange = { newProjNameInput = it },
                        textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                        placeholder = { Text("Cybermind-Alpha", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjNameInput.isNotBlank()) {
                            currentProjectName = newProjNameInput
                            projectList = projectList + newProjNameInput
                            newProjNameInput = ""
                        }
                        showProjectSelectorDialog = false
                    }
                ) {
                    Text("SAVE & LOAD", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProjectSelectorDialog = false }) {
                    Text("CLOSE", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        )
    }
}

// ==========================================
// DETAILED COMPOSTABLE SUITE VIEWS
// ==========================================

@Composable
fun CanvasWorkspaceView(
    activeCardState: CardWorkspaceState,
    density: androidx.compose.ui.unit.Density,
    draggingFromPalette: BlockType?,
    onPaletteDragStart: (BlockType, Offset) -> Unit,
    onPaletteDrag: (Offset) -> Unit,
    onPaletteDragEnd: () -> Unit,
    onAddInstant: (BlockType) -> Unit,
    onCanvasPositioned: (Rect) -> Unit,
    onBlockSelect: (String) -> Unit,
    selectedBlockId: String?,
    onBlockMove: (String, Float, Float) -> Unit,
    selectedBlock: CanvasBlock?,
    onBlockChange: (CanvasBlock) -> Unit,
    onBlockDelete: () -> Unit,
    isDarkTheme: Boolean
) {
    Row(modifier = Modifier.fillMaxSize()) {
        
        // LEFT PALETTE SIDEBAR
        BlockPaletteSidebar(
            onDragStart = onPaletteDragStart,
            onDrag = onPaletteDrag,
            onDragEnd = onPaletteDragEnd,
            onAddInstant = onAddInstant
        )

        // CANVAS WORKSPACE GRID
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, if (isDarkTheme) Color(0xFF1E1E2A) else Color(0xFFCBD5E1))
                .onGloballyPositioned { coordinates ->
                    onCanvasPositioned(coordinates.boundsInWindow())
                }
                .drawBehind {
                    val gridSpacing = 20.dp.toPx()
                    for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                        drawLine(
                            color = Color(0xFF4F46E5).copy(alpha = 0.07f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                        drawLine(
                            color = Color(0xFF4F46E5).copy(alpha = 0.07f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }
                .background(if (isDarkTheme) Color(0xFF030305) else Color(0xFFF8FAFC))
        ) {
            if (activeCardState.blocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Grid4x4,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "CANVAS EMPTY IN CARD #${activeCardState.cardId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Drag layout blocks here or tap '+' sidebar icons.",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            activeCardState.blocks.forEach { block ->
                key(block.id) {
                    CanvasBlockItem(
                        block = block,
                        isSelected = block.id == selectedBlockId,
                        onSelect = { onBlockSelect(block.id) },
                        onMove = { dx, dy -> onBlockMove(block.id, dx, dy) }
                    )
                }
            }
        }

        // RIGHT PROPERTY INSPECTOR PANEL
        PropertyInspectorPanel(
            selectedBlock = selectedBlock,
            onBlockChange = onBlockChange,
            onDelete = onBlockDelete
        )
    }
}

// ==========================================
// CHARACTER DATABASE TAB VIEW
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterHubView(
    activeCardState: CardWorkspaceState,
    onUpdateCard: () -> Unit,
    onAddCharacter: (CharacterInfo) -> Unit,
    onRemoveCharacter: (String) -> Unit,
    isDarkTheme: Boolean
) {
    var charName by remember { mutableStateOf("") }
    var charRole by remember { mutableStateOf(CharacterRole.MAIN) }
    var charDesc by remember { mutableStateOf("") }

    val contentBg = if (isDarkTheme) Color(0xFF0F0F14) else Color.White
    val textCol = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👥 CHARACTER SYNC CENTER",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
        )
        Text(
            text = "Add characters to compile list and bind them to cognitive synapse cards.",
            fontSize = 11.sp,
            color = textCol.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New character form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentBg)
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ADD NEW CHARACTER BLOCK", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                OutlinedTextField(
                    value = charName,
                    onValueChange = { charName = it },
                    label = { Text("Character Name", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Dropdown Selection grid
                Text("ROLE CATEGORY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = textCol.copy(alpha = 0.5f))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CharacterRole.values().forEach { role ->
                        val isSel = charRole == role
                        FilterChip(
                            selected = isSel,
                            onClick = { charRole = role },
                            label = { Text(role.label, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                        )
                    }
                }

                OutlinedTextField(
                    value = charDesc,
                    onValueChange = { charDesc = it },
                    label = { Text("Description & Notes", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (charName.isNotBlank()) {
                            onAddCharacter(
                                CharacterInfo(
                                    name = charName,
                                    role = charRole,
                                    description = charDesc
                                )
                            )
                            charName = ""
                            charDesc = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("ADD TO CARD", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Registered list
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentBg)
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "REGISTERED ON CARD #${activeCardState.cardId}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )

                if (activeCardState.charactersInCard.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No characters linked to this card index.", fontSize = 11.sp, color = textCol.copy(alpha = 0.4f))
                    }
                } else {
                    activeCardState.charactersInCard.forEach { char ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(char.role.color.copy(alpha = 0.12f))
                                .border(0.5.dp, char.role.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(char.role.color))
                                    Text(char.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textCol)
                                }
                                Text(char.role.label.uppercase(), fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = char.role.color)
                                if (char.description.isNotBlank()) {
                                    Text(char.description, fontSize = 10.sp, color = textCol.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            IconButton(onClick = { onRemoveCharacter(char.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STORY FIELDS & ROLLS MATRIX VIEW
// ==========================================

@Composable
fun RollsMatrixView(
    activeCardState: CardWorkspaceState,
    onUpdateCard: (CardWorkspaceState) -> Unit,
    isDarkTheme: Boolean
) {
    val textCol = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎲 ROLLS MATRIX & STORY MAPPING DETAILS",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Story metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("STORY MAPPING INDEX", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeCardState.actNum,
                        onValueChange = { onUpdateCard(activeCardState.copy(actNum = it)) },
                        label = { Text("Act #", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.pgNum,
                        onValueChange = { onUpdateCard(activeCardState.copy(pgNum = it)) },
                        label = { Text("Page ##", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.trackNum,
                        onValueChange = { onUpdateCard(activeCardState.copy(trackNum = it)) },
                        label = { Text("Track #", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = activeCardState.videoClip,
                    onValueChange = { onUpdateCard(activeCardState.copy(videoClip = it)) },
                    label = { Text("Video Clip Reference ID", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeCardState.sceneName,
                    onValueChange = { onUpdateCard(activeCardState.copy(sceneName = it)) },
                    label = { Text("Scene Title", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeCardState.locationSetting,
                    onValueChange = { onUpdateCard(activeCardState.copy(locationSetting = it)) },
                    label = { Text("INT/EXT Location", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Narrative dynamics
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NARRATIVE SEQUENCE GUIDE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                OutlinedTextField(
                    value = activeCardState.startsWith,
                    onValueChange = { onUpdateCard(activeCardState.copy(startsWith = it)) },
                    label = { Text("IN (Entrance / starts with...)", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeCardState.actionBeat1,
                    onValueChange = { onUpdateCard(activeCardState.copy(actionBeat1 = it)) },
                    label = { Text("Beat 1: Action/Response", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeCardState.reversalTurn,
                    onValueChange = { onUpdateCard(activeCardState.copy(reversalTurn = it)) },
                    label = { Text("TURN: Realization / Reversal", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeCardState.startEmotion,
                        onValueChange = { onUpdateCard(activeCardState.copy(startEmotion = it)) },
                        label = { Text("EMO Start", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.endEmotion,
                        onValueChange = { onUpdateCard(activeCardState.copy(endEmotion = it)) },
                        label = { Text("EMO End", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ATMOSPHERIC ROLLS & MATRIX SHOT SETTINGS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ATMOSPHERIC ROLLS SLOTS (A-F)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeCardState.rollA,
                        onValueChange = { onUpdateCard(activeCardState.copy(rollA = it)) },
                        label = { Text("Roll A", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.rollB,
                        onValueChange = { onUpdateCard(activeCardState.copy(rollB = it)) },
                        label = { Text("Roll B", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.rollC,
                        onValueChange = { onUpdateCard(activeCardState.copy(rollC = it)) },
                        label = { Text("Roll C", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeCardState.rollD,
                        onValueChange = { onUpdateCard(activeCardState.copy(rollD = it)) },
                        label = { Text("Roll D", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.rollE,
                        onValueChange = { onUpdateCard(activeCardState.copy(rollE = it)) },
                        label = { Text("Roll E", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeCardState.rollF,
                        onValueChange = { onUpdateCard(activeCardState.copy(rollF = it)) },
                        label = { Text("Roll F", fontSize = 10.sp) },
                        textStyle = TextStyle(fontSize = 11.sp, color = textCol),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("CAMERA / AUDIO / SFX METRICS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                OutlinedTextField(
                    value = activeCardState.cameraShot,
                    onValueChange = { onUpdateCard(activeCardState.copy(cameraShot = it)) },
                    label = { Text("SHOT Coverage", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeCardState.soundsTheme,
                    onValueChange = { onUpdateCard(activeCardState.copy(soundsTheme = it)) },
                    label = { Text("Sounds / Ambience Background", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeCardState.visualEfx,
                    onValueChange = { onUpdateCard(activeCardState.copy(visualEfx = it)) },
                    label = { Text("Visual EFX (e.g. Laser Particle)", fontSize = 11.sp) },
                    textStyle = TextStyle(fontSize = 12.sp, color = textCol),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS & GENERATIVE RENDER VIEW
// ==========================================

@Composable
fun PreviewsRenderView(
    activeCardState: CardWorkspaceState,
    projectName: String,
    priceQuote: Double,
    renderingPicture: Boolean,
    pictureProgress: Float,
    generatedPictureUri: String?,
    onGeneratePicture: () -> Unit,
    renderingSound: Boolean,
    soundProgress: Float,
    generatedSoundUri: String?,
    onGenerateSound: () -> Unit,
    renderingVideo: Boolean,
    videoProgress: Float,
    generatedVideoUri: String?,
    onGenerateVideo: () -> Unit,
    isDarkTheme: Boolean
) {
    val textCol = if (isDarkTheme) Color.White else Color.Black
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎬 ACTIVE MULTIMEDIA RENDERERS",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
        )

        // Rendering progress rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Image Generator
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PICTURE GENERATOR", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)
                
                if (generatedPictureUri != null) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.DarkGray)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw synthetic visual artwork matrix
                            drawRect(
                                brush = Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFFEC4899)))
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("SYNTH FRAME", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = textCol.copy(alpha = 0.2f), modifier = Modifier.size(60.dp))
                }

                if (renderingPicture) {
                    LinearProgressIndicator(progress = pictureProgress, modifier = Modifier.fillMaxWidth())
                    Text("Rendering picture details... ${(pictureProgress * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = textCol)
                } else {
                    Button(onClick = onGeneratePicture) {
                        Text(if (generatedPictureUri != null) "RE-GENERATE" else "GENERATE", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Audio Sound Generator
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SOUND SYNTHESIS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                if (generatedSoundUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                        Text("MP3: Active Waveform", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Green)
                    }
                } else {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = textCol.copy(alpha = 0.2f), modifier = Modifier.size(60.dp))
                }

                if (renderingSound) {
                    LinearProgressIndicator(progress = soundProgress, modifier = Modifier.fillMaxWidth())
                    Text("Synthesizing waveforms... ${(soundProgress * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = textCol)
                } else {
                    Button(onClick = onGenerateSound) {
                        Text(if (generatedSoundUri != null) "RE-GENERATE" else "SYNTHESIZE", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // High Fidelity Video Render
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VIDEO CLIP RENDERER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                if (generatedVideoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color.DarkGray)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF3B82F6)))
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("RENDER OK", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = textCol.copy(alpha = 0.2f), modifier = Modifier.size(60.dp))
                }

                if (renderingVideo) {
                    LinearProgressIndicator(progress = videoProgress, modifier = Modifier.fillMaxWidth())
                    Text("Assembling videoclip... ${(videoProgress * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = textCol)
                } else {
                    Button(onClick = onGenerateVideo) {
                        Text(if (generatedVideoUri != null) "RE-RENDER VIDEO" else "COMPILE VIDEO", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // PRICE QUOTING SECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF312E81).copy(alpha = 0.2f))
                .border(1.dp, Color(0xFF4F46E5).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("INTEGRATED QUOTE SYSTEM", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)
                Text("Calculated pricing for active card blocks, linked characters, and multi-track database sync.", fontSize = 9.sp, color = textCol.copy(alpha = 0.6f))
            }
            Text(
                text = "$${String.format("%.2f", priceQuote)} USD",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
            )
        }

        // HTML & TEXT PREVIEW SECTION
        Text(
            text = "📜 COGNITIVE RAW DATA & HTML SOURCE",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textCol)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Raw text layout preview
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("STORY MAPPING TEXT PREVIEW", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val previewStr = """
                    PROJECT: $projectName
                    CARD ID: #${activeCardState.cardId}
                    ACT: ${activeCardState.actNum} // PAGE: ${activeCardState.pgNum}
                    SCENE: ${activeCardState.sceneName}
                    LOCATION: ${activeCardState.locationSetting}
                    IN: ${activeCardState.startsWith}
                    BEAT: ${activeCardState.actionBeat1}
                    TURN: ${activeCardState.reversalTurn}
                    ROLLS: [A: ${activeCardState.rollA}] [B: ${activeCardState.rollB}]
                    CHARACTERS: ${activeCardState.charactersInCard.joinToString(", ") { it.name }}
                """.trimIndent()

                Text(
                    text = previewStr,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = textCol.copy(alpha = 0.8f), lineHeight = 14.sp)
                )
            }

            // Raw HTML Preview markup
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("HTML EMBED PAYLOAD", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val markup = """
                    <div class="quantum-card">
                      <h3>${activeCardState.sceneName}</h3>
                      <p class="meta">Act ${activeCardState.actNum} / Page ${activeCardState.pgNum}</p>
                      <p class="narrative">${activeCardState.actionBeat1}</p>
                    </div>
                """.trimIndent()

                Text(
                    text = markup,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = textCol.copy(alpha = 0.6f), lineHeight = 14.sp)
                )
            }
        }
    }
}

// ==========================================
// USER PORTFOLIO TAB VIEW
// ==========================================

@Composable
fun UserPortfolioView(
    isDarkTheme: Boolean
) {
    val textCol = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👤 USER COGNITIVE PORTFOLIO",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("COGNITIVE USER DETAILS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF818CF8))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("NC", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column {
                        Text("Nova Creator Alpha", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
                        Text("Rank: Quantum Architect", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF818CF8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("User Memory Block:", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = textCol.copy(alpha = 0.6f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(8.dp)
                ) {
                    Text(
                        "Active session tracks synchronized with stable synapse networks. Total memory usage: 4.8 KB. Sector partition complete.",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Green
                    )
                }
            }

            // Portfolio Subpages
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("PORTFOLIO SHOWCASE SUBPAGES", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                val portfolios = listOf(
                    "Synergy Codex" to "A deep cybernetic collection of visual script layouts.",
                    "The Unbound Link" to "Quantum science and AI-generated cinematic notes.",
                    "Pulse Division Archive" to "Historic memory nodes saved from stable networks."
                )

                portfolios.forEach { port ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(port.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textCol)
                        Text(port.second, fontSize = 10.sp, color = textCol.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// ==========================================
// SYSTEM NEWS & LOGS VIEW
// ==========================================

@Composable
fun SystemNewsView(
    isDarkTheme: Boolean
) {
    val textCol = if (isDarkTheme) Color.White else Color.Black
    val panelBg = if (isDarkTheme) Color(0xFF111116) else Color.White
    val borderCol = if (isDarkTheme) Color(0xFF1E1E2A) else Color(0xFFE2E8F0)
    var isBatchLoading by remember { mutableStateOf(false) }
    var loadedBatchName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📰 COGNITIVE NETWORK NEWS & SECTION FILES",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textCol)
        )

        // BATCH FILES LOADING SYSTEM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(panelBg)
                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📁 SYSTEM BATCH IMPORT ENGINE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = textCol
            )
            Text(
                text = "Instantly load bulk multi-card storyboard parameters, actor portfolios, or sound library presets directly into the workspace active card database.",
                fontSize = 10.sp,
                color = textCol.copy(alpha = 0.6f)
            )

            if (isBatchLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Decompressing story archive segments...", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF818CF8))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isBatchLoading = true
                            loadedBatchName = "cinematic_space_opera_v2.json"
                            // Simulated fast load delay
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isBatchLoading = false
                            }, 1500)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LOAD SPACE OPERA BATCH", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            isBatchLoading = true
                            loadedBatchName = "cyber_noir_neon_detective.json"
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isBatchLoading = false
                            }, 1500)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1B4B))
                    ) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LOAD NEON DETECTIVE BATCH", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            if (loadedBatchName != null && !isBatchLoading) {
                Text(
                    text = "STATUS: Successfully integrated $loadedBatchName! Active cards 1-5 have been auto-populated with theme layouts.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Green
                )
            }
        }

        // SHOW FILES FOR EACH SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(panelBg)
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("STORY SECTION FILES", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)
                
                val storyFiles = listOf("act1_genesis_intro.txt", "act2_mainframe_breach.txt", "character_nova_profile.json")
                storyFiles.forEach { fName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                            Text(fName, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = textCol)
                        }
                        Text("1.2 KB", fontSize = 8.sp, color = textCol.copy(alpha = 0.4f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(panelBg)
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("MEDIA & RENDERS SECTION FILES", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textCol)

                val mediaFiles = listOf("omega_synth_reverb.mp3", "video_sequence_render.mp4", "synth_frame_302.png")
                mediaFiles.forEach { fName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(14.dp))
                            Text(fName, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = textCol)
                        }
                        Text("2.4 MB", fontSize = 8.sp, color = textCol.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // LATEST RELEASES SYSTEM NEWS
        Text(
            text = "LATEST COGNITIVE MAINFRAME DEV LOGS",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = textCol
        )

        val newsItems = listOf(
            "Quantum Matrix Update v4.1 Released" to "Users can now manage up to 100 card profiles independently per project. Seamless layout generation speeds increased by 40%.",
            "Liquid Laser Core Stabilized" to "Cognitive mainframe reports zero partitions broken. Safe cybernetic nodes have been successfully compiled in stable sectors.",
            "Visual Sound Generation Synthesis" to "Interactive generators now simulate picture and sound rendering utilizing localized vector canvas loops."
        )

        newsItems.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF818CF8)))
                    Text(item.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textCol)
                }
                Text(item.second, fontSize = 11.sp, color = textCol.copy(alpha = 0.7f), lineHeight = 15.sp)
            }
        }
    }
}

// ==========================================
// PALETTE SIDEBAR COMPOSABLE
// ==========================================

@Composable
fun BlockPaletteSidebar(
    onDragStart: (BlockType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onAddInstant: (BlockType) -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F0F14))
            .drawBehind {
                drawLine(
                    color = Color(0xFF1E1E2A),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "LAYOUT BLOCKS",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        )

        BlockType.values().forEach { type ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF16161F))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onDragStart(type, Offset(offset.x, offset.y))
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Column {
                            Text(
                                text = type.name,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "${type.defaultWidth}x${type.defaultHeight}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { onAddInstant(type) },
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add node",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF312E81).copy(alpha = 0.3f))
                .border(1.dp, Color(0xFF4F46E5).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "TUTORIAL: Long-press and drag any node block to the canvas grid, or tap '+' to instantiate layouts instantly.",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontSize = 9.sp,
                    color = Color(0xFFC7D2FE),
                    lineHeight = 13.sp
                )
            )
        }
    }
}

// ==========================================
// RENDERED BLOCK ITEM COMPONENT ON CANVAS
// ==========================================

@Composable
fun CanvasBlockItem(
    block: CanvasBlock,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (dx: Float, dy: Float) -> Unit
) {
    val (primaryColor, gradientColors) = remember(block.colorTheme) {
        when (block.colorTheme) {
            "Liquid Laser" -> Color(0xFFEF4444) to listOf(Color(0xFF7F1D1D), Color(0xFF1F1212))
            "Neon Pulse" -> Color(0xFFEC4899) to listOf(Color(0xFF701A4F), Color(0xFF1F0C16))
            "Quantum Mint" -> Color(0xFF10B981) to listOf(Color(0xFF064E3B), Color(0xFF0C1F18))
            else -> Color(0xFF818CF8) to listOf(Color(0xFF1E1B4B), Color(0xFF0C0B1B))
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(block.x.roundToInt(), block.y.roundToInt()) }
            .size(block.width.dp, block.height.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        when (block.type) {
            BlockType.TITLE -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = when (block.textAlignment) {
                        TextAlign.Center -> Alignment.CenterHorizontally
                        TextAlign.Right -> Alignment.End
                        else -> Alignment.Start
                    }
                ) {
                    Text(
                        text = block.title.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = block.content,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = block.textAlignment
                    )
                }
            }
            
            BlockType.NARRATIVE -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = block.title.uppercase(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = primaryColor
                            )
                        )
                        Icon(imageVector = Icons.Default.Notes, contentDescription = null, tint = primaryColor.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.content,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        ),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = block.textAlignment
                    )
                }
            }

            BlockType.TECH_SPEC -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = block.title.uppercase(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = primaryColor
                        )
                    )
                    
                    val specParts = remember(block.content) {
                        block.content.split("//").map { it.trim() }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        specParts.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { part ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(0.5.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = part,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            BlockType.VISUAL_SYNTH -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = block.title,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = primaryColor)
                    )
                    
                    Canvas(modifier = Modifier.size(50.dp)) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.15f),
                            radius = size.width / 2f
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = size.width / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                            alpha = 0.4f
                        )
                    }

                    Text(
                        text = block.content,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                    )
                }
            }

            BlockType.STATUS_BANNER -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Column {
                        Text(
                            text = block.title.uppercase(),
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = block.content,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            BlockType.DIVIDER -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        drawLine(
                            color = primaryColor,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CANVASS BLOCK PROPERTY INSPECTOR PANEL
// ==========================================

@Composable
fun PropertyInspectorPanel(
    selectedBlock: CanvasBlock?,
    onBlockChange: (CanvasBlock) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F0F14))
            .drawBehind {
                drawLine(
                    color = Color(0xFF1E1E2A),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PROPERTY INSPECTOR",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        )

        if (selectedBlock == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SELECT NODE ON GRID TO CUSTOMIZE DETAILS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(text = "NODE TYPE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    Text(text = selectedBlock.type.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (selectedBlock.type != BlockType.DIVIDER) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "LABEL / OVERHEAD", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                        OutlinedTextField(
                            value = selectedBlock.title,
                            onValueChange = { onBlockChange(selectedBlock.copy(title = it)) },
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth().testTag("inspect_label_input"),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                if (selectedBlock.type != BlockType.DIVIDER) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "PAYLOAD CONTENT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                            Text(
                                text = "${selectedBlock.content.length} / 900",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedBlock.content.length >= 900) Color.Red else Color.White.copy(alpha = 0.5f)
                            )
                        }
                        OutlinedTextField(
                            value = selectedBlock.content,
                            onValueChange = { 
                                if (it.length <= 900) {
                                    onBlockChange(selectedBlock.copy(content = it))
                                }
                            },
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("inspect_content_input"),
                            maxLines = 4,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "LAYOUT DIMENSIONS (DP)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "W: ${selectedBlock.width}dp", fontSize = 9.sp, color = Color.White)
                            Slider(
                                value = selectedBlock.width.toFloat(),
                                onValueChange = { onBlockChange(selectedBlock.copy(width = it.roundToInt())) },
                                valueRange = 100f..350f,
                                steps = 25
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "H: ${selectedBlock.height}dp", fontSize = 9.sp, color = Color.White)
                            Slider(
                                value = selectedBlock.height.toFloat(),
                                onValueChange = { onBlockChange(selectedBlock.copy(height = it.roundToInt())) },
                                valueRange = 30f..250f,
                                steps = 22
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "VISUAL INTENSITY THEME", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                    val themes = listOf("Cyber Slate", "Liquid Laser", "Neon Pulse", "Quantum Mint")
                    
                    themes.forEach { themeName ->
                        val isSel = selectedBlock.colorTheme == themeName
                        val thCol = when (themeName) {
                            "Liquid Laser" -> Color(0xFFEF4444)
                            "Neon Pulse" -> Color(0xFFEC4899)
                            "Quantum Mint" -> Color(0xFF10B981)
                            else -> Color(0xFF818CF8)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) thCol.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (isSel) thCol else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { onBlockChange(selectedBlock.copy(colorTheme = themeName)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(thCol))
                            Text(text = themeName, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                if (selectedBlock.type == BlockType.TITLE || selectedBlock.type == BlockType.NARRATIVE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "TEXT ALIGNMENT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val alignOptions = listOf(
                                TextAlign.Left to Icons.Default.AlignHorizontalLeft,
                                TextAlign.Center to Icons.Default.AlignHorizontalCenter,
                                TextAlign.Right to Icons.Default.AlignHorizontalRight
                            )
                            alignOptions.forEach { (align, icon) ->
                                val isSel = selectedBlock.textAlignment == align
                                IconButton(
                                    onClick = { onBlockChange(selectedBlock.copy(textAlignment = align)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSel) Color(0xFF312E81) else Color.White.copy(alpha = 0.05f))
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.fillMaxWidth().testTag("inspect_delete_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DELETE NODE", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ==========================================
// UTILITY TEMPLATE POPULATORS & PRINTERS
// ==========================================

private fun getInitialTemplates(): List<CanvasBlock> {
    return listOf(
        CanvasBlock(
            type = BlockType.TITLE,
            x = 40f,
            y = 30f,
            width = 330,
            height = 70,
            title = "COGNITIVE SYSTEM",
            content = "SYNAPSE NODE ACTIVE"
        ),
        CanvasBlock(
            type = BlockType.NARRATIVE,
            x = 40f,
            y = 120f,
            width = 330,
            height = 140,
            title = "ATMOSPHERIC TRANSCRIPT",
            content = "Nova's thoughts synchronize inside the container. Liquid laser matrix channels pulse with information. Security partition holds firm."
        ),
        CanvasBlock(
            type = BlockType.DIVIDER,
            x = 40f,
            y = 280f,
            width = 330,
            height = 25,
            title = "",
            content = ""
        ),
        CanvasBlock(
            type = BlockType.TECH_SPEC,
            x = 40f,
            y = 320f,
            width = 330,
            height = 130,
            title = "COGNITIVE PARAMETERS MATRIX",
            content = "ROLL A: ACTIVE // ROLL B: SYNAPSE // ROLL C: SECURE // ROLL D: UNBOUND"
        )
    )
}

fun printCanvasLayout(context: Context, blocks: List<CanvasBlock>, projectName: String, cardId: Int) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = view.createPrintDocumentAdapter("Synapse_Canvas_Card_$cardId")
            printManager.print("Synapse_Canvas_Card_$cardId", printAdapter, PrintAttributes.Builder().build())
        }
    }

    val htmlBlocks = blocks.joinToString("") { block ->
        val borderStyle = when (block.colorTheme) {
            "Liquid Laser" -> "border-left: 6px solid #EF4444; background: #fff5f5;"
            "Neon Pulse" -> "border-left: 6px solid #EC4899; background: #fdf2f8;"
            "Quantum Mint" -> "border-left: 6px solid #10B981; background: #f0fdf4;"
            else -> "border-left: 6px solid #818CF8; background: #f5f3ff;"
        }
        val headingColor = when (block.colorTheme) {
            "Liquid Laser" -> "#EF4444"
            "Neon Pulse" -> "#EC4899"
            "Quantum Mint" -> "#10B981"
            else -> "#818CF8"
        }

        when (block.type) {
            BlockType.TITLE -> """
                <div class="block-card" style="$borderStyle">
                    <div class="block-type" style="color: $headingColor;">HEADING TITLE [Y: ${block.y.roundToInt()}dp]</div>
                    <div style="font-size: 11px; text-transform: uppercase; letter-spacing: 1px;">${block.title}</div>
                    <div style="font-size: 18px; font-weight: 900; margin-top: 3px; text-align: ${getTextAlignHtml(block.textAlignment)};">${block.content}</div>
                </div>
            """.trimIndent()

            BlockType.NARRATIVE -> """
                <div class="block-card" style="$borderStyle">
                    <div class="block-type" style="color: $headingColor;">NARRATIVE TRANSCRIPT [Y: ${block.y.roundToInt()}dp]</div>
                    <div style="font-weight: bold; font-size: 11px; margin-bottom: 5px;">${block.title}</div>
                    <div style="font-size: 13px; line-height: 1.5; white-space: pre-wrap; text-align: ${getTextAlignHtml(block.textAlignment)};">${block.content.take(900)}</div>
                </div>
            """.trimIndent()

            BlockType.TECH_SPEC -> {
                val gridHtml = block.content.split("//").map { it.trim() }.joinToString("") { 
                    """<div style="background: #eee; padding: 4px 8px; border-radius: 4px; font-family: monospace; font-size: 11px;">$it</div>"""
                }
                """
                <div class="block-card" style="$borderStyle">
                    <div class="block-type" style="color: $headingColor;">TECH MATRIX [Y: ${block.y.roundToInt()}dp]</div>
                    <div style="font-weight: bold; font-size: 11px; margin-bottom: 6px;">${block.title}</div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                        $gridHtml
                    </div>
                </div>
                """.trimIndent()
            }

            BlockType.VISUAL_SYNTH -> """
                <div class="block-card" style="$borderStyle text-align: center;">
                    <div class="block-type" style="color: $headingColor;">VISUAL SYNAPSE RING [Y: ${block.y.roundToInt()}dp]</div>
                    <div style="font-weight: bold; font-size: 10px; color: #666;">${block.title}</div>
                    <div style="margin: 12px auto; width: 40px; height: 40px; border-radius: 50%; border: 3px dashed $headingColor;"></div>
                    <div style="font-family: monospace; font-size: 11px;">${block.content}</div>
                </div>
            """.trimIndent()

            BlockType.STATUS_BANNER -> """
                <div class="block-card" style="$borderStyle display: flex; align-items: center; gap: 10px;">
                    <div style="width: 10px; height: 10px; border-radius: 50%; background-color: $headingColor;"></div>
                    <div style="flex: 1;">
                        <div class="block-type" style="color: $headingColor; margin-bottom: 2px;">STATUS ALERT</div>
                        <div style="font-size: 12px; font-weight: bold; font-family: monospace;">${block.content}</div>
                    </div>
                </div>
            """.trimIndent()

            BlockType.DIVIDER -> """
                <div style="margin: 15px 0; border-top: 2px dashed #ccc; text-align: center;">
                    <span style="background: #fff; padding: 0 10px; font-size: 9px; font-family: monospace; color: #999;">LINK SEPARATOR</span>
                </div>
            """.trimIndent()
        }
    }

    val htmlContent = """
        <html>
        <head>
            <style>
                @page {
                    size: A4 portrait;
                    margin: 20mm;
                }
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    background-color: #ffffff;
                    color: #111111;
                    line-height: 1.5;
                }
                .container {
                    max-width: 700px;
                    margin: 0 auto;
                }
                .main-header {
                    border-bottom: 3px double #111;
                    padding-bottom: 15px;
                    margin-bottom: 25px;
                    text-align: center;
                }
                .main-title {
                    font-size: 24px;
                    font-weight: 900;
                    letter-spacing: 1px;
                }
                .main-subtitle {
                    font-size: 10px;
                    font-family: monospace;
                    color: #555;
                    margin-top: 5px;
                }
                .block-card {
                    border-radius: 8px;
                    padding: 16px;
                    margin-bottom: 18px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
                }
                .block-type {
                    font-family: monospace;
                    font-weight: 900;
                    font-size: 9px;
                    margin-bottom: 4px;
                    text-transform: uppercase;
                }
                .footer {
                    margin-top: 40px;
                    border-top: 1px solid #ddd;
                    padding-top: 15px;
                    text-align: center;
                    font-size: 10px;
                    font-family: monospace;
                    color: #666;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="main-header">
                    <div class="main-title">${projectName.uppercase()}</div>
                    <div class="main-subtitle">SYNAPSE ARCHIVE // CARD INDEX #$cardId // PRINTED OVERHEAD</div>
                </div>
                
                $htmlBlocks
                
                <div class="footer">
                    END OF SEQUENCE REPORT // COGNITIVE MAIN SYSTEM SECURED // COPIES AUTHORIZED
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
}

private fun getTextAlignHtml(alignment: TextAlign): String {
    return when (alignment) {
        TextAlign.Center -> "center"
        TextAlign.Right -> "right"
        else -> "left"
    }
}
