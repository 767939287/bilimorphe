# Hook 点清单（1.23.3 源码 → B 站 9.20.0 运行时模块）

> 生成日期：2026-11-27　｜　下一步执行者：明天继续

## 0. 结论摘要

| 项目 | 数值 |
|---|---|
| 1.23.3 补丁（Patch 对象） | 83 |
| 1.23.3 指纹（Fingerprint） | 101 |
| 修改版新增类 | 1,184（kofua 1125 + settings 57 + protobuf 2） |
| 修改版中引用注入类的宿主文件 | 359 |
| 已由运行时网络桥接覆盖 | 30 个 OkHttp hook（1.23.3 OkHttpPatch.hooks） |

## 1. 已完成（M1）

| 模块 | 文件 | 说明 |
|---|---|---|
| 入口 | `xposed/.../MainHook.kt` | libxposed onPackageReady |
| 引擎 | `xposed/.../PatchEngine.kt` | classLoader / findClass / findMethod |
| 桥接 | `xposed/.../patch/BridgePatch.kt` | 宿主 ClassLoader 注入、Utils.context、attached、CrossProcessPreferences |
| 网络 | `xposed/.../patch/OkHttpHookPatch.kt` | DexKit 定位 getResponseWithInterceptorChain，请求/响应转发给 integrations |
| 注册 | `META-INF/xposed/*` | minApiVersion=101, staticScope=true, scope=tv.danmaku.bili |
| CI | `.github/workflows/build.yml` | assembleDebug + artifact |

## 2. 待办：按功能分批（优先级从高到低）

### A. 网络/内容类（多数已通过 OkHttp 桥接生效，只需开开关）

| # | 功能 | 补丁对象 | 说明 |
|---|---|---|---|
| A1 | 集成BLKV | `BLKVPatch` | Blkv |
| A2 | 集成Brotli | `BrotliPatch` | Brotli |
| A3 | 使 ProtoBuf toString 打印更具可读性 | `ProtoBufPrintPatch` | ProtoBuf print |
| A4 | 通用阿里Fastjson反序列化数据修改 | `JSONPatch` | Json |
| A5 | 首页推荐流hook | `PegasusPatch` | Pegasus hook |
| A6 | OkHttp网络请求响应Hook | `OkHttpPatch` | OkHttp |
| A7 | 哔哩哔哩页面路由修改 | `BLRoutePatch` | BL route intercept |
| A8 | 禁止泰区评论 | `BlockThaiCommentPatch` | Block thai comment |
| A9 | 缓存行为重定向，允许外部工具直接下载 | `CacheRedirectPatch` | Cache redirect |
| A10 | 频道相关 model 类构造修复(官方 BUG) | `ChannelModelFixPatch` | Channel model fix |
| A11 | 分享处理相关补丁 | `SharePatch` | Share |
| A12 | 自定义更新辅助补丁 | `UpgradePatch` | Upgrade |
| A13 | 屏蔽评论关键词搜索功能 | `CommentReplyUrlPatch` | Comment word search url |

### B. 行为/UI 类（需要逐个 DexKit 定位宿主方法）

