# 开发路线图（2026-11-27 冻结版）

> 基线：BiliRoamingX 1.23.3（GPL-3.0，最后公开源码版）
> 目标：LSPosed 运行时模块，宿主 `tv.danmaku.bili` 9.20.0（仅 arm64-v8a）
> 本文件是唯一入口文档：换机器/重启后从这里恢复上下文

---

## 一、当前状态

已完成 7 项（见下节），CI 可出包（artifact `bilimorphe-apk`，4.3MB，含 1780 处 integrations 引用）。
**尚未实机验证** —— 这是下一步的硬门槛。

---

## 二、Todo（21 项，按执行顺序）

### 已完成
| # | 项目 |
|---|---|
| 1 | 拉取 1.23.3 源码并 fork 进仓库（`integrations/` + `xposed/`） |
| 2 | 接入 xposed 模块（libxposed 102、META-INF/xposed、AGP9 兼容开关） |
| 3 | 运行时桥接（宿主 ClassLoader 注入、Utils.context、attached、CrossProcessPreferences） |
| 4 | OkHttp hook 层（DexKit：`returnType=okhttp3.Response` + `paramCount=0` + `"Canceled"`） |
| 5 | CI 出包验证（`.github/workflows/build.yml`） |
| 6 | hook 点清单（`docs/hook-points.md`，83 补丁） |
| 7 | classes35.dex 反编译（6783 文件，存 `/root/project/Android-hook-file/jadx35-classes35/`） |

### 待办（14 项）
| # | 项目 | 要点 |
|---|---|---|
| 8 | **实机冒烟验证**（硬门槛） | 装 APK → LSPosed 授权 → 看日志 `network entry point:` |
| 9 | 最小设置入口 | 反射枚举 Settings 生成开关 + 7 个文本输入（`tw/hk/cn/th_server`、`upos_host`、`access_key_main`、`access_key_th`）+ RemotePreferences 跨进程 |
| 10 | 架构修正 | 删 `BridgePatch.enableTemporaryDefaults()`；`MainHook` 的多进程处理（B 站有 158 处 `android:process`，当前 `isFirstPackage` 只处理首进程） |
| 11 | 稳定性地基 | BridgePatch 全 try-catch（防带崩宿主）、日志查看 UI、OkHttp 白名单（防热路径拖慢） |
| 12 | 重合 A 类 13 项 + 自动领券 + P2P/CDN | 区域解锁、番剧/剧集/media/season、评论 URL、UPOS、分享、泰国评论、Brotli、ProtoBuf、Config、BLKV、BLRoute、频道模型修正、缓存重定向；另加 `CouponAutoReceiver`（自动领 B 币券）、`DisableP2PUpload`、`PreferStableCdn` |
| 13 | 重合 B 类净化隐藏（~15） | 屏蔽 UP 推荐广告、隐藏关注按钮、移除大会员横幅、视频/直播弹窗净化、搜索净化、去直播横幅、右侧按钮定制 |
| 14 | 重合 B 类内容过滤（~8） | 评论（关键词/类型）、动态、首页推荐、热门、搜索、故事 |
| 15 | 重合 B 类播放器与 UI（含版本/主题色） | 播放器定制、首页/抽屉/底栏/tab/我的/空间/直播间/视频详情、深色模式、DPI、显示尺寸、文本折叠、字幕样式、`PlayerVersion`、`CustomTheme` |
| 16 | 重合 B 类杂项（~10） | 崩溃日志、通知图标、数字格式化、自动点赞、复制增强、卸载保留数据、多窗口伪装、分 P 评论跳转 |
| 17 | 新增-低难度 | SponsorBlock、地区代理、泰国动画/电影、广告增强、偏好底栏、**云控开关覆盖**（见功能扩展） |
| 18 | 新增-中难度 | 评论增强、迷你模式、播放增强（无损音质等） |
| 19 | 新增-高难度 | AI 字幕翻译、AI 视频过滤 |
| 20 | 新增-界面细节（12 项） | |
| 21 | 收尾 | 设置导入导出（access_key 脱敏）、签名与发布、单元测试、README/兼容矩阵/共存说明 |

---

## 三、功能全景

- **1.28.6**：41 个设置页、257 个设置项
- **1.23.3**：83 个补丁（其中 12 个是 ReVanced 专用脚手架，LSPosed 不需要）
- 重合 **207 项**（用 1.23.3 源码，只差开关）；1.28.6 新增 **42 项**（需重写）；1.23.3 独有 **15 项**（待逐项裁定去留）

