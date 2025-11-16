# data/log

- QuickAction や ActionLog の内容を `/Android/data/.../logs/` に書き出し、PC 連携用のスナップショットを生成する。
- `LogDirectoryWriter` が排他制御とZIP生成を担い、他クラスは単純な append/write API だけを呼び出す。
