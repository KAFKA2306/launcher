# Gemini フィードバックループ仕様

KafkaLauncher は端末内ログを 3 時間ごとに Gemini Pro 2.5 preview へ送り、Structured Output の推薦を `QuickActionRow` `FavoriteAppsRow` `AiRecommendationPreview` に反映する。`GeminiSyncWorker` `GeminiPayloadBuilder` `GeminiRecommendationStore` `AiRecommendationPreview` を含む最小構成を実装済みであり、端末内で完結したループを保持する。

## 目的と実装状況

- `GeminiWorkScheduler` がアプリ起動時と端末ブート後に `GeminiSyncWorker` を登録し、3 時間周期の WorkManager を維持する。
- `LauncherInitReceiver` が `BOOT_COMPLETED` を受信し、端末再起動後も Gemini 同期を自動復旧する。
- `LauncherViewModel` は `GeminiRecommendationStore` の Flow を取り込み、QuickAction/Favorite/UI 状態を常時更新する。
- `AiRecommendationPreviewButton` とカード UI は `HomeScreen` に常駐し、Gemini の最終更新時刻と内容を表示する。

## 最小構成

| レイヤー | 役割 | 主要 API |
| --- | --- | --- |
| config | `GeminiConfig` に周期・エンドポイント・モデル名・生成設定・ファイルパス・WorkName・APIキー保存先を集約。 | `GeminiConfig.periodHours` `GeminiConfig.endpoint` `GeminiConfig.apiKeyStoreFileName` |
| data | `ActionLogRepository` が DAO/ファイル、`GeminiRecommendationStore` が JSON ファイルを提供。 | `exportEvents(limit)` `statsSnapshot(limit)` `GeminiRecommendationStore.data` |
| domain | `GeminiPayloadBuilder` が UTC 正規化と JSON 生成、`RecommendActionsUseCase` がフォールバックを返す。 | `build(events, stats)` |
| worker | `GeminiSyncWorker` が `WorkManager` から実行され、Payload 生成→API 呼び出し→ストア更新を直列化。 | `doWork()` |
| remote | `GeminiApiClient` が `OkHttpClient` で 1 回の POST を送信し Structured Output を解析。 | `fetchRecommendations(payload, apiKey)` |
| store | `GeminiRecommendationStore` が `/files/config/gemini_recommendations.json` を直接読み書きし、`GeminiApiKeyStore` が EncryptedSharedPreferences で API キーを保持する。 | `GeminiRecommendationStore.data` `GeminiApiKeyStore.data` |
| launcher | `LauncherViewModel` が quick actions / stats / Gemini Flow を集約し UI state に落とし込む。 | `refreshGeminiOutputs()` |
| ui | `HomeScreen` の 3 ボタン行と `AiRecommendationPreview` が Gemini 状態を描画。 | `AiRecommendationPreview(state)` |

## 周期とトリガー

1. `GeminiWorkScheduler.schedule(context)` が `PeriodicWorkRequestBuilder<GeminiSyncWorker>` を `ExistingPeriodicWorkPolicy.UPDATE` で登録する。周期は `GeminiConfig.periodHours`、ネットワーク条件は `GeminiConfig.networkType` (`UNMETERED`) を使用する。
2. `LauncherInitReceiver` は `BOOT_COMPLETED` 受信時に同スケジューラを呼び出し、ユーザー操作なしでループを再開する。
3. Worker は `GeminiRecommendationStore.snapshot()` を読み、`generatedAt` との差分が 3 時間未満なら早期に `Result.success()` を返す。

## 前処理

1. `ActionLogRepository.exportEvents(GeminiConfig.payloadEventLimit)` と `statsSnapshot` が Room から直近ログと統計を読み込む。値は UTC に正規化して `GeminiPayloadBuilder` に渡す。
2. `GeminiPayloadBuilder` は `GeminiConfig.timeWindows`（weekday/weekend × morning/daytime/night）の定義を用い、各ウィンドウごとに `topActions` `topApps` `recentActionSequence` を JSON 化する。ログが空の場合は `statsSnapshot` の上位レコードをフォールバックとして挿入する。
3. 出力 JSON は `timeWindowStats` と `recentAnomalies`（実装では空配列）を含む `payload.json` 相当の文字列となり、HTTP Body に直接渡す。

`payload.json` 例:

