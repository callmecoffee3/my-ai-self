package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject

class StoryRepository(private val context: Context) {
    private val db = StoryDatabase.getDatabase(context)
    private val dao = db.storyDao()

    val projectInfo: Flow<ProjectInfoEntity?> = dao.getProjectInfo()
    val tracks: Flow<List<TrackEntity>> = dao.getTracks()
    val acts: Flow<List<ActEntity>> = dao.getActs()
    val cards: Flow<List<CardEntity>> = dao.getCards()

    fun getCardById(id: Int): Flow<CardEntity?> = dao.getCardById(id)

    suspend fun updateCard(card: CardEntity) = withContext(Dispatchers.IO) {
        dao.updateCard(card)
    }

    suspend fun insertCard(card: CardEntity) = withContext(Dispatchers.IO) {
        dao.insertCard(card)
    }

    suspend fun getProjectInfoSync(): ProjectInfoEntity? = withContext(Dispatchers.IO) {
        dao.getProjectInfoSync()
    }

    suspend fun getCardByIdSync(id: Int): CardEntity? = withContext(Dispatchers.IO) {
        dao.getCardByIdSync(id)
    }

    /**
     * Initializes the database with data from assets/initial_data.json if empty.
     */
    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val existingInfo = dao.getProjectInfoSync()
            if (existingInfo != null) {
                Log.d("StoryRepository", "Database already initialized.")
                return@withContext
            }

            Log.d("StoryRepository", "Pre-populating database from assets...")
            val jsonString = context.assets.open("initial_data.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            // 1. Parse Project Info
            val projectName = root.optString("projectName", "My AI Self")
            val logline = root.optString("logline", "")
            val synopsis = root.optString("synopsis", "")
            val info = ProjectInfoEntity(name = projectName, logline = logline, synopsis = synopsis)
            dao.insertProjectInfo(info)

            // 2. Parse Tracks
            val tracksArray = root.optJSONArray("tracks")
            val tracksList = mutableListOf<TrackEntity>()
            if (tracksArray != null) {
                for (i in 0 until tracksArray.length()) {
                    val trackObj = tracksArray.getJSONObject(i)
                    tracksList.add(
                        TrackEntity(
                            id = trackObj.getInt("id"),
                            name = trackObj.getString("name"),
                            color = trackObj.getString("color")
                        )
                    )
                }
            }
            dao.insertTracks(tracksList)

            // 3. Parse Acts
            val actsArray = root.optJSONArray("acts")
            val actsList = mutableListOf<ActEntity>()
            if (actsArray != null) {
                for (i in 0 until actsArray.length()) {
                    val actObj = actsArray.getJSONObject(i)
                    actsList.add(
                        ActEntity(
                            id = actObj.getInt("id"),
                            name = actObj.getString("name"),
                            color = actObj.getString("color")
                        )
                    )
                }
            }
            dao.insertActs(actsList)

            // 4. Parse Cards
            val cardsArray = root.optJSONArray("cards")
            val cardsList = mutableListOf<CardEntity>()
            if (cardsArray != null) {
                for (i in 0 until cardsArray.length()) {
                    val cardObj = cardsArray.getJSONObject(i)
                    val id = cardObj.getInt("id")
                    val title = cardObj.getString("title")
                    val description = cardObj.getString("description")
                    val content = cardObj.optString("content", "")
                    val trackId = cardObj.getInt("track")
                    val actId = cardObj.getInt("act")
                    val status = cardObj.getString("status")
                    val orderIndex = cardObj.getInt("order")

                    val emo = cardObj.optString("emo", "")
                    val syfy = cardObj.optString("syfy", "")

                    // Parse rolls if any
                    var rA = ""
                    var rB = ""
                    var rC = ""
                    var rD = ""
                    var rE = ""
                    var rF = ""
                    val rollsObj = cardObj.optJSONObject("rolls")
                    if (rollsObj != null) {
                        rA = rollsObj.optString("A", "")
                        rB = rollsObj.optString("B", "")
                        rC = rollsObj.optString("C", "")
                        rD = rollsObj.optString("D", "")
                        rE = rollsObj.optString("E", "")
                        rF = rollsObj.optString("F", "")
                    }

                    cardsList.add(
                        CardEntity(
                            id = id,
                            title = title,
                            description = description,
                            content = content.take(900),
                            trackId = trackId,
                            actId = actId,
                            status = status,
                            orderIndex = orderIndex,
                            emo = emo,
                            syfy = syfy,
                            rollA = rA,
                            rollB = rB,
                            rollC = rC,
                            rollD = rD,
                            rollE = rE,
                            rollF = rF
                        )
                    )
                }
            }
            dao.insertCards(cardsList)
            Log.d("StoryRepository", "Database pre-populated with ${cardsList.size} cards.")
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error prepopulating database", e)
        }
    }
}
