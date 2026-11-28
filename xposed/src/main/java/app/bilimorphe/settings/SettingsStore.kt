package app.bilimorphe.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置读写入口。
 *
 * 文件名固定为 [FILE]，宿主进程侧通过 libxposed 的 RemotePreferences（同名）读取，
 * 因此这里不要改名。
 */
object SettingsStore {

    /** SharedPreferences 文件名，同时用作 RemotePreferences 名称。 */
    const val FILE = "bilimorphe_settings"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 开关是否开启；缺省 false（fail-open，未开启时不 hook）。 */
    fun isEnabled(prefs: SharedPreferences, key: String): Boolean =
        prefs.getBoolean(key, false)

    fun setEnabled(prefs: SharedPreferences, key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
