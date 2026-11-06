# Changelog

All notable changes to Daiflix are documented here. This project follows
[Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`).

## [Unreleased]

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
