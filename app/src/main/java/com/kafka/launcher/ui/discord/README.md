# ui/discord

- Discord WebView まわりの ViewModel と Compose 画面を配置する。
- `DiscordViewModel` が `DiscordInteractor` の Flow をそのまま公開し、`DiscordScreen` や通知/設定用セクションが購読する。
