# data/store

- JSON ファイルを直接読み書きする専用ストアを配置し、Gemini 推薦など複合データを 1 ファイルで管理する。
- `GeminiRecommendationStore` は Flow でスナップショットを返し、`LauncherViewModel` が直接購読する唯一のファイルストアとなる。
