# data/remote

- `GeminiApiClient` が OkHttp を用いて Structured Output API を 1 リクエストだけ呼び、レスポンスをドメインモデルへ変換する。
- ネットワーク層はこのクラスのみとし、エンドポイントやテンプレートは `GeminiConfig` に集約する。
