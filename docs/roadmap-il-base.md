# 路线图（IL 基座版，2026-11-27）

> 本文件取代 `docs/roadmap.md` 的执行顺序部分（原文件保留为历史记录）。
> 基线切换：**BiliRoamingX 1.23.3 从「底座」降级为「能力源」**，底座改为 `jichuo1/Bilibili_Innocent_Lab`（GPL-3.0，libxposed 102，已适配 8.84.0～9.20.0）。
> 目标形态：单模块，IL 基座 + 1.23.3 缺口移植 + 1.28.6 独有功能，宿主 `tv.danmaku.bili` 9.20.0。

---

## 一、为什么切换

| 维度 | 1.23.3 底座 | IL 底座 |
|---|---|---|
| 已适配 9.20.0 的功能 | 0（全部要重新定位） | 200+ |
| 定位机制 | 自己写 DexKit matcher | `VersionAdapter`（8109 行）+ 两级缓存 |
| 缺 1.28.6 功能 | 67 项 | 49 项（其中 40 个新键 / 32 个真功能；`grpc_proxies` 不做） |
| 工期 | 2.5–3 个月 | 1.5–2 周 |
| 代价 | — | 要读懂上游框架、跟 stable tag 同步、GPL 开源 |

结论：IL 底座简单 6–10 倍。唯一保留项是 **1.23.3 的区域解锁体系（IL 完全没有）**。

**ADR-002（2026-11-27 用户确认维持）**：区域解锁采用 1.23.3 方案 —— 4 个区域反向代理地址（`tw/hk/cn/th_server`）+ `upos_host` + `access_key_main/th` + Pink 客户端伪装；**不实现** 1.28.6 的 `grpc_proxies` 地区代理。IL 的 `KmpGrpcWireAdapter` 仅作 gRPC 报文拦截层使用。模块不自带任何服务器/密钥，未填写时解锁不生效，因此开关默认关。

---

## 二、资产处置

| 资产 | 位置 | 处置 |
|---|---|---|
| 1.23.3 源码 | `/root/project/refs/biliroamingx-1.23.3/` | ✅ 能力源（只读） |
| 103 指纹 / 83 补丁清单 | `docs/fingerprint-verify.md`、`docs/hook-points.md` | ✅ 作为映射表底稿 |
| 两版设置键 | `docs/data/keys_1233.txt`、`keys_1286.txt` | ✅ 差集依据（+41 / −14） |
| 1.28.6 反编译 | `/root/project/Android-hook-file/jadx35-classes35/` | ✅ 行为参考（不抄代码，ADR-001） |
| 本项目 `xposed/` 骨架（9 个 .kt） | `xposed/` | ⏸ 冻结，结论可借，代码不搬 |
| 本项目 `integrations/` fork（193 个 .kt） | `integrations/` | ⏸ 只留解锁相关文件作移植素材 |

---

## 三、原 21 项 Todo 处置

