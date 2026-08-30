# E-Ink Launcher Reborn

专为墨水屏设备打造的极简启动器，基于 [E-Ink Launcher](https://github.com/nicehash/E-Ink-Launcher) 重构。

## 功能

- 自定义网格布局（行列数可调）
- 应用名称字体大小调整
- 按名称 / 安装时间 / 使用频率排序
- 隐藏或卸载应用
- 一键锁屏
- 一键开关 WiFi
- WiFi 名称显示 / 隐藏
- 自定义图标替换
- 内置 HTTP 文件传输（浏览器访问，无需 FTP 客户端）
- 状态栏显示控制
- 分隔线显示控制

## 下载

从 [Releases](https://github.com/ACG-Q/elinkLaun-Reborn/releases) 下载最新 APK。

## 构建

```bash
git clone https://github.com/ACG-Q/elinkLaun-Reborn.git
cd elinkLaun-Reborn
./gradlew assembleRelease
```

APK 输出路径：`app/build/outputs/apk/release/`

## 在线构建（GitHub Actions）

本仓库配置了 GitHub Actions，推送到 `master` 分支或推送 `v*` 标签时会自动构建 Release APK：

| 触发方式 | 结果 |
|----------|------|
| 推送到 `master` 分支 | APK 上传到 Actions Artifacts（保留 30 天） |
| 推送 `v*` 标签（如 `v0.2.0`） | APK 上传到 Artifacts + 发布到 GitHub Releases |
| 手动触发 | 进入 Actions 页面点击 **Run workflow** |

```bash
# 推送到 master 触发构建
git push origin master

# 打 tag 触发构建并发布 Release
git tag v0.2.0
git push origin v0.2.0
```

构建产物可在 GitHub 仓库的 **Actions** 页面下载。

### Fork 后使用自己的签名构建

如果你 fork 了本仓库并希望通过 GitHub Actions 用**自己的签名**构建，可以修改 `.github/workflows/build.yml`：

1. 删除或注释掉 **Generate CI keystore** 步骤
2. 添加 **Decode keystore from secrets** 步骤，将你的 keystore base64 通过 GitHub Secrets 传入：

```yaml
- name: Decode keystore from secrets
  run: |
    echo "$KEYSTORE_BASE64" | base64 -d > "$HOME/release-key.jks"
    echo "KEYSTORE_FILE=$HOME/release-key.jks" >> $GITHUB_ENV
    echo "KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD" >> $GITHUB_ENV
    echo "KEY_ALIAS=$KEY_ALIAS" >> $GITHUB_ENV
    echo "KEY_PASSWORD=$KEY_PASSWORD" >> $GITHUB_ENV
  env:
    KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

然后在仓库 **Settings → Secrets and variables → Actions** 中添加以下 4 个 secret：

| Secret 名称 | 值 |
|-------------|-----|
| `KEYSTORE_BASE64` | 你的 keystore 文件 base64 编码 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | key 别名（如 `release`） |
| `KEY_PASSWORD` | key 密码 |

> 生成 base64：`base64 -i my-release-key.jks | tr -d '\n'`（Linux/macOS）或 `[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks"))`（PowerShell）

## 自定义图标

长按应用图标可查看包名。将图标文件重命名为对应包名，放到以下目录：

```
Documents/E-Ink Launcher/icon/
```

内置功能图标文件名：

| 功能 | 文件名 |
|------|--------|
| 一键锁屏 | `E-ink_Launcher.Lock.png` |
| WiFi 开启 | `E-ink_Launcher.WifiOn.png` |
| WiFi 关闭 | `E-ink_Launcher.WifiOff.png` |

本项目 `icons/` 目录提供了一套墨水屏风格图标，可直接使用。

## 图标预览

| 锁屏 | WiFi 开 | WiFi 关 | KOReader | 设置 | 信息 | 电话 | 通讯录 | EinkBro |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Lock | WifiOn | WifiOff | KOReader | Settings | Messages | Phone | Contacts | EinkBro |

## 设备兼容

- 最低 Android 版本：4.0 (API 14)
- 推荐用于 YotaPhone、海信墨水屏、Boox 等 E-Ink 设备

## 开发者

- 原作者：Modificator
- 重构：六记 & AI (MiMo V2.5 & Opencode)

## 许可

本项目基于原 E-Ink Launcher 重构，遵循相应开源协议。
