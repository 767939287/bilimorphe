package app.bilimorphe.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

/**
 * 通用子设置页：按 arguments 里的 `page` 加载对应 XML。
 *
 * 1.23.3 的设置页布局已原样搬入 `res/xml/biliroaming_setting_*.xml`，
 * 键名与 integrations 的 `Settings` 完全一致，因此只要用户改了开关，
 * 宿主侧读到的就是同一份配置（见 [app.bilimorphe.patch.feature.IntegrationCallFeature] 等）。
 */
class SubSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val page = arguments?.getString(ARG_PAGE)
        if (page.isNullOrEmpty()) return
        val resId = resources.getIdentifier(page, "xml", requireContext().packageName)
        if (resId != 0) {
            setPreferencesFromResource(resId, rootKey)
        }
    }

    companion object {
        const val ARG_PAGE = "page"
    }
}
