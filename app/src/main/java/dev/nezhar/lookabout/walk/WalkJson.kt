package dev.nezhar.lookabout.walk

import org.json.JSONArray
import org.json.JSONObject

/** The version of the saved walk's JSON. A file with another version is not loaded. */
const val WALK_JSON_VERSION = 1

/**
 * A walk as saved to disk: the walk itself and whether the walker had moved on to Summary
 * (by Finish walk, or Skip on the last picture), so the app reopens where the walker left off.
 */
data class SavedWalk(
    val walk: Walk,
    val onSummary: Boolean,
)

/** The whole walk as JSON, with its pending photo path, check-at-end setting and whether Summary was open. */
fun encodeSavedWalk(saved: SavedWalk): String {
    val walk = saved.walk
    val shots = JSONArray()
    walk.shots.forEach { shots.put(encodeShot(it)) }
    return JSONObject()
        .put("version", WALK_JSON_VERSION)
        .put("onSummary", saved.onSummary)
        .put("topic", walk.topic)
        .put("sky", walk.sky)
        .put("startedAtMillis", walk.startedAtMillis)
        .put("currentIndex", walk.currentIndex)
        .put("pendingPhotoPath", walk.pendingPhotoPath ?: JSONObject.NULL)
        .put("checkAtEnd", walk.checkAtEnd)
        .put("shots", shots)
        .toString()
}

/**
 * The walk saved by [encodeSavedWalk], or null when [text] is not a readable saved walk: not
 * JSON, another version, a missing or wrong field, or a walk the walk rules reject.
 */
fun decodeSavedWalk(text: String): SavedWalk? =
    try {
        val json = JSONObject(text)
        if (json.getInt("version") != WALK_JSON_VERSION) {
            null
        } else {
            val shots = json.getJSONArray("shots")
            val walk = Walk(
                topic = json.getString("topic"),
                sky = json.getString("sky"),
                startedAtMillis = json.getLong("startedAtMillis"),
                shots = (0 until shots.length()).map { decodeShot(shots.getJSONObject(it)) },
                currentIndex = json.getInt("currentIndex"),
                pendingPhotoPath = json.stringOrNull("pendingPhotoPath"),
                // Missing in walks saved before the setting existed: those check each photo at once.
                checkAtEnd = if (json.has("checkAtEnd")) json.getBoolean("checkAtEnd") else false,
            )
            SavedWalk(walk, json.getBoolean("onSummary"))
        }
    } catch (e: Exception) {
        // JSONException, and IllegalArgumentException from the walk rules or an unknown outcome.
        null
    }

private fun encodeShot(shot: Shot): JSONObject =
    JSONObject()
        .put("text", shot.text)
        .put("questions", JSONArray().also { array -> shot.questions.forEach { array.put(it) } })
        .put("answers", JSONArray().also { array -> shot.answers.forEach { array.put(it) } })
        .put("corrected", JSONArray().also { array -> shot.corrected.forEach { array.put(it) } })
        .put("photoPath", shot.photoPath ?: JSONObject.NULL)
        .put("outcome", shot.outcome.name)
        .put("checkMillis", shot.checkMillis ?: JSONObject.NULL)

private fun decodeShot(json: JSONObject): Shot {
    val questions = json.getJSONArray("questions")
    val answers = json.getJSONArray("answers")
    val corrected = json.getJSONArray("corrected")
    return Shot(
        text = json.getString("text"),
        questions = (0 until questions.length()).map { questions.getString(it) },
        answers = (0 until answers.length()).map { answers.getBoolean(it) },
        corrected = (0 until corrected.length()).map { corrected.getBoolean(it) },
        photoPath = json.stringOrNull("photoPath"),
        outcome = Outcome.valueOf(json.getString("outcome")),
        checkMillis = if (json.isNull("checkMillis")) null else json.getLong("checkMillis"),
    )
}

/** The string at [key], or null when it is missing or JSON null. */
private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
