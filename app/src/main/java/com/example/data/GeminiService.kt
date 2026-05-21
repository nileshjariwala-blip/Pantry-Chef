package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini API Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

// --- Domain Models for JSON Parsing ---

@JsonClass(generateAdapter = true)
data class IdentifiedPantryItems(
    val items: List<ScannedItem>
)

@JsonClass(generateAdapter = true)
data class ScannedItem(
    val name: String,
    val quantity: String = "",
    val category: String = "Fridge"
)

@JsonClass(generateAdapter = true)
data class SuggestedRecipeData(
    val title: String,
    val ingredients: List<String>,
    val instructions: List<String>
)

@JsonClass(generateAdapter = true)
data class GroceryListMissingItems(
    val missingItems: List<ScannedItem>
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // Convert bitmap to Base64 (helper)
    fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun getApiKey(): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            ""
        } else {
            apiKey
        }
    }

    /**
     * Identifies items inside pantry or fridge from user-provided photograph.
     */
    suspend fun identifyPantryItems(bitmap: Bitmap): List<ScannedItem> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return emptyList()

        val prompt = "Identify all fridge or pantry food items found in this picture. Return your answer as a JSON object matching this schema:\n" +
                "{\n" +
                "  \"items\": [\n" +
                "    { \"name\": \"Apple\", \"quantity\": \"5\", \"category\": \"Fridge\" },\n" +
                "    { \"name\": \"Oatmeal\", \"quantity\": \"1 box\", \"category\": \"Pantry\" }\n" +
                "  ]\n" +
                "}\n" +
                "Only return the valid JSON, categorize items into either 'Fridge' or 'Pantry'. Be accurate and extract quantities if visible."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(IdentifiedPantryItems::class.java)
                val parsed = adapter.fromJson(jsonText)
                parsed?.items ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Suggests a healthy recipe based on current pantry items and culinary experiences (dishes prepared before).
     */
    suspend fun suggestHealthyRecipe(
        pantryItems: List<PantryItem>,
        pastDishes: List<CulinaryDish>
    ): SuggestedRecipeData? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val pantryStr = pantryItems.joinToString(", ") { "${it.name} (${it.quantity})" }
        val culinaryHistoryStr = pastDishes.joinToString(", ") { it.title }

        val prompt = "Based on my available pantry/fridge ingredients: [$pantryStr]. " +
                "And keeping in mind my culinary experience dishes that I like to prepare: [$culinaryHistoryStr], " +
                "suggest a healthy recipe that utilizes as many available ingredients as possible. " +
                "Respond with a single JSON structure matching this exact JSON schema: " +
                "{\n" +
                "  \"title\": \"Recipe Name\",\n" +
                "  \"ingredients\": [\"ingredient 1\", \"ingredient 2\"],\n" +
                "  \"instructions\": [\"step 1\", \"step 2\"]\n" +
                "}\n" +
                "Ensure step-by-step instructions are clear, concise, and focused on healthy eating. Do not return any other text than the JSON."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(SuggestedRecipeData::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Finds missing ingredients comparing the top 5 culinary cooked dishes against the current pantry.
     * Generates a grocery list.
     */
    suspend fun generateMissingGroceryList(
        topFiveDishes: List<CulinaryDish>,
        pantryItems: List<PantryItem>
    ): List<ScannedItem> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return emptyList()

        val dishesStr = topFiveDishes.joinToString(", ") { it.title }
        val pantryStr = pantryItems.joinToString(", ") { it.name }

        val prompt = "Here is a list of culinary dishes I want to prepare: [$dishesStr]. " +
                "Here is my active fridge/pantry inventory: [$pantryStr]. " +
                "Infer what ingredients are required to cook these dishes, compare them to my active inventory, " +
                "and identify what ingredients are MISSING that I need to buy. " +
                "Prepare a grocery list of missing items, and format the response as a JSON object matching this schema:\n" +
                "{\n" +
                "  \"missingItems\": [\n" +
                "    { \"name\": \"Garlic\", \"quantity\": \"1 bulb\" },\n" +
                "    { \"name\": \"Spinach\", \"quantity\": \"1 bunch\" }\n" +
                "  ]\n" +
                "}\n" +
                "Only return valid JSON. Correctly estimate missing ingredients based on standard recipes for those dish titles."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = moshi.adapter(GroceryListMissingItems::class.java)
                val parsed = adapter.fromJson(jsonText)
                parsed?.missingItems ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
