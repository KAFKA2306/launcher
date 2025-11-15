# KafkaLauncher

最小構成の Android ランチャー。Jetpack Compose + ViewModel でホーム画面 / アプリドロワー / クイックアクションだけに絞り、設定値と端末依存ロジックは `LauncherConfig` と `NavigationInfoResolver` に集約しています。Xiaomi/Redmi/POCO などジェスチャー強制無効端末では 3 ボタン前提 UI に自動でフォールバックし、ホーム・設定画面で警告カード（`NavigationNotice`）を表示します。

## 0. ビルドと署名

| コマンド | 用途 |
| --- | --- |
| `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` を生成。エミュレーター検証向け |
| `./gradlew lint` | Lint を実行し API レベル/未使用リソース/潜在バグを検知 |
| `./gradlew clean build` | Debug/Release 両方を再生成し、CI やリリース前スナップショットを取得 |
| `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release.apk` を生成。`apksigner verify` で署名確認後に配布 |

署名キーストアは `local.properties` で `launcherRelease*` エントリを指定して Gradle へ渡します。

## 1. 画面と機能

### 1.1 ホーム画面

- `KafkaSearchBar` でアプリ名 / クイックアクションの部分一致検索。入力中は `SearchResults` セクションだけを表示。
- 検索が空のときは `QuickActionRow` でおすすめ（`LauncherViewModel.recommendedActions`）と全 QuickAction を表示。
- DataStore で保持する `showFavorites` が有効かつ統計が取れていれば `FavoriteAppsRow` を表示。`ActionLogRepository` の `stats` で得た上位 5 アプリを並べます。
- 最下段でアプリドロワー / 設定ボタンを配置。NavigationInfo が 3 ボタン判定なら `NavigationNotice` をクッションとして表示。

### 1.2 アプリドロワー

- `AppDrawerScreen` はホームと同じ `searchQuery` を共有。検索中はフィルタ済みのアプリのみ表示。
- `AppGrid` で `GridCells.Adaptive(96.dp)`。長押し等のアクションは持たず、タップで `MainActivity.openInstalledApp` を起動。

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

`QuickActionIntentFactory` はパッケージ指定 Intent が解決できなかった場合に一般ブラウザ Intent を fallback するため、Discord Deep Link も UI から隠れません。

### 1.5 ログ出力（Gemini 連携 / 行動追跡）

`QuickActionAuditLogger` が次のファイルを `Android/data/com.kafka.launcher/files/logs/` に書き出します。

- `quickactions_snapshot.txt` : 現在利用可能な QuickAction 一覧。`QuickActionRepository` のリロード時に置換。
- `quickactions_events.txt` : QuickAction 実行イベント。ID / プロバイダー / 種別 / 入力クエリを 1 行追記。

Gemini 連携や不具合調査時はこのディレクトリを共有するだけで行動ログを参照できます。

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

- `LauncherViewModel` が AppRepository/QuickActionRepository/SettingsRepository/ActionLogRepository を監視し、`LauncherState` に統合。
- `RecommendActionsUseCase` は `ActionStats` と QuickAction 一覧からおすすめを抽出。統計が空なら fallback で単純上位を返す。
- `NavigationInfoResolver` が `Settings.Secure.navigation_mode` と OEM 名でジェスチャー可否を判定。
- `LauncherConfig` は推奨アイテム数や Discord Deep Link を集中管理。

## 3. データフロー

1. `MainActivity` 起動 → `LauncherViewModel` 初期化 → `AppRepository.loadApps()` / `QuickActionRepository.observe()` / `ActionLogRepository.stats()` / `SettingsRepository.settings` を並行監視。
2. QuickAction 実行 (`MainActivity.handleQuickAction`) → `QuickActionExecutor` が Intent を解決し、成功時に `QuickActionAuditLogger.logExecution` と `ActionLogRepository.log()` を呼ぶ。
3. アプリ起動 (`openInstalledApp`) も `ActionLogRepository` へ記録し、次回の使用頻度ソートやお気に入り候補に反映。

## 4. 既知の制約

- ホーム/ドロワーの UI は縦スクロールのみ。アプリの長押しピン留めやカテゴリ別表示は未実装。
- Theme 切り替えやアイコンサイズ設定はまだ備えていない。設定画面はお気に入り表示とソート切り替えのみ。
- LLM 連携や通知リスナー等の機能は含まれていない。必要な情報は前述のログ出力で取得する。

## 5. 参考コマンド

```bash
# 推奨ビルドフロー
./gradlew clean build
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Lint だけを回す
./gradlew lint
```
