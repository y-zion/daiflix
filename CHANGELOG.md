# Changelog

All notable changes to Daiflix are documented here. This project follows
[Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`).

## [Unreleased]

### 新增

- 播放时保持屏幕常亮：播放路由挂载 `KeepScreenOn`（`View.keepScreenOn` 即 `FLAG_KEEP_SCREEN_ON`，无需 WakeLock），退出播放自动恢复，其他页面仍可正常休眠

### 修复（UI/UX 审计 2026-11-06 · P0/P1）

- **P0-1 历史页必崩**：`WatchHistory.id` 恒为 0，`LazyVerticalGrid` 报 `Key "0" was already used` → 历史列表改用 `vodId` 作 key，模型 id 由 `vodId` 派生
- **P0-2 搜索页布局**：屏幕键盘吃掉约 700px，结果区只剩 ~70px 且方向键出不了键盘 → 改为「左结果 + 右键盘」两栏；键盘底行 DOWN 进入结果首卡、结果首行 UP 回键盘底行、搜索提交后自动把焦点交给结果区
- **P0-3 详情页封面/背景为空**：详情解析新增多级图片回退（`data-src` / `data-original` / 多种 class → `og:image` / `twitter:image`），海报与背景补占位/错误色；保存观看记录时保留已有封面，避免解析失败把封面覆盖成空
- **P1-1 主按钮焦点不可见**：主/次按钮改用 `trackFocus` 驱动 + 外描边焦点环，首帧即显示
- **P1-2 导航栏焦点与选中不可区分**：焦点 = 半透明胶囊底 + 轻微放大，选中 = 品牌红下划线，两者可同时辨认
- **P1-3 首屏焦点样式不渲染**：新增 `RequestFocusWhenReady`（等首帧后再请求焦点）与 `rememberFocusState` / `trackFocus`，替换依赖 `collectIsFocusedAsState` 的写法；空态/错误态补上可聚焦入口，避免方向键失灵
- **P1-4 播放页 BACK 连退两层**：删除重复的 `BackHandler`，BACK 只由根 `onPreviewKeyEvent` 处理（面板 → 控制层 → 退出各一步），并抽出可单测的 `playerBackAction`
- **P1-5 详情页进入即滚动错位**：进入时焦点落在返回钮、滚动位置归零，DOWN 显式转到「播放」
- **P1-6 首页 Hero 图文错配**：文案与封面必须同源，无法对齐的通用促销 banner 直接丢弃，不再拿无关影片补位
- **P1-7 清除观看记录无危险语义**：新增 `DaiflixDangerButton`（红描边 + 红字，焦点红底白字 + 白焦点环）
- **P1-8 选集面板 chip 文字贴左上角**：chip 内布局补 `fillMaxSize()`，文字水平垂直居中（与详情页集数 chip 一致）

### 测试

- 新增 `PlayerBackActionTest`（5 例）覆盖 BACK 决策；`VodHtmlParserTest` 补充详情封面/背景回退 3 例

### 工程

- `Release` 工作流改为「版本号递增即自动发版」：推送到 `master` 后检测 `versionName`，高于最新 `v*` tag 时自动跑单测、构建签名 APK、打 tag 并发布 GitHub Release（详见 README「自动发版」）

## [2.0.0] — 2026-11-06

首次以 **Daiflix** 品牌开源发布。

### 新增

- 全新的仿 Netflix 电视 UI：全屏 Hero、横向海报流、统一的电视焦点系统（1.08 倍放大 + 2dp 白色描边）
- 自绘播放器控制层：可聚焦进度条、快退/快进 10 秒、播放/暂停、下一集、选集面板、换源面板、倍速、弹幕开关
- 播放器交互：任意按键唤出控制层，4 秒无操作自动隐藏；BACK 依次关闭面板 → 隐藏控制层 → 退出
- 品牌资产：方形「D」Logo、字标、TV Banner、自适应图标，以及 `docs/brand/` 下的高清素材
- Debug 包专属标识：Banner / 启动器图标右上角琥珀色 `DEBUG` 角标，应用内导航栏 `DEBUG` 标签，应用名 `Daiflix Debug`

### 变更

- 包名：`com.y_zion.daiflix` → `com.yzion.daiflix.tv`（debug 包为 `com.yzion.daiflix.tv.debug`，可与正式包共存）
- 版本号：`1.1.0` → `2.0.0`
- 配色与字体：近黑 `#0B0B0B` + 品牌红 `#E50914`，重新标定的电视字号与间距
- 设置页从手机 Material 组件迁移到电视友好组件，版本号改为运行时读取
- 首页 / 分类 / 详情 / 搜索 / 历史 / 收藏 全面重做布局与焦点顺序

### 修复

- 修复播放地址解析、详情 ID 映射、历史续播与搜索在部分数据下的问题（承接 `1.1.1` 之前的修复）
- 设置页版本号与 `build.gradle.kts` 不一致
