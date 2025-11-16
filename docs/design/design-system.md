# Kafka Launcher Design System

## ゴール
- Issue「UI/UX 改善仕様」で定義した 5 課題を数値化し、Color / Typography / Shape / Elevation の各トークンとして固定する。
- Config (`LauncherConfig`) で一元管理し、Compose Theme から参照することでハードコーディングとレイヤー差の揺れを防ぐ。

## Color Tokens
| Token | Config Key | 値 | 主な適用対象 |
| --- | --- | --- | --- |
| BaseBackground | `homeBackgroundColor` | `#F4F6FA` | 全画面の背景。ΔL ≒ 9 でカード層との差を担保。 |
| SurfaceCard | `cardBackgroundColor` | `#FFFFFF` | セクションカード（おすすめ / 最近 / よく使う / クイックアクション / 検索結果）。 |
| SectionTitle | `sectionTitleColor` | `#2A2E39` | セクション見出し。 |
| PrimaryButton | `primaryButtonColor` | `#6C4DFF` | ドロワー / AI / 設定ボタンなど主要アクション。 |
| OnPrimary | `primaryButtonContentColor` | `#FFFFFF` | Primary ボタンのテキスト / アイコン。 |

## Typography Tokens
| Token | Config Key | 値 | 用途 |
| --- | --- | --- | --- |
| AppGridLabel | `appGridLabelFontSizeSp` `appGridLabelLineHeightSp` | `12sp / 16sp` | 「すべてのアプリ」グリッドのラベル。`maxLines=2`, `TextAlign.Center`, `softWrap=true`, `TextOverflow.Ellipsis` を必須条件として適用。 |
| SectionTitle | `sectionTitleLineHeightSp` | `20sp` | セクション見出しの lineHeight を統一。 |

ラベル幅は `appGridLabelWidthDp = 68dp` で固定し、アイコン幅と中央寄せを一致させる。全 TextStyle は `Typography` 拡張 (`appGridLabel`, `sectionTitle`) 経由で利用する。

## Shape Tokens
- 角丸は `cornerRadiusDp = 12dp` を共通値とし、Card / Button / BottomSheet / 検索結果リストに適用する。
- Button の shape は MaterialTheme `shapes.medium`、Card は `shapes.large` へ同値を割当てる。

## Elevation Tokens
| コンポーネント | Elevation |
| --- | --- |
| セクションカード (通常表示) | `2dp`
| プライマリアクションボタン | `3dp`（`pressed=1dp`）
| フローティング / モーダルメニュー | `8dp`

## レイアウト/スペーシング（参照値）
- セクション上 24dp / 下 16dp、グリッド間隔 16dp。
- カード内パディング 16dp、ラベル上下 8dp を推奨。

以上のトークンは必ず Config から取得し、Compose 側では再定義しない。新規セクションやカードを追加する際はこのドキュメントに追記した上で `LauncherConfig` を更新する。
