# data/local/datastore

- `settingsDataStore` など Preferences DataStore の拡張を提供し、設定値やピン留め情報を保存する。
- データアクセスは suspend/Flow API で返され、リポジトリが直接インジェクトして UI へ流す。
