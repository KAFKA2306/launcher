# data/local/db

- `KafkaDatabase` が Room のエントリポイント、`ActionLogDao` が行動ログや統計のクエリを提供する。
- Gemini Payload で必要な最新イベント/統計もここからスナップショットとして取得する。
