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

```bash
# 推送到 master 触发构建
git push origin master

# 打 tag 触发构建并发布 Release
git tag v0.2.0
git push origin v0.2.0
```

构建产物可在 GitHub 仓库的 **Actions** 页面下载。

### Fork 后使用自己的签名构建

如果你 fork 了本仓库并希望通过 GitHub Actions 用**自己的签名**构建，需要配置以下 4 个 GitHub Secrets：

#### 步骤 1：生成签名密钥

```bash
keytool -genkeypair -v \
  -keystore my-release-key.jks \
  -alias release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <你的密码> -keypass <你的密码> \
  -dname "CN=你的名字, OU=你的组织, O=你的公司, L=城市, ST=省份, C=国家代码"
```

#### 步骤 2：转为 Base64

**Linux / macOS：**
```bash
base64 -i my-release-key.jks | tr -d '\n' > keystore_base64.txt
```

**Windows（PowerShell）：**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks")) | Set-Content keystore_base64.txt
```

#### 步骤 3：添加 Secrets

进入你的 fork 仓库 → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**，添加以下 4 个 secret：

| Secret 名称 | 值 |
|-------------|-----|
| `KEYSTORE_BASE64` | 上一步生成的 base64 字符串 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | `release` |
| `KEY_PASSWORD` | key 密码 |

> 如果只配置了 `KEYSTORE_BASE64` 而**没有配置其他 3 个**，GitHub Actions 会使用默认密码 `android` 和别名 `release`（仅 CI 临时签名，不推荐用于正式发布）。

#### 步骤 4：触发构建

配置完成后，推送到 `master` 或打 `v*` 标签即可触发带签名的构建。

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
