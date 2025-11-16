# data/store

- Preferences DataStore を使った専用ストアを配置し、Gemini 推薦など複合JSONを1ファイルで管理する。
- `GeminiRecommendationStore` は Flow でスナップショットを返し、`LauncherViewModel` が直接購読する唯一の DataStore ラッパー。
