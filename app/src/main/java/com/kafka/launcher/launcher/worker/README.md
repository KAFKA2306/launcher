# launcher/worker

- Gemini 同期のジョブ管理を担当。`GeminiWorkScheduler` が WorkManager を登録し、`GeminiSyncWorker` が Payload 生成→API呼び出し→DataStore更新を直列に実行する。
- 外部からは `GeminiWorkScheduler.schedule(context)` だけを呼べば周期登録と再登録が完了する。
