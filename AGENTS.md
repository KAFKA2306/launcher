# Repository Guidelines

エラーハンドリング禁止。コメント禁止。最小のコードで機能を実現。Rootにファイル作成禁止。テスト禁止。一時ファイル禁止。ベストプラクティスに倣う。公式ドキュメントに従う。DRYの原則必須。ハードコーディング禁止。設定値はすべてConfigで一元管理。lintで不要な関数の特性と削除など常に実施。各階層にREADME.md設置し、常に更新。buildなどのコマンドを明記。モックアップ禁止。返信は日本語。

## プロジェクト構成とモジュール配置
- Android 本体コードは `app/src/main/java/com/kafka/launcher` に集約し、`launcher` (画面状態・NavHost)、`data` (Repository/DB/DataStore)、`domain` (model/usecase)、`quickactions` (Google/Discord/Brave 連携) の層で依存方向を一定に保ちます。
- Compose UI・設定画面などのレイアウトは `ui/` 以下で画面別に分割し、再利用コンポーネントは `ui/components` に配置します。
- マニフェスト、テーマ、文字列、アイコン等のリソースは `app/src/main/res` にまとめ、バリアント固有資産は `res` の `-debug` など適切なフォルダに置きます。
- 補足資料や仕様書は `docs/`（例: `docs/3C.md`, `docs/design/`）に追加し、PR 説明から必ずリンクしてください。

## ビルド・開発コマンド

### WSL/Linux
- `./gradlew assembleDebug` — デバッグ APK を生成
- `./gradlew lint` — Lint 実行
- `./gradlew clean build` — 完全リビルド
- `./gradlew testDebugUnitTest` — Robolectric 単体テスト

### Windows
- `task win` — **1コマンドで完結**（最新コード同期 → ビルド → インストール → 起動 → ログ取得 → クラッシュ判定）

## テスト環境

### WSL/Linux
- Robolectric 4.15 を使用した Activity 起動テストを `app/src/test/` に配置
- デバッグビルドは `isMinifyEnabled = false` と `isDebuggable = true` に設定

### Windows エミュレータテスト
- **環境**: Windows PowerShell + Android SDK (android-clt) + Java 17
- **AVD**: Pixel_7_API_35 (Android 35, Google APIs, x86_64)
- **実行**: エミュレータ起動後、`cd C:\Users\$env:USERNAME\projects\launcher` → `task win`
- **自動処理**: WSLから最新コード同期 → ビルド → adb再起動 → インストール → アプリ起動 → ログ10秒間キャプチャ → クラッシュ自動判定

## コーディングスタイルと命名規約
- Kotlin は 4 スペースインデント、`val` 優先、`when`/`sealed interface` で分岐を明示します。長い関数は 60 行以内を目安に分割してください。
- Compose の画面コンポーザブルは `HomeScreen`, `AppDrawerScreen` のように末尾 `Screen` を付け、`LauncherNavHost` などナビゲーション用 Composable は `launcher` パッケージにまとめます。
- DI は手動 Factory（`LauncherViewModelFactory`）で統一し、依存の順序を `data -> domain -> ui` に限定します。
- Lint/IDE で Detekt/Ktlint 同等のフォーマッタと最適化を常時有効化し、`*.iml` など IDE 生成物はコミットしません。

## コミットと Pull Request 規約
- 既存履歴に合わせて `feat:`, `fix:`, `chore:`, `docs:` などの prefix + 簡潔な概要でコミットメッセージを作成し、1 コミット 1 トピックを維持します。
- WIP を複数積む場合はローカルで squash、PR では目的・実装要約・テスト結果（実行コマンド、スクリーンショット、動画）を箇条書きで添付します。
- UI 変更やテーマ更新時は Before/After を PR 説明に貼り、影響範囲（端末/向き/フォームファクタ）を列挙します。
- Issue 連携は `Fixes #ID`/`Refs #ID` を本文最後に記載し、レビュアーを `launcher`, `data` など担当領域に応じてアサインします。
- app/build/outputs/apk/**/*.apkだけはcommitで管理する。

## セキュリティと設定 Tips
- API キーや署名ファイルは `local.properties` や `~/.gradle/gradle.properties` に置く。
- `WindowCompat.setDecorFitsSystemWindows(window, false)` など端末依存の設定は API レベル条件をコード内に残し、Lint 警告が出ないことを確認してからマージします。
- ネットワークやログ連携を追加する場合は `quickactions` もしくは `remote/ApiClient` に閉じ込める。