### 已确认的功能遗漏（V4 Pro 审查）
| 遗漏 | 来源 |
|---|---|
| 自动领取 B 币券/卡券/每日经验 | 1.23.3 `CouponAutoReceiver`、`Settings.AutoReceiveCoupon` |
| AccessKey 配置（`access_key_main`/`access_key_th`） | `Settings.kt` L43-44（泰国解锁依赖） |
| 播放器版本切换（`PlayerVersion`） | `Settings.kt` L30 |
| P2P 上传禁用 / 优先稳定 CDN | `Settings.kt` L41-42 |
| 自定义主题色（`CustomTheme`） | `CustomThemePatch` |
| **云控开关覆盖**（自研） | hook `com.bilibili.lib.blconfig.ConfigManager`（`isHitFF` / `Contract.get$default`）+ 禁用热更新 |

---

## 四、参考资源（本地，持久）

| 路径 | 内容 |
|---|---|
| `/root/project/refs/biliroamingx-1.23.3/` | 上游 patches（101 指纹）+ integrations 源码 |
| `/root/project/refs/biliroamingx-1.28.6-res/` | 1.28.6 反编译资源（xml/values/中文 strings） |
| `/root/project/refs/feSHt377_FuckBiliAds/` | ⭐ 去广告参考（DexKit+API102，目标 9.19.0，含「我的」页入口注入） |
| `/root/project/refs/ch6vip_lsposed-bili-sponsorblock/` | SponsorBlock（仅国际版，思路参考） |
| `/root/project/refs/mengwuzhuanshou_BiliTamer/` | AI 字幕参考（MIT） |
| `/root/project/refs/AndroidReverser-Test_bilibiliX/` | 去广告 hook 点 |
| `/root/project/refs/MeowBug_BiCleaner/` | 净化参考 |
| `/root/project/Android-hook-file/jadx35-classes35/` | 1.28.6 核心反编译（6783 java 文件） |
| `/root/project/Android-hook-file/哔哩哔哩v9.20.0.apk` | 原版 APK |
| `/root/project/Android-hook-file/Bili-v9.20.0-x1.28.6@bb_show.apk` | 修改版 APK |

### 仓库内数据
| 文件 | 内容 |
|---|---|
| `docs/hook-points.md` | 83 补丁分 A/B 类清单 |
| `docs/data/injected-refs.txt` | 359 个注入点宿主文件 |
| `docs/data/new_classes.txt` | 1184 个新增类 |
| `docs/data/keys_1233.txt` / `keys_1286.txt` | 两版设置项 key（对比用） |
| `docs/data/patch_list.tsv` | 83 补丁的名称/描述/路径 |

---

## 五、环境恢复（关机/换机后必做）

```bash
# 1) SDK（若 /opt 保留则跳过）
ls /opt/android-sdk/platforms   # 需 android-37.2

# 2) local.properties
echo "sdk.dir=/opt/android-sdk" > /root/project/revanced-bilibili-patches/local.properties

# 3) 构建（走代理）
cd /root/project/revanced-bilibili-patches
export http_proxy=http://192.168.50.15:1080 https_proxy=http://192.168.50.15:1080
./gradlew :xposed:assembleDebug

# 4) 若需要原版 smali 做定位（4.5G，用完删）
apktool d -r -f -o /tmp/smali_orig "/root/project/Android-hook-file/哔哩哔哩v9.20.0.apk"

# 5) 若需要 1.28.6 的 classes35.dex
cd /tmp && mkdir d35 && cd d35
unzip -o "/root/project/Android-hook-file/Bili-v9.20.0-x1.28.6@bb_show.apk" classes35.dex
# 反编译：jadx -d /tmp/jadx35 --no-res -j 2 classes35.dex
```

---

## 六、关键决策（ADR）

### ADR-001：不直接复用修改版反编译代码，一律重写
- 理由：后期维护（B 站升级）、许可证干净（1.23.3 官方 GPL 源码）、架构一致（DexKit 定位集中管理）
- 做法：jadx 只作阅读材料（1.28.6 逻辑）→ 按本项目风格重写 → DexKit 按特征定位
- 1.23.3 源码优先作为重写素材（可读性优于反编译产物）

### ADR-002：区域解锁保留 1.23.3 方式
- 1.23.3：填 4 个区域服务器地址 + 伪装 Pink 客户端
- 1.28.6：改成地区代理（HTTP/HTTPS/SOCKS5）
- 决定：保留现有方式（用户选择）

### ADR-003：去广告类功能不重复造轮子
- FuckBiliAds（DexKit+API102，9.15~9.19 实测）已很完整
- 两个模块可共存（作用不同 hook 点），文档需注明"勿同时启用同类 hook"

---

## 七、已知坑（已踩）

