# com.kafka.launcher パッケージ

- `MainActivity` が ViewModel/WorkManager 初期化と Compose ルートを担い、他のパッケージに処理を委譲する。
- `config` `data` `domain` `launcher` `quickactions` `ui` を分割し、依存方向を data → domain → launcher/ui へ固定する。
- 端末設定・Gemini 同期・UI ナビゲーションといった中核ロジックはそれぞれのサブフォルダで完結させ、ここから直接触るクラスは最小限に保つ。
