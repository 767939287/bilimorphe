package app.revanced.bilibili.settings.fragments

import android.os.Bundle
import app.revanced.bilibili.settings.search.annotation.SettingFragment
import app.revanced.bilibili.utils.Utils

@SettingFragment("bili_setting_customize_search")
class CustomizeSearchFragment : BiliBaseSettingFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        disablePreference("search_type", PrefsDisableReason.AppVersion) {
            Utils.isHd()
        }
        disablePreference("search_keywords", PrefsDisableReason.AppVersion) {
            Utils.isHd()
        }
    }
}
