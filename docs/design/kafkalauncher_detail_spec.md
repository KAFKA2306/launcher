# 0. 全体概要

## 0.1 目的

Android 向けホームアプリ **KafkaLauncher** は「アプリではなく行動をホーム最下部からワンタップで呼び出す」ことを目的とする。検索・レコメンド・アプリグリッドなど実際に触れる要素はすべて下端に寄せ、設定やシステム情報は上部へ逃がす。

## 0.2 ユースケース

- 左下からよく使う/最近使ったアプリやクイックアクションを片手でタップする
- 設定は画面上部のボタンからのみ呼び出し、誤タップを避ける
- ホーム下半分に常設した 8 列グリッドでアプリ一覧を即座に開く

## 0.3 制約

- すべてローカル実装（Room/DataStore/Compose）。外部バックエンドや Gemini 連携は対象外。
- 画面要素は再利用コンポーネント化し、設定値は `LauncherConfig` に集約する。
- エラーハンドリングやコメントは最小限ではなく排除する。

---

# 1. システム構成

## 1.1 コンポーネント

1. **Android クライアント**（唯一の実装ターゲット）
   - ランチャー UI（`HomeScreen` / `AppDrawerScreen`）
   - クイックアクション実行・行動ログ保存
   - 設定保存（DataStore）

## 1.2 技術スタック

- Kotlin + Jetpack Compose（Material3）
- Navigation Compose で `home` / `drawer` / `settings` を制御
- Room（`ActionLogDao`）でアクション履歴 + 集計
- Preferences DataStore で表示設定を保持

## 1.3 パッケージ構成

```text
app/src/main/java/com/kafka/launcher
  MainActivity.kt
  config/LauncherConfig.kt
  data/repo/*.kt
  domain/model/*.kt
  domain/usecase/RecommendActionsUseCase.kt
  launcher/LauncherViewModel.kt, LauncherState.kt, LauncherNavigation.kt
  ui/home/HomeScreen.kt
  ui/drawer/AppDrawerScreen.kt
  ui/components/*.kt
  ui/settings/SettingsScreen.kt
```

## 1.4 ビルド・Lint

- `./gradlew assembleDebug`
- `./gradlew lint`
- `./gradlew clean build`

`LauncherConfig` の値から UI レイアウト（列数やプレビュー件数）を制御する。

---

# 2. ドメインとデータモデル

## 2.1 InstalledApp / AppCategory

```kotlin
data class InstalledApp(
    val packageName: String,
    val componentName: ComponentName,
    val label: String,
    val icon: Drawable,
    val category: AppCategory,
    val isPinned: Boolean = false
)

enum class AppCategory(val priority: Int) {
    COMMUNICATION(0),
    WORK(1),
    MEDIA(2),
    TRAVEL(3),
    GAMES(4),
    TOOLS(5),
    OTHER(6)
}
```

`AppRepository` が `ApplicationInfo.category` を `AppCategory` に丸め、同 priority 順でドロワー表示する。マッピング例：

- `CATEGORY_SOCIAL` / `CATEGORY_NEWS` → `COMMUNICATION`
- `CATEGORY_PRODUCTIVITY` → `WORK`
- `CATEGORY_AUDIO` / `CATEGORY_VIDEO` / `CATEGORY_IMAGE` → `MEDIA`
- `CATEGORY_MAPS` → `TRAVEL`
- `CATEGORY_GAME` → `GAMES`
- `CATEGORY_ACCESSIBILITY` → `TOOLS`
- その他は `OTHER`

## 2.2 QuickAction

```kotlin
data class QuickAction(
    val id: String,
    val providerId: String,
    val label: String,
    val actionType: ActionType,
    val data: String? = null,
    val packageName: String? = null,
    val priority: Int = 0
)
```

`RecommendActionsUseCase` が `ActionStats` に基づきおすすめ順を返し、`LauncherState.recommendedActions` で消費する。`QuickActionRow` はホーム下部に移設し、左下に並ぶよう LazyRow を維持する。

## 2.3 ActionLog / ActionStats

```kotlin
@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: String,
    val timestamp: Long
)

data class ActionStats(
    val actionId: String,
    val count: Long
)
```

`ActionLogRepository` は `ActionLogDao` に委譲し、`stats(limit)` で上位利用アクションを Flow で返す。

## 2.4 Settings

```kotlin
data class Settings(
    val showFavorites: Boolean = true,
    val appSort: AppSort = AppSort.NAME
)

enum class AppSort { NAME, USAGE }
```

## 2.5 LauncherState

`LauncherViewModel` が Flow で公開する状態：

- `searchQuery`
- `quickActions` / `recommendedActions`
- `installedApps`（ソート済み）
- `filteredApps`（検索結果）
- `categorizedApps: Map<AppCategory, List<InstalledApp>>`
- `favoriteApps`
- `recentApps`
- `settings`
- `navigationInfo`
- `isLoading`

