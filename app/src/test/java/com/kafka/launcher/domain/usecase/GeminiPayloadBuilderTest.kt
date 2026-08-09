package com.kafka.launcher.domain.usecase

import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPayloadBuilderTest {
    @Test
    fun includesRecommendationOutcomeFeedbackWithoutSecrets() {
        val payload = GeminiPayloadBuilder().build(
            events = listOf(ActionLog(actionId = "qa.search", timestamp = 1_700_000_000_000)),
            stats = listOf(ActionStats(actionId = "qa.search", count = 4)),
            feedback = listOf(
                GeminiFeedbackSignal(
                    id = "qa.search",
                    usageCount = 4,
                    acceptedCount = 2,
                    dismissedCount = 1
                )
            )
        )

        assertTrue(payload.contains("\"recommendationFeedback\""))
        assertTrue(payload.contains("\"id\":\"qa.search\""))
        assertTrue(payload.contains("\"usageCount\":4"))
        assertTrue(payload.contains("\"acceptedCount\":2"))
        assertTrue(payload.contains("\"dismissedCount\":1"))
        assertTrue(!payload.contains("apiKey", ignoreCase = true))
    }

    @Test
    fun escapesFeedbackIdsAsJsonStrings() {
        val payload = GeminiPayloadBuilder().build(
            events = emptyList(),
            stats = emptyList(),
            feedback = listOf(GeminiFeedbackSignal("qa.\"quoted", 0, 0, 1))
        )

        assertTrue(payload.contains("qa.\\\"quoted"))
    }
}
