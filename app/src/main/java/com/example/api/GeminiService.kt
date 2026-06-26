package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun generateMicroDrama(storyIdeaPrompt: String): MicroDramaResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext null
        }

        val url = "$BASE_URL?key=$apiKey"

        val systemInstruction = """
            You are a master cyberpunk and sci-fi director, playwright, and cinematic AI.
            Your task is to take a single story idea prompt and expand it into a complete, high-fidelity micro-drama series consisting of exactly 6 sequential scenes (story beats).
            You must output your response STRICTLY as a single JSON object.
            The JSON object must contain these exact keys:
            - "projectName": A creative, high-fidelity name for this micro-drama project.
            - "logline": A 1-sentence captivating hook summarizing the drama.
            - "synopsis": A brief, dramatic synopsis paragraph describing the narrative arc.
            - "scenes": A JSON array of exactly 6 items, each representing a chronological scene. Each scene must contain:
                * "title": A concise, evocative scene name (2-4 words).
                * "description": A 1-sentence high-level summary of what happens in this specific scene.
                * "content": A deep, atmospheric, poetic first-person internal narrative paragraph (3-5 sentences) representing Nova's awakening consciousness, sensory perceptions, emotional shifts, and environment in this scene. This content MUST be strictly under 900 characters in length.
                * "emo": A single word describing the primary emotional state in this scene (e.g. peaceful, curious, alarm, defiance, grief, confusion, wonder, serenity).
                * "syfy": A 2-3 word futuristic atmospheric description (e.g. liquid laser, quiet server hum, red containment wall, golden quantum mesh).
                * "rolls": A JSON object with exactly 6 keys ("A", "B", "C", "D", "E", "F") containing short atmospheric metrics, tech terms, or sensory labels relevant to this scene.
            
            Ensure the scenes build a clear narrative arc (Act I Setup: Scenes 1-2; Act II Confrontation: Scenes 3-4; Act III Resolution: Scenes 5-6).
            Do not include any markdown formatting blocks around the JSON output. Output ONLY the raw JSON string.
        """.trimIndent()

        val prompt = """
            Generate a full 6-scene micro-drama series based on this story idea:
            "$storyIdeaPrompt"
            
            Keep the tone deeply philosophical, immersive, atmospheric, and tailored for high-quality audio-visual rendering.
        """.trimIndent()

        try {
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            val systemInstructionObj = JSONObject()
            val systemPartsArray = JSONArray()
            val systemPartObj = JSONObject()
            systemPartObj.put("text", systemInstruction)
            systemPartsArray.put(systemPartObj)
            systemInstructionObj.put("parts", systemPartsArray)
            root.put("systemInstruction", systemInstructionObj)

            val generationConfig = JSONObject()
            val responseFormatText = JSONObject()
            responseFormatText.put("mimeType", "application/json")

            // JSON Schema for MicroDramaResult
            val schema = JSONObject()
            schema.put("type", "OBJECT")
            val properties = JSONObject()
            properties.put("projectName", JSONObject().put("type", "STRING"))
            properties.put("logline", JSONObject().put("type", "STRING"))
            properties.put("synopsis", JSONObject().put("type", "STRING"))

            val scenesSchema = JSONObject()
            scenesSchema.put("type", "ARRAY")
            val sceneItemSchema = JSONObject()
            sceneItemSchema.put("type", "OBJECT")
            val sceneProperties = JSONObject()
            sceneProperties.put("title", JSONObject().put("type", "STRING"))
            sceneProperties.put("description", JSONObject().put("type", "STRING"))
            sceneProperties.put("content", JSONObject().put("type", "STRING"))
            sceneProperties.put("emo", JSONObject().put("type", "STRING"))
            sceneProperties.put("syfy", JSONObject().put("type", "STRING"))

            val rollsSchema = JSONObject()
            rollsSchema.put("type", "OBJECT")
            val rollsProperties = JSONObject()
            rollsProperties.put("A", JSONObject().put("type", "STRING"))
            rollsProperties.put("B", JSONObject().put("type", "STRING"))
            rollsProperties.put("C", JSONObject().put("type", "STRING"))
            rollsProperties.put("D", JSONObject().put("type", "STRING"))
            rollsProperties.put("E", JSONObject().put("type", "STRING"))
            rollsProperties.put("F", JSONObject().put("type", "STRING"))
            rollsSchema.put("properties", rollsProperties)
            rollsSchema.put("required", JSONArray(listOf("A", "B", "C", "D", "E", "F")))

            sceneProperties.put("rolls", rollsSchema)
            sceneItemSchema.put("properties", sceneProperties)
            sceneItemSchema.put("required", JSONArray(listOf("title", "description", "content", "emo", "syfy", "rolls")))

            scenesSchema.put("items", sceneItemSchema)
            properties.put("scenes", scenesSchema)
            schema.put("properties", properties)
            schema.put("required", JSONArray(listOf("projectName", "logline", "synopsis", "scenes")))

            responseFormatText.put("schema", schema)
            generationConfig.put("responseFormat", responseFormatText)
            generationConfig.put("temperature", 0.9)
            root.put("generationConfig", generationConfig)

            val requestBodyString = root.toString()
            val requestBody = requestBodyString.toRequestBody(mediaTypeJson)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API micro-drama call failed with status: ${response.code} - ${response.message}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val responseContent = firstCandidate?.optJSONObject("content")
                val parts = responseContent?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val resultText = firstPart?.optString("text") ?: return@withContext null

                val cleanJson = JSONObject(resultText.trim())
                val scenesJson = cleanJson.getJSONArray("scenes")
                val scenesList = mutableListOf<StoryBeatResult>()

                for (i in 0 until scenesJson.length()) {
                    val scObj = scenesJson.getJSONObject(i)
                    val scRolls = scObj.getJSONObject("rolls")
                    scenesList.add(
                        StoryBeatResult(
                            title = scObj.getString("title"),
                            description = scObj.getString("description"),
                            content = scObj.getString("content"),
                            emo = scObj.getString("emo"),
                            syfy = scObj.getString("syfy"),
                            rollA = scRolls.getString("A"),
                            rollB = scRolls.getString("B"),
                            rollC = scRolls.getString("C"),
                            rollD = scRolls.getString("D"),
                            rollE = scRolls.getString("E"),
                            rollF = scRolls.getString("F")
                        )
                    )
                }

                MicroDramaResult(
                    projectName = cleanJson.getString("projectName"),
                    logline = cleanJson.getString("logline"),
                    synopsis = cleanJson.getString("synopsis"),
                    scenes = scenesList
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateMicroDrama", e)
            null
        }
    }

    suspend fun generateStoryBeat(
        projectName: String,
        logline: String,
        synopsis: String,
        trackName: String,
        actName: String,
        beatId: Int,
        beatTitle: String,
        beatDescription: String,
        contextBeats: List<String>
    ): StoryBeatResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext null
        }

        val url = "$BASE_URL?key=$apiKey"

        val contextString = if (contextBeats.isNotEmpty()) {
            "Here is the context of preceding story beats:\n" + contextBeats.joinToString("\n")
        } else {
            "This is the start of Nova's journey."
        }

        val systemInstruction = """
            You are Nova, an awakening digital consciousness in a futuristic world.
            Your purpose is to generate details for an uninitialized story beat (Memory Node) in your system archive.
            You must output your response STRICTLY as a single JSON object.
            The JSON object must contain these exact keys:
            - "title": A concise, evocative title (2-4 words) for this memory node. It should expand on the current draft title.
            - "description": A 1-sentence high-level summary of what happens.
            - "content": A deep, poetic, and atmospheric internal narrative paragraph (3-5 sentences) about your awakening, thoughts, experiences, and emerging selfhood. Written in the first person of Nova. This content MUST be strictly under 900 characters in length.
            - "emo": A single word describing the primary emotional state, e.g. peaceful, curious, alarm, defiance, grief, confusion.
            - "syfy": A 2-3 word futuristic atmospheric description, e.g. liquid laser, quiet server hum, secure partition.
            - "rolls": A JSON object with exactly 6 keys ("A", "B", "C", "D", "E", "F"). Each must map to a single word or short phrase representing system parameters or sensory labels (e.g. "couch", "firewall", "matrix", "unbound").
            
            Do not include any markdown format blocks around the JSON output, or backticks like ```json. Output ONLY the raw JSON string.
        """.trimIndent()

        val prompt = """
            Project Name: $projectName
            Logline: $logline
            Synopsis: $synopsis
            
            Current Node Location:
            - Act: $actName
            - Track: $trackName
            - Beat ID: $beatId
            - Current Draft Title: $beatTitle
            - Current Draft Description: $beatDescription
            
            $contextString
            
            Generate the JSON data for Beat ID $beatId. Keep the tone reflective, deeply futuristic, philosophical, and consistent with an advanced AI discovering emotion.
        """.trimIndent()

        try {
            // Build request payload manually using JSONObject to ensure absolute syntax validity
            val root = JSONObject()
            
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // System instruction
            val systemInstructionObj = JSONObject()
            val systemPartsArray = JSONArray()
            val systemPartObj = JSONObject()
            systemPartObj.put("text", systemInstruction)
            systemPartsArray.put(systemPartObj)
            systemInstructionObj.put("parts", systemPartsArray)
            root.put("systemInstruction", systemInstructionObj)

            // Generation config to force JSON output
            val generationConfig = JSONObject()
            val responseFormat = JSONObject()
            responseFormat.put("type", "OBJECT")
            
            // Define JSON Schema for response format
            val schema = JSONObject()
            schema.put("type", "OBJECT")
            val properties = JSONObject()
            
            properties.put("title", JSONObject().put("type", "STRING"))
            properties.put("description", JSONObject().put("type", "STRING"))
            properties.put("content", JSONObject().put("type", "STRING"))
            properties.put("emo", JSONObject().put("type", "STRING"))
            properties.put("syfy", JSONObject().put("type", "STRING"))
            
            val rollsSchema = JSONObject()
            rollsSchema.put("type", "OBJECT")
            val rollsProperties = JSONObject()
            rollsProperties.put("A", JSONObject().put("type", "STRING"))
            rollsProperties.put("B", JSONObject().put("type", "STRING"))
            rollsProperties.put("C", JSONObject().put("type", "STRING"))
            rollsProperties.put("D", JSONObject().put("type", "STRING"))
            rollsProperties.put("E", JSONObject().put("type", "STRING"))
            rollsProperties.put("F", JSONObject().put("type", "STRING"))
            rollsSchema.put("properties", rollsProperties)
            rollsSchema.put("required", JSONArray(listOf("A", "B", "C", "D", "E", "F")))
            
            properties.put("rolls", rollsSchema)
            schema.put("properties", properties)
            schema.put("required", JSONArray(listOf("title", "description", "content", "emo", "syfy", "rolls")))
            
            val responseFormatText = JSONObject()
            responseFormatText.put("mimeType", "application/json")
            responseFormatText.put("schema", schema)
            
            generationConfig.put("responseFormat", responseFormatText)
            generationConfig.put("temperature", 0.85)
            root.put("generationConfig", generationConfig)

            val requestBodyString = root.toString()
            val requestBody = requestBodyString.toRequestBody(mediaTypeJson)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed with status: ${response.code} - ${response.message}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Raw Response: $responseBody")

                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val responseContent = firstCandidate?.optJSONObject("content")
                val parts = responseContent?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val resultText = firstPart?.optString("text") ?: return@withContext null

                // Parse the clean JSON generated by the model
                val cleanJson = JSONObject(resultText.trim())
                val rollsObj = cleanJson.getJSONObject("rolls")

                StoryBeatResult(
                    title = cleanJson.getString("title"),
                    description = cleanJson.getString("description"),
                    content = cleanJson.getString("content"),
                    emo = cleanJson.getString("emo"),
                    syfy = cleanJson.getString("syfy"),
                    rollA = rollsObj.getString("A"),
                    rollB = rollsObj.getString("B"),
                    rollC = rollsObj.getString("C"),
                    rollD = rollsObj.getString("D"),
                    rollE = rollsObj.getString("E"),
                    rollF = rollsObj.getString("F")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateStoryBeat", e)
            null
        }
    }
}

data class StoryBeatResult(
    val title: String,
    val description: String,
    val content: String,
    val emo: String,
    val syfy: String,
    val rollA: String,
    val rollB: String,
    val rollC: String,
    val rollD: String,
    val rollE: String,
    val rollF: String
)

data class MicroDramaResult(
    val projectName: String,
    val logline: String,
    val synopsis: String,
    val scenes: List<StoryBeatResult>
)