## 2.6 Repository 責務

- **AppRepository**: インストール済みアプリ列挙、カテゴリ判定、検索フィルタ
- **QuickActionRepository**: 行動定義の監視とフィルタ。`QuickActionIntentFactory`で実行可能性を検証し、Discord Deep Linkを含むすべての行動を端末状態に合わせて絞り込み、ブロードキャスト受信後に即リロード。再計算した一覧は `QuickActionAuditLogger` を通じて `Android/data/com.kafka.launcher/files/logs/quickactions_snapshot.txt` へ書き出す。
- **ActionLogRepository**: 実行ログ書き込み、利用頻度算出
- **SettingsRepository**: DataStore から `Settings` Flow を提供

---

# 3. UI レイアウト方針

## 3.1 ホーム画面

- 画面は「上段=制御」「下段=操作」の 2 層で構成する。
- 上段：
  - 左にドロワー（フルスクリーン一覧）ボタン、右に設定ボタンのみを置き、普段触らない要素をここへ追いやる。
  - 3 ボタン専用ナビ注意は上段の下に差し込む。
- 下段（常に画面の 60% 以上を占有）：
  - `BottomLauncherPanel` は `Box` で下端に貼り付けた `LazyColumn` とし、`navigationBarsPadding()` を必ず適用して 3 ボタンと重ならないようにする。
  - セクション順序は `SearchBar` → `SearchResults or RecommendedRow` → `RecentAppsRow` → `FavoriteAppsRow` → `QuickActionRow` → `AppGrid` を `item` として並べる。
  - `AppGrid` は `GridCells.Fixed(LauncherConfig.appsPerRow)` で 8 列固定。`height = homeGridMinHeight` で常に有限高さに収め、グリッド内部のスクロールにすべて委ねる。
  - 見出しとアクションはすべて下段 `LazyColumn` 内で完結させ、二重スクロールを発生させない。

## 3.2 アプリドロワー

- サーチバーは最上段固定。検索中は `state.filteredApps` を 8 列グリッドで全画面表示。
- 非検索時も同じ 8 列 (`GridCells.Fixed(LauncherConfig.appsPerRow)`) で全アプリを表示し、下部に余白を入れてオーバーレイを配置する。
- ボトムオーバーレイは画面幅いっぱいの `Surface` を `Alignment.BottomStart` に配置し、カテゴリプレビューと説明をまとめる。

## 3.3 ジャンルパネル

- 見出しは `strings.xml` の `categories_title`。
- `LazyRow` で `AppCategory` ごとのカードを並べ、各カードには最大 `LauncherConfig.categoryPreviewLimit` 個のアイコンを表示。`AppGrid` と同じ `AppIcon` を再利用。
- カードをタップすると最初のアプリで即起動する。より細かい選択は上の 8 列グリッドで行うためモックアップ禁止。

## 3.4 よく使う / 最近の領域

- `LauncherConfig.favoritesLimit` をホーム下段での「よく使う」枠数に使う。
- `LauncherConfig.recentLimit` を使い `ActionLogRepository.recent` の結果から最近起動アプリを抽出し、`FavoriteAppsRow` を流用して表示する。
- 検索やレコメンドと同じく、この領域も `BottomLauncherPanel` 内で下寄せ配置し、スクロールせずとも 3〜5 件は常に見えるよう `Spacer` を抑制する。

---

# 4. データ保持と同期

## 4.1 Room

- `action_logs` テーブルのみを保持し、`ActionLogDao` で `insert`, `recent(limit)`, `stats(limit)` を提供。
- データはローカル専用で、アンインストール時に削除される。

## 4.2 DataStore

- `showFavorites` と `appSort` を Preferences DataStore に保存。
- ホーム画面の左下領域は `showFavorites=false` の場合でも空き枠を確保する。

## 4.3 LauncherConfig

| Key | 役割 |
| --- | --- |
| `statsLimit` | `ActionLogRepository.stats` の取得件数 |
| `recommendationFallbackCount` | レコメンドのフォールバック数 |
| `favoritesLimit` | ホームで表示する「よく使う」最大件数 |
| `recentLimit` | 最近起動アプリの抽出件数 |
| `appsPerRow` | ホーム/ドロワー共通のグリッド列数（固定 8）|
| `categoryPreviewLimit` | ジャンルカード内アイコンの最大数 |
| `bottomQuickActionLimit` | 下段クイックアクションの表示数 |
| `homeGridMinHeightDp` | ホーム常設グリッドの最小高さ（dp） |
| `appUsagePrefix` | ログ ID プレフィックス |

これらの値を変えると UI の行動数／並びが即時反映される。