```json
{
  "timeWindowStats": [
    {
      "windowId": "weekday_morning",
      "topActions": [
        {"id": "discord_dm", "count": 34, "successRate": 1.0},
        {"id": "calendar_today", "count": 29, "successRate": 1.0}
      ],
      "topApps": [
        {"packageName": "com.discord", "count": 21},
        {"packageName": "com.brave.browser", "count": 13}
      ],
      "recentActionSequence": ["calendar_today", "maps_commute", "discord_dm"]
    }
  ],
  "recentAnomalies": []
}
```

## Gemini API preview 呼び出し

- モデル: `gemini-2.5-pro-exp`
- エンドポイント: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro-exp:generateContent`
- `generationConfig`: `GeminiConfig.generationConfig`（temperature/topP/responseMimeType/ActionRecommendationSet schema）
- リクエスト構造: `contents[0]` に指示文、`contents[1].parts[0].text` に `payload.json` を JSON 文字列として埋め込む。
- API キー: 設定画面で入力した値を `GeminiApiKeyStore` が `GeminiConfig.apiKeyStoreFileName`（EncryptedSharedPreferences + MasterKey）に保存し、`GeminiSyncWorker` は `GeminiApiKeyStore.current()` から取得して空の場合は即終了する。
- レスポンス解析: `GeminiApiClient` が `candidates[0].content.parts[0].text` を JSON として `GeminiRecommendationJson.decode` へ渡し、欠損時は `null` を返す。

## 推薦保存と配信

1. Worker が受信した `GeminiRecommendations` を `GeminiRecommendationStore.update()` で `/files/config/gemini_recommendations.json` に保存する。JSON ファイルそのものが単一のソースとなり、他レイヤーは同ファイルのみを参照する。
2. `LauncherViewModel` は `GeminiRecommendationStore.data` を Flow で購読し、`quickActions`（抑止 ID 除外）、`recommendedActions`、`favoriteApps`、`AiRecommendationPreview`、`LauncherState.currentTimeWindowId`、`settings` 画面の最終更新表示を同時に更新する。
3. `globalPins` はアプリのお気に入りを先頭から埋めるリストとして扱い、`suppressions` は QuickAction を UI 全域で非表示にする。
4. `AiRecommendationPreview` は設定画面内のカードとして常時表示し、`windows` 行では QuickAction ラベル、`rationales` では対象 ID をラベル／ID で表示する。

## 画面挙動

- `HomeScreen` の上部は「アプリ一覧 / AI / 設定」の 3 ボタン構成で、AI ボタンと設定ボタンはいずれも `SettingsScreen` へ遷移する。
- `AiRecommendationPreview` カードは設定画面に配置し、`generatedAt` のローカル時刻表示、`window.id`、`primary/fallback` のラベル列、`rationales` の要約を同時に描画し、データが空でも最後のスナップショットを保持する。
- `QuickActionRow` には Gemini 推薦（`window.primaryActionIds` → 実在する QuickAction から最大 4 件）が表示され、欠損時は `RecommendActionsUseCase` のフォールバックを使用する。
- `FavoriteAppsRow` は `GeminiRecommendations.globalPins` → `PinnedAppsRepository` → 行動ログ順の優先順位で 5 件を決定する。
- 設定画面には `Gemini 最終更新` セクションと API キー入力欄を置き、`LauncherState.geminiApiKeyInput` を編集→「保存」で `GeminiApiKeyStore.save`、`isGeminiApiKeyConfigured` に応じて「削除」を制御する。

## 最小リリース手順

1. アプリ起動後に設定画面を開き、「Gemini APIキー」に自身のキーを貼り付けて保存する（EncryptedSharedPreferences に永続化され、アップデート後も維持される）。
2. アプリ起動または `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` で `GeminiWorkScheduler` が登録されていることを `adb shell dumpsys jobscheduler` で確認する。
3. `adb logcat | grep GeminiSyncWorker` で payload 生成→API POST→`GeminiRecommendationStore` 更新のログを確認する。
4. `adb shell run-as com.kafka.launcher cat files/config/gemini_recommendations.json` で JSON が更新されることを確認する。
5. 設定画面の Gemini プレビューカードで `QuickActionRow` と `FavoriteAppsRow` が Gemini の `globalPins`/`primaryActionIds` に従って更新されることを確認する。

以上の構成で Android 内のログ収集から Gemini 推薦表示までを閉じたループとして運用する。
