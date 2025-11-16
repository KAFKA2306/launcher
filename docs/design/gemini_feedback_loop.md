# Gemini フィードバックループ仕様

KafkaLauncher は端末内ログを 3 時間単位で Gemini Pro 2.5 preview に送り、Gemini の推薦で `QuickActionRow` `FavoriteAppsRow` `AiRecommendationPreview` を差し替える構成を目指す。本仕様は Android 内だけで完結する最小フィードバックループを定義し、`GeminiSyncWorker` `GeminiPayloadBuilder` `GeminiRecommendationStore` `AiRecommendationPreview` などはまだ実装されていないことを明記する。

## 目的と実装状況

- app/src 以下に Gemini 連携用 Worker / Store / ViewModel 拡張 / Compose UI は存在しない。
- `GeminiSyncWorker` `GeminiPayloadBuilder` `GeminiRecommendationStore` `AiRecommendationPreview` は本仕様でのみ定義された概念であり、現行ビルドは Gemini 呼び出しやプレビュー UI を含まない。
- 設計変更時は README と本ドキュメントを同時更新し、未実装ステータスを保つ。

## 最小構成

| レイヤー | 役割 | 主要 API |
| --- | --- | --- |
| config | `GeminiConfig` に周期・エンドポイント・モデル名・DataStore パスを集約。 | `GeminiConfig(periodHours=3, endpoint=..., model=..., storePath=...)` |
| data | `ActionLogRepository` からイベント/統計、`QuickActionAuditLogger` から成功率を読み出す。 | `exportEvents()` `stats()` `snapshot()` |
| worker | `GeminiSyncWorker` が `WorkManager` 周期登録と `GeminiPayloadBuilder` を直列で実行。 | `PeriodicWorkRequestBuilder` |
| remote | `GeminiApiClient` が `OkHttpClient` POST を 1 回だけ送信。 | `generateContent` |
| store | `GeminiRecommendationStore` が `DataStore<Preferences>` で `/files/config/gemini_recommendations.json` を単一ソースにする。 | `observe()` `update()` |
| ui | `LauncherViewModel` が Flow を集約し `LauncherState` に反映。 | `LauncherState.recommendedActions` |

上記の役割以外の新規コンポーネントは追加しない。周期やモデル名などの設定値はすべて `GeminiConfig` から取得し、ハードコーディングを避ける。

## 周期とトリガー

1. `GeminiSyncWorker` を `PeriodicWorkRequestBuilder`（3 時間、`NetworkType.UNMETERED`）で登録する。周期は `GeminiConfig.periodHours` を使用する。
2. 端末ブート時に `LauncherInitReceiver` が `WorkManager` 再登録を行い、外部トリガー不要でループを継続。
3. Worker は `ActionLogRepository.lastGeminiUpdate` を読み、直近 3 時間以内ならスキップフラグを返し即終了する。

## 前処理

1. Worker が `ActionLogRepository.exportEvents()` と `ActionLogRepository.stats()` を UTC に正規化して読み込む。
2. `GeminiPayloadBuilder` が一日を `weekday_morning (05-10)` `weekday_daytime (10-18)` `weekday_night (18-05)` `weekend_daytime (08-20)` `weekend_night (20-08)` の 5 つに分割し、使用回数・最近シーケンス・平均インターバルを算出する。
3. `QuickActionAuditLogger.snapshot()` の成功/失敗統計を突き合わせて `successRate` を補完する。
4. Builder は JSON を文字列化し、HTTP Body にそのまま渡せる `payload.json` を生成する。

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

## Gemini API preview 呼び出し

- モデル: `gemini-2.5-pro-exp`（無料プレビュー / Structured Output）。
- エンドポイント: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro-exp:generateContent`。
- `generationConfig`: `{"temperature":0.3,"topP":0.95,"responseMimeType":"application/json","responseSchema":ActionRecommendationSet}`。
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

`GeminiApiClient` は `OkHttpClient` の `POST` を 1 回送信し、`payload.json` を `contents[1].parts[0].text` に封入する。プレビュー API なので API キーのみで完結し、レスポンスが欠けても既存ビューは保持される。

## 推薦保存と配信

1. Worker は Gemini 応答を `GeminiRecommendationStore`（`DataStore<Preferences>` ラッパー）へ保存する。ストアは `/files/config/gemini_recommendations.json` を単一情報源として保持する。
2. ファイル構造は次のとおり。

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

3. `GeminiRecommendationStore.observe()` は `LauncherViewModel` へ Flow で繋ぎ、`QuickActionRepository.observe()` と合流して `LauncherState.recommendedActions` `LauncherState.favoritePins` `LauncherState.aiPreview` を 1 つの更新ループにまとめる。
4. `globalPins` は `QuickActionRepository` の `priorityOverrides` を動的に書き換え、`suppressions` は `LauncherConfig.blockedActionIds` に統合する。
5. `FavoriteAppsRow` は `favoritePins` を最優先で埋め、残り枠のみを ActionLog 上位アプリで補完する。`AiRecommendationPreview` も同じ `LauncherState.aiPreview` を使い、Gemini が Brave を 2 位に格上げした場合でもホーム画面上部とカードの両方が同時に変化する。

## 画面挙動

- `QuickActionRow` は `LauncherState.currentTimeWindowId`（`RecommendActionsUseCase` が判定）に応じて `windows.primaryActionIds` を最大 4 件表示する。Gemini が `weekday_morning` を更新すると即座にカードが差し替わる。
- `FavoriteAppsRow` は `globalPins` を左端から配置し、3 件に満たない場合のみ ActionLog 上位アプリで補完する。`suppressions` 指定 ID は非表示を維持する。
- `AiRecommendationPreviewButton` はアプリドロワーと設定ボタンの 3 連ボタン中央に置き、`LauncherState.aiPreview.isExpanded` をトグルするのみで追加リクエストは発生しない。`AiRecommendationPreview` は `windows.primaryActionIds` を時刻順に並べ rationales を 1 行メモとして描画し、レスポンスが空でも最後のスナップショットを保持する。
- 設定画面プレビューカードは `gemini_recommendations.json.generatedAt` を表示し、再スコアリング時刻を把握できるようにする。

## 最小リリース手順

1. `GeminiConfig` を定義し、周期・モデル名・エンドポイント・ストアパス・API キーキー名を集中管理する。
2. `GeminiSyncWorker` を登録し、ActionLog と AuditLogger からの入力を `GeminiPayloadBuilder` に渡す。
3. `GeminiApiClient` で `generateContent` を呼び `GeminiRecommendationStore` へ保存する。
4. `LauncherViewModel` が `GeminiRecommendationStore.observe()` を取り込み、`LauncherState` を再計算する。
5. UI コンポーネントが Flow を購読し、`QuickActionRow` `FavoriteAppsRow` `AiRecommendationPreview` の 3 箇所を同時に更新する。

以上で Gemini 連携の最小フィードバックループを定義し、Android 内部のみで完結する実装計画とする。
