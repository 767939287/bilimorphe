# 9.20.0 指纹验证进度表

> 来源：`refs/biliroamingx-1.23.5/FINGERPRINTS.md`（BiliRoamingX-AI 1.23.5 自动生成，103 个 Fingerprint → 80+ Patch，17 个功能域）
> 用途：todo #8「hook 点验证」的逐项核对清单 —— 每个指纹在 9.20.0 上是否存在、是否需改匹配规则。
> 状态：⬜ 待验证 / ✅ 存在可用 / ⚠️ 类存在但特征变了（需改匹配） / ❌ 缺失（功能要换实现） / ➖ 不适用（孤儿）

## 汇总

| 功能域 | 指纹数 | 验证方式建议 |
|---|---|---|
| 播放器 video/player | 20 | DexKit 类名/方法签名 |
| 字幕 video/subtitle | 6 | 同上 |
| 画质 video/quality | 1 | 同上 |
| 主题 misc/theme | 9 | 字符串/资源 |
| 深色模式 misc/darkswitch | 2 | 方法调用链 |
| 侧边栏 misc/drawer | 4 | 方法调用链 |
| 复制增强 misc/copy | 7 | 类名 |
| 通知 misc/notification | 4 | 类名 |
| 配置 misc/config | 3 | 类名 |
| OKHttp 网络 misc/okhttp | 9 | 类名（okhttp3 未混淆） |
| JSON misc/json | 4 | 类名/字符串 |
| Protobuf misc/protobuf | 2 | 类名 |
| 设置 misc/settings | 6 | 类名 |
| 集成框架 misc/integrations | 6 | 类名 |
| 杂项 misc/other | 23 | 混合 |
| 布局 layout（孤儿） | 3 | ➖ |

---

## 播放器（20）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| BangumiKeyDownHandlerFingerprint | 番剧 HD 按键映射 | ⬜ |
| IjkMediaPlayerOptionsFingerprint | 强制硬件解码 | ⬜ |
| MenuFuncSegmentFingerprint | 速度选择回调 | ⬜ |
| MenuServiceCreateSpeedFingerprint | 速度菜单回调 | ⬜ |
| PlaySpeedManagerImplFingerprint | 默认播放速度 | ⬜ |
| PlaybackSpeedSettingFingerprint | 速度设置 UI | ⬜ |
| PlayerGestureListenerFingerprint | 长按禁用 | ⬜ |
| PlayerGestureRotateFingerprint | 旋转禁用 | ⬜ |
| PlayerOnPreparedFingerprint | 播放器就绪回调 | ⬜ |
| PlayerResizableGestureListenerFingerprint | 缩放/平移手势 | ⬜ |
| PlayerSeekToFingerprint | seek 回调 | ⬜ |
| PlayerSettingCreateSpeedFingerprint | 速度设置创建 | ⬜ |
| PlayerSpeedChooseWidgetFingerprint | 速度选择 UI | ⬜ |
| PlayerSpeedWidgetFingerprint | 速度选项 UI | ⬜ |
| RemoteServiceHandlerOnStartFingerprint | OnlineInfoChanged | ⬜ |
| ResetResizeFunctionWidgetFingerprint | 重置可见性 | ⬜ |
| ShowPlayerToastFingerprint | Toast 服务 | ⬜ |
| TripleSpeedServiceFingerprint | 长按倍速 | ⬜ |
| TripleSpeedServiceUniteFingerprint | 长按倍速（统一版） | ⬜ |
| UnitePlayerSetSpeedMenuFingerprint | 速度菜单覆盖 | ⬜ |

## 字幕（6）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| FunctionWidgetServiceFingerprint | 字幕功能组件 | ⬜ |
| FunctionWidgetTokenFingerprint | 字幕令牌 | ⬜ |
| PlayerSubtitleFunctionWidgetFingerprint | 字幕 UI 组件 | ⬜ |
| RecordSelectedSubtitleFingerprint | 字幕选择持久化 | ⬜ |
| SetDmViewReplyFingerprint | 弹幕字幕回复 | ⬜ |
| PlayerContainerFingerprint | 孤儿 | ➖ |

## 画质（1）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| PlayerSettingHelperFingerprint | 默认画质 | ⬜ |

## 主题（9）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| BuiltInThemesFingerprint | 内置主题数据 | ⬜ |
| BuiltInThemesFingerprint2 | 内置主题（变体） | ⬜ |
| SkinListFingerprint | 皮肤列表 | ⬜ |
| ThemeClickFingerprint | 主题选择 | ⬜ |
| ThemeColorsFingerprint | 主题颜色 | ⬜ |
| ThemeHelperFingerprint | 颜色数组 | ⬜ |
| ThemeNameFingerprint | 主题名称 | ⬜ |
| ThemeProcessorFingerprint | 主题重置 | ⬜ |
| WebActivityBuildUriFingerprint | 颜色 ID 映射 | ⬜ |

