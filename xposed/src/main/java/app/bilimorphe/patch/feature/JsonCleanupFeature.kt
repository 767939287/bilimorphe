package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Modifier

/**
 * JSON 层三项净化（对应 1.28.6 的 `disable_comm_skin` / `force_old_favorite` / `direct_owner_space`）。
 *
 * 行为依据（`kofua/inbb.java`、`kofua/SLT.java`）：
 * - `disable_comm_skin`：配置里移除 `common_equip`（通用装扮）；
 * - `force_old_favorite`：把 uri 含 `favourite` 的入口改成 `bilibili://user_center/favourite`（旧版收藏页）；
 * - `direct_owner_space`：在 `ab_result` 中写入 `view_up_space = {"success":true,"path":<语言>}`，
 *   让宿主直接进 UP 主空间。
 *
 * 实现方式：hook fastjson 的 `JSON.parseObject(...)` 出口，只对包含特征子串的文本做处理，
 * 避免给热路径增加无谓开销（三项都关闭时整个 Feature 不安装）。
 */
object JsonCleanupFeature : Patch(
    name = "Json cleanup",
    description = "Removes common skin config, rewrites favourite entries and enables direct owner space.",
) {

    private const val JSON = "com.alibaba.fastjson.JSON"
    private const val DATA_PATH = "data"
    private const val FAVOURITE_OLD = "bilibili://user_center/favourite"

    override fun applyTo(engine: PatchEngine) {
        val disableCommSkin = ModulePrefs.isEnabled(SettingsKeys.DISABLE_COMM_SKIN)
        val forceOldFavorite = ModulePrefs.isEnabled(SettingsKeys.FORCE_OLD_FAVORITE)
        val directOwnerSpace = ModulePrefs.isEnabled(SettingsKeys.DIRECT_OWNER_SPACE)
        if (!disableCommSkin && !forceOldFavorite && !directOwnerSpace) {
            Logger.i("[json_cleanup] off, skip")
            return
        }

        val cls = engine.findClass(JSON)
        if (cls == null) {
            Logger.i("[json_cleanup] fastjson JSON missing")
            return
        }

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val text = param.args.getOrNull(0) as? String ?: return
                val result = param.result as? JSONObject ?: return
                runCatching {
                    if (disableCommSkin && text.contains("common_equip")) {
                        result.remove("common_equip")
                    }
                    if (forceOldFavorite && text.contains("favourite")) {
                        rewriteFavourite(result)
                    }
                    if (directOwnerSpace && text.contains("ab_result")) {
                        injectViewUpSpace(result)
                    }
                }.onFailure { Logger.e("[json_cleanup] apply failed", it) }
            }
        }

        var hooked = 0
        for (m in cls.declaredMethods) {
            if (m.name != "parseObject" || !Modifier.isStatic(m.modifiers)) continue
            if (m.parameterTypes.isEmpty() || m.parameterTypes[0] != String::class.java) continue
            runCatching { XposedBridge.hookMethod(m, hook); hooked++ }
        }
        Logger.i(
            "[json_cleanup] hooked=$hooked commSkin=$disableCommSkin oldFav=$forceOldFavorite ownerSpace=$directOwnerSpace"
        )
    }

    /** 递归把含 `favourite` 的 uri 改成旧版收藏页。 */
    private fun rewriteFavourite(node: Any?) {
        when (node) {
            is JSONObject -> {
                if (node.optString("uri").contains("favourite")) {
                    runCatching { node.put("uri", FAVOURITE_OLD) }
                }
                node.keys().forEach { key -> rewriteFavourite(node.opt(key)) }
            }

            is JSONArray -> for (i in 0 until node.length()) rewriteFavourite(node.opt(i))
            else -> Unit
        }
    }

    /** 在 ab_result 中声明「直接进 UP 主空间」可用。 */
    private fun injectViewUpSpace(root: JSONObject) {
        val data = root.optJSONObject(DATA_PATH) ?: return
        val abResult = data.optJSONObject("ab_result") ?: return
        val viewUpSpace = abResult.optJSONObject("view_up_space") ?: JSONObject()
        runCatching {
            viewUpSpace.put("success", true)
            viewUpSpace.put(DATA_PATH, java.util.Locale.getDefault().toLanguageTag())
            abResult.put("view_up_space", viewUpSpace)
        }.onFailure { Logger.e("[json_cleanup] view_up_space failed", it) }
    }
}
