# hook 点在原版 9.20.0 上的验证（2026-11-27）

> 背景：此前所有分析基于**修改版** `Bili-v9.20.0-x1.28.6@bb_show.apk`（宿主被注入过代码），
> 存在「注入引入的类/方法在原版不存在」的风险。本文件记录用**原版** `哔哩哔哩v9.20.0.apk` 的复核结果。
>
> 方法：解压原版 38 个 dex（314 MB）→ `scripts/dex_index.py` 生成全量索引
> （方法 232.4 万条 / 字段 130.3 万条）→ 逐项比对修改版索引。

## 一、结论：18 个方法锚点全部在原版存在

| 功能 | 锚点（原版确认） |
|---|---|
| `block_back` | `LiveRoomActivityV3` / `UnitedBizDetailsActivity` / `UnitedPlaylistActivity` 各自声明 `onBackPressed()V` |
| `not_suicide` | `tv.danmaku.bili.MainActivityV2.onBackPressed()V` |
| `skip_reward_ad` | `com.bilibili.ad.reward.activity.RewardAdDisplayActivity.onCreate(Bundle)V` |
| 云控覆盖（4 项） | `com.bilibili.lib.blconfig.ConfigManager.isHitFF(String)Z`、`getConfig(String,String)String` |
| `pic_no_fold` | `ReplyInfo.parseFrom`×10、`ReplyControl.setFoldPictures(Z)V` |
| 评论三项 | `MainListReply.parseFrom`×10、`clearOperationV2()V`、`clearEsportsGradeCard()V`、`removeSubjectTopCards(I)V` |
| 播放两项 | `app.playurl.v1.PlayURLReply`、`app.playurl.v1.PlayViewReply`、`pgc.v1/v2.PlayViewReply`、`cheese.v1.PlayViewReply` 均有 `parseFrom`×10 |
| 直播净化 | `JavaBeanDeserializer.deserialze`×2、`JSON.parseObject(String)` |
| `hide_live_banner` | `BiliLiveHomeFeedPage.pageList : List`、`BiliLiveHomeFeedData.cardType : String` |
| `live_no_block` | `BiliLiveRoomInfo.blockInfo : BlockInfo` |
| `prefer_bottom_tab` | `home.components.pagebuild.a.e(Bundle)V`；字符串 `key_selected_bottom_tab_url` 存在于 classes30.dex |
| 截图水印 | 默认包类 `Lvdv0;.a(Bitmap,Rect,Bitmap,Drawable,String,DisplayMetrics,boolean)Bitmap` |

## 二、发现并修正的 3 个错误

| # | 错误 | 原因 | 修正 |
|---|---|---|---|
| 1 | `defpackage.vdv0` 无法加载 | jadx 把**默认包**（无包名）的类显示成 `defpackage.xxx`，dex 里的真实描述符是 `Lvdv0;`，运行时类名**不带包名** | 候选改为 `"vdv0"` |
| 2 | 奖励广告类名写错 | 误写成 `com.bilibili.bilipay.rewardad.RewardAdDisplayActivity`（原版无此类） | 改为 `com.bilibili.ad.reward.activity.RewardAdDisplayActivity` |
| 3 | `live_no_block` 多做了事 | 把 `newSwitchInfo["room-player-watermark"]` 归进来；该操作在 1.28.6 属于另一开关（`Nhkb.Fufb`，不在 38 键内），且字段名是 `newSwitchInfo` 而非 `switchInfo` | 只保留 `blockInfo = null` |

另外，`hide_live_banner` 的过滤条件原先按「类名含 Banner」推测，核实 1.28.6 的 `cOm(3)` 后
改为**精确条件** `cardType == "banner_v2"`。

## 三、仍需实机确认的部分

这 5 项靠 DexKit 字符串探针（`MiniPlayerProbeFeature`）在实机采集真实签名后再固化：

`always_mini_play`、`mini_danmaku`、`mini_subtitle`、`mini_follow_full`、`ai_video_filter_enabled`

原因：宿主侧相关类名在 dex 中仅以字符串常量出现（`autoMiniPlayerSettingSuppressor` 见于
classes9 / 11 / 35，索引中无对应类），无稳定类名锚点。

## 四、复现命令

```bash
# 1) 解压原版 dex
mkdir -p /tmp/dexorig && cd /tmp/dexorig
unzip -o -q "/root/project/Android-hook-file/哔哩哔哩v9.20.0.apk" "classes*.dex"

# 2) 建索引（方法 232 万条，约 5 秒）
python3 scripts/dex_index.py /tmp/dexorig -o /root/project/Android-hook-file/dex-index-9.20.0-orig.tsv
python3 scripts/dex_index.py /tmp/dexorig --fields -o /tmp/fields-orig.tsv
```
