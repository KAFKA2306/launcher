package com.kafka.launcher.domain.model

import org.json.JSONArray
import org.json.JSONObject

object GeminiRecommendationJson {
    fun encode(data: GeminiRecommendations): String {
        val root = JSONObject()
        root.put("generatedAt", data.generatedAt)
        root.put("windows", JSONArray().apply { data.windows.forEach { put(windowObject(it)) } })
        root.put("globalPins", JSONArray(data.globalPins))
        root.put("suppressions", JSONArray(data.suppressions))
        root.put("rationales", JSONArray().apply { data.rationales.forEach { put(rationaleObject(it)) } })
        return root.toString()
    }

    fun decode(json: String): GeminiRecommendations {
        val root = JSONObject(json)
        val windows = root.optJSONArray("windows")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(windowFromJson(array.getJSONObject(index)))
                }
            }
        } ?: emptyList()
        val pins = root.optJSONArray("globalPins")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        } ?: emptyList()
        val suppressions = root.optJSONArray("suppressions")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        } ?: emptyList()
        val rationales = root.optJSONArray("rationales")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(rationaleFromJson(array.getJSONObject(index)))
                }
            }
        } ?: emptyList()
        return GeminiRecommendations(
            generatedAt = root.optString("generatedAt"),
            windows = windows,
            globalPins = pins,
            suppressions = suppressions,
            rationales = rationales
        )
    }

    private fun windowObject(window: GeminiRecommendationWindow): JSONObject {
        val node = JSONObject()
        node.put("id", window.id)
        window.start?.let { node.put("start", it) }
        window.end?.let { node.put("end", it) }
        node.put("primaryActionIds", JSONArray(window.primaryActionIds))
        node.put("fallbackActionIds", JSONArray(window.fallbackActionIds))
        return node
    }

    private fun rationaleObject(rationale: GeminiRecommendationRationale): JSONObject {
        val node = JSONObject()
        node.put("targetId", rationale.targetId)
        node.put("summary", rationale.summary)
        return node
    }

    private fun windowFromJson(node: JSONObject): GeminiRecommendationWindow {
        val primary = node.optJSONArray("primaryActionIds")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        } ?: emptyList()
        val fallback = node.optJSONArray("fallbackActionIds")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        } ?: emptyList()
        return GeminiRecommendationWindow(
            id = node.optString("id"),
            start = node.optString("start", null),
            end = node.optString("end", null),
            primaryActionIds = primary,
            fallbackActionIds = fallback
        )
    }

    private fun rationaleFromJson(node: JSONObject): GeminiRecommendationRationale {
        return GeminiRecommendationRationale(
            targetId = node.optString("targetId"),
            summary = node.optString("summary")
        )
    }
}
