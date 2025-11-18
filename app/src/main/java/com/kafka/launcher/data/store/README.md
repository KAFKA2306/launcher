# data/store

- JSON ファイルを直接読み書きする専用ストアを配置し、Gemini 推薦など複合データを 1 ファイルで管理する。
- `GeminiRecommendationStore` は Flow でスナップショットを返し、`LauncherViewModel` が直接購読する唯一のファイルストアとなる。
- `GeminiApiKeyStore` は EncryptedSharedPreferences を使用して API キーを保存し、`LauncherViewModel` と `GeminiSyncWorker` から共有される。
- `DiscordPreferencesStore` は Discord WebView 向けのミュート名・自分の表示名・ランキング重み・ガイド表示状態を JSON ファイルに集約し、Flow で更新を配信する。
- `DiscordChannelStore` が DiscordChannel 一覧を JSON 1 ファイルで保持し、ユーザーが編集したサーバー名やタグを含めて管理する。
- `DiscordUsageStore` が ChannelUsageStats を永続化し、WebView と通知処理が同一データを共有できるようにする。
- `DiscordNotificationStore` が 通知ログ 100 件分を時系列で保存し、詳細表示や削除を実装しやすくする。
- `DiscordNotificationPatternStore` が NotificationPatternRule 群を JSON で管理し、通知ミラーからの保存・削除操作を即時反映させる。
