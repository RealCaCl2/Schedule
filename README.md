# 轻课表 (Schedule)

一个基于 Jetpack Compose 的 Android 课程表应用，支持通过强智教务系统网页导入课程，并在本地离线查看周课表与今日课表。

## 功能

- 周课表视图（按周切换）
- 今日课表视图
- 强智教务系统 WebView 导入
- 课程本地存储（Room）
- 学期与显示设置（DataStore）
- 导入与数据清理流程

## 技术栈

- Kotlin
- Jetpack Compose
- AndroidX Navigation Compose
- Room
- DataStore Preferences
- Jsoup
- Gradle (Kotlin DSL)

## 环境要求

- Android Studio（建议最新稳定版）
- JDK 21
- Android SDK（以本地 `local.properties` 为准）

## 本地构建

```bash
./gradlew assembleDebug
```



## 运行说明

1. 首次启动先配置学期参数与教务系统 URL。
2. 进入“导入课表”，在 WebView 中登录并打开学期理论课表页面。
3. 点击“提取课表”完成解析并导入本地数据库。

## 隐私与数据

- 课表与设置数据默认仅保存在本地设备。
- 登录由系统 WebView 管理，应用不主动收集账号密码文本。

## 许可证

本项目使用 Apache License 2.0，详见 [LICENSE](./LICENSE)。
