# 38 键实现状态（2026-11-27）

> 口径：1.28.6 新增 41 键 − `grpc_proxies`（ADR-002 不做）− 泰区 2 项（用户决定不做）= 38 键。
> 实现原则：**以 1.28.6 行为为准**（ADR-006），1.28.6 源码仅作行为参考，不复制代码（ADR-001）。

## 一、已实现（23 项，已编译并确认进包）

| 键 | 1.28.6 落点 | 本项目实现 | 文件 |
|---|---|---|---|
| `disable_hot_patch` | `Nhkb.mWZ` → 清空 `context.getDir("tribe")` | Application.onCreate 后递归清空 | `DisableHotPatchFeature` |
| `block_back` | 三个页面 `onBackPressed` 直接 return | hook 三 Activity 的 `onBackPressed` | `BlockBackFeature` |
| `remove_video_screenshot_watermark` | `Et.Tggb(drawable)` 返回 null | hook 截图合成方法，drawable 参数置 null | `RemoveVideoScreenshotWatermarkFeature` |
| `pic_no_fold` | `replyControl.setFoldPictures(false)` | hook `ReplyInfo.parseFrom` | `PicNoFoldFeature` |
| `force_old_comments` | `Acbb` 覆盖表（12 键 → false） | `ConfigManager.isHitFF` 拦截 | `ConfigOverrideFeature` |
| `release_audio_focus` | 覆盖 `ff_player_audio_focus_gain=false` | 同上 | `ConfigOverrideFeature` |
| `block_play_pause_ad` | 覆盖 `dd_ad_pausedpage_enable=false` | 同上 | `ConfigOverrideFeature` |
| `force_tablet_mode` | 覆盖 `dd_screen_adjust_xiaomi_864="large"` | `ConfigManager.getConfig` 拦截 | `ConfigOverrideFeature` |
| `block_comment_op_search` | `clearOperationV2()` + 移除 SEARCH 卡片 | hook `MainListReply.parseFrom` | `CommentTweaksFeature` |
| `block_comment_rating` | `clearEsportsGradeCard()` | 同上 | `CommentTweaksFeature` |
| `show_sex` | `location + " " + sex` | 同上 | `CommentTweaksFeature` |
| `live_no_block` | `blockInfo = null`、水印开关置 0 | hook fastjson 反序列化出口 | `LiveDataCleanupFeature` |
| `hide_live_banner` | `pageList` 移除 banner | 同上 | `LiveDataCleanupFeature` |
| `block_activity_label` | `headLine.clearLabel()` | 同上 | `LiveDataCleanupFeature` |
| `clear_audio` | 空无损/空杜比 → clear | hook playurl `parseFrom` | `PlayUrlTweaksFeature` |
| `prefer_lossless` | 无损与杜比并存 → `clearDolby()` | 同上 | `PlayUrlTweaksFeature` |
| `not_suicide` | `MainActivityV2` 返回 → `moveTaskToBack(true)` | hook `MainActivityV2.onBackPressed` | `MainScreenTweaksFeature` |
| `skip_reward_ad` | 激励广告页出现即关闭 | hook `RewardAdDisplayActivity.onCreate` | `MainScreenTweaksFeature` |
| `prefer_bottom_tab` | 无 url 时按偏好返回 tab url | hook 宿主 `pagebuild.a.e(Bundle)` 写入 `key_selected_bottom_tab_url` | `BottomTabFeature` |
| `disable_comm_skin` | 移除 `common_equip` | hook fastjson `parseObject` 出口 | `JsonCleanupFeature` |
| `force_old_favorite` | `favourite` uri → `bilibili://user_center/favourite` | 同上 | `JsonCleanupFeature` |
| `direct_owner_space` | `ab_result.view_up_space = {success:true}` | 同上 | `JsonCleanupFeature` |
| `disable_coloros_transitions` | 反射置空 `mSeamlessThirdParty` | Application.onCreate 后反射执行 | `ColorOsTransitionsFeature` |

## 二、探针模式（5 项，已编译进包）

这 5 项的宿主类名在 dex 里只是字符串常量（`autoMiniPlayerSettingSuppressor` 可见于 classes9 / 11 / 35，
索引中无对应类），无法写死锚点。按 `docs/roadmap.md` 9.9「findClass 观测法」，
先用 DexKit 按字符串特征定位候选方法并只记录日志（类名 / 方法 / 结果），**实机跑一次即可固化指纹**。

实现：`MiniPlayerProbeFeature`，日志前缀 `[probe:<开关名>]`。

| 键 | 1.28.6 落点 | 已知线索 |
|---|---|---|
| `always_mini_play` | `qLE`（抑制 `autoMiniPlayerSettingSuppressor`） | 播放器小窗自动关闭逻辑 |
| `mini_danmaku` | `eGI.GJbb()` | 小窗弹幕画布 |
| `mini_subtitle` | `eGI.fro()` | 小窗字幕 |
| `mini_follow_full` | `mMX`（`lxB.xyT(vCbp.JFW(0), container)`） | 小窗随全屏 |
| `ai_video_filter_enabled` | `JFW`（AI 判定链路） | 依赖 AI 配置与提示词 |

## 三、非功能键（10 项，仅设置入口）

`ai_config`、`ai_subtitle_translation_prompt`、`ai_video_filter_prompt`、
`category_ai_subtitle_translation`、`category_ai_video_filter`、`category_danmaku`、
`hidden_features_group`、`report`、`support`、`sb_category`
—— 分类页 / 配置弹窗 / 外部入口，已在 `res/xml/bilimorphe_settings.xml` 中以占位项呈现。
