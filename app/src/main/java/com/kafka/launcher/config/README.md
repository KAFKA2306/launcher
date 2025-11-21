# config

- `LauncherConfig` がファイル名やUI制限など全体設定を一元管理する。
- `GeminiConfig` が WorkManager 周期・レスポンススキーマ・DataStore/EncryptedSharedPreferences パスといった Gemini 連携の共通値を保持する。
- `DiscordConfig` が WebView/通知/ランキング重み/クイックアクション ID など Discord 機能専用の設定を集中管理する。
- `AppSearchConfig` がアプリ検索のエイリアス（SNS・国内金融アプリ含む）を保持し、`AppRepository` から参照される。
- 他レイヤーはここから定数を取得し、ハードコーディングを禁止するポリシーを支える。
