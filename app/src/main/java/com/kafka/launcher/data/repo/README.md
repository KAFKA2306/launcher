# data/repo

- UI とドメインが利用するリポジトリ群。Room・DataStore・ログファイル・QuickAction Provider を統合して隠蔽する。
- `ActionLogRepository` はログ追記とファイル書き込み、`QuickActionRepository` はアプリ検出と監視、`SettingsRepository`/`PinnedAppsRepository` は DataStore の CRUD を担当する。
