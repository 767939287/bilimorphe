package app.bilimorphe.settings

/**
 * 设置键常量。键名与 BiliRoamingX 1.28.6 保持一致，便于从旧配置导入/对照。
 *
 * 共 38 项 = 1.28.6 新增 41 键 − `grpc_proxies`（ADR-002 不做）− 泰区 2 项（用户决定不做）。
 */
object SettingsKeys {

    // ---- 区域解锁（1.23.3 方案，ADR-002）----
    /** 解锁总开关（integrations 内部键名即 `main_func`）。 */
    const val MAIN_FUNC = "main_func"
    const val ALLOW_DOWNLOAD = "allow_download"
    const val TW_SERVER = "tw_server"
    const val HK_SERVER = "hk_server"
    const val CN_SERVER = "cn_server"
    const val TH_SERVER = "th_server"
    const val UPOS_HOST = "upos_host"
    const val ACCESS_KEY_MAIN = "access_key_main"

    // ---- AI ----
    const val CATEGORY_AI_SUBTITLE_TRANSLATION = "category_ai_subtitle_translation"
    const val AI_SUBTITLE_TRANSLATION_PROMPT = "ai_subtitle_translation_prompt"
    const val CATEGORY_AI_VIDEO_FILTER = "category_ai_video_filter"
    const val AI_VIDEO_FILTER_ENABLED = "ai_video_filter_enabled"
    const val AI_VIDEO_FILTER_PROMPT = "ai_video_filter_prompt"
    const val AI_CONFIG = "ai_config"

    // ---- SponsorBlock ----
    const val SB_CATEGORY = "sb_category"
    const val SKIP_REWARD_AD = "skip_reward_ad"

    // ---- 小窗 / 迷你 ----
    const val ALWAYS_MINI_PLAY = "always_mini_play"
    const val MINI_DANMAKU = "mini_danmaku"
    const val MINI_FOLLOW_FULL = "mini_follow_full"
    const val MINI_SUBTITLE = "mini_subtitle"
    const val NOT_SUICIDE = "not_suicide"

    // ---- 播放 ----
    /** 时间点空降（1.23.3 键名，非 38 键之内）。 */
    const val TIME_AIRBORNE = "time_airborne"
    const val PREFER_LOSSLESS = "prefer_lossless"
    const val RELEASE_AUDIO_FOCUS = "release_audio_focus"
    const val CLEAR_AUDIO = "clear_audio"

    // ---- 广告 / 净化 ----
    /** 1.23.3 键名：屏蔽 UP 主推荐广告。 */
    const val BLOCK_UP_RCMD_ADS = "block_up_rcmd_ads"

    /** 1.23.3 键名：隐藏关注按钮（原为 comment/dynamic 的 StringSet，这里简化为布尔）。 */
    const val BLOCK_FOLLOW_BUTTON = "block_follow_button"

    /** 1.23.3 键名：移除我的页大会员横图区块。 */
    const val REMOVE_VIP_SECTION = "remove_vip_section"
    /** 1.23.3 键名：播放页伪装未处于多窗口模式。 */
    const val FAKE_NOT_IN_MULTI_WINDOW = "fake_not_in_multi_window"

    /** 1.23.3 能力：直播防误触三项。 */
    const val FORBID_LIVE_ROOM_AUTO_FLOAT = "forbid_live_room_auto_float"
    const val FORBID_LIVE_ROOM_SLIDE_LEFT = "forbid_live_room_slide_left"
    const val DISABLE_LIVE_ROOM_DOUBLE_CLICK = "disable_live_room_double_click"
    const val FORBID_SWITCH_LIVE_ROOM = "forbid_switch_live_room"

    /** 1.23.3 能力（简化）：评论/动态文本不折叠。 */
    const val TEXT_UNFOLD = "text_unfold"
    const val BLOCK_PLAY_PAUSE_AD = "block_play_pause_ad"
    const val HIDE_LIVE_BANNER = "hide_live_banner"
    const val BLOCK_COMMENT_OP_SEARCH = "block_comment_op_search"
    const val BLOCK_COMMENT_RATING = "block_comment_rating"

