# data

- `local` が Room/DataStore など端末内ストレージの実装を提供する。
- `log` が QuickAction や ActionLog のファイル出力、`remote` が Gemini API クライアント、`store` が DataStore ラッパーを担当する。
- `quickaction` が AI 生成クイックアクションのカタログ JSON と統計を管理する。
- `repo` は UI からの唯一のデータアクセス窓口として、上記ソースをまとめて Flow/サスペンドAPIで返す。
