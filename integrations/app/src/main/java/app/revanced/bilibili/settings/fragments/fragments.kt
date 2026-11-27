package app.revanced.bilibili.settings.fragments

import app.revanced.bilibili.patches.json.JSONPatch
import app.revanced.bilibili.settings.Settings
import app.revanced.bilibili.settings.search.annotation.SettingFragment

@SettingFragment("bili_setting_block_follow")
class BlockFollowFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_customize_bottom")
class CustomizeBottomFragment :
    BaseDynamicItemsFragment(Settings.ShowingBottomItems, JSONPatch.bottomItems)

@SettingFragment("bili_setting_customize_dynamic")
class CustomizeDynamicFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_home_tab")
class CustomizeHomeTabFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_customize_live_room")
class CustomizeLiveRoomFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_customize_mine")
class CustomizeMineFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_customize_space")
class CustomizeSpaceFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_display_size")
class DisplaySizeFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_filter_comment")
class FilterCommentFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_filter_dynamic_by_type")
class FilterDynamicByTypeFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_filter_home_by_type")
class FilterHomeRcmdByTypeFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_filter_search_type")
class FilterSearchTypeFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_filter_story_by_type")
class FilterStoryByTypeFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_full_screen_quality_mobile")
class FullScreenQualityMobileSettingFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_full_screen_quality")
class FullScreenQualitySettingFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_half_screen_quality")
class HalfScreenQualitySettingFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_player_version")
class PlayerVersionFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_live_popups")
class PurifyLivePopupsFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_space")
class SpaceFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_subtitle_translate_server")
class SubtitleTranslateServerFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_upos")
class UposSettingFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_story_ui_style")
class StoryUIStyleFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_cover_ratio")
class PegasusCoverRatioFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_video_popups")
class RemoveVideoPopupsFragment : BiliBaseSettingFragment()

@SettingFragment("bili_setting_purify_search_types")
class PurifySearchTypesFragment : BiliBaseSettingFragment()