| # | 原项 | 处置 | 说明 |
|---|---|---|---|
| 1–7 | 源码 fork / xposed 接入 / 桥接 / OkHttp 层 / CI / hook 清单 / 反编译 | ✅ 已完成，保留成果 | 文档与清单仍是输入 |
| 8 | hook 点验证 + 实机冒烟 | 🔁 转为 IL 阶段 0/1 | ①IL 基线构建+实机能启动 ②103 指纹在 9.20.0 的存在性并入映射表 |
| 9 | 最小设置入口（寄生 BiliPreferencesActivity） | ⏸ 作废 | IL 已有设置体系；仅保留「长按入口」设计参考 |
| 10 | 架构修正（多进程 / 临时默认值） | ⏸ 作废 | IL 已有 `HostThreadGuard` + 多进程处理 |
| 11 | 稳定性地基（try-catch / 日志 / 白名单） | ⏸ 作废 | IL 已有异常隔离与安装证据链 |
| 12 | 重合 A 类 13 项 + 领券 + P2P/CDN | 🔁 转入 IL 阶段 4 | 只补 IL 没有的：领券、P2P 禁用、稳定 CDN、BLKV、泰区评论 |
| 13 | 重合 B 类净化隐藏 ~15 | ❌ 作废 | IL 自带 |
| 14 | 重合 B 类内容过滤 ~8 | ❌ 作废 | IL 自带（含本地 AI 评论识别） |
| 15 | 重合 B 类播放器与 UI | ⚠ 部分保留 | IL 缺：`PlayerVersion`、`CustomTheme`、字幕样式三项 |
| 16 | 重合 B 类杂项 ~10 | ⚠ 部分保留 | IL 缺：通知图标、数字格式化、卸载保留数据、多窗口伪装、分 P 评论跳转 |
| 17 | 新增-低难度（SponsorBlock / 地区代理 / 泰区 / 云控覆盖） | 🔁 转 1.28 专项 | 见第五节；其中地区代理（`grpc_proxies`）按 ADR-002 **不做** |
| 18 | 新增-中难度（评论增强 / 迷你模式 / 无损） | 🔁 转 1.28 专项 | 见第五节 |
| 19 | 新增-高难度（AI 字幕 / AI 视频过滤） | 🔁 转 1.28 专项 | AI 字幕有 1.23.3 现成 845 行 |
| 20 | 新增-界面细节 12 项 | 🔁 转 1.28 专项 | 见第五节 |
| 21 | 收尾（导入导出 / 签名 / 测试 / 文档） | ✅ 保留 | access_key 脱敏、GPL 头、共存说明 |

---

## 四、新 Todo：1.23.3 学进 IL

| 阶段 | 内容 | 落点 | 验证 | 估时 |
|---|---|---|---|---|
| **0 基线** | IL 构建通过 + 9.20.0 实机能启动并出日志 | — | `assembleDebug` + 启动日志 | 0.5 天 |
| **1 学习（只读）** | 精读 1.23.3 解锁链路，产出 83 项映射表 | `docs/1.23-to-il-map.md` | 每项有「IL 落点 + 定位方式 + 状态」 | 1 天 |
| **2 解锁骨架** | 新增 `AreaUnlockFeatureInstaller` + OkHttp/playurl 改写 | `hook/feature/area/` | 填服务器后能拉番剧 playurl | 1.5 天 |
| **3 gRPC/Moss 三路** | PGC / Unite / UGC / ListenPlayURL + BangumiSeasonHook | `hook/adapter/KmpGrpcWireAdapter.kt` | 番剧/剧集/media/season 各 1 条 | 1.5 天 |
| **4 解锁周边** | UPOS 替换、BLKV、签名/appKey、泰区评论、试用画质、P2P、领券 | 各自 installer | 单测 + 实机抽验 | 1.5 天 |
| **5 批量缺口** | 字幕翻译（Translator 223 + SubtitleHelper 543）、字幕导入、主题色、字幕样式、杂项 | 按批 3–5 项 | 每批构建 + 抽验 | 4–6 天 |
| **6 收尾** | 导入导出、诊断、README/兼容矩阵、roaming 共存策略 | 文档 | 全量回归 | 1 天 |

### 落点固定四处

```
① hook/feature/<域>/XxxFeatureInstaller.kt
② HookEntry.kt（当前 5830 行 / 76 个 installer）
③ hook/adapter/AdaptPlanCatalog.kt（381 行，不登记会被裁剪）
④ FeaturePreferences + settings/SettingsUiCatalog
```

### 1.23.3 关键源码（行数）

| 文件 | 行数 | 用途 |
|---|---|---|
| `patches/protobuf/BangumiPlayUrlHook.kt` | 1252 | PGC 播放地址改写（核心） |
| `api/BiliRoamingApi.kt` | 655 | 播放/剧集/season 接口 |
| `patches/okhttp/BangumiSeasonHook.kt` | 551 | season 解锁 |
| `utils/SubtitleHelper.kt` | 543 | 字幕导入保存 |
| `settings/Settings.kt` | 289 | 4 服务器 + access_key 键定义 |
| `utils/Translator.kt` | 223 | AI 字幕翻译 |

---

## 五、新 Todo：1.28.6 独有 40 键（32 个真功能）