| # | 功能 | 补丁对象 | 说明 |
|---|---|---|---|
| B1 | 屏蔽UP主推荐广告 | `UpRcmdAdsPatch` | Block up recommend ads |
| B2 | 不显示关注按钮 | `HideFollowButtonPatch` | Hide follow button |
| B3 | 移除我的页面大会员横幅 | `RemoveVipSectionPatch` | Remove vip section |
| B4 | Config hook | `ConfigPatch` | Config |
| B5 | 自由复制补丁 | `CopyEnhancePatch` | Copy enhance |
| B6 | 我的页面深色模式切换弹框确认 | `DarkSwitchPatch` | Dark switch |
| B7 | 我的页面移至侧滑栏辅助补丁 | `DrawerPatch` | Drawer |
| B8 | 集成获取Config方法 | `ConfigPatch` | Config integration |
| B9 | 自定义dpi辅助补丁 | `DpiPatch` | Dpi |
| B10 | Integrations | `IntegrationsPatch` | Integrations |
| B11 | 查找网络请求签名及获取appKey方法 | `LibBiliPatch` | Lib bili |
| B12 | 代理部分MainActivity方法补丁 | `MainActivityPatch` | Main activity patch |
| B13 | 修改类或类成员修饰符，方便操作 | `ModifyModifierPatch` | Modify modifier |
| B14 | Anti RecyclerView obfuscation | `NormalizeRecyclerViewPatch` | Anti RecyclerView obfuscation |
| B15 | 原生音乐通知样式 | `MusicNotificationPatch` | Music notification |
| B16 | 附加更多搜索类型补丁 | `AppendExtraSearchTypePatch` | Search type |
| B17 | meta-data 附加原始签名信息，允许 LSPatch 及类似物处理过的 APP 获取本 APP 的原始签名 | `AppendSignatureInfoPatch` | Append signature info for LSPatch |
| B18 | 视频自动点赞补丁 | `AutoLikePatch` | Auto like |
| B19 | 放宽日志打印级别 | `BLogPatch` | BLog |
| B20 | 调整部分布局UI | `BiliLayoutAdjustPatch` | Bili adjust layout (codes) |
| B21 | 调整部分布局UI | `BiliLayoutAdjustResourcesPatch` | Bili adjust layout (resources) |
| B22 | so库修补（目前绕过了签名校验） | `BiliLibraryPatch` | Bili library patch |
| B23 | APP设置部分页面字体大小不统一修复 | `BiliSettingPreferenceLayoutPatch` | Fix app setting preference layout |
| B24 | 崩溃日志记录 | `CrashHandlerPatch` | Crash handler |
| B25 | 自定义启动图 | `CustomSplashPatch` | Custom splash |
| B26 | 播放页伪装未处于多窗口模式 | `FakeNotInMultiWindowPatch` | Fake not in multi window mode |
| B27 | 禁止自动勾选订阅合集 | `FavFolderDialogPatch` | Forbid auto subscribe |
| B28 | 禁止直播间点小窗播放时自动开启“播放被中断时自动小窗播放” | `ForbidLiveRoomAutoFloatPatch` | Forbid live room auto float |
| B29 | 禁止直播间向左滑动弹出抽屉 | `ForbidLiveRoomSlideLeftPatch` | Forbid live room slide left |
| B30 | 禁止上下滑动切换直播间 | `ForbidSwitchLiveRoomPatch` | Forbid switch live room |
| B31 | 允许分P及交互视频评论时间点可点击 | `ForceCommentNavigablePatch` | Force comment time navigable |
| B32 | 注入可访问私有目录的 DocumentsProvider | `InjectDataProviderPatch` | Inject DocumentsProvider |
| B33 | 向 AndroidManifest.xml 注入更多可处理的 deep link | `InjectMoreSchemePatch` | Inject more scheme |
| B34 | APP卸载时保留数据 | `KeepDataWhenUninstallPatch` | Keep data when uninstall |
| B35 | 禁用直播间双击点赞 | `LiveRoomPatch` | Disable live room double click |
| B36 | 为部分 TextView 添加可选择属性，方便复制 | `MakeTextSelectablePatch` | Make text selectable |
| B37 | 移除管理存储空间入口 | `NoManageSpaceActivityPatch` | Remove managerSpaceActivity |
| B38 | 通知图标修复 | `NotificationPatch` | Notification icon |
| B39 | 数字格式化补丁 | `NumberFormatPatch` | Number format |
| B40 | 适配 Android 13 按应用设定语言特性，允许单独设置本 APP 的语言偏好 | `PerAppLanguagePatch` | Per app language |
| B41 | Play版 AndroidManifest 声明缺失补全 | `PlayManifestCompatibilityPatch` | Play manifest compatibility |
| B42 | 禁止自动转到动态 | `PublishToFollowingPatch` | Disable auto select |
| B43 | 保存评论图片补丁 | `SaveCommentImagePatch` | Save comment images |
| B44 | 禁用青少年模式弹窗 | `TeenagerModePatch` | Teenager mode |
| B45 | 文本折叠控制 | `TextFoldPatch` | Text fold |
| B46 | 试用画质辅助补丁 | `TrialQualityPatch` | Trial quality |
| B47 | 用户空间页复制UID时去掉“UID:”前缀 | `UidCopyNoPrefixPatch` | Uid copy no prefix |
| B48 | gRPC 通信引擎服务 hook | `MossPatch` | Moss |
| B49 | 公开ProtoBuf实体构建方法 | `UnlockProtobufPatch` | Unlock ProtoBuf |
| B50 | 修复PreferenceManager被混淆后引起的问题 | `FixPreferenceManagerPatch` | Fix preference manager |
| B51 | HD版本设置辅助补丁 | `HdPreferenceFragmentPatch` | Hd preference fragment |
| B52 | 哔哩漫游X设置入口 | `SettingsResourcePatch` | BiliRoamingX settings entrance |
| B53 | 自定义主题色 | `CustomThemePatch` | Custom theme color |
| B54 | 调用APP自带Toast补丁 | `ToastPatch` | Toast |
| B55 | WebView 补丁 | `WebViewPatch` | Webview |
| B56 | 加回弹幕默认使用推荐字体设置 | `AddBackDmSystemFontPatch` | Add back danmaku system font |
| B57 | HD版启用番剧页面键盘按键映射 | `BangumiKeyDownHandlerPatch` | Enable Key mapping |
| B58 | 自定义播放器默认播放速度 | `DefaultPlaybackSpeedPatch` | Default playback speed |
| B59 | 强制硬件解码 | `HwCodecPatch` | Force hardware codec |
| B60 | 自定义播放器长按播放速度 | `LongPressPlaybackSpeedPatch` | Long press playback speed |
| B61 | 自定义播放器播放速度列表 | `OverridePlaybackSpeedPatch` | Override playback speed |
| B62 | 播放地址拦截处理 | `PlayUrlPatch` | Play url |
| B63 | 播放器 GestureDetector hook | `PlayerGestureDetectorPatch` | Player gesture detector hook |
| B64 | 播放器核心hook | `PlayerPatch` | Player core |
| B65 | 播放器特定样式toast显示拦截 | `PlayerToastPatch` | Player toast |
| B66 | 记住播放速度变化 | `RememberPlaybackSpeedPatch` | Remember playback speed |
| B67 | 视频默认画质设置 | `VideoQualityPatch` | Video default quality |
| B68 | tv.danmaku.bili | `SubtitleImportSaveButtonPatch` | tv.danmaku.bili |
| B69 | 导入及保存字幕相关功能补丁 | `SubtitleImportSavePatch` | Subtitle import and save |
| B70 | 字幕样式相关补丁 | `SubtitlePatch` | Subtitle style |

