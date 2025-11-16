# config

- `LauncherConfig` がファイル名やUI制限など全体設定を一元管理する。
- `GeminiConfig` が WorkManager 周期・レスポンススキーマ・DataStore/EncryptedSharedPreferences パスといった Gemini 連携の共通値を保持する。
- 他レイヤーはここから定数を取得し、ハードコーディングを禁止するポリシーを支える。
