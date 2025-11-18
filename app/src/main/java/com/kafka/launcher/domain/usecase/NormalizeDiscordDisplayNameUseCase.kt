package com.kafka.launcher.domain.usecase

import java.text.Normalizer
import java.util.Locale

class NormalizeDiscordDisplayNameUseCase {
    private val emojiRegex = Regex("[\\p{So}\\p{Sk}]")
    private val decorationRegex = Regex("[\\[\\]\\(\\)<>:;]")

    operator fun invoke(input: String): String {
        if (input.isEmpty()) return ""
        val lower = Normalizer.normalize(input.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFKC)
        val withoutEmoji = emojiRegex.replace(lower, "")
        return decorationRegex.replace(withoutEmoji, "")
    }
}