## 深色模式（2）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| SwitchDarkModeFingerprint | 深色模式切换 | ⬜ |
| HdNewSwitchDarkModeFingerprint | HD 版深色切换 | ⬜ |

## 侧边栏（4）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| OpenDrawerControlFingerprint | 打开抽屉 | ⬜ |
| CloseDrawerControlFingerprint | 关闭抽屉 | ⬜ |
| DrawerIsOpenFingerprint | 抽屉状态 | ⬜ |
| DrawerLayoutParamsFingerprint | 抽屉布局参数 | ⬜ |

## 复制增强（7）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| BaseCommentCopyFingerprint | 评论复制基类 | ⬜ |
| CommentCopyOldFingerprint | 旧版复制 | ⬜ |
| CommentCopyNewFingerprint | 新版复制 | ⬜ |
| Comment3CopyFingerprint | 评论 3 复制 | ⬜ |
| Comment3DialogCopyFingerprint | 评论 3 弹窗复制 | ⬜ |
| ConversationCopyFingerprint | 会话复制 | ⬜ |
| DescCopyFingerprint | 描述复制 | ⬜ |

## 通知（4）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| LiveNotificationHelperFingerprint | 直播通知 | ⬜ |
| NotificationStyleAbFingerprint | 通知样式 | ⬜ |
| MediaSessionCallbackApi21Fingerprint | 媒体按钮 | ⬜ |
| HeadsetMediaSessionCallbackFingerprint | 耳机按钮 | ⬜ |

## 配置（3）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| ABSourceFingerprint | AB 实验源 | ⬜ |
| ConfigSourceFingerprint | 配置源 | ⬜ |
| DDContractImplFingerprint | DDContract | ⬜ |

## OKHttp 网络（9）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| HttpUrlFingerprint | URL 解析 | ⬜ |
| MediaTypeGetFingerprint | 内容类型 | ⬜ |
| RequestFingerprint | 请求拦截 | ⬜ |
| ResponseBodyFingerprint | 响应体 | ⬜ |
| ResponseFingerprint | 响应字段 | ⬜ |
| BufferFingerprint | 缓冲流 | ⬜ |
| HeadersFingerprint | 响应头 | ⬜ |
| RealCallFingerprint | execute() | ⬜ |
| BodyWrapperFingerprint | 响应体封装 | ⬜ |

## JSON（4）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| JSONFingerprint | FastJSON parseObject | ⬜ |
| PegasusParserFingerprint | 推荐流解析 | ⬜ |
| CardClickProcessorFingerprint | 旧版卡片点击 | ⬜ |
| CardClickProcessorNewFingerprint | 新版卡片点击 | ⬜ |

## Protobuf（2）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| MossServiceFingerprint | gRPC Service | ⬜ |
| MossMiddlewareGaiaFingerprint | gRPC 中间件 | ⬜ |

## 设置（6）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| PreferenceManagerFingerprint | 混淆修复 | ⬜ |
| HdOnSettingsClickFingerprint | HD 设置点击 | ⬜ |
| HdOnWatchLaterClickFingerprint | HD 稍后观看 | ⬜ |
| HdOnImClickFingerprint | HD 通信 | ⬜ |
| HdOnContactClickFingerprint | HD 联系人 | ⬜ |
| HdOnCourseClickFingerprint | HD 课程 | ⬜ |

## 集成框架（6）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| AppCompatActivityFingerprint | DPI 缩放 | ⬜ |
| BaseMainFrameFragmentFingerprint | 主框架配置 | ⬜ |
| BLKVFingerprint | KV 存储 | ⬜ |
| BrotliFingerprint | Brotli 解压 | ⬜ |
| DanmakuFontSwitchPreferenceFingerprint | 弹幕字体 | ⬜ |
| RecyclerViewHolderFingerprint | RecyclerView | ⬜ |

