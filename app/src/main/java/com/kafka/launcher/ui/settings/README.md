# ui/settings

- 設定画面の Compose 実装。お気に入り表示トグル、ソート選択、Home Role 要求、Gemini 最終更新表示、Gemini API キー入力をまとめて描画する。
- 画面ロジックは ViewModel から渡された `Settings` と `NavigationInfo` を元に状態レスな UI を実現する。
