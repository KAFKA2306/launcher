# design フォルダ

- `kafkalauncher_detail_spec.md`: 現行 UI / ドメイン仕様。常に最新状態に保つ。
- `3C.md`: 3C/提供価値メモ。市場背景の整理に利用。
- `launcher_gesture_support.md`: ジェスチャーナビ判定の制約メモ。
- `gemini_feedback_loop.md`: Gemini 連携とログ活用サイクルの実装仕様。`GeminiSyncWorker` / `GeminiRecommendationStore` / `AiRecommendationPreview` のフローと、`GeminiConfig` を起点にした最小構成・API キー読み込み方法を記載。
- `design-system.md`: Color / Typography / Shape / Elevation / Spacing の共通トークン定義。UI 変更時は必ず参照し、Config を経由して利用する。
- `discord_webview_v4.md`: Discord WebView / 通知ミラー / ランキング仕様。URL 正規化キー、JS ブリッジ API、権限誘導、ログ除外範囲を変更するときは本書を更新してから実装する。

仕様変更は `kafkalauncher_detail_spec.md` を起点に議論し、他の資料には必要な引用だけを残す。
