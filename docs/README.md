# docs ディレクトリ

KafkaLauncher の仕様・検討資料を配置する。主要ドキュメントは `design/` 配下にまとめ、UI やアーキテクチャの更新時は必ず該当セクションを更新する。

## 更新フロー

1. 新しい機能や画面改修を決める
2. `design/kafkalauncher_detail_spec.md` に反映
3. 実装とドキュメント差分を確認

## 関連コマンド

- `./gradlew assembleDebug`
- `./gradlew lint`
- `./gradlew clean build`
