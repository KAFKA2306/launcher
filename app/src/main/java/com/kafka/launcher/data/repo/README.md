# data/repo

- UI とドメインが利用するリポジトリ群。Room・DataStore・ログファイル・QuickAction Provider を統合して隠蔽する。
- `ActionLogRepository` はログ追記とファイル書き込み、`QuickActionRepository` はアプリ検出と監視、`SettingsRepository`/`PinnedAppsRepository` は DataStore の CRUD を担当する。
- `DiscordRepository` は Discord WebView 用のミュート名・自分の表示名・ランキング重みに加えて、チャンネル一覧・ChannelUsageStats・通知ログ・パターンルールを JSON ストアから読み書きする。
