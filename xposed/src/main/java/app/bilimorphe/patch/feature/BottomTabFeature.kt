package app.bilimorphe.patch.feature

import android.os.Bundle
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 底栏默认 tab（对应 1.28.6 的 `prefer_bottom_tab`，字符串项）。
 *
 * 行为依据（`kofua/ZXc.java` + 宿主 `tv/danmaku/bili/home/components/pagebuild/a.java`）：
 * 宿主启动时读取 `bundle.getString("key_selected_bottom_tab_url")`，1.28.6 在无显式 url 时
 * 按设置返回 `bilibili://following/home`（动态）或 `bilibili://user_center/mine`（我的）。
 *
 * 本项目重写为运行时 hook：进入宿主该组件前，若 bundle 未指定 url，则按偏好写入。
 *
 * 注意：宿主类名为 `tv.danmaku.bili.home.components.pagebuild.a`（含混淆短类名），
 * 跨版本可能变化；候选列表可按需追加。
 */
object BottomTabFeature : Patch(
    name = "Bottom tab preference",
    description = "Applies the preferred default bottom tab when the host did not specify one.",
) {

    private const val TAB_URL_KEY = "key_selected_bottom_tab_url"

    private val CLASS_CANDIDATES = listOf(
        "tv.danmaku.bili.home.components.pagebuild.a",
    )

    override fun applyTo(engine: PatchEngine) {
        val preference = ModulePrefs.getString(SettingsKeys.PREFER_BOTTOM_TAB, "home")
        if (preference.isEmpty() || preference == "home") {
            Logger.i("[bottom_tab] preference=$preference (host default), skip")
            return
        }

        val url = when (preference) {
            "dynamic" -> "bilibili://following/home"
            "mine" -> "bilibili://user_center/mine"
            else -> {
                Logger.i("[bottom_tab] unknown preference: $preference")
                return
            }
        }

        var hooked = 0
        for (className in CLASS_CANDIDATES) {
            val cls = engine.findClass(className) ?: continue
            val method = cls.declaredMethods.firstOrNull {
                it.name == "e" && it.parameterCount == 1 && it.parameterTypes[0] == Bundle::class.java
            } ?: continue
            runCatching {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val bundle = param.args[0] as? Bundle ?: return
                        if (bundle.getString(TAB_URL_KEY).isNullOrEmpty()) {
                            bundle.putString(TAB_URL_KEY, url)
                        }
                    }
                })
                hooked++
                Logger.i("[bottom_tab] hooked ${cls.name}#${method.name} -> $url")
            }.onFailure { Logger.e("[bottom_tab] hook failed: $className", it) }
        }

        if (hooked == 0) Logger.i("[bottom_tab] no target matched")
    }
}
