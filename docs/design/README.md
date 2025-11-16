# design フォルダ

- `kafkalauncher_detail_spec.md`: 現行 UI / ドメイン仕様。常に最新状態に保つ。
- `3C.md`: 3C/提供価値メモ。市場背景の整理に利用。
- `launcher_gesture_support.md`: ジェスチャーナビ判定の制約メモ。
- `gemini_feedback_loop.md`: Gemini 連携とログ活用サイクルの設計メモ。`GeminiSyncWorker` や `AiRecommendationPreview` など未実装コンポーネントの仕様に加え、`GeminiConfig` を起点に最小構成でループを完成させる手順を記載。

仕様変更は `kafkalauncher_detail_spec.md` を起点に議論し、他の資料には必要な引用だけを残す。
