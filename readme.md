# XMage — Custom Build

* Custom XMage build based on the [official XMage project](https://github.com/magefree/mage).
* Fully compatible with the official server  

# Customizations vs the official build

* Downloads images automatically! (client)
* Docker buld (server)

## Downloads

### Release (client + server)

Use [xmage-launcher-qt](https://github.com/t-my/xmage-launcher-qt) to download and run xmage.

### Docker image (server only)

Published to GitHub Container Registry:

```
ghcr.io/t-my/mage:latest
ghcr.io/t-my/mage:xmage_1.4.58V1
```

## Running the server

```bash
docker run -d \
  --name xmage-server \
  -p 17171:17171 \
  -p 17179:17179 \
  -e XMAGE_DOCKER_SERVER_NAME="your.server.hostname" \
  -e JAVA_MAX_MEMORY="1024M" \
  -v /path/to/db:/xmage/db \
  ghcr.io/t-my/mage:latest
```

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `XMAGE_DOCKER_SERVER_ADDRESS` | `0.0.0.0` | Bind address inside the container |
| `XMAGE_DOCKER_SERVER_NAME` | `mage-server` | Hostname advertised to clients |
| `XMAGE_DOCKER_PORT` | `17171` | Primary server port |
| `XMAGE_DOCKER_SECONDARY_BIND_PORT` | `17179` | Secondary server port |
| `XMAGE_DOCKER_MAX_SECONDS_IDLE` | `600` | Idle timeout before auto-concede |
| `XMAGE_DOCKER_AUTHENTICATION_ACTIVATED` | `false` | Require user registration |
| `JAVA_MIN_MEMORY` | `256M` | JVM minimum heap |
| `JAVA_MAX_MEMORY` | `512M` | JVM maximum heap |

### Ports

Forward both `17171` and `17179` on your router if clients connect from outside your LAN.

### Data persistence

Mount `/xmage/db` to preserve the server database across restarts.

## Branching model

This repo tracks the official [magefree/mage](https://github.com/magefree/mage) releases:

- **`upstream`** — pinned to the latest official release tag (e.g. `xmage_1.4.58V1`)
- **`master`** — `upstream` + custom commits (Docker build, fixes)

### Syncing to a new upstream release

```bash
# Add upstream remote (one-time setup)
git remote add xmage git@github.com:magefree/mage.git

# Fetch the new release
git fetch xmage --tags

# Update upstream branch to the new tag
git checkout upstream
git reset --hard xmage_X.Y.ZVN
git push origin upstream --force

# Rebase custom commits onto new upstream
git checkout master
git rebase upstream
git push origin master --force-with-lease
```

Pushing to `master` triggers the GitHub Actions workflow, which builds the Maven project, publishes a Docker image, and creates a GitHub Release with the combined client+server zip.

## Building locally

```bash
docker build . -t xmage
docker run --rm -p 17171:17171 -p 17179:17179 xmage
```

## Credits

Based on [XMage](https://github.com/magefree/mage) by magefree, with Docker setup inspired by [xmage-docker](https://github.com/mage-docker/xmage-docker).
