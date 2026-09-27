# Kimi 社区复刻 Android 应用

> ⚠️ **免责声明**：本项目仅用于技术交流学习，请勿用于非法用途。打开后请在 24 小时内删除，否则后果自负，与本人无任何关系。本应用为社区复刻学习项目，与 Kimi 官方无任何关联。

## 项目简介

基于 Kimi 社区 API（kimi_3.1.0）复刻的 Android 客户端，使用 Kotlin + Jetpack Compose (Material 3) 开发。

## 功能特性

### 已实现
- ✅ 底部导航 4 Tab（首页/搜索/消息/我的）
- ✅ 首页信息流（推荐/关注，下拉刷新，自动分页加载）
- ✅ 搜索功能（动态/用户/话题，自动分页加载）
- ✅ 作品详情页（评论列表，自动分页加载）
- ✅ 评论系统（发表评论、点赞、楼中楼回复、点击用户进入主页）
- ✅ 消息通知（列表、已读、自动分页加载）
- ✅ 用户主页（作品列表，自动分页加载）
- ✅ 关注/取关、屏蔽用户、举报用户
- ✅ 点赞动态、收藏动态
- ✅ 作品预览自适应（CDN内嵌/会话享/图片/多图横滑）
- ✅ WebView 登录（从 localStorage 获取 token，手动保存登录态）
- ✅ Token 自动刷新（API 刷新失败时后台启动透明 WebView 获取新 token）
- ✅ 错误码自动应对（401/403 刷新重放，429/503/504 退避重试）
- ✅ 7 色主题选择器（实时生效）
- ✅ 深色/浅色/跟随系统模式
- ✅ 调试模式（API 请求日志浮动面板）
- ✅ 全局崩溃日志捕获
- ✅ 应用更新检测（正式版/测试版渠道选择）
- ✅ 完整免责声明

### 技术栈
- **语言**：Kotlin
- **UI**：Jetpack Compose (Material 3)
- **网络**：Retrofit + OkHttp
- **存储**：DataStore
- **异步**：Kotlin Coroutines + Flow
- **图片加载**：Coil
- **最低 SDK**：31 (Android 12)
- **目标 SDK**：35 (Android 15)

## 项目结构

```
app/src/main/java/com/kimi/community/
├── KimiApp.kt                    # Application 类（全局崩溃捕获）
├── MainActivity.kt               # 主 Activity（底部导航）
├── data/
│   ├── api/                      # API 层（Retrofit Service、拦截器、错误处理）
│   ├── model/                    # 数据模型
│   ├── notification/             # 通知轮询（WorkManager）
│   ├── prefs/                    # DataStore 偏好存储
│   └── repository/               # 数据仓库
├── ui/
│   ├── auth/                     # 登录/Token 刷新
│   ├── components/               # 通用组件
│   ├── debug/                    # 调试面板
│   ├── detail/                   # 作品详情
│   ├── home/                     # 首页/搜索
│   ├── messages/                 # 消息通知
│   ├── profile/                  # 用户主页
│   ├── settings/                 # 设置
│   ├── theme/                    # 主题
│   └── webview/                  # WebView 封装
```

## 构建说明

### 环境要求
- JDK 17
- Android SDK (compileSdk 35)
- Gradle 8.7

### 构建命令
```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

### 构建产物
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## API 参考

本项目严格遵循 [Kimi API 文档](Kimi_API_zh.md) 接口定义，包含 69 个服务、260+ 方法。

### 主要 API 服务
- `moment.v1.FeedService` — 信息流
- `moment.v1.CommentService` — 评论
- `moment.v1.NotificationService` — 消息通知
- `moment.v1.UserService` — 用户
- `moment.v1.FollowService` — 关注/屏蔽
- `moment.v1.MuteService` — 屏蔽内容
- `moment.v1.ComplaintService` — 举报
- `moment.v1.SearchService` — 搜索
- `account.gateway.v1.AuthService` — 鉴权

## 免责声明

```
本项目仅用于技术交流学习，请勿用于非法用途。
打开后请在 24 小时内删除，否则后果自负，与本人无任何关系。
本应用为社区复刻学习项目，与 Kimi 官方无任何关联。
```

## 许可证

本项目仅用于学习研究，不提供任何形式的许可证。

## 版本历史

- **v3.1.0** — 分页加载修复、收藏动态、Token 后台自动刷新、界面可视度优化、作品预览自适应、GitHub 链接、更新检测、渠道选择
- **v3.0.3** — 崩溃防御加固 + 全局崩溃日志捕获
- **v3.0.2** — API 响应格式修复（无 envelope）+ 主题色并发收集修复
- **v3.0.1** — 登录态手动保存 + token 格式校验收紧
- **v3.0.0** — 按新 API 文档重构全部接口 + 真实图标 + 7 色主题
- **v2.0.0** — 评论系统 + 消息通知 + 用户主页 + 设置
- **v1.0.0** — 基础框架 + 首页信息流 + WebView 登录
