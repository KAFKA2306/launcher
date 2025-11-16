# Gemini フィードバックループ仕様

KafkaLauncher は端末内のログを 3 時間ごとにまとめ、無料の Gemini Pro 2.5 preview へ再スコアリングを依頼し、ホーム画面の `QuickActionRow` `FavoriteAppsRow` `AiRecommendationPreview` を差し替える。本仕様は Android 内部で完結する Worker / Store / UI 更新フローを定義する。

## 1. 周期とトリガー

- `GeminiSyncWorker` を `WorkManager` の `PeriodicWorkRequestBuilder`（3 時間周期、ネットワーク制約=UNMETERED）で登録する。
- 端末ブート時に `LauncherInitReceiver` が `WorkManager` 再登録を行い、外部トリガーなしでループが継続する。
- Worker は `ActionLogRepository.lastGeminiUpdate` を読み、直近 3 時間以内に更新済みならスキップフラグを返して即終了する。

## 2. 前処理

1. Worker が `ActionLogRepository.exportEvents()` と `ActionLogRepository.stats()` を同一スレッドで読み込み、UTC タイムスタンプに正規化する。
2. `GeminiPayloadBuilder` が 1 日を `weekday_morning (05-10)` `weekday_daytime (10-18)` `weekday_night (18-05)` `weekend_daytime (08-20)` `weekend_night (20-08)` の 5 つに分割し、それぞれの使用回数・最近のシーケンス・平均インターバルを算出する。
3. `QuickActionAuditLogger.snapshot()` が返す Intent 成功/失敗統計を突き合わせ、`successRate` を補完する。
4. Builder は JSON を文字列化し、HTTP Body としてそのまま Gemini へ送れる `payload.json` を生成する。

`payload.json` 例:

```json
{
  "timeWindowStats": [
    {
      "windowId": "weekday_morning",
      "topActions": [
        {"id": "discord_dm", "count": 34, "successRate": 1.0},
        {"id": "calendar_today", "count": 29, "successRate": 0.96}
      ],
      "topApps": [
        {"packageName": "com.discord", "count": 21},
        {"packageName": "com.brave.browser", "count": 13}
      ],
      "recentActionSequence": ["calendar_today", "maps_commute", "discord_dm"]
    }
  ],
  "recentAnomalies": [
    {"actionId": "maps_geo_search", "reason": "Intent failed 3 times consecutively"}
  ]
}
```

## 3. Gemini API preview 呼び出し

