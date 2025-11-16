# data/local

- 端末内永続化を担当し、Room DB と Preferences DataStore を配置する。
- 上位レイヤーはこの階層の具体実装を `data/repo` 経由でのみ参照し、直接触れないようにする。