## 3. 修改版注入点分布（前 30 个包）

| 宿主包 | 文件数 |
|---|---|
| `` | 160 |
| `com/bilibili/cron` | 5 |
| `com/bilibili/video/story/player` | 4 |
| `com/bilibili/biligame/utils` | 3 |
| `com/bilibili/bplus/im/conversation` | 3 |
| `com/bilibili/lib/dd/internal` | 3 |
| `com/bilibili/mini/player/biz` | 2 |
| `com/tencent/open` | 2 |
| `com/tencent/smtt/sdk` | 2 |
| `com/bilibili/bilipay/web/hybrid` | 2 |
| `com/bilibili/lib/bilipay/ui/base/hybrid` | 2 |
| `tv/danmaku/biliplayerv2/service/interact/biz/chronos/chronosrpc/methods/send` | 2 |
| `tv/danmaku/biliplayerv2/service/interact/biz/chronos/chronosrpc/methods/receive` | 2 |
| `tv/danmaku/bili/ui/theme` | 2 |
| `tv/danmaku/bili` | 2 |
| `tv/danmaku/video/bilicardplayer/player` | 2 |
| `com/bapis/bilibili/app/dynamic/v2` | 2 |
| `com/bapis/bilibili/main/community/reply/v1` | 2 |
| `com/tencent/turingcam` | 2 |
| `com/bilibili/rpc/ignet` | 2 |
| `com/bilibili/bplus/im/conversation/widget` | 2 |
| `com/bilibili/lib/ui/util` | 2 |
| `com/bilibili/lib/ighttp` | 2 |
| `com/bilibili/lib/blconfig/internal` | 2 |
| `com/bilibili/video/story/action/widget` | 2 |
| `com/cmic/sso/sdk/e` | 2 |
| `tv/danmaku/biliplayerv2/service/setting` | 1 |
| `tv/danmaku/biliplayerv2` | 1 |
| `cmbapi` | 1 |
| `tv/danmaku/ijk/media/player` | 1 |

