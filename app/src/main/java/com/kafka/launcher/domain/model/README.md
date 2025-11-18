# domain/model

- UI や Worker が利用するシリアライズしやすい Kotlin データクラス群をまとめる。
- `GeminiRecommendations` など AI 連携用の構造体もここに置き、JSON 変換は `GeminiRecommendationJson` へ委譲する。
- Discord WebView の `DiscordChannel` や `ChannelUsageStats` などもここで定義し、URL 正規化キーやランキング重みの基礎データを一元管理する。
