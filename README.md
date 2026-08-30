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
