package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "project_info")
data class ProjectInfoEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val logline: String,
    val synopsis: String
)

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val color: String
)

@Entity(tableName = "acts")
data class ActEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val color: String
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val content: String,
    val trackId: Int,
    val actId: Int,
    val status: String, // "complete" or "empty"
    val orderIndex: Int,
    val emo: String = "",
    val syfy: String = "",
    val rollA: String = "",
    val rollB: String = "",
    val rollC: String = "",
    val rollD: String = "",
    val rollE: String = "",
    val rollF: String = ""
)

@Dao
interface StoryDao {
    @Query("SELECT * FROM project_info LIMIT 1")
    fun getProjectInfo(): Flow<ProjectInfoEntity?>

    @Query("SELECT * FROM project_info LIMIT 1")
    suspend fun getProjectInfoSync(): ProjectInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectInfo(projectInfo: ProjectInfoEntity)

    @Query("SELECT * FROM tracks")
    fun getTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getTracksSync(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM acts")
    fun getActs(): Flow<List<ActEntity>>

    @Query("SELECT * FROM acts")
    suspend fun getActsSync(): List<ActEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActs(acts: List<ActEntity>)

    @Query("SELECT * FROM cards ORDER BY orderIndex ASC")
    fun getCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY orderIndex ASC")
    suspend fun getCardsSync(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getCardById(id: Int): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardByIdSync(id: Int): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("DELETE FROM cards")
    suspend fun clearCards()
}
