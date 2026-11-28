package app.bilimorphe.patch.feature

import android.content.ClipData
import android.content.ClipboardManager
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 复制 UID 时去掉 `UID:` 前缀（1.23.3 的 `Uid copy no prefix`）。
 *
 * 1.23.3 的钩法是改宿主格式化字符串（`String.format("UID:%s")` 所在方法），
 * 但 9.21.0 已无该字符串，类名也每版都变。
 *
 * 本项目改用**剪贴板入口**实现：hook `ClipboardManager.setText` / `setPrimaryClip`，
 * 若复制内容形如 `UID:12345` 则去掉前缀。这样与宿主版本、混淆完全解耦。
 *
 * 开关键沿用 1.23.3：`uid_copy_no_prefix`（设置入口在「用户空间」页）。
 */
object UidCopyFeature : Patch(
    name = "Uid copy no prefix",
    description = "Strips the UID: prefix when copying a user id to the clipboard.",
) {

    private const val PREFIX = "UID:"

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.UID_COPY_NO_PREFIX)) {
            Logger.i("[uid_copy] off, skip")
            return
        }

        val clipboard = runCatching { Class.forName("android.content.ClipboardManager") }.getOrNull()
        if (clipboard == null) {
            Logger.i("[uid_copy] ClipboardManager missing")
            return
        }

        var hooked = 0

        clipboard.declaredMethods
            .firstOrNull { it.name == "setText" && it.parameterCount == 1 }
            ?.let { method ->
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val text = param.args[0]?.toString() ?: return
                            strip(text)?.let { param.args[0] = it }
                        }
                    })
                    hooked++
                }.onFailure { Logger.e("[uid_copy] setText hook failed", it) }
            }

        clipboard.declaredMethods
            .firstOrNull { it.name == "setPrimaryClip" && it.parameterCount == 1 }
            ?.let { method ->
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val clip = param.args[0] as? ClipData ?: return
                            if (clip.itemCount == 0) return
                            val original = clip.getItemAt(0).text?.toString() ?: return
                            val stripped = strip(original) ?: return
                            param.args[0] = ClipData.newPlainText(clip.description?.label, stripped)
                        }
                    })
                    hooked++
                }.onFailure { Logger.e("[uid_copy] setPrimaryClip hook failed", it) }
            }

        Logger.i("[uid_copy] hooked=$hooked")
    }

    /** 只处理「UID:数字」形式，避免误伤其它复制内容。 */
    private fun strip(text: String): String? {
        val trimmed = text.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        val rest = trimmed.removePrefix(PREFIX).trim()
        if (rest.isEmpty() || rest.any { !it.isDigit() }) return null
        return rest
    }
}
