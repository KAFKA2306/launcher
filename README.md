# KafkaLauncher

最小構成の Android ランチャー。Jetpack Compose + ViewModel でホーム画面 / アプリドロワー / クイックアクションだけに絞り、設定値と端末依存ロジックは `LauncherConfig` と `NavigationInfoResolver` に集約しています。Xiaomi 端末では 3 ボタン前提 UI に自動でフォールバックし、ホーム・設定画面で警告カード（`NavigationNotice`）を表示します。

dist:
https://github.com/KAFKA2306/launcher/blob/main/app/build/outputs/apk/debug/app-debug.apk

## 0. ビルドと署名

| コマンド | 用途 |
| --- | --- |
| `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` を生成。エミュレーター検証向け |
| `./gradlew lint` | Lint を実行し API レベル/未使用リソース/潜在バグを検知 |
| `./gradlew clean build` | Debug/Release 両方を再生成し、CI やリリース前スナップショットを取得 |
| `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release.apk` を生成。`apksigner verify` で署名確認後に配布 |

署名鍵を共有していないためリポジトリにはデバッグ APK のみを含め、署名前の `app-release-unsigned.apk` は削除した。Release 版は各自の keystore を使ってローカルで生成し、配布前に `apksigner verify` を通す。

署名キーストアは `local.properties` で `launcherRelease*` エントリを指定して Gradle へ渡します。

## 1. 画面と機能

### 1.1 ホーム画面

- `KafkaSearchBar` でアプリ名 / クイックアクションの部分一致検索。入力中は `SearchResults` セクションだけを表示。
- 検索が空のときは `QuickActionRow` でおすすめ（`LauncherViewModel.recommendedActions`）と全 QuickAction を表示。
- DataStore で保持する `showFavorites` が有効なときは `FavoriteAppsRow` を表示。長押しでお気に入り登録したアプリを優先し、`ActionLogRepository` の `stats` に基づく使用頻度上位を組み合わせます。
- 最下段でアプリ一覧 / AI プレビュー / 設定の 3 ボタンを横並びに配置。AI ボタンは `LauncherViewModel.toggleAiPreview()` で `AiRecommendationPreview` を開閉し、`NavigationNotice` は 3 ボタン判定時のみ表示する。
- `AiRecommendationPreview` は `GeminiRecommendationStore` の `globalPins` `windows` `rationales` をそのまま描画し、Gemini の最終更新時刻をローカル時刻で表示する。QuickAction おすすめとお気に入り行も同じストアからのデータを優先的に採用する。

### 1.2 アプリドロワー

- `AppDrawerScreen` はホームと同じ `searchQuery` を共有。検索中はフィルタ済みのアプリのみ表示。
- `AppGrid` はアプリをタップで起動、長押しでお気に入り登録/解除やアンインストールダイアログを開けます。

### 1.3 設定画面

- 「よく使うアプリ表示」トグルと「並び順（名前 / 使用頻度）」ラジオボタン。
- 「デフォルトホームに設定」ボタンで `RoleManager` へ遷移要求。
- ナビゲーションが 3 ボタンの場合はホーム同様 `NavigationNotice` を最上段に表示。

### 1.4 クイックアクション

`QuickActionProvider` が返す `QuickAction` を `QuickActionRepository` がまとめ、未インストールアプリや解決できない Intent を除外したうえで ViewModel へ流します。現在のモジュール構成：

| Provider | 内容 |
| --- | --- |
| GoogleCalendarModule | 今日の予定、イベント作成（それぞれ Calendar VIEW / INSERT） |
| GoogleMapsModule | `geo:0,0?q=` / `google.navigation:q=` を使うマップ検索・ナビ |
| GmailModule | Inbox 起動、メール作成（`mailto:`） |
| DiscordModule | Discord アプリ起動に加えて `LauncherConfig.discordShortcuts` の Deep Link（既定では DM 一覧、Discord Testers #faq） |
| BraveModule | Brave 起動、Brave検索、固定 URL（X / Perplexity / VRChat） |

`QuickActionIntentFactory` は指定パッケージで解決可能な Intent のみを返し、解決できない組み合わせは UI 上からも除外されます。

### 1.5 ログ出力（Gemini 連携 / 行動追跡）

`QuickActionAuditLogger` と `ActionLogFileWriter` が `Android/data/com.kafka.launcher/files/logs/` に次のファイルを生成します。

- `quickactions_snapshot.txt` : 現在利用可能な QuickAction 一覧。`QuickActionRepository` のリロード時に置換。
- `quickactions_events.txt` : QuickAction 実行イベント。ID / プロバイダー / 種別 / 入力クエリを 1 行追記。
- `action_events.jsonl` : `ActionLogRepository.log` の履歴。LLM 解析向けに ISO 時刻つき JSON Lines で追記。
- `action_recent.json` : 最近のアプリ使用ログを JSON で保存。
- `action_stats.json` : アプリ使用頻度ランキングを JSON で保存。
- `logs_manifest.json` : 上記ファイルと `logs_bundle.zip` のメタデータ（サイズ / 更新時刻）を列挙。
- `logs_bundle.zip` : すべてのログと `logs_manifest.json` をまとめたアーカイブ。PC 側はこれを定期的に Pull するだけで同期完了。

