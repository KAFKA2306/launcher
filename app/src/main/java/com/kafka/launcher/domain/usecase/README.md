# domain/usecase

- 単一責任のビジネスロジックを提供する層。入力/出力をデータクラスで受け渡し、他レイヤーから簡単にテストできるようにする。
- `RecommendActionsUseCase` や `GeminiPayloadBuilder` が代表で、行動統計を QuickAction 推薦や Gemini API ペイロードに変換する。
