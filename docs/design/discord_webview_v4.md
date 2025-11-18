# Discord WebView 仕様 v4.1 メモ

v4 要件を前提に、実装着手前に決め打ちしておくべき設計軸を整理する。Discord WebView／通知ミラーの全体仕様は本書と v4 定義書を併読すること。

## 1. URL 正規化キー

- `DiscordChannelKey` を `guildId`, `channelId`, `threadId?` の組で保持し、`DiscordChannel.id` と 1:1 対応させる。
- WebView から得た URL は `https://discord.com/channels/{guild}/{channel}` を基準に、以下の順で正規化する。
  1. クエリとフラグメントを除去。
  2. `@me` など特殊パスは `guildId = "@me"` として扱う。
  3. thread が含まれる `/threads/{thread}` などは `threadId` に格納し、存在しない場合は `null`。
- `ChannelUsageStats` は `channelKey` を主キーとし、open/post/notification 系カウンタは常に正規化済みキーを経由して増分する。
- `incrementPostCount(url)` など JS 側から渡る URL は Kotlin 側で同じ正規化ルールを適用してキー化する。

## 2. 既存 DiscordModule / DiscordShortcut との棲み分け

- 既存の `DiscordModule` は「ネイティブ Discord を開く」導線として維持し、`LauncherConfig.discordShortcuts` の Deep Link も引き続き公式アプリへ渡す。
- WebView 版は `DiscordWebModule`（仮）を追加し、`ActionType.DISCORD_WEBVIEW` で DiscordScreen を起動させる。QuickAccess 上でも WebView アクションは独立 ID を用意する。
- ユーザー設定で「ネイティブより WebView を優先する」といった切替を用意する場合も、既存 ID を潰さずに別設定キーで制御する。

## 3. 初期状態 UX

- `DiscordChannelStore` が空の場合、DiscordScreen は `https://discord.com/app` を即時ロードし、サイドパネルには「チャンネルを登録してください」CTA を表示する。
- Self/Mute DisplayName も初期は空リストとし、Discord 設定セクションに「登録するとランキングが自分向けになります」の案内を常設する。
- QuickAccess の Discord 枠は Favorites が 0 件でも表示し、初回のみ「チャンネル登録と通知連携でランキングが有効になります」ガイドを差し込む。

## 4. 通知アクセス権限フロー

- NotificationListenerService はマニフェスト登録し、初回起動時は待機。権限の案内は 2 箇所で行う。
  - DiscordInboxScreen 初回表示時に専用セクションを最上部に表示し、システム設定へ遷移するボタンを置く。
  - 設定画面の Discord セクションにも常時「通知アクセスが必要です」行を出し、状態に応じて遷移ボタン／許可済み表示を切り替える。
- 権限未許可時でも DiscordInboxScreen 自体は開け、リスト部分は「権限未許可」のプレースホルダーのみを描画する。

## 5. JS ブリッジ API

Kotlin → JS:

- `setMutedCanonicalNames(names: Array<String>)`
- `setSelfCanonicalNames(names: Array<String>)`
- `setSearchQuery(query: String)`

JS → Kotlin:

- `onPostDetected(url: String)`
- `onChannelUrlChanged(url: String)`
- `onBootstrapReady()`

上記以外の API を追加する場合は、本書に追記してから Kotlin/JS 双方を更新する。全呼び出しは正規化キー経由で ChannelUsageStats と連動させる。

## 6. ランキング設定

- `w_open`, `w_focus`, `w_post`, `w_notif` は `LauncherConfig` 配下の Discord 設定オブジェクトに定義し、初期値は v4 と同一（1/1/3/1）。
- 設定画面では「詳細設定」トグルを ON にしたときのみ重みを編集可能にし、通常ユーザーには固定値で提供する。
- QuickAccess の Discord ブロックは常にランキング順（favorite → score → lastActiveAt）で表示し、Servers/All モード側にはオプションの「ランキング順」トグルを設ける。

## 7. ログ／デバッグ境界

- Discord 関連の DataStore / DB ファイルは `discord/` 名前空間に分離し、`ActionLogFileWriter` や `logs_bundle.zip` 生成処理の収集対象から除外する。
- 通知ログや channel metadata は端末ローカル限定とし、サポートログの収集フローでは明示的にスキップする旨を docs に記載する。

これらを v4.1 の前提としておくことで、URL キー重複や権限フロー迷子による実装ブロッカーを避けられる。追記内容を実装フェーズの TODO に転記し、差分が出た場合は本書を起点に更新する。
