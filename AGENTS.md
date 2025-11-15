# Repository Guidelines

エラーハンドリング禁止。コメント禁止。最小のコードで機能を実現。Rootにファイル作成禁止。テスト禁止。一時ファイル禁止。ベストプラクティスに倣う。公式ドキュメントに従う。DRYの原則必須。ハードコーディング禁止。設定値はすべてConfigで一元管理。lintで不要な関数の特性と削除など常に実施。各階層にREADME.md設置。buildなどのコマンドを明記。

## プロジェクト構成とモジュール配置
- Jetpack Compose を含む主要コードは `app/src/main/java/com/kafka/launcher` に集約し、`launcher`, `data`, `modules/*` など役割別パッケージで保守性を確保します。
- UI リソース（アイコン、カラー、文字列）は `app/src/main/res` に配置し、再利用可能なテーマ定義を `values/` で共有します。
- 仕様書やデザイン資料は `docs/`（例: `docs/3C.md`, `docs/design/`）に置き、PR で更新理由をリンクしてください。

## ビルド・テスト・開発コマンド
- `./gradlew assembleDebug` — デバッグ APK（`.debug` サフィックス）を生成し、実機検証に使用。
- `./gradlew lint` — Android Lint を実行。警告もレビュー前に解消します。
- `./gradlew testDebugUnitTest` — `app/src/test` 内の JVM ユニットテストを実行。
- `./gradlew connectedDebugAndroidTest` — `app/src/androidTest` の Compose/UI テストを接続デバイスで走らせます。
- `./gradlew clean build` — 生成物をクリアして debug/release の完全ビルドを行い、リリース前の確認に使います。

## コーディングスタイルと命名規約
- Kotlin は 4 スペースインデント、`val` 優先、機能単位のトップレベル `@Composable` を採用します。
- Compose UI は `HomeScreen`, `QuickActionCard` など末尾サフィックスで役割を明示し、状態管理は `Launcher*ViewModel` に置きます。
- `data`, `model`, `modules/google` など意味のあるパッケージでファイルを 200 行以内に保ちます。
- Detekt/Ktlint 相当の検査を IDE で有効にし、生成された IDE 設定ファイルはコミットしません。

## テストガイドライン
- リポジトリや QuickAction のビジネスロジックは JUnit4 + `kotlinx-coroutines-test` で `app/src/test/java` に配置します。
- ナビゲーションや検索導線は `androidx.compose.ui.test.junit4` を用いて `app/src/androidTest/java` で検証します。
- テスト名は `givenState_whenAction_thenResult` 形式でシナリオを明示し、新機能追加時はクリティカルフロー（検索、クイックアクション、学習処理）を網羅します。
- 失敗再現テストを追加してから修正することを原則とし、未カバー箇所は PR 説明で理由を共有します。

## コミットと Pull Request 規約
- Git ログに合わせて `feat: ...`, `fix: ...`, `chore: ...` のように `type: summary` 形式でメッセージを作成します。
- 細かい WIP はローカルで squash し、本文で関連 Issue や検証結果（実行コマンド、スクリーンショット）を記載します。
- PR では変更範囲、テストマトリクス（端末/API レベル）、設定変更点を明記し、UI 変更にはスクリーンショットまたは GIF を添付します。
- CI を意識し lint/test をパスした状態でレビュー依頼し、ドキュメント更新やレビュアーアサインを忘れないでください。

## セキュリティと設定 Tips
- API キーや署名設定はリポジトリに置かず、`~/.gradle/gradle.properties` などローカル設定に保持します。
- `WindowCompat.setDecorFitsSystemWindows(window, false)` の挙動は各画面サイズで UX を確認し、没入モードによるレイアウト崩れを必ずレビューします。