按 `keys_1233.txt`(222) 与 `keys_1286.txt`(249) 差集：**新增 41 / 移除 14**；其中 `grpc_proxies` 按 ADR-002 不做，实际待补 **40 键**。
IL 精确键名零重合（已 grep 验证）；功能面重合 3 处：SponsorBlock、AI 字幕、小窗能力位。

| 分组 | 键 | 数量 |
|---|---|---|
| AI | `category_ai_subtitle_translation` `ai_subtitle_translation_prompt` `category_ai_video_filter` `ai_video_filter_enabled` `ai_video_filter_prompt` `ai_config` | 6 |
| SponsorBlock | `sb_category` `skip_reward_ad` | 2 |
| 泰区 | `add_thai_animation` `add_thai_movie` | 2 |
| ~~地区代理~~ | `grpc_proxies` ❌ **不做**（ADR-002：解锁锁定 1.23.3 服务器方案） | 0 |
| 小窗/迷你 | `always_mini_play` `mini_danmaku` `mini_follow_full` `mini_subtitle` `not_suicide` | 5 |
| 播放 | `prefer_lossless` `release_audio_focus` `clear_audio` | 3 |
| 广告/净化 | `block_play_pause_ad` `hide_live_banner` `block_comment_op_search` `block_comment_rating` | 4 |
| 界面/外观/系统 | `block_activity_label` `block_back` `direct_owner_space` `disable_coloros_transitions` `disable_comm_skin` `disable_hot_patch` `force_tablet_mode` `pic_no_fold` `remove_video_screenshot_watermark` `show_sex` `prefer_bottom_tab` `category_danmaku` | 12 |
| 旧版回退 | `force_old_comments` `force_old_favorite` | 2 |
| 杂项 | `live_no_block` `report` `support` `hidden_features_group` | 4 |
| **合计** | 新增 41 键 − `grpc_proxies` = **待补 40 键** | 40 |

**1.28.6 移除/改名 14 项**（1.23.3 有）：
`add_channel`、`custom_server→grpc_proxies`、`disable_auto_select`、`disable_auto_subscribe`、`external_downloader`、`force_hw_codec`、`fragment_external_downloader`、`old_dm_panel`、`package_name`、`player_version`、`prefer_stable_cdn`、`prefs_per_app_language`、`save_th_history`、`story_ui_style`

---

## 六、A/B 补丁分批策略（83 补丁）

| 批次 | 数量 | 处理 |
|---|---|---|
| A 网络/内容类 | 13 | IL 已有 OkHttp 桥接 → 只补 IL 缺的 5 项（BLKV、泰区评论、缓存重定向、分享、评论词搜索） |
| B 行为/UI 类 | 70 | 逐项与 IL 功能表比对，只移植 IL 没有的（预计 ≤15 项） |

> 比对结论写进 `docs/1.23-to-il-map.md`，状态列用：`IL已有 / 待移植 / 不适用 / 放弃`。

---

## 七、红线（沿用）

1. 不抄 1.28.6 反编译代码（ADR-001），只作行为参考。
2. 移植文件保留 GPL 版权头 + 来源注释。
3. 每个 installer 独立 try-catch，定位失败只跳过本功能（fail-open）。
4. 检测到外部 BiliRoaming 模块时，内置解锁让位（沿用 IL 的 `RoamingCompatHook`）。
5. access_key 掩码显示、日志脱敏、导出提示。
6. 措辞避开「漫游 / roaming」，README 注明「仅供学习研究，不分发安装包」。

---

## 八、未决问题

1. ✅ **内置解锁方案（2026-11-27 已定）**：用 1.23.3 方案（`tw/hk/cn/th_server` + `access_key_main/th` + Pink 伪装），不做 `grpc_proxies`；开关默认关，需用户自填服务器。
2. **与 IL 上游同步节奏**：跟 stable tag（约 3 周一次 merge），不跟 main 日常提交。
3. **应用名 / applicationId 改名**：半天工作量，需在首次发版前定。
4. **模块形态**：单模块（IL fork 内置解锁）vs 双模块共存（IL + bilimorphe 瘦身）→ 当前选单模块。
