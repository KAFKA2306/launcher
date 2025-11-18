# launcher

- ホーム画面専用の状態管理。`LauncherViewModel` が各リポジトリ・`GeminiRecommendationStore`・`GeminiApiKeyStore` を統合し `LauncherState` を生成する。
- `LauncherNavigation` が Compose NavHost を構築し、`LauncherViewModelFactory` が依存を手動注入する。
- `DiscordProvider` が Discord 用の Repository インスタンスを生成し、今後の Discord 画面で使い回せるようにする。
