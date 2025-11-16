# data/system

- 端末やOSに依存する情報を取得する層。現在は `NavigationInfoResolver` が `Settings.Secure` とメーカー情報から操作モードを判定する。
- 実際の UI は `LauncherViewModel` を介して通知され、ここでは純粋にシステムAPIだけに集中する。
