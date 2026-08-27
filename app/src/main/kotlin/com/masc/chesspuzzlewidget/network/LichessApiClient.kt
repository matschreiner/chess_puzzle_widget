package com.masc.chesspuzzlewidget.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class LichessApiException(val code: Int, body: String? = null) :
    Exception("Lichess API request failed with status $code: ${body?.take(1500)}")

class LichessApiClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getNextPuzzle(
        accessToken: String,
        angle: String? = null,
        difficulty: String? = null
    ): PuzzleAndGameDto = withContext(Dispatchers.IO) {
        val url = "https://lichess.org/api/puzzle/next".toHttpUrl().newBuilder()
            .apply {
                if (angle != null) addQueryParameter("angle", angle)
                if (difficulty != null) addQueryParameter("difficulty", difficulty)
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw LichessApiException(response.code, body)
            }
            try {
                json.decodeFromString(PuzzleAndGameDto.serializer(), body)
            } catch (e: Exception) {
                throw LichessApiException(response.code, body)
            }
        }
    }

    /**
     * Tells Lichess a puzzle was solved (or given up on), so its personalized queue advances past
     * it — without this, `/api/puzzle/next` can keep re-serving the same "unseen" puzzle. Uses
     * `nb=0` since we don't need a new puzzle back from this call (the app fetches those separately).
     */
    suspend fun confirmSolved(accessToken: String, angle: String, puzzleId: String, win: Boolean) =
        withContext(Dispatchers.IO) {
            val url = "https://lichess.org/api/puzzle/batch/$angle".toHttpUrl().newBuilder()
                .addQueryParameter("nb", "0")
                .build()
            val bodyJson = json.encodeToString(
                SolveRequestDto.serializer(),
                SolveRequestDto(solutions = listOf(SolutionEntryDto(id = puzzleId, win = win)))
            )
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw LichessApiException(response.code, response.body?.string())
                }
            }
        }
}
