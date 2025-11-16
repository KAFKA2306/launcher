# app モジュール概要

## 署名設定

1. `../AdLessTwitter` で使用しているリリースキーストアを共有する。
2. `launcher/local.properties` に以下を追記する。

```
launcherReleaseStoreFile=/home/kafka/projects/AdLessTwitter/keystore/adless-release.jks
launcherReleaseStorePassword=your-store-pass
launcherReleaseKeyAlias=adlessShared
launcherReleaseKeyPassword=your-key-pass
```

3. `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` で署名を確認し、`adb install -r app/build/outputs/apk/release/app-release.apk` で端末へ配布する。

## ビルドコマンド

- `./gradlew assembleDebug`
- `./gradlew lint`
- `./gradlew assembleRelease`
- `./gradlew clean build`

## 出力確認

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`（リポジトリには含めず、各自で keystore を指定して生成する）
