# =========================================

# **Android ランチャー：KafkaLauncher v1.0 仕様書（完全版）**

XiaomiがサードパーティのAndroid Launcherにおいて基本のジェスチャーコントロール（Homeへ戻る操作など）をサポートしていないため、全てのサードパーティLauncherのユーザー体験が最悪であり、開発を凍結する。


# =========================================

---

# 0. **目的**

本ランチャーは、既存ランチャー（Focus Launcher / KISS / Lawnchair）のベストプラクティス、
および Google / Discord / Brave など主要アプリとの連携を継承しつつ、

> **最小構造（極小コードベース）で、 “ホーム画面 + アプリドロワー + クイックアクション” を完全実装すること**

を目的とする。

後の拡張（世界モデルUI化、生成動画UI化、LLMカード統合）を見据えた
**モジュール型構造**を採用する。

---

## 0.1 ビルド・署名・配布フロー

- `./gradlew assembleDebug` : `app/build/outputs/apk/debug/app-debug.apk` を生成し、そのまま `adb install -r` で検証。
- `./gradlew lint` : API レベル差分や未使用リソースを洗い出し、`docs/` の仕様と乖離が無いか確認。
- `./gradlew clean build` : Debug/Release を再生成して差分を吸収。
- `./gradlew assembleRelease` : release 署名 APK (`app/build/outputs/apk/release/app-release.apk`) を生成し、`apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` で証明書を確認後に `adb install -r` する。

### local.properties 設定 (AdLessTwitter 共有キーストア利用例)

`../AdLessTwitter` で既に作成済みのリリースキーストアを再利用する前提で、`launcher` 直下の `local.properties` に以下を追加する（例: `/home/kafka/projects/AdLessTwitter/keystore/adless-release.jks`）。

```
launcherReleaseStoreFile=/home/kafka/projects/AdLessTwitter/keystore/adless-release.jks
launcherReleaseStorePassword=your-store-pass
launcherReleaseKeyAlias=adlessShared
launcherReleaseKeyPassword=your-key-pass
```

AdLessTwitter と同じキーストアを使うことで証明書フィンガープリントを統一でき、インストール時の「パッケージが無効」エラーを防止できる。キーストアを別途保管する場合も同じキー名称を用い、端末へは release APK のみを配布する。

---

# 1. **機能要件（Functional Requirements）**

## 1.1 ホーム画面

### 表示要素

1. **上部：検索バー**

   * プレースホルダ：「アプリ・Webを検索」
   * 入力文字列を即時検索（インクリメンタル検索）
   * 検索対象：

     * インストールアプリ名
     * QuickAction（Google/Discord/Brave）
     * Web検索（Brave指定）
   * 検索結果は下部にドロップダウン表示

2. **中央：クイックアクション行（モジュール別）**

   * Google系（Calendar / Maps / Gmail）
   * Discord
   * Brave
   * 各アクションは「アイコン＋ラベル＋タップで起動」

3. **下部：アプリドロワーまたは“↑”ボタン**

   * タップでアプリ一覧画面へ遷移

4. **「よく使うアプリ」行（任意ON/OFF）**

   * 利用頻度 Top5 を自動表示
   * 左→右に降順

5. **壁紙の上にUI（透明背景）**

   * OS壁紙をそのまま表示
   * Material You 自動カラー反映（アクセント色）

---

## 1.2 アプリドロワー（全アプリ一覧）

### 動作

* PackageManager で
  `ACTION_MAIN` + `CATEGORY_LAUNCHER`
  を持つ Activity を列挙
* 50〜300アプリ環境で高速動作

### 仕様

1. グリッド表示（Adaptive: 72dp）
2. スクロールは LazyVerticalGrid（Compose）
3. 長押し → 「お気に入りにピン留め」
4. 検索時は自動フィルタ
5. ソート：

   1. 名前昇順（デフォルト）
   2. 使用頻度順（検索バー・設定で切り替え）

---

## 1.3 クイックアクション

**QuickActionProvider** を通じて
Google / Discord / Brave に対して “行動単位” の起動を提供する。端末に対象アプリが存在しないアクションは QuickActionRepository がリアルタイムに除外し、パッケージ追加/削除のブロードキャストを受けて即時同期する。

### 必須アクション（v1.0）

#### Google Calendar

* 今日の予定を表示
* 新規イベント作成（ACTION_INSERT）

#### Google Maps

* マップを開く（geo:0,0?q=）
* ナビ開始（検索バー入力 → Maps に投げる）

#### Gmail

* 未読インボックスを開く
* 新規メール作成（ACTION_SENDTO mailto:）

#### Discord

* Discordアプリ起動
* 「直近通知のチャンネル」へジャンプ（※v1はアプリ起動のみ）

#### Brave

* Brave起動
* 検索バー文字列 → Brave検索
  URL: `https://www.google.com/search?q=xxx`
* よく使う固定URLを Brave 指定で開く
  （例：X, Perplexity, VRChatログイン）

---

## 1.4 検索バー（全体の司令塔）

### 検索フロー

1. ユーザー入力イベントを受け取り
2. 以下の順で候補を生成し結合：

```
[1] アプリ名部分一致
[2] QuickAction のラベル部分一致
[3] Web検索候補（Braveで検索: “xxx”）
```

