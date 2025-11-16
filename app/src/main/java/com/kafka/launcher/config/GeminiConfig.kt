package com.kafka.launcher.config

import androidx.work.NetworkType

object GeminiConfig {
    const val model = "gemini-2.5-pro-exp"
    const val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
    const val periodHours = 3L
    const val recommendationDirectory = "config"
    const val recommendationFileName = "gemini_recommendations.json"
    const val recommendationStoreKey = "gemini_recommendations_payload"
    const val workName = "GeminiSync"
    const val payloadActionLimit = 4
    const val payloadAppLimit = 4
    const val payloadSequenceLimit = 3
    const val payloadEventLimit = 200
    const val aiPreviewWindowLimit = 4
    const val aiPreviewRationaleLimit = 6
    val networkType: NetworkType = NetworkType.UNMETERED
    private val responseSchema = """{""" +
        "\"type\":\"object\",\"properties\":{\"timeWindows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"windowId\":{\"type\":\"string\"},\"primaryActionIds\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"maxItems\":4},\"fallbackActionIds\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"maxItems\":4}},\"required\":[\"windowId\",\"primaryActionIds\"]}},\"globalPins\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"maxItems\":6},\"suppressions\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"maxItems\":6},\"rationales\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"targetId\":{\"type\":\"string\"},\"summary\":{\"type\":\"string\"}},\"required\":[\"targetId\",\"summary\"]}}},\"required\":[\"timeWindows\"]}""".trimIndent()
    val generationConfig = """{""" +
        "\"temperature\":0.3,\"topP\":0.95,\"responseMimeType\":\"application/json\",\"responseSchema\":$responseSchema}""".trimIndent()
    val timeWindows = listOf(
        TimeWindowDefinition(id = "weekday_morning", startHour = 5, startMinute = 0, endHour = 10, endMinute = 0, appliesToWeekdays = true, appliesToWeekends = false),
        TimeWindowDefinition(id = "weekday_daytime", startHour = 10, startMinute = 0, endHour = 18, endMinute = 0, appliesToWeekdays = true, appliesToWeekends = false),
        TimeWindowDefinition(id = "weekday_night", startHour = 18, startMinute = 0, endHour = 5, endMinute = 0, appliesToWeekdays = true, appliesToWeekends = false),
        TimeWindowDefinition(id = "weekend_daytime", startHour = 8, startMinute = 0, endHour = 20, endMinute = 0, appliesToWeekdays = false, appliesToWeekends = true),
        TimeWindowDefinition(id = "weekend_night", startHour = 20, startMinute = 0, endHour = 8, endMinute = 0, appliesToWeekdays = false, appliesToWeekends = true)
    )

    data class TimeWindowDefinition(
        val id: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val appliesToWeekdays: Boolean,
        val appliesToWeekends: Boolean
    )
}