    // ---- 界面 / 外观 / 系统 ----
    const val BLOCK_ACTIVITY_LABEL = "block_activity_label"
    const val BLOCK_BACK = "block_back"
    const val DIRECT_OWNER_SPACE = "direct_owner_space"
    const val DISABLE_COLOROS_TRANSITIONS = "disable_coloros_transitions"
    const val DISABLE_COMM_SKIN = "disable_comm_skin"
    const val DISABLE_HOT_PATCH = "disable_hot_patch"
    const val FORCE_TABLET_MODE = "force_tablet_mode"
    const val PIC_NO_FOLD = "pic_no_fold"
    const val REMOVE_VIDEO_SCREENSHOT_WATERMARK = "remove_video_screenshot_watermark"
    const val SHOW_SEX = "show_sex"
    const val PREFER_BOTTOM_TAB = "prefer_bottom_tab"
    const val CATEGORY_DANMAKU = "category_danmaku"

    // ---- 旧版回退 ----
    const val FORCE_OLD_COMMENTS = "force_old_comments"
    const val FORCE_OLD_FAVORITE = "force_old_favorite"

    // ---- 杂项 ----
    const val LIVE_NO_BLOCK = "live_no_block"
    const val REPORT = "report"
    const val SUPPORT = "support"
    const val HIDDEN_FEATURES_GROUP = "hidden_features_group"

    /** 全部布尔开关键（分类页 / 配置项 / 入口项不计入），供批量读写与导入导出使用。 */
    val ALL_BOOLEAN: List<String> = listOf(
        MAIN_FUNC,
        ALLOW_DOWNLOAD,
        TIME_AIRBORNE,
        BLOCK_UP_RCMD_ADS,
        BLOCK_FOLLOW_BUTTON,
        REMOVE_VIP_SECTION,
        FAKE_NOT_IN_MULTI_WINDOW,
        FORBID_LIVE_ROOM_AUTO_FLOAT,
        FORBID_LIVE_ROOM_SLIDE_LEFT,
        DISABLE_LIVE_ROOM_DOUBLE_CLICK,
        FORBID_SWITCH_LIVE_ROOM,
        TEXT_UNFOLD,
        AI_VIDEO_FILTER_ENABLED,
        SKIP_REWARD_AD,
        ALWAYS_MINI_PLAY,
        MINI_DANMAKU,
        MINI_FOLLOW_FULL,
        MINI_SUBTITLE,
        NOT_SUICIDE,
        PREFER_LOSSLESS,
        RELEASE_AUDIO_FOCUS,
        CLEAR_AUDIO,
        BLOCK_PLAY_PAUSE_AD,
        HIDE_LIVE_BANNER,
        BLOCK_COMMENT_OP_SEARCH,
        BLOCK_COMMENT_RATING,
        BLOCK_ACTIVITY_LABEL,
        BLOCK_BACK,
        DIRECT_OWNER_SPACE,
        DISABLE_COLOROS_TRANSITIONS,
        DISABLE_COMM_SKIN,
        DISABLE_HOT_PATCH,
        FORCE_TABLET_MODE,
        PIC_NO_FOLD,
        REMOVE_VIDEO_SCREENSHOT_WATERMARK,
        SHOW_SEX,
        PREFER_BOTTOM_TAB,
        FORCE_OLD_COMMENTS,
        FORCE_OLD_FAVORITE,
        LIVE_NO_BLOCK,
    )

    /** 非布尔项：分类页、配置弹窗、外部入口。 */
    val NON_BOOLEAN: List<String> = listOf(
        TW_SERVER,
        HK_SERVER,
        CN_SERVER,
        TH_SERVER,
        UPOS_HOST,
        ACCESS_KEY_MAIN,
        CATEGORY_AI_SUBTITLE_TRANSLATION,
        AI_SUBTITLE_TRANSLATION_PROMPT,
        CATEGORY_AI_VIDEO_FILTER,
        AI_VIDEO_FILTER_PROMPT,
        AI_CONFIG,
        SB_CATEGORY,
        CATEGORY_DANMAKU,
        REPORT,
        SUPPORT,
        HIDDEN_FEATURES_GROUP,
    )

    /** 首轮纵切验证用的三项（定位/hook 链路最短）。 */
    val SLICE_FIRST: List<String> = listOf(
        DISABLE_HOT_PATCH,
        BLOCK_BACK,
        PIC_NO_FOLD,
    )
}