#### PC へのログ取得手順

1. `adb devices` で端末が接続済みであることを確認する。
2. `adb shell` で `/sdcard/Android/data/com.kafka.launcher/files/logs/` を確認し、`logs_bundle.zip` の更新時刻とサイズを把握する。
3. `adb shell "toybox cp -f /sdcard/Android/data/com.kafka.launcher/files/logs/logs_bundle.zip /sdcard/Download/launcher_logs_bundle.zip"` で誰でも読める `Download` フォルダへ複製する。
4. `adb pull /sdcard/Download/launcher_logs_bundle.zip ./launcher_logs_bundle.zip` を実行して PC へ取得する。
5. `unzip launcher_logs_bundle.zip -d launcher_logs_bundle` で展開し、`logs_manifest.json` に沿って差分管理する。

`logs_manifest.json` は生成タイミングの ISO 時刻と各ファイルのサイズ・更新時刻を持つため、Gemini とのフィードバックループや PC 自動収集スクリプトは差分検出に利用できる。

#### Gemini 再スコアリングサイクル（仕様段階）

- `GeminiSyncWorker` / `GeminiPayloadBuilder` / `GeminiRecommendationStore` / `AiRecommendationPreview` はまだコード化されていない。ここで説明している 3 時間サイクルは、端末内でログ→`gemini-2.5-pro-exp`→UI 反映まで閉じる将来実装の設計メモである。
- 設計では WorkManager が `ActionLogRepository` と `QuickActionAuditLogger` の統計を収集し、`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro-exp:generateContent` へ送信する。Structured Output で受け取った推薦を DataStore に保存し、差分だけを `LauncherViewModel` の Flow に流す計画になっている。
- `QuickActionRow` / `FavoriteAppsRow` / `AiRecommendationPreview` は DataStore の更新をそのまま描画し、Gemini 応答が遅延・欠損した場合でも端末内スナップショットを使って UI を維持する想定。詳細なパイプラインとスキーマは `docs/design/gemini_feedback_loop.md` にまとめている。

## 2. アーキテクチャ

```
app/src/main/java/com/kafka/launcher
├─ MainActivity.kt
├─ config/LauncherConfig.kt
├─ data/
│   ├─ local/datastore/SettingsDataStore.kt
│   ├─ local/db/(ActionLogDao|KafkaDatabase).kt
│   ├─ log/QuickActionAuditLogger.kt
│   ├─ repo/(AppRepository|QuickActionRepository|ActionLogRepository|SettingsRepository).kt
│   └─ system/NavigationInfoResolver.kt
├─ domain/model/(InstalledApp|QuickAction|ActionLog|Settings|NavigationInfo).kt
├─ domain/usecase/RecommendActionsUseCase.kt
├─ launcher/(LauncherViewModel|LauncherState|LauncherNavigation|LauncherViewModelFactory).kt
├─ quickactions/(各 Module + QuickActionExecutor + QuickActionIntentFactory).kt
└─ ui/(home|drawer|settings|components)/...
```

- `LauncherViewModel` が AppRepository / QuickActionRepository / SettingsRepository / ActionLogRepository / `GeminiRecommendationStore` を監視し、Gemini 推薦とローカル統計をまとめて `LauncherState` に統合。
- `RecommendActionsUseCase` は `ActionStats` と QuickAction 一覧から一致する ID だけを抽出し、Gemini 推薦が空のときのフォールバックとして利用する。
- `NavigationInfoResolver` が `Settings.Secure.navigation_mode` と OEM 名でジェスチャー可否を判定。
- `LauncherConfig` は推奨アイテム数や Discord Deep Link を集中管理。

## 3. データフロー

1. `MainActivity` 起動 → `LauncherViewModel` 初期化 → `AppRepository.loadApps()` / `QuickActionRepository.observe()` / `ActionLogRepository.stats()` / `SettingsRepository.settings` を並行監視。
2. QuickAction 実行 (`MainActivity.handleQuickAction`) → `QuickActionExecutor` が Intent を解決し、成功時に `QuickActionAuditLogger.logExecution` と `ActionLogRepository.log()` を呼ぶ。
3. アプリ起動 (`openInstalledApp`) も `ActionLogRepository` へ記録し、次回の使用頻度ソートやお気に入り候補に反映。

## 4. 既知の制約

- ホーム/ドロワーの UI は縦スクロールのみでウィジェット領域やレイアウト切り替えは未対応。
- Theme 切り替えやアイコンサイズ設定はまだ備えていない。設定画面はお気に入り表示とソート切り替えのみ。
- LLM 連携は Gemini 推薦のみに限定しており、通知リスナーや外部サービスへのリアルタイム連携は含まれていない。

## 5. 参考コマンド

```bash
# 推奨ビルドフロー
./gradlew clean build
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Lint だけを回す
./gradlew lint
```
