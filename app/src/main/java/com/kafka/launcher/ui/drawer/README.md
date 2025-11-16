# ui/drawer

- アプリドロワー画面のCompose実装をまとめ、検索や長押しアクションを `LauncherViewModel` から受け取って描画する。
- ホームと同じ `searchQuery` を共有するため、ステート管理は親ナビゲーションに委任しここでは UI ロジックのみに集中する。
