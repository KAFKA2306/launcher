仕様書モード、いきます。
「KafkaLauncher（行動ランチャー）」の**詳細設計仕様書 v0.1**としてまとめます。

---

# 0. 全体概要

## 0.1 目的

Android 向けランチャーアプリ **KafkaLauncher** を設計する。目的は：

* **アプリではなく“行動”をワンタップで起動**するホームランチャー
* 行動履歴をローカルに蓄積し、
  ローカルスコアリング＋ **Gemini 2.5 Preview** により
  「今このユーザーがやりたい行動」を高精度で推薦
* 既存 Android ガイドライン（Room / DataStore / RoleManager / Structured Output など）に準拠した実装

---

# 1. システム構成

## 1.1 コンポーネント

1. **Android クライアント（KafkaLauncher アプリ）**

   * ランチャー UI（HOME アプリ）
   * 行動（QuickAction）実行
   * 行動ログ記録（Room）
   * 設定・ユーザー好み保存（DataStore）
   * バックエンドとの同期

2. **Backend API**

   * `/logs/upload`：行動ログ同期
   * `/recommendations/get`：おすすめ行動取得
   * Gemini 2.5 API 呼び出し（Structured Output）

3. **Gemini 2.5 Preview**

   * モデル：`gemini-2.5-pro-preview` を想定
   * Structured Output（JSON Schema）による
     `recommended_actions` / `new_quick_actions` 返却

---

# 2. Android クライアント詳細設計

## 2.1 アーキテクチャ

* Kotlin + Jetpack Compose
* MVVM + UseCase レイヤ
* 永続化：

  * 行動ログ・統計 → Room（Jetpack Room）
  * 設定・ユーザー好み → Jetpack DataStore（Preferences）

### 2.1.1 パッケージ構成

```text
app/
  MainActivity.kt
  launcher/
    LauncherViewModel.kt
    LauncherState.kt
    LauncherNavigation.kt
  ui/
    home/HomeScreen.kt
    drawer/AppDrawerScreen.kt
    settings/SettingsScreen.kt
    components/
      SearchBar.kt
      QuickActionRow.kt
      RecommendedSlotRow.kt
      AppGrid.kt
  data/
    repo/AppRepository.kt
    repo/QuickActionRepository.kt
    repo/ActionLogRepository.kt
    repo/SettingsRepository.kt
    local/db/KafkaDatabase.kt
    local/datastore/SettingsDataStore.kt
  domain/
    model/InstalledApp.kt
    model/QuickAction.kt
    model/UserQuickActionConfig.kt
    model/ActionLog.kt
    model/ActionStats.kt
    model/Settings.kt
    usecase/RecommendActionsUseCase.kt
    usecase/SyncLogsUseCase.kt
  quickactions/
    GoogleCalendarModule.kt
    GoogleMapsModule.kt
    GmailModule.kt
    DiscordModule.kt
    BraveModule.kt
  remote/
    ApiClient.kt
```

---

## 2.2 ランチャーとしての登録

### 2.2.1 AndroidManifest.xml

* `HOME` カテゴリを持つ `MAIN` Activity を定義：

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask">

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>

</activity>
```

これは Android のホームアプリ実装の基本パターンに従う。

### 2.2.2 RoleManager によるデフォルト HOME リクエスト

Android 10+ では `RoleManager` の `ROLE_HOME` を使って
デフォルトホームアプリのロールをユーザーにリクエストできる。

* 起動時 or 設定画面で以下の処理を実行：

1. `context.getSystemService(RoleManager::class.java)` で RoleManager を取得
2. `isRoleAvailable(ROLE_HOME)` を確認
3. `isRoleHeld(ROLE_HOME)` が false の場合、
   `createRequestRoleIntent(ROLE_HOME)` の Intent を `startActivityForResult` 相当で起動

---

## 2.3 データモデル

### 2.3.1 InstalledApp

ランチャーに表示するインストール済みアプリ情報。

```kotlin
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
```

取得ロジック：

* `PackageManager` の `queryIntentActivities` で
  `ACTION_MAIN` + `CATEGORY_LAUNCHER` を持つ Activity を列挙。

### 2.3.2 QuickAction（行動）

「行いたい行動」を抽象化した UI 実行単位。

```kotlin
data class QuickAction(
    val id: String,
    val providerId: String,
    val label: String,
    val actionType: ActionType,
    val param: String?,
    val iconResId: Int?,
    val priority: Int
)