- モデル: `gemini-2.5-pro-exp`（無料プレビュー / Structured Output）
- エンドポイント: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro-exp:generateContent`
- `generationConfig`: `{"temperature":0.3,"topP":0.95,"responseMimeType":"application/json","responseSchema":ActionRecommendationSet}`
- `ActionRecommendationSet` スキーマ:

```json
{
  "type": "object",
  "properties": {
    "timeWindows": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "windowId": {"type": "string"},
          "primaryActionIds": {"type": "array", "items": {"type": "string"}, "maxItems": 4},
          "fallbackActionIds": {"type": "array", "items": {"type": "string"}, "maxItems": 4}
        },
        "required": ["windowId", "primaryActionIds"]
      }
    },
    "globalPins": {"type": "array", "items": {"type": "string"}, "maxItems": 6},
    "suppressions": {"type": "array", "items": {"type": "string"}, "maxItems": 6},
    "rationales": {
      "type": "array",
      "items": {"type": "object", "properties": {"targetId": {"type": "string"}, "summary": {"type": "string"}}, "required": ["targetId", "summary"]}
    }
  },
  "required": ["timeWindows"]
}
```

`GeminiApiClient` は `okhttp3.OkHttpClient` の `POST` を使い、`payload.json` を `contents[1].parts[0].text` にそのまま封入する。プレビュー API なので API キーだけで完結し、レスポンスが欠けても既存ビューは保持される。

## 4. アプリ内での適用パイプライン

1. Worker は Gemini 応答を `GeminiRecommendationStore`（`DataStore<Preferences>` ラッパー）へ保存する。ストアは `/files/config/gemini_recommendations.json` を単一情報源として持つ。
2. ファイル構造は次のように固定し、起動直後から `LauncherViewModel` が Flow を購読できるようにする。

```json
{
  "generatedAt": "2024-06-25T09:00:00Z",
  "windows": [
    {
      "id": "weekday_morning",
      "start": "05:00",
      "end": "09:59",
      "primaryActionIds": ["discord_dm", "calendar_today"],
      "fallbackActionIds": ["maps_commute", "brave_vrc"]
    }
  ],
  "globalPins": ["brave_search", "discord_dm"],
  "suppressions": ["maps_geo_search"]
}
```

3. `GeminiRecommendationStore.observe()` は `LauncherViewModel` へ Flow で繋ぎ、`QuickActionRepository.observe()` と合流させて `LauncherState.recommendedActions` / `LauncherState.favoritePins` / `LauncherState.aiPreview` を 1 つの更新ループにまとめる。
4. `globalPins` は `QuickActionRepository` の `priorityOverrides` を動的に書き換え、Compose 側の `QuickActionRow` が再コンポーズされた瞬間にカード順序が入れ替わる。`suppressions` で指定された ID は `LauncherConfig.blockedActionIds` と統合し、端末内で非表示処理を完結させる。
5. `FavoriteAppsRow` は `favoritePins` を最優先でスロットに配置し、余った枠だけを `ActionLogRepository.stats()` のアプリで埋める。`AiRecommendationPreview` は同じ `LauncherState.aiPreview` を使い、Gemini が Brave を 2 位に格上げした場合でもホーム画面上部のプレビューとカードの双方で即座に変化を確認できる。

### 4.1 画面レベルの変化

- ホーム画面の `QuickActionRow` は `LauncherState.currentTimeWindowId`（`RecommendActionsUseCase` が判定）に応じて `windows.primaryActionIds` を最大 4 件表示する。Gemini が `weekday_morning` を更新すると 1 フレーム以内でカードが差し替わる。
- `FavoriteAppsRow` は `globalPins` を左端から順に並べ、ピンが 3 件に満たない場合のみ ActionLog のトップアプリで補完する。Gemini が行動抑制した ID はここにも現れない。
- 最下段の `AiRecommendationPreviewButton` はアプリドロワーや設定ボタンと同列に配置され、押下時に `AiRecommendationPreview` を展開する。プレビューは `windows.primaryActionIds` と `globalPins` をタイムライン形式で描画し、Gemini 応答が欠けた場合も最後のスナップショットを維持する。
- 設定画面のプレビューカードは `gemini_recommendations.json.generatedAt` を表示し、ユーザーが次回の再スコアリング時刻を把握できるようにする。

### 4.2 例: 平日朝の差し替え

1. Gemini 応答で `weekday_morning.primaryActionIds` が `[discord_dm, calendar_today, maps_commute]` に変わる。
2. `LauncherViewModel` が `GeminiRecommendationStore` から新しい `windows` を受信し、`LauncherState.recommendedActions` を再計算。
3. Compose が再コンポーズし、QuickActionRow 左から Discord DM / 今日の予定 / 通勤経路に並び替わる。
4. `AiRecommendationPreview` も同じ配列で上段プレビューを更新し、`AiRecommendationPreviewButton` を押していなくても `globalPins` に Brave 検索が含まれていれば `FavoriteAppsRow` の 1 スロットが Brave に固定される。

`GeminiRecommendationStore` は常に最新スナップショットを保持するため、Worker の次回更新が遅延してもホーム画面は直近の推薦を描画し続ける。Android 外のオーケストレーションに依存せず、Gemini の応答状況にかかわらず安定した UI を提供できる。
