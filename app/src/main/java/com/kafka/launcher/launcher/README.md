# launcher

- ホーム画面専用の状態管理。`LauncherViewModel` が各リポジトリと Gemini ストアを統合し `LauncherState` を生成する。
- `LauncherNavigation` が Compose NavHost を構築し、`LauncherViewModelFactory` が依存を手動注入する。
