# quickactions

- Google/Discord/Brave などの `QuickActionProvider` と Intent 生成ロジックをまとめる。
- `QuickActionExecutor` が実行と監査ログ記録を担当し、`QuickActionIntentFactory` がパッケージ解決と Intent 組み立てを一元化する。
