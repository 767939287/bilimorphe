# 开发路线图（2026-11-27 版）

> 基线：BiliRoamingX 1.23.3 源码 + 自建 LSPosed 模块（app.bilimorphe）
> 目标 APK：tv.danmaku.bili 9.20.0（仅 arm64-v8a）

## 已完成

| # | 项目 | 产物 |
|---|---|---|
| 1 | 拉取 1.23.3 源码并 fork 进仓库 | `integrations/{app,dummy,extend,ksp}` + `:xposed` |
| 2 | 接入 xposed 模块 | `libxposed 102`、`META-INF/xposed/*`、AGP 9 兼容开关 |
| 3 | 运行时桥接 | `BridgePatch.kt`（ClassLoader / Utils.context / attached / CrossProcessPreferences） |
| 4 | OkHttp hook 层 | `OkHttpHookPatch.kt`（DexKit: returnType=okhttp3.Response + paramCount=0 + "Canceled"） |
| 5 | CI 出包验证 | `.github/workflows/build.yml` → artifact `bilimorphe-apk` |
| 6 | hook 点清单 | `docs/hook-points.md`（83 补丁）+ `docs/data/injected-refs.txt`（359 注入点） |

## 待办

| # | 项目 | 内容 | 依赖 |
|---|---|---|---|
| 7 | 最小设置入口 | 反射枚举 `Settings` 生成 Boolean 开关列表；`tw/hk/cn/th_server`、`upos_host` 五个文本输入；LSPosed RemotePreferences 跨进程同步 | 无 |
| 8 | 实机验证 DexKit 定位 | 装 APK → LSPosed 授权 tv.danmaku.bili → 查看 `BiliMorphe` 日志是否出现 `network entry point:` | MuMu 12（需 Magisk+Zygisk）或 arm64 真机 |
| 9 | 移除临时硬开开关 | 删 `BridgePatch.enableTemporaryDefaults()` | 7 完成 |
| 10 | A 类 13 个网络功能接入开关 | 区域解锁、番剧/剧集/media/season、评论 URL、UPOS、分享、泰国评论、Brotli、ProtoBuf、Config、BLKV、BLRoute、频道模型修正、缓存重定向 | 7 |
| 11 | B 类净化隐藏（~15） | 屏蔽 UP 推荐广告、隐藏关注按钮、移除大会员横幅、视频弹窗净化、直播弹窗净化、搜索净化、去直播横幅、右侧按钮定制等 | 8 |
| 12 | B 类内容过滤（~8） | 评论（关键词/类型）、动态、首页推荐、热门、搜索类型/关键词、故事 | 8 |
| 13 | B 类播放器与 UI（~15） | 播放器定制、首页/抽屉/底栏/tab/我的/空间/直播间/视频详情定制、深色模式、DPI、显示尺寸、文本折叠、字幕样式 | 8 |
| 14 | B 类杂项（~10） | 崩溃日志、通知图标、数字格式化、自动点赞、复制增强、卸载保留数据、多窗口伪装、分 P 评论跳转 | 8 |
| 15 | 更新 README 与功能清单 | 可用功能、设置说明、构建方式、致谢 | 10-14 |

## 执行顺序

```
7 → 8（硬门槛）→ 10 → 11 → 12 → 13 → 14 → 9/15
```

## 关键事实备忘

- 区域解锁 = 填「区域服务器地址」（tw/hk/cn/th_server）+ 客户端伪装 Pink；**不是代理**；1.28.6 改为「地区代理」，按决定保留 1.23.3 方式
- 1.23.3 的 83 个补丁里有 12 个是 ReVanced 专用脚手架（Integrations/Config/修饰符/so 修补等），LSPosed 方案不需要
- 1.28.6 的 `classes35.dex` 入口已混淆（`kofua/*`），不可直接复用
- 修改版里的 `fakeSignatures` 是「绕过签名校验」的功能，LSPosed 方案不改 APK，天然不需要
- 设备限制：B 站只有 arm64-v8a；MuMu 12 可跑（DexKit 有 x86_64 so），但需 Magisk+Zygisk，且视频解码靠转译较慢

## 环境

- 本地：JDK 21、`/opt/android-sdk`（platform 37.2）、Gradle 9.9.2、代理 `192.168.50.15:1080`
- 构建：`./gradlew :xposed:assembleDebug`；`local.properties` 写 `sdk.dir=/opt/android-sdk`
- 已知坑：`.gitignore` 用 `build/`；`android.builtInKotlin=false` + `android.newDsl=false`；`compileSdkVersion("android-37.2")`