enum class ActionType {
    OPEN_APP,
    WEB_SEARCH,
    MAP_NAVIGATION,
    CALENDAR_VIEW,
    CALENDAR_INSERT,
    EMAIL_COMPOSE,
    DISCORD_OPEN
}
```

* `providerId`：

  * `"google_maps"`, `"google_calendar"`, `"gmail"`, `"discord"`, `"brave"` など
* `param`：

  * Maps：住所または検索クエリ
  * Brave：URLまたは検索クエリ
  * Discord：チャンネルID/URL など

### 2.3.3 UserQuickActionConfig

ユーザー定義のカスタム行動。

```kotlin
data class UserQuickActionConfig(
    val id: String,
    val providerId: String,
    val label: String,
    val actionType: ActionType,
    val param: String
)
```

* 永続化：Room または DataStore（件数が少ないのでどちらでも可）

### 2.3.4 ActionLog（行動履歴）

Room の Entity として定義。

```kotlin
enum class TimeSlot { MORNING, DAYTIME, EVENING, NIGHT }

enum class NetworkType { WIFI, CELLULAR, OFFLINE }

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,             // epoch millis
    val actionType: ActionType,
    val packageName: String?,
    val quickActionId: String?,
    val query: String?,
    val timeSlot: TimeSlot,
    val dayOfWeek: Int,              // ISO-8601: 1=Mon ... 7=Sun
    val networkType: NetworkType,
    val isCharging: Boolean
)
```

Room は SQLite に対する抽象レイヤとして Entity / DAO / Database で構成する。

### 2.3.5 ActionStats（暗黙フィードバック）

おすすめスロットでの表示回数・クリック回数。

```kotlin
@Entity(tableName = "action_stats")
data class ActionStats(
    @PrimaryKey val quickActionId: String,
    val impressions: Int,
    val clicks: Int
)
```

### 2.3.6 Settings（DataStore）

DataStore は SharedPreferences の代替として非同期・トランザクションな保存を提供する。

保持項目：

* `preferredSlotCount: Int`（3〜5）
* `pinnedIds: Set<String>`
* `hiddenIds: Set<String>`
* `precisionMode: Boolean`
* `loggingEnabled: Boolean`
* `geminiSyncEnabled: Boolean`

---

## 2.4 Repository 層

### 2.4.1 AppRepository

責務：

* インストール済みアプリの一覧取得
* `PackageManager` と `LauncherApps` API のラッパ

### 2.4.2 QuickActionRepository

責務：

* 各 `QuickActionProvider` から QuickAction を収集
* `UserQuickActionConfig` を読み込んで QuickAction を生成
* `hiddenIds` を除外

### 2.4.3 ActionLogRepository

責務：

* `ActionLog` の insert / query / rotation
* 古いログの削除（最大 50,000 件 / 90 日など）

Room DAO 例（抽象）：

* `insert(log: ActionLog)`
* `getRecentLogs(limit: Int, since: Long): List<ActionLog>`
* `deleteOlderThan(timestamp: Long)`
* `count(): Long`

### 2.4.4 SettingsRepository

責務：

* DataStore の読み書き（Flow で observe）

---

## 2.5 UseCase 層（ロジック）

### 2.5.1 ActionExecutor

すべての行動を通す「実行ゲート」。

```kotlin
class ActionExecutor(
    private val context: Context,
    private val logRepo: ActionLogRepository,
    private val statsRepo: ActionStatsRepository
) {
    fun executeQuickAction(action: QuickAction, now: Long, contextInfo: ContextInfo) {
        logRepo.insert(action.toActionLog(now, contextInfo))
        statsRepo.incrementClick(action.id)
        action.toIntent(context).let { context.startActivity(it) }
    }

    fun recordImpression(actionId: String) {
        statsRepo.incrementImpression(actionId)
    }
}
```

* UI からは **必ず ActionExecutor 経由で実行**
  → ログと統計が抜け漏れなく保存される

### 2.5.2 RecommendActionsUseCase（ローカル）

ローカルスコアリングの責務：

1. `ActionLogRepository` から直近 N 件（例：300件）取得
2. `QuickActionRepository` から現在有効な QuickAction 一覧を取得
3. スコアリング
4. スコアがしきい値以上のものを「候補セット」として抽出
5. precisionMode に応じたフィルタリング

---

# 3. ローカル推薦ロジックの詳細

## 3.1 スコア要素

各 `quickActionId` について以下を計算：

* `Sf`：頻度スコア（30 日以内の実行回数を正規化）
* `Sr`：直近スコア（最後の実行時刻に基づく）
* `St`：時間帯スコア（現在の timeSlot での利用の多さ）
* `Sd`：曜日スコア（現在の dayOfWeek での利用の多さ）
* `Sp`：直前アクション ペアスコア（`prevActionId -> actionId` の遷移頻度）
* `Su`：ユーザー Feedback（ピン留め / 非表示）

## 3.2 重みづけ

```text
S =  w_f * Sf
   + w_r * Sr
   + w_t * St
   + w_d * Sd
   + w_p * Sp
   + w_u * Su
