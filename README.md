# Daiflix

面向 Android TV 的影视聚合客户端。数据来自 VidHub 站点公开网页（MacCMS 模板），本仓库只包含客户端实现，不包含任何视频内容。

## 功能

- 首页推荐、Hero 海报、继续观看
- 分类浏览（电影 / 电视剧 / 综艺 / 动漫）
- 搜索：使用站点 `/ajax/suggest` 接口，电视端内置遥控器可操作的全屏键盘（支持拼音）
- 详情：多播放线路、选集、收藏、续播入口
- 播放：Media3 ExoPlayer，HLS，断点续播、自动下一集、倍速、弹幕
- 观看历史、收藏列表
- 设置：可配置数据源地址、弹幕透明度/字号、倍速、自动连播

## 技术栈

- Kotlin 2.0.21，Jetpack Compose for TV
- MVVM + StateFlow + Hilt
- Retrofit + OkHttp + Jsoup（HTML/JSON 解析）
- Room（观看历史 / 搜索历史 / 收藏）
- Media3 ExoPlayer + HLS
- Coil（豆瓣图片自动补 Referer）

## 构建

要求：JDK 17、Android SDK 35。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17   # macOS 示例
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`（包名 `com.y_zion.daiflix.debug`，可与正式包共存）。

单元测试与静态检查：

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

> 当前 AGP 8.7.3 自带的 lint 与 lifecycle 2.8.7 / Compose 1.7 的部分 UAST 检测器不兼容，`app/build.gradle.kts` 中禁用了 4 个会崩溃的检测器，其余检查正常执行。

## 签名发布

未配置签名时 `assembleRelease` 产出的是 unsigned APK。配置以下环境变量后会生成已签名包：

```bash
export DAIFLIX_KEYSTORE_FILE=/absolute/path/release.jks
export DAIFLIX_KEYSTORE_PASSWORD=...
export DAIFLIX_KEY_ALIAS=...
export DAIFLIX_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

R8 已开启（minify + shrinkResources），发版前请安装 release 包做一次完整回归。

## 数据源与配置

- 默认数据源：`https://vidhub3.top`，可在「设置 → 数据源地址」修改。
- 播放地址解析：`/vodplay/{id}-{线路}-{集数}.html` → iframe 中的 `u` 参数 → `/player/api/resolve.php?u=...` → HLS。
- 站点搜索页有人机验证，客户端改用无需验证的 `/ajax/suggest`，因此搜索结果按关键词匹配，不带分页。
- 豆瓣图片需要 `Referer: https://movie.douban.com/`，已在 Coil 图片客户端中处理。

## 已知限制

- 仅基于网页结构抓取，站点改版会导致解析失败；没有拿到官方 App 的原生 API（动态域名 + 签名 + JWT）。
- 分类页没有真正的下一页，客户端只加载第一页。
- 不支持下载、清晰度切换（站点只暴露单一 HLS 主播放列表）。
- 弹幕来自站点接口，内容与可用性由站点决定。
- 本轮修改未在真实电视上做安装/播放回归，发布前需人工验证。

## 免责声明

本项目仅用于学习与技术研究，不存储、不分发任何影视资源。数据与播放地址均来自第三方站点，请遵守当地法律与站点条款。