| 坑 | 解法 |
|---|---|
| AGP 9 默认 builtInKotlin/newDsl，禁 kotlin.android 插件 | `gradle.properties`: `android.builtInKotlin=false` + `android.newDsl=false` |
| 旧 DSL 下 compileSdk 37.2 写法 | `compileSdkVersion("android-37.2")` |
| `.gitignore` 只写 `/build` 会漏子模块 | 必须写 `build/` |
| `rsync --exclude 'patches'` 会误删 integrations 内同名目录 | 用精确路径 |
| upstream `integrations/app` 是 application 模块 | 改 `setupLibraryModule` |
| upstream `useFullR8` / `externalNativeBuild cmake` | AGP 9 已移除，需删 |
| adb/模拟器 | B 站仅 arm64-v8a；MuMu 12 需 Magisk+Zygisk，视频解码靠转译较慢 |

---

## 八、三大风险

1. **DexKit 定位可行性**（最大不确定）：1.23.3 的 101 个指纹针对旧版，9.20.0 上 70+ 补丁需重新定位
2. **多进程 + ClassLoader hack**：改 `ClassLoader.parent` 脆弱；hook 若只生效主进程，下载/播放相关功能静默失效
3. **版本锁定维护 + 合规**：B 站更新需重定位；律师函前例（BiliRoaming 已删库）需警惕措辞与分发方式

---

## 九、第二轮审查补充（V4 Pro，2026-11-27）

### 9.1 验证体系（最高优先级补充）

| 机制 | 做法 |
|---|---|
| **功能-指纹-验证登记表** | 扩展 `docs/data/patch_list.tsv`：加列 `hook点 / DexKit指纹 / 开关key / 验证方法 / 状态` |
| **指纹聚簇** | 207 功能 → 83 补丁 → **约 30~40 个 hook 点簇**；每簇一次定位一次验证，覆盖其下全部功能 |
| **日志断言** | 每个 patch 输出结构化日志：`[patch] name=xx located=true/false hit=true/false`；用户反馈日志即可构成回归依据 |
| **快照回归** | 对 9.20.0 保存全量 hook 命中快照 `docs/data/hook-snapshot-9.20.0.json`；改版后 diff 出意外失效项 |
| **端到端抽查** | 定位成功≠功能生效：必须验证「开关翻转 → UPOS 真变 / 广告真消失 / 番剧真解锁」 |

### 9.2 升级适配 SOP（B 站发新版时）

```
① 反编译新版 classes*.dex
② 跑快照对比：上一版全过 vs 新版哪些 fail
③ 只修 fail 的指纹
④ 更新 docs/data/ 快照 + 兼容声明
```

### 9.3 发布与回滚

- 版本号：`9.20.0-<自研版本>`，直接体现宿主兼容目标
- 每个 release 保留 tag + artifact（回滚 = 装回旧 APK）
- 高风险功能（AI、云控覆盖）**默认关**，出问题单开关关闭，不整包回滚
- CHANGELOG 按 `feat / fix / compat` 分类

### 9.4 多宿主预留（暂不实做）

- 作用域判断改为 **ApplicationID 集合**（`tv.danmaku.bili` / `tv.danmaku.bilibilihd` / `com.bilibili.app.in`），不硬编码单值
- 指纹库与命中快照**按宿主隔离存放**

### 9.5 安全与合规

| 项 | 要求 |
|---|---|
| access_key | 设置页掩码显示；日志输出正则脱敏；导出时提示或加密 |
| 日志导出 | 固定格式（模块版本 + 宿主版本 + 时间 + 各 hook 命中状态），配 issue 模板 |
| GPL 义务 | 源码公开（已做）+ 移植文件保留版权头 + LICENSE/NOTICE |
| 措辞 | 全站避开「漫游 / roaming」；README 强调「仅供学习研究，不提供安装包分发」 |

### 9.6 三个易忽略点

1. **多进程的隐蔽坑**：RemotePreferences 跨进程写一致性、子进程改开关主进程不刷新（比 `isFirstPackage` 更难排查）
2. **42 项新增无源码兜底**：指纹只能从 jadx 反编译推断，失败率最高 → **验证应提前到与实机冒烟同一批**
3. **签名固定**：同一 ApplicationID 换签名会导致 LSPosed 不识别，第一版就要固定并归档 keystore；与 FuckBiliAds 的 hook 重叠点需列具体清单

### 9.7 执行原则变更：纵向切片

**放弃按功能类别平铺**，改为：
```
#9/#10/#11 完成后 → 立刻做「区域解锁」端到端切片
（DexKit 定位 → 开关 → UPOS 生效 → 真机验证）
→ 跑通后其余 200 项 = 换指纹 + 换逻辑的复制
→ 再按 hook 簇横向铺开
```
