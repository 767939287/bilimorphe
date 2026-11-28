# 键差集核对：BiliRoamingX 1.23.3 vs 1.28.6

> 目的：把「哪些设置键 1.23 有、哪些 1.28 才有」一次钉死，供移植与开关对齐使用。
> 数据源：`docs/data/keys_1233.txt`（222 键）、`docs/data/keys_1286.txt`（249 键）。两者均为一行一键、已去重。

## 一、核对方法（可复现）

```bash
cd docs/data
comm -23 <(sort -u keys_1233.txt) <(sort -u keys_1286.txt)   # 仅 1.23 有
comm -13 <(sort -u keys_1233.txt) <(sort -u keys_1286.txt)   # 仅 1.28 有
comm -12 <(sort -u keys_1233.txt) <(sort -u keys_1286.txt)   # 两版共有
```

## 二、结论

| 项 | 数量 | 校验 |
|---|---|---|
| 仅 1.23 有（1.28 已移除/改名） | 14 | 222 = 208 + 14 ✅ |
| 仅 1.28 有（新增） | 41 | 249 = 208 + 41 ✅ |
| 两版共有 | 208 | — |

要点：

- 14 项经逐键 `grep -cx` 复核，确认 **1233=1、1286=0**，不是解析误差。
- 41 项经逐键复核，确认 **1233=0、1286=1**，确属 1.28 独有。
- 其中 `custom_server` → `grpc_proxies` 属**改名**而非纯新增；按 ADR-002，`grpc_proxies`（地区代理）本项目**不实现**。
- **泰区功能不做**（2026-11-27 用户决定）：`add_thai_animation`、`add_thai_movie` 两项排除，连同 `save_th_history`（已在 14 项移除表中）一并放弃。
- 故 1.28 侧实际待补为 41 − 1（`grpc_proxies`）− 2（泰区）= **38 键**。
- 1.23 侧 14 项需逐项裁定去留（多数是低价值或已被 1.28 内建能力取代）。

## 三、仅 1.23 有（1.28 没有）——14 项

| 键 | 说明（按键名/上游语义） | 备注 |
|---|---|---|
| `add_channel` | 侧栏「添加频道」入口 | 1.28 弃用 |
| `custom_server` | 自定义服务器地址 | 1.28 由 `grpc_proxies` 替代；ADR-002 两版均不采用 |
| `disable_auto_select` | 禁用自动选集 | — |
| `disable_auto_subscribe` | 禁用自动订阅 | — |
| `external_downloader` | 外部下载器 | 与 `fragment_external_downloader` 同组 |
| `force_hw_codec` | 强制硬件解码 | — |
| `fragment_external_downloader` | 外部下载器设置页 | 同上 |
| `old_dm_panel` | 旧版弹幕面板 | 1.28 有 `force_old_comments` 等替代 |
| `package_name` | 目标包名配置 | ReVanced 构建期参数，LSPosed 不需要 |
| `player_version` | 播放器版本切换 | 1.28 移除 |
| `prefer_stable_cdn` | 优先稳定 CDN | 1.28 移除 |
| `prefs_per_app_language` | 按应用设置界面语言 | 1.28 移除 |
| `save_th_history` | 保存泰区观看历史 | 1.28 移除 |
| `story_ui_style` | 故事/竖屏 UI 风格 | 1.28 移除 |

## 四、仅 1.28 有（1.23 没有）——41 项

分层与分组沿用 `docs/roadmap-il-base.md` 第五节；「待补」＝扣除 `grpc_proxies` 后的口径。

| 分组 | 键 | 数量 |
|---|---|---|
| AI | `category_ai_subtitle_translation` `ai_subtitle_translation_prompt` `category_ai_video_filter` `ai_video_filter_enabled` `ai_video_filter_prompt` `ai_config` | 6 |
| SponsorBlock | `sb_category` `skip_reward_ad` | 2 |
| 泰区（不做） | `add_thai_animation` `add_thai_movie` | 2 |
| 地区代理（不做） | `grpc_proxies` | 1 |
| 小窗/迷你 | `always_mini_play` `mini_danmaku` `mini_follow_full` `mini_subtitle` `not_suicide` | 5 |
| 播放 | `prefer_lossless` `release_audio_focus` `clear_audio` | 3 |
| 广告/净化 | `block_play_pause_ad` `hide_live_banner` `block_comment_op_search` `block_comment_rating` | 4 |
| 界面/外观/系统 | `block_activity_label` `block_back` `direct_owner_space` `disable_coloros_transitions` `disable_comm_skin` `disable_hot_patch` `force_tablet_mode` `pic_no_fold` `remove_video_screenshot_watermark` `show_sex` `prefer_bottom_tab` `category_danmaku` | 12 |
| 旧版回退 | `force_old_comments` `force_old_favorite` | 2 |
| 杂项 | `live_no_block` `report` `support` `hidden_features_group` | 4 |
| **合计** | 新增 41 − `grpc_proxies` − 泰区 2 = **待补 38** | 41 / 38 |

## 五、维护约定

- 两版 key 清单若有更新，重跑第一节命令刷新本表。
- 本文件只记录**键层面**的差集；功能层面的移植状态见 `docs/1.23-to-il-map.md`、`docs/roadmap-il-base.md`。
