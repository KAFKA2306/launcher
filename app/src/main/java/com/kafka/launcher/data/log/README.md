# data/log

- QuickAction / ActionLog / Gemini同期監査を app-specific external storage の `logs/` に書き出し、PC 連携用のスナップショットを生成する。
- `LogDirectoryWriter` が排他制御、`logs_manifest.json`、`logs_bundle.zip` の生成を担い、他クラスは append/write API だけを呼び出す。
- `gemini_sync_events.jsonl` は Gemini 同期の `skipped / request / failed / completed`、理由、入力件数、推薦件数、所要時間、payload SHA-256 を記録する。API key と raw payload は保存しない。
- `recommendationFeedback` には AI QuickAction の usage / accepted / dismissed 集計だけを返し、Geminiの次回同期に利用する。検索語、credential、raw request/response はフィードバックログへ保存しない。