## 4. 明天续接步骤

```bash
# 1) /tmp 下 smali 若已被清理，重新解包（约 4.5G / 每次 3~5 分钟）
apktool d -r -f -o /tmp/smali_orig "/root/project/Android-hook-file/哔哩哔哩v9.20.0.apk"

# 2) 从 1.23.3 源码提取指纹特征
ls /tmp/biliroamingx-src/patches/src/main/kotlin/app/revanced/patches/bilibili/**/fingerprints/

# 3) 用指纹的 strings 在原版 smali 中定位候选类
grep -rl --include=*.smali "特定字符串" /tmp/smali_orig/smali 2>/dev/null
```

优先级建议：A 类先做（成本低、见效快），B 类按「净化弹窗 → 评论过滤 → 首页过滤 → UI 定制」顺序。

## 附录：全部 1.23.3 补丁清单

| 补丁对象 | 名称 | 描述 | 路径 |
|---|---|---|---|
| `UpRcmdAdsPatch` | Block up recommend ads | 屏蔽UP主推荐广告 | `ads/patch/UpRcmdAdsPatch.kt` |
| `HideFollowButtonPatch` | Hide follow button | 不显示关注按钮 | `layout/patch/HideFollowButtonPatch.kt` |
| `RemoveVipSectionPatch` | Remove vip section | 移除我的页面大会员横幅 | `layout/patch/RemoveVipSectionPatch.kt` |
| `ConfigPatch` | Config | Config hook | `misc/config/patch/ConfigPatch.kt` |
| `CopyEnhancePatch` | Copy enhance | 自由复制补丁 | `misc/copy/patch/CopyEnhancePatch.kt` |
| `DarkSwitchPatch` | Dark switch | 我的页面深色模式切换弹框确认 | `misc/darkswitch/patch/DarkSwitchPatch.kt` |
| `DrawerPatch` | Drawer | 我的页面移至侧滑栏辅助补丁 | `misc/drawer/patch/DrawerPatch.kt` |
| `BLKVPatch` | Blkv | 集成BLKV | `misc/integrations/patch/BLKVPatch.kt` |
| `BrotliPatch` | Brotli | 集成Brotli | `misc/integrations/patch/BrotliPatch.kt` |
| `ConfigPatch` | Config integration | 集成获取Config方法 | `misc/integrations/patch/ConfigPatch.kt` |
| `DpiPatch` | Dpi | 自定义dpi辅助补丁 | `misc/integrations/patch/DpiPatch.kt` |
| `IntegrationsPatch` | Integrations |  | `misc/integrations/patch/IntegrationsPatch.kt` |
| `LibBiliPatch` | Lib bili | 查找网络请求签名及获取appKey方法 | `misc/integrations/patch/LibBiliPatch.kt` |
| `MainActivityPatch` | Main activity patch | 代理部分MainActivity方法补丁 | `misc/integrations/patch/MainActivityPatch.kt` |
| `ModifyModifierPatch` | Modify modifier | 修改类或类成员修饰符，方便操作 | `misc/integrations/patch/ModifyModifierPatch.kt` |
| `NormalizeRecyclerViewPatch` | Anti RecyclerView obfuscation |  | `misc/integrations/patch/NormalizeRecyclerViewPatch.kt` |
| `ProtoBufPrintPatch` | ProtoBuf print | 使 ProtoBuf toString 打印更具可读性 | `misc/integrations/patch/ProtoBufPrintPatch.kt` |
| `JSONPatch` | Json | 通用阿里Fastjson反序列化数据修改 | `misc/json/patch/JSONPatch.kt` |
| `PegasusPatch` | Pegasus hook | 首页推荐流hook | `misc/json/patch/PegasusPatch.kt` |
| `MusicNotificationPatch` | Music notification | 原生音乐通知样式 | `misc/notification/patch/MusicNotificationPatch.kt` |
| `OkHttpPatch` | OkHttp | OkHttp网络请求响应Hook | `misc/okhttp/patch/OkHttpPatch.kt` |
| `AppendExtraSearchTypePatch` | Search type | 附加更多搜索类型补丁 | `misc/other/patch/AppendExtraSearchTypePatch.kt` |
| `AppendSignatureInfoPatch` | Append signature info for LSPatch | meta-data 附加原始签名信息，允许 LSPatch 及类似物处理过的 APP 获取本 APP 的原始签名 | `misc/other/patch/AppendSignatureInfoPatch.kt` |
| `AutoLikePatch` | Auto like | 视频自动点赞补丁 | `misc/other/patch/AutoLikePatch.kt` |
| `BLRoutePatch` | BL route intercept | 哔哩哔哩页面路由修改 | `misc/other/patch/BLRoutePatch.kt` |
| `BLogPatch` | BLog | 放宽日志打印级别 | `misc/other/patch/BLogPatch.kt` |
| `BiliLayoutAdjustPatch` | Bili adjust layout (codes) | 调整部分布局UI | `misc/other/patch/BiliLayoutAdjustPatch.kt` |
| `BiliLayoutAdjustResourcesPatch` | Bili adjust layout (resources) | 调整部分布局UI | `misc/other/patch/BiliLayoutAdjustResourcesPatch.kt` |
| `BiliLibraryPatch` | Bili library patch | so库修补（目前绕过了签名校验） | `misc/other/patch/BiliLibraryPatch.kt` |
| `BiliSettingPreferenceLayoutPatch` | Fix app setting preference layout | APP设置部分页面字体大小不统一修复 | `misc/other/patch/BiliSettingPreferenceLayoutPatch.kt` |
| `BlockThaiCommentPatch` | Block thai comment | 禁止泰区评论 | `misc/other/patch/BlockThaiCommentPatch.kt` |
| `CacheRedirectPatch` | Cache redirect | 缓存行为重定向，允许外部工具直接下载 | `misc/other/patch/CacheRedirectPatch.kt` |
| `ChannelModelFixPatch` | Channel model fix | 频道相关 model 类构造修复(官方 BUG) | `misc/other/patch/ChannelModelFixPatch.kt` |
| `CrashHandlerPatch` | Crash handler | 崩溃日志记录 | `misc/other/patch/CrashHandlerPatch.kt` |
| `CustomSplashPatch` | Custom splash | 自定义启动图 | `misc/other/patch/CustomSplashPatch.kt` |
| `FakeNotInMultiWindowPatch` | Fake not in multi window mode | 播放页伪装未处于多窗口模式 | `misc/other/patch/FakeNotInMultiWindowPatch.kt` |
| `FavFolderDialogPatch` | Forbid auto subscribe | 禁止自动勾选订阅合集 | `misc/other/patch/FavFolderDialogPatch.kt` |
| `ForbidLiveRoomAutoFloatPatch` | Forbid live room auto float | 禁止直播间点小窗播放时自动开启“播放被中断时自动小窗播放” | `misc/other/patch/ForbidLiveRoomAutoFloatPatch.kt` |
| `ForbidLiveRoomSlideLeftPatch` | Forbid live room slide left | 禁止直播间向左滑动弹出抽屉 | `misc/other/patch/ForbidLiveRoomSlideLeftPatch.kt` |
| `ForbidSwitchLiveRoomPatch` | Forbid switch live room | 禁止上下滑动切换直播间 | `misc/other/patch/ForbidSwitchLiveRoomPatch.kt` |
| `ForceCommentNavigablePatch` | Force comment time navigable | 允许分P及交互视频评论时间点可点击 | `misc/other/patch/ForceCommentNavigablePatch.kt` |
| `InjectDataProviderPatch` | Inject DocumentsProvider | 注入可访问私有目录的 DocumentsProvider | `misc/other/patch/InjectDataProviderPatch.kt` |
| `InjectMoreSchemePatch` | Inject more scheme | 向 AndroidManifest.xml 注入更多可处理的 deep link | `misc/other/patch/InjectMoreSchemePatch.kt` |
| `KeepDataWhenUninstallPatch` | Keep data when uninstall | APP卸载时保留数据 | `misc/other/patch/KeepDataWhenUninstallPatch.kt` |
| `LiveRoomPatch` | Disable live room double click | 禁用直播间双击点赞 | `misc/other/patch/LiveRoomPatch.kt` |
| `MakeTextSelectablePatch` | Make text selectable | 为部分 TextView 添加可选择属性，方便复制 | `misc/other/patch/MakeTextSelectablePatch.kt` |
| `NoManageSpaceActivityPatch` | Remove managerSpaceActivity | 移除管理存储空间入口 | `misc/other/patch/NoManageSpaceActivityPatch.kt` |
| `NotificationPatch` | Notification icon | 通知图标修复 | `misc/other/patch/NotificationPatch.kt` |
| `NumberFormatPatch` | Number format | 数字格式化补丁 | `misc/other/patch/NumberFormatPatch.kt` |
| `PerAppLanguagePatch` | Per app language | 适配 Android 13 按应用设定语言特性，允许单独设置本 APP 的语言偏好 | `misc/other/patch/PerAppLanguagePatch.kt` |
| `PlayManifestCompatibilityPatch` | Play manifest compatibility | Play版 AndroidManifest 声明缺失补全 | `misc/other/patch/PlayManifestCompatibilityPatch.kt` |
| `PublishToFollowingPatch` | Disable auto select | 禁止自动转到动态 | `misc/other/patch/PublishToFollowingPatch.kt` |
| `SaveCommentImagePatch` | Save comment images | 保存评论图片补丁 | `misc/other/patch/SaveCommentImagePatch.kt` |
| `SharePatch` | Share | 分享处理相关补丁 | `misc/other/patch/SharePatch.kt` |
| `TeenagerModePatch` | Teenager mode | 禁用青少年模式弹窗 | `misc/other/patch/TeenagerModePatch.kt` |
| `TextFoldPatch` | Text fold | 文本折叠控制 | `misc/other/patch/TextFoldPatch.kt` |
| `TrialQualityPatch` | Trial quality | 试用画质辅助补丁 | `misc/other/patch/TrialQualityPatch.kt` |
| `UidCopyNoPrefixPatch` | Uid copy no prefix | 用户空间页复制UID时去掉“UID:”前缀 | `misc/other/patch/UidCopyNoPrefixPatch.kt` |
| `UpgradePatch` | Upgrade | 自定义更新辅助补丁 | `misc/other/patch/UpgradePatch.kt` |
| `CommentReplyUrlPatch` | Comment word search url | 屏蔽评论关键词搜索功能 | `misc/protobuf/patch/CommentReplyUrlPatch.kt` |
| `MossPatch` | Moss | gRPC 通信引擎服务 hook | `misc/protobuf/patch/MossPatch.kt` |
| `UnlockProtobufPatch` | Unlock ProtoBuf | 公开ProtoBuf实体构建方法 | `misc/protobuf/patch/UnlockProtobufPatch.kt` |
| `FixPreferenceManagerPatch` | Fix preference manager | 修复PreferenceManager被混淆后引起的问题 | `misc/settings/patch/FixPreferenceManagerPatch.kt` |
| `HdPreferenceFragmentPatch` | Hd preference fragment | HD版本设置辅助补丁 | `misc/settings/patch/HdPreferenceFragmentPatch.kt` |
| `SettingsResourcePatch` | BiliRoamingX settings entrance | 哔哩漫游X设置入口 | `misc/settings/patch/SettingsResourcePatch.kt` |
| `CustomThemePatch` | Custom theme color | 自定义主题色 | `misc/theme/patch/CustomThemePatch.kt` |
| `ToastPatch` | Toast | 调用APP自带Toast补丁 | `misc/toast/patch/ToastPatch.kt` |
| `WebViewPatch` | Webview | WebView 补丁 | `misc/webview/patch/WebViewPatch.kt` |
| `AddBackDmSystemFontPatch` | Add back danmaku system font | 加回弹幕默认使用推荐字体设置 | `video/player/patch/AddBackDmSystemFontPatch.kt` |
| `BangumiKeyDownHandlerPatch` | Enable Key mapping | HD版启用番剧页面键盘按键映射 | `video/player/patch/BangumiKeyDownHandlerPatch.kt` |
| `DefaultPlaybackSpeedPatch` | Default playback speed | 自定义播放器默认播放速度 | `video/player/patch/DefaultPlaybackSpeedPatch.kt` |
| `HwCodecPatch` | Force hardware codec | 强制硬件解码 | `video/player/patch/HwCodecPatch.kt` |
| `LongPressPlaybackSpeedPatch` | Long press playback speed | 自定义播放器长按播放速度 | `video/player/patch/LongPressPlaybackSpeedPatch.kt` |
| `OverridePlaybackSpeedPatch` | Override playback speed | 自定义播放器播放速度列表 | `video/player/patch/OverridePlaybackSpeedPatch.kt` |
| `PlayUrlPatch` | Play url | 播放地址拦截处理 | `video/player/patch/PlayUrlPatch.kt` |
| `PlayerGestureDetectorPatch` | Player gesture detector hook | 播放器 GestureDetector hook | `video/player/patch/PlayerGestureDetectorPatch.kt` |
| `PlayerPatch` | Player core | 播放器核心hook | `video/player/patch/PlayerPatch.kt` |
| `PlayerToastPatch` | Player toast | 播放器特定样式toast显示拦截 | `video/player/patch/PlayerToastPatch.kt` |
| `RememberPlaybackSpeedPatch` | Remember playback speed | 记住播放速度变化 | `video/player/patch/RememberPlaybackSpeedPatch.kt` |
| `VideoQualityPatch` | Video default quality | 视频默认画质设置 | `video/quality/patch/VideoQualityPatch.kt` |
| `SubtitleImportSaveButtonPatch` | tv.danmaku.bili |  | `video/subtitle/patch/SubtitleImportSaveButtonPatch.kt` |
| `SubtitleImportSavePatch` | Subtitle import and save | 导入及保存字幕相关功能补丁 | `video/subtitle/patch/SubtitleImportSavePatch.kt` |
| `SubtitlePatch` | Subtitle style | 字幕样式相关补丁 | `video/subtitle/patch/SubtitlePatch.kt` |
