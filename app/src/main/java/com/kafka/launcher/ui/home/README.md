# ui/home

- メインホーム画面のレイアウト。検索フィールド、QuickActionセクション、AIタブ、アプリグリッドを1カラムで構成する。
- `HomeScreen` は引数経由でイベントを受け取り、Gemini 推薦やお気に入りリストの表示だけに責務を限定する。
- 背景は LauncherConfig.homeBackgroundColor を Color 変換して塗り、システムバーとの階層差を確保する。
- 上部タブの文言は strings.xml の `ai_button` を含むリソースで統一し、「アプリ一覧 / AI / 設定」の3種のみを想定する。