### 選択動作

* Enter or タップ → 最優先の QuickAction → 実行
* 候補がアプリのみ → アプリ起動
* 候補が0件 → 自動で Brave 検索

---

## 1.5 設定画面（最小構成）

* ダークモード（On/Off/System）
* アプリアイコンサイズ（S/M/L）
* 使用頻度学習（On/Off）
* よく使うアプリ表示（On/Off）
* クイックアクション表示順
  （Google / Discord / Brave の順番変更）
* デフォルトホームアプリ設定誘導（RoleManager）: 設定画面下部の「デフォルトホームに設定」ボタンから即時リクエスト

---

# 2. **非機能要件（NFR）**

## 2.1 パフォーマンス

* ホーム起動：**500ms以内**
* アプリ一覧表示：**1000ms以内**
* 検索バー：**50ms以内に候補反映**
* APKサイズ：**5〜10MB以下**
  * 2025-11-15時点で release APK は `./gradlew assembleRelease` による R8 + resource shrink + ja/en ロケールフィルタ適用で **約1.4MB**（`app/build/outputs/apk/release/app-release-unsigned.apk`）を維持。Material Icons は専用ベクターアセットへ置換し、余剰リソースは Gradle 側で除外済み。
* Compose baselineprofile により初回起動高速化

## 2.2 バッテリー

* 常駐サービスを持たない
* 使用頻度学習はアプリ起動時のみ反映（リアルタイム集計しない）

## 2.3 安定性

* セッションごとに ViewModel の状態を保存しない（初期ロードで再構築）
* 依存最小化（Coroutines + Hilt + DataStore のみ）

---

# 3. **アーキテクチャ設計**

## 3.1 パッケージ構造（最小かつ拡張可能）

```
app/
  MainActivity.kt
  launcher/
    LauncherViewModel.kt
    LauncherState.kt
    LauncherNavigation.kt
  data/
    AppRepository.kt
    UsageRepository.kt
    QuickActionRepository.kt
    SettingsRepository.kt
  model/
    InstalledApp.kt
    QuickAction.kt
    QuickActionProvider.kt
  modules/
    google/GoogleCalendarModule.kt
    google/GoogleMapsModule.kt
    google/GmailModule.kt
    discord/DiscordModule.kt
    brave/BraveModule.kt
  ui/
    home/HomeScreen.kt
    drawer/AppDrawerScreen.kt
    components/
       SearchBar.kt
       QuickActionRow.kt
       AppGrid.kt
  settings/
    SettingsScreen.kt
```

---

# 4. **データフロー（Data Flow）**

### 起動時

```
MainActivity → LauncherViewModel.init()
    → AppRepository.loadInstalledApps()
    → QuickActionRepository.loadAllProviders()
    → UsageRepository.loadStats()
    → SettingsRepository.loadSettings()
```

### ユーザーの行動時

* アプリ起動 → UsageRepository に「1回使用」記録
  → DataStore に翌回保存
  → よく使うランキングに反映

### 検索時

* ViewModel 内で検索 → フィルタ結果 → UI更新

---

# 5. **QuickActionProvider 設計（拡張の核）**

インターフェース：

```kotlin
interface QuickActionProvider {
    val id: String
    fun isAvailable(context: Context): Boolean
    fun actions(context: Context): List<QuickAction>
}
```

`QuickAction`：

```kotlin
data class QuickAction(
    val id: String,
    val label: String,
    val icon: Painter?,
    val action: () -> Unit
)
```

---

# 6. **AndroidManifest（HOMEランチャーとして認識）**

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

### Android 10+ の HOME Role 設定

```kotlin
val roleManager = context.getSystemService(RoleManager::class.java)
if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    startActivity(intent)
}
```

---

# 7. **セキュリティ要件**

* 特別な権限を要求しない（Notification Listener は v2 で検討）
* アプリ起動は PackageManager 経由（URIスキーム含む）
* 外部ネットワーク通信は不要（検索時のみ Intent によるブラウザ起動）

---

# 8. **開発ロードマップ（実装順）**

## ● Week 1：基盤構築

* プロジェクト生成（Kotlin + Compose + Hilt + DataStore）
* Manifest に HOME intent-filter
* AppRepository 実装（アプリ一覧取得）

## ● Week 2：ホーム画面 MVP

* SearchBar 作成
* QuickActionProvider 3種（Google/Discord/Brave）
* QuickActionRow 実装
* アプリ起動連携確認

## ● Week 3：アプリドロワー

* LazyVerticalGrid
* 長押しでピン留め
* 使用頻度学習追加
* “よく使うアプリ” 行追加

## ● Week 4：設定画面

* DataStore 保存
* ライト/ダーク/システム
* アイコンサイズ

## ● Week 5：UI調整 & 公開（内部向けビルド）

* Material You 対応
* アニメーション最適化
* baselineprofile

---

# 9. **拡張予定（v2〜v3）**

* Discord通知チャンネルジャンプ（Notification Listener）
* LLM要約カード（Inbox / Discord / Gmailの内容要約）
* 世界モデルUI（1画面内に情報を自動構成）
* Brave / Maps / Gmail の API 連携強化
