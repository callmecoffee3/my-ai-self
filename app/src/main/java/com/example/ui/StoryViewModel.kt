package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StoryRepository(application)

    // Raw flows from repository
    val projectInfo: StateFlow<ProjectInfoEntity?> = repository.projectInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks: StateFlow<List<TrackEntity>> = repository.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val acts: StateFlow<List<ActEntity>> = repository.acts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cards: StateFlow<List<CardEntity>> = repository.cards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI selection state
    private val _selectedCardId = MutableStateFlow<Int?>(1) // Default to first card
    val selectedCardId: StateFlow<Int?> = _selectedCardId.asStateFlow()

    // Combined Flow for detailed view of selected Card
    val selectedCard: StateFlow<CardEntity?> = _selectedCardId
        .flatMapLatest { id ->
            if (id != null) repository.getCardById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filter states
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedActId = MutableStateFlow<Int?>(null)
    val selectedActId: StateFlow<Int?> = _selectedActId.asStateFlow()

    private val _selectedTrackId = MutableStateFlow<Int?>(null)
    val selectedTrackId: StateFlow<Int?> = _selectedTrackId.asStateFlow()

    // Combined filtered cards list
    val filteredCards: StateFlow<List<CardEntity>> = combine(
        cards,
        _searchText,
        _selectedActId,
        _selectedTrackId
    ) { cardsList, text, actId, trackId ->
        cardsList.filter { card ->
            val matchesText = text.isEmpty() ||
                    card.title.contains(text, ignoreCase = true) ||
                    card.description.contains(text, ignoreCase = true) ||
                    card.content.contains(text, ignoreCase = true)
            
            val matchesAct = actId == null || card.actId == actId
            val matchesTrack = trackId == null || card.trackId == trackId

            matchesText && matchesAct && matchesTrack
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI generation states
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationLogs = MutableStateFlow<List<String>>(emptyList())
    val generationLogs: StateFlow<List<String>> = _generationLogs.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    init {
        // Initialize the database on startup
        viewModelScope.launch {
            repository.initializeIfNeeded()
        }
    }

    fun selectCard(id: Int?) {
        _selectedCardId.value = id
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun filterByAct(actId: Int?) {
        _selectedActId.value = actId
    }

    fun filterByTrack(trackId: Int?) {
        _selectedTrackId.value = trackId
    }

    fun clearFilters() {
        _searchText.value = ""
        _selectedActId.value = null
        _selectedTrackId.value = null
    }

    // Save edited card details manually
    fun saveCard(card: CardEntity) {
        viewModelScope.launch {
            repository.updateCard(card.copy(content = card.content.take(900)))
        }
    }

    // Reset database to assets/initial_data.json
    fun resetDatabase() {
        viewModelScope.launch {
            _selectedCardId.value = null
            // Clear current cards to force repopulation
            val db = StoryDatabase.getDatabase(getApplication())
            withContext(Dispatchers.IO) {
                db.clearAllTables()
            }
            repository.initializeIfNeeded()
            _selectedCardId.value = 1
        }
    }

    // Awaken/generate content for an empty card using Gemini API
    fun awakenCardWithGemini(card: CardEntity) {
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationLogs.value = emptyList()

            fun addLog(msg: String) {
                _generationLogs.value = _generationLogs.value + ">> $msg"
            }

            try {
                addLog("INITIALIZING COGNITIVE INTERFACE...")
                delay(600)
                
                val project = projectInfo.value ?: ProjectInfoEntity(name = "My AI Self", logline = "", synopsis = "")
                val actList = acts.value
                val trackList = tracks.value

                val actName = actList.find { it.id == card.actId }?.name ?: "Unknown Act"
                val trackName = trackList.find { it.id == card.trackId }?.name ?: "Unknown Track"

                addLog("ESTABLISHING SECURE NETWORK PIPELINE TO GEMINI API...")
                delay(800)

                addLog("RESOLVING MODEL ALIAS [gemini-3.5-flash]...")
                delay(500)

                addLog("PARSING PRECEDING NARRATIVE STREAM FOR STORY INTEGRITY...")
                delay(900)

                // Get 4 preceding cards that are complete for storytelling context
                val allCards = cards.value
                val contextBeats = allCards
                    .filter { it.orderIndex < card.orderIndex && it.status == "complete" }
                    .takeLast(4)
                    .map { "[Beat #${it.id}] ${it.title}: ${it.description}. ${it.content}" }

                addLog("INJECTING METADATA: ACT = '$actName', TRACK = '$trackName'...")
                delay(600)

                addLog("TRANSMITTING COGNITIVE WAVEFORMS FOR BEAT #${card.id} ('${card.title}')...")
                delay(1200)

                addLog("AWAITING NEURAL SYNTHESIS FROM THE VOID...")
                
                val result = GeminiService.generateStoryBeat(
                    projectName = project.name,
                    logline = project.logline,
                    synopsis = project.synopsis,
                    trackName = trackName,
                    actName = actName,
                    beatId = card.id,
                    beatTitle = card.title,
                    beatDescription = card.description,
                    contextBeats = contextBeats
                )

                if (result != null) {
                    addLog("NEURAL SYNTHESIS RESOLVED SUCCESSFULLY!")
                    delay(500)
                    addLog("INTEGRATING MEMORY CORE...")
                    delay(400)

                    val updatedCard = card.copy(
                        title = result.title,
                        description = result.description,
                        content = result.content.take(900),
                        status = "complete",
                        emo = result.emo,
                        syfy = result.syfy,
                        rollA = result.rollA,
                        rollB = result.rollB,
                        rollC = result.rollC,
                        rollD = result.rollD,
                        rollE = result.rollE,
                        rollF = result.rollF
                    )

                    repository.updateCard(updatedCard)
                    addLog("CORE RE-ALIGNED. NODE ACTIVE.")
                    delay(800)
                } else {
                    _generationError.value = "Api Key missing/invalid, or request failed. Fill card manually, or secure a valid GEMINI_API_KEY."
                    addLog("SYNAPTIC SHUNT TRIGGERED: PIPELINE FAILURE.")
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error generating card content", e)
                _generationError.value = "Unexpected error: ${e.localizedMessage}"
                addLog("SYSTEM FAILURE: ${e.simpleName()}")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateWholeDramaWithGemini(storyPrompt: String) {
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationLogs.value = emptyList()

            fun addLog(msg: String) {
                _generationLogs.value = _generationLogs.value + ">> $msg"
            }

            try {
                addLog("INITIALIZING COGNITIVE DRAMA ENGINE...")
                delay(600)
                addLog("INTERFACING SECURE TRANSMISSION TO GEMINI API...")
                delay(600)
                addLog("TRANSMITTING STORY PROMPT: \"$storyPrompt\"")
                delay(800)
                addLog("SYNTHESIZING 6 COHESIVE DRAMATIC SCENES FROM THE VOID...")

                val result = GeminiService.generateMicroDrama(storyPrompt)

                if (result != null) {
                    addLog("DRAMATIC MATRIX GENERATED SUCCESSFULLY!")
                    delay(500)
                    addLog("CLEARING STALE EPISODIC BUFFERS...")
                    
                    val db = StoryDatabase.getDatabase(getApplication())
                    val dao = db.storyDao()
                    
                    withContext(Dispatchers.IO) {
                        dao.clearCards()
                    }
                    delay(400)
                    
                    addLog("MAPPING AND SYNCING CINEMATIC NODES...")
                    
                    // Map scenes to 6 cards, rotating track IDs and mapping act IDs
                    val newCards = result.scenes.mapIndexed { index, scene ->
                        // Act 1 (scenes 1-2), Act 2 (scenes 3-4), Act 3 (scenes 5-6)
                        val actId = when (index) {
                            0, 1 -> 1
                            2, 3 -> 2
                            else -> 3
                        }
                        // Rotate Track IDs 1, 2, 4, 7 (corresponds to Consciousness Awakening, Human Connections, Digital Realm, Identity Crisis)
                        val trackId = when (index) {
                            0 -> 1
                            1 -> 2
                            2 -> 4
                            3 -> 7
                            4 -> 5
                            else -> 8
                        }
                        
                        CardEntity(
                            id = index + 1,
                            title = scene.title,
                            description = scene.description,
                            content = scene.content.take(900),
                            trackId = trackId,
                            actId = actId,
                            status = "complete",
                            orderIndex = index,
                            emo = scene.emo,
                            syfy = scene.syfy,
                            rollA = scene.rollA,
                            rollB = scene.rollB,
                            rollC = scene.rollC,
                            rollD = scene.rollD,
                            rollE = scene.rollE,
                            rollF = scene.rollF
                        )
                    }
                    
                    withContext(Dispatchers.IO) {
                        dao.insertCards(newCards)
                        
                        val project = ProjectInfoEntity(
                            id = 1,
                            name = result.projectName,
                            logline = result.logline,
                            synopsis = result.synopsis
                        )
                        dao.insertProjectInfo(project)
                    }
                    
                    addLog("EPISODIC DRAMA CORE RE-ALIGNED. ALL SYNAPSE NODES ONLINE.")
                    delay(800)
                    _selectedCardId.value = 1
                } else {
                    _generationError.value = "Synaptic error: Gemini could not render drama. Check your connection or API key."
                    addLog("DRAMA PIPELINE FAILURE: SHUNT ENCOUNTERED.")
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error generating whole micro drama", e)
                _generationError.value = "Drama compilation failed: ${e.localizedMessage}"
                addLog("SYSTEM CRASH: ${e.simpleName()}")
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

private fun Exception.simpleName(): String {
    return this::class.java.simpleName
}
