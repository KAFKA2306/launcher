# Kafka Launcher Design System

## ゴール
- Issue「UI/UX 改善仕様」で定義した 5 課題を数値化し、Color / Typography / Shape / Elevation の各トークンとして固定する。
- Config (`LauncherConfig`) で一元管理し、Compose Theme から参照することでハードコーディングとレイヤー差の揺れを防ぐ。

## Color Tokens
| Token | Config Key | 値 | 主な適用対象 |
| --- | --- | --- | --- |
| BaseBackground | `homeBackgroundColor` | `#0D1117` | 画面ベース。ダーク前提で全体を引き締める。 |
| SurfaceCard | `cardBackgroundColor` | `#1E242C` | セクションカード / 検索結果。 |
| SurfaceLow | `surfaceLowColor` | `#161B22` | 全アプリタイルや小型コンポーネント。 |
| SurfaceBorder | `surfaceBorderColor` | `#232A33` | タイル境界線。 |
| OnSurface | `sectionTitleColor` | `#E6EAF2` | セクション見出し / ラベル。 |
| OnSurfaceMuted | `sectionTitleVariantColor` | `#9DA7B8` | サブテキスト。 |
| PrimaryButton | `primaryButtonColor` | `#6C4DFF` | 主要アクション。 |
| OnPrimary | `primaryButtonContentColor` | `#FFFFFF` | Primary ボタン内のテキスト / アイコン。 |

## Typography Tokens
| Token | Config Key | 値 | 用途 |
| --- | --- | --- | --- |
| AppGridLabel | `appGridLabelFontSizeSp` `appGridLabelLineHeightSp` | `13sp / 18sp` | 全アプリラベル。`maxLines=2` `TextAlign.Center` `softWrap=true` `TextOverflow.Ellipsis` が必須。 |
| SectionTitle | `sectionTitleLineHeightSp` | `20sp` | セクション見出しの lineHeight を統一。 |

ラベル幅は `appGridLabelWidthDp = 68dp` で固定し、アイコン幅と中央寄せを一致させる。TextStyle は `Typography.appGridLabel` を共有。

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
- セクション上 8dp / 下 8dp、縦方向の並びも 8dp。
- グリッド縦横 8dp。
- セクションカード padding = 横12dp / 縦10dp。
- Home 画面外側の余白 = 8dp。

以上のトークンは必ず Config から取得し、Compose 側では再定義しない。新規セクションやカードを追加する際はこのドキュメントに追記した上で `LauncherConfig` を更新する。
