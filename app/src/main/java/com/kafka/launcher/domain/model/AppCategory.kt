package com.kafka.launcher.domain.model

enum class AppCategory(val priority: Int) {
    COMMUNICATION(0),
    WORK(1),
    MEDIA(2),
    TRAVEL(3),
    GAMES(4),
    TOOLS(5),
    OTHER(6)
}
