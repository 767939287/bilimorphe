package app.bilimorphe.settings

import app.bilimorphe.util.Logger
import de.robv.android.xposed.XSharedPreferences

/**
 * 宿主进程侧读取模块设置。
 *
 * 设置页运行在模块自己的进程（`app.bilimorphe`），宿主进程需要用
 * [XSharedPreferences] 读取模块私有 SharedPreferences；LSPosed 对其有兼容实现。
 *
 * 注意：每次读取都会 `reload()`，因此开关改动在下一次读取时生效；已注册的 hook
 * 是否生效仍取决于安装时机（见各 Feature 的注释）。
 */
object ModulePrefs {

    /** 模块包名，与 `xposed/build.gradle.kts` 的 applicationId 一致。 */
    private const val MODULE_PACKAGE = "app.bilimorphe"

    private val prefs: XSharedPreferences? by lazy {
        runCatching { XSharedPreferences(MODULE_PACKAGE, SettingsStore.FILE) }
            .onSuccess { Logger.i("module prefs opened: ${SettingsStore.FILE}") }
            .onFailure { Logger.e("module prefs unavailable", it) }
            .getOrNull()
    }

    /** 读取布尔开关；任何异常都按关闭处理（fail-open：不 hook 宿主）。 */
    fun isEnabled(key: String): Boolean {
        val p = prefs ?: return false
        return runCatching {
            p.reload()
            p.getBoolean(key, false)
        }.onFailure { Logger.e("read pref failed: $key", it) }
            .getOrDefault(false)
    }

    /** 读取字符串项（提示词、代理地址等）。 */
    fun getString(key: String, default: String = ""): String {
        val p = prefs ?: return default
        return runCatching {
            p.reload()
            p.getString(key, default) ?: default
        }.onFailure { Logger.e("read pref failed: $key", it) }
            .getOrDefault(default)
    }
}