## 杂项（23）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| AppendTrackingInfoFingerprint | 分享链接净化 | ⬜ |
| AttachChannelInfoFingerprint | 升级检查 | ⬜ |
| BLRouteBuilderFingerprint | 路由构建 | ⬜ |
| CommentConfigFingerprint | 评论导航 | ⬜ |
| CommentImageSetLayoutBindViewFingerprint | 保存评论图 | ⬜ |
| ConfigV3PreloadFingerprint | 模块预下载 | ⬜ |
| FavFolderOnDataSuccessFingerprint | 收藏夹（旧） | ⬜ |
| FavFolderOnDataSuccessNewFingerprint | 收藏夹（新） | ⬜ |
| LiveRoomSetFloatWindowFingerprint | 禁止悬浮窗 | ⬜ |
| LiveRoomTouchDispatchViewModelFingerprint | 禁止左滑 | ⬜ |
| LogcatAdapterFingerprint | BLog hook | ⬜ |
| MainCommonServiceImplFingerprint | 升级检查 | ⬜ |
| MineBindAccountStateFingerprint | 数字格式化 | ⬜ |
| OgvSearchResultFingerprint ×3 | 搜索类型 | ⬜ |
| OnOgvDownloadFingerprint | 缓存重定向 | ⬜ |
| PublishToFollowingConfigFingerprint | 发布设置 | ⬜ |
| QualityViewHolderFingerprint | 试用画质 | ⬜ |
| RouteRequestFingerprint | 路由拦截 | ⬜ |
| SectionFingerprint | 自动点赞 | ⬜ |
| SetUnitedTabLayoutFingerprint | 布局调整 | ⬜ |
| SpaceBindAccountStateFingerprint | 数字格式化 | ⬜ |
| TeenagersModeFingerprint | 青少年模式 | ⬜ |
| UidCopyFingerprint | UID 复制 | ⬜ |
| UniteDownloadMenuInvokeFingerprint | 下载菜单 | ⬜ |
| WebConfigFingerprint | Web 配置 | ⬜ |

## 布局（孤儿，3）

| Fingerprint | 功能 | 9.20.0 |
|---|---|---|
| PlayerFullStoryWidgetFingerprint | 孤儿 | ➖ |
| GeminiPlayerFullStoryWidgetFingerprint | 孤儿 | ➖ |
| PlayerContainerFingerprint | 孤儿（见字幕域） | ➖ |

---

## 无指纹 Patch（不受版本升级影响的备忘）

直接按类名/资源/Manifest 操作，B 站升级时**通常不受影响**：

```
HideFollowButtonPatch, RemoveVipSectionPatch, UpRcmdAdsPatch, IntegrationsPatch,
LibBiliPatch, MainActivityPatch, ModifyModifierPatch, ProtoBufPrintPatch,
AppendSignatureInfoPatch, BiliLayoutAdjustResourcesPatch, BiliLibraryPatch,
BiliSettingPreferenceLayoutPatch, BlockThaiCommentPatch, ChannelModelFixPatch,
CrashHandlerPatch, CustomSplashPatch, FakeNotInMultiWindowPatch,
ForbidSwitchLiveRoomPatch, InjectDataProviderPatch, InjectMoreSchemePatch,
KeepDataWhenUninstallPatch, LiveRoomPatch, MakeTextSelectablePatch,
NoManageSpaceActivityPatch, NotificationPatch, PerAppLanguagePatch,
PlayManifestCompatibilityPatch, TextFoldPatch, UnlockProtobufPatch,
CommentReplyUrlPatch, HdPreferenceFragmentPatch, SettingsResourcePatch,
SubtitlePatch, SubtitleImportSaveButtonPatch, AddBackDmSystemFontPatch,
PlayUrlPatch
```

## 验证方法

1. **类名直查**：`grep -c '<指纹对应的类名>' /tmp/d_all/classes*.dex`（本次已解压 39 个 dex）
2. **DexKit 特征**：在 `xposed/` 里写一次性 probe，打印 `findMethod/findClass` 命中数
3. **疑似缺失**：先用 jadx 单 dex 反编译确认，再决定换实现

## 已完成的定向验证（2026-11-27）

| 目标 | 结论 | 证据 |
|---|---|---|
| `PlaySpeedManagerImpl` | ✅ 存在 | 原版 `classes12/16/17.dex` |
| `IjkMediaPlayer` / `AbstractMediaPlayer` | ✅ 存在 | 原版 `classes7/8/31.dex`（31 有 27 处） |
| `tangram/basic` 包 | ✅ 存在 | 19 个 dex 命中 |
| `BiliPreferencesActivity` + `extra:key:fragment` 协议 | ✅ 可用 | 1.28.6 成品依赖它（`kofua/OtG.java`、`kofua/xyT.java`） |
| `com.bilibili.lib.ui.BasePreferenceFragment` | ✅ 存在 | 1.28.6 `kofua/NPT.java` 继承它 |

> 注：以上为反编译/字符串层面确认，「类能加载、hook 能装」仍需实机验证。