```

初期値例：

* `w_f = 1.0`（頻度）
* `w_r = 2.0`（直近）
* `w_t = 1.5`（時間帯）
* `w_d = 1.0`（曜日）
* `w_p = 2.0`（ペア）
* `w_u = 5.0`（ユーザー）

### 3.2.1 各要素の定義（例）

* `Sf`：

  * 過去30日間の実行回数 `count30` に対し
    `Sf = log(1 + count30)`（対数で飽和）

* `Sr`：

  * `delta = now - lastTimestamp`
  * 1時間以内：`Sr=3`
  * 当日：`Sr=2`
  * 3日以内：`Sr=1`
  * それ以降：`Sr=0`

* `St`：

  * 現在の `timeSlot` における実行回数を集計し、
    全時間帯での実行回数で正規化して 0〜1 にスケール

* `Sd`：

  * 現在の `dayOfWeek` での実行回数 / 全実行回数

* `Sp`：

  * 直前に実行された `prevActionId` からの遷移回数
    `transition(prevActionId, actionId)` を
    全遷移で正規化した値

* `Su`：

  * ピン留めされている：`Su = +1`
  * 「今後表示しない」指定：`Su = -1`
  * それ以外：`Su = 0`

## 3.3 precisionMode におけるしきい値

設定 `precisionMode = true` のとき：

* スコア `S < T` のアクションは**おすすめ候補から除外**
* `T` の初期値例：`T = 3.0`
* 候補が 0 件のとき：

  * 「おすすめスロット」は空にし、
    Fallback として「最近使った行動」や「ピン留め行動」を表示

---

# 4. Gemini 2.5 連携ロジック

Google の Gemini API は `response_mime_type: "application/json"` と
`response_json_schema` で構造化出力をサポートする。

## 4.1 呼び出しタイミング

* ホーム画面が前面に出たとき
* 前回の Gemini 呼び出しから **15分以上経過**している
* `geminiSyncEnabled = true` のときのみ

## 4.2 Backend へのリクエスト

Android → Backend:

```json
{
  "user_id": "device-xxxxxxxx",
  "timestamp": 1731651000000,
  "mode": "recommend_only",
  "context": {
    "time_slot": "EVENING",
    "day_of_week": 5,
    "battery": { "is_charging": true, "level": 0.78 },
    "network": { "type": "WIFI", "is_metered": false },
    "location": { "coarse_area": "Osaka", "home_distance_m": 5100, "work_distance_m": 850 }
  },
  "action_logs": [ ... 最新300件 ... ],
  "current_quick_actions": [ ... ],
  "user_settings": {
    "preferred_slot_count": 4,
    "pinned_ids": ["maps_nav_home"],
    "hidden_ids": ["discord_spam_channel"],
    "precision_mode": true
  }
}
```

Backend ではこの JSON をそのまま Gemini に渡すか、必要に応じて前処理。

## 4.3 Gemini Structured Output のスキーマ

Gemini API の Structured Output 仕様に合わせて JSON Schema を指定する。

```json
{
  "type": "object",
  "properties": {
    "recommended_actions": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "quick_action_id": { "type": "string" },
          "confidence": { "type": "number" },
          "reason": { "type": "string" }
        },
        "required": ["quick_action_id", "confidence"]
      }
    },
    "new_quick_actions": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "provider_id": { "type": "string" },
          "suggested_id": { "type": "string" },
          "label": { "type": "string" },
          "action_type": { "type": "string" },
          "param": { "type": "string" },
          "reason": { "type": "string" }
        },
        "required": [
          "provider_id",
          "suggested_id",
          "label",
          "action_type",
          "param"
        ]
      }
    }
  },
  "required": ["recommended_actions"]
}
```

* `response_mime_type = "application/json"`
* `response_json_schema = 上記` を指定

## 4.4 Gemini へのプロンプト（概要）

* system：

  * ランチャーの行動推薦エンジンとしての役割
  * `current_quick_actions` の ID 以外を推奨しないこと
  * 破壊的行動を提案しないこと

* user：

  * `action_logs`、`context`、`current_quick_actions`、`user_settings` を含む JSON を base64 エンコードして渡す

## 4.5 Backend → Android の返却形式

Backend は Gemini のレスポンスを受け取り、
Android クライアント向けに整形：

```json
{
  "slot_recommendations": [
    "maps_nav_work",
    "brave_search_recent",
    "discord_last_channel"
  ],
  "slot_confidence": {
    "maps_nav_work": 0.92,
    "brave_search_recent": 0.78,
    "discord_last_channel": 0.65
  },
  "new_action_candidates": [
    {
      "id": "maps_nav_home_evening",
      "provider_id": "google_maps",
      "label": "帰宅ルートを見る",
      "action_type": "MAP_NAVIGATION",
      "param": "home_address"
    }
  ]
}
```

Android 側は：

* `slot_recommendations`：

  * スロット数 `preferredSlotCount` までをおすすめスロットに表示
* `new_action_candidates`：

  * 設定画面やダイアログで「追加候補」として表示
  * ユーザーが明示的に承認したものだけ `UserQuickActionConfig` として保存

---

# 5. UI ロジック

## 5.1 ホーム画面

1. 描画フロー

   * `LauncherViewModel` が `LauncherState` を Flow で expose
   * state:

     * `recommendedActions: List<QuickAction>`
     * `pinnedActions: List<QuickAction>`
     * `frequentApps: List<InstalledApp>`
     * `searchQuery: String`
     * `searchResults: List<SearchResultItem>`

2. コンポーネント

   * 上部：検索バー
   * その下：おすすめ行動スロット（Gemini or ローカル）
   * その下：ピン留め行動
   * 下部：ドロワーへ遷移する「↑」ボタン

3. イベント

   * スロット表示時：`ActionExecutor.recordImpression(action.id)`
   * タップ時：`ActionExecutor.executeQuickAction(action, now, contextInfo)`
   * 長押し：

     * ピン留め / ピン解除
     * 今後表示しない
     * 詳細を見る（reason / 統計）

## 5.2 アプリドロワー

* `AppRepository` から取得した `InstalledApp` のグリッド表示
* アプリアイコン長押し → 「クイックアクションに追加」など

---

# 6. データ保持とプライバシー

## 6.1 ローカル保持

* `ActionLog`：最大 50,000 件 or 90 日を上限にローテーション
* `ActionStats`：全 QuickAction の統計（件数は少ない）
* `Settings`：DataStore（Preferences）

Room / DataStore はローカルストレージにデータを保持し、
アプリアンインストールで削除される。

## 6.2 バックエンド／Gemini 側

* バックエンド DB：最大 365 日（1年）保持、日次バッチで削除
* Gemini API には行動ログ一部（直近 max 300 件）＋集計情報のみ送信
* `loggingEnabled = false` の場合：

  * 行動ログの保存自体を行わない
* `geminiSyncEnabled = false` の場合：

  * ログはローカルのみ（Backend 送信なし）
  * おすすめは完全にローカルスコアリングのみで生成

---

# 7. 非機能要件

## 7.1 パフォーマンス

* ホーム起動：500ms 以内
* おすすめスロット決定：

  * ローカルスコアリング：100ms 以内
  * Gemini 連携は非同期（結果が来るまでローカル候補使用）
* APK サイズ：10MB 以下を目標

Room / DataStore / Compose はモバイル端末での利用を前提に設計されているため、適切なクエリ・Flow 運用で要件を満たせる想定。

## 7.2 バッテリー

* 常駐サービスなし
* 同期バッチ：

  * 1時間に1回以下
  * Wi-Fi 時優先

---

# 8. 拡張ポイント（将来）

* Discord の通知チャンネル Deep Link 対応（公式 URI / Intent 調査）
* UsageStatsManager を使った「ランチャー外の行動」も含めた推薦（ユーザー許可が前提）
* Gemini 2.5 の Structured Output 改良に合わせた複雑スキーマ対応（multi-agent 化など）

---

ここまでで、

* Android ランチャーとしての基盤（HOME / RoleManager / Room / DataStore）
* 行動ログモデルと記録ロジック
* ローカルのスコアリングと precision モード
* Gemini 2.5 Preview Structured Output との JSON I/O 仕様
* UI・データフロー
* データ保持・プライバシー

をすべて **「既存ドキュメントに沿った形」で** 明示しました。

この仕様をそのまま、

* `docs/design/kafkalauncher_detail_spec.md`

みたいなファイルに貼り込んで、
次のステップとしては：

* Kotlin data class 群の雛形
* Room DAO / Database 定義
* DataStore のキー定義
* Backend の `/logs/upload`, `/recommendations/get` インターフェース定義

に落としていけば、実装フェーズに入れます。
