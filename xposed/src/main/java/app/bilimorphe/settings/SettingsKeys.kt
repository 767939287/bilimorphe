package app.bilimorphe.settings

/**
 * 设置键常量。键名与 BiliRoamingX 1.28.6 保持一致，便于从旧配置导入/对照。
 *
 * 共 38 项 = 1.28.6 新增 41 键 − `grpc_proxies`（ADR-002 不做）− 泰区 2 项（用户决定不做）。
 */
object SettingsKeys {

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
    const val PREFER_LOSSLESS = "prefer_lossless"
    const val RELEASE_AUDIO_FOCUS = "release_audio_focus"
    const val CLEAR_AUDIO = "clear_audio"

    // ---- 广告 / 净化 ----
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
