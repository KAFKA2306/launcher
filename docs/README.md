# docs ディレクトリ

KafkaLauncher の仕様・検討資料を配置する。主要ドキュメントは `design/` 配下にまとめ、UI やアーキテクチャの更新時は必ず該当セクションを更新する。

## 主要ドキュメント

- `design/gemini_feedback_loop.md`: Gemini Sync Worker / Payload / Store / UI 連携の決定版仕様。WorkManager 周期や DataStore 形式を変更した場合は最初に更新する。
- `design/kafkalauncher_detail_spec.md`: 画面仕様・状態遷移を集約。Compose コンポーネント変更時はここを基準に差分チェックする。
- `design/launcher_gesture_support.md`: ナビゲーションモード検出や OEM 制約のメモ。`NavigationNotice` の表示条件を変える際に参照する。

## 更新フロー

1. 新しい機能や画面改修を決める
2. `design/kafkalauncher_detail_spec.md` に反映
3. 実装とドキュメント差分を確認

## 関連コマンド

- `./gradlew assembleDebug`
- `./gradlew lint`
- `./gradlew clean build`
