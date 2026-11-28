package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 图片不折叠（对应 1.28.6 的 `pic_no_fold`）。
 *
 * 行为依据：1.28.6 在评论数据落地处执行
 * `replyInfo.getReplyControl().setFoldPictures(false)`，对顶层评论与楼中楼各做一次。
 *
 * 本项目重写为运行时 hook：拦截评论 protobuf 的 `ReplyInfo.parseFrom(...)`，
 * 解析完成后把 `foldPictures` 置 false，并向下处理一层楼中楼。
 * 只依赖 protobuf 生成的方法名（`parseFrom` / `hasReplyControl` / `setFoldPictures`），
 * 这些名字在宿主中未被混淆。
 */
object PicNoFoldFeature : Patch(
    name = "Pic no fold",
    description = "Keeps comment pictures expanded by clearing the fold flag on parsed replies.",
) {

    private const val REPLY_INFO = "com.bapis.bilibili.main.community.reply.v1.ReplyInfo"
    private const val REPLY_CONTROL = "com.bapis.bilibili.main.community.reply.v1.ReplyControl"

    /** 楼中楼最多向下处理两层，避免大评论区递归过深拖慢解析。 */
    private const val MAX_DEPTH = 2

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.PIC_NO_FOLD)) {
            Logger.i("[pic_no_fold] off, skip")
            return
        }

        val infoCls = engine.findClass(REPLY_INFO)
        val ctrlCls = engine.findClass(REPLY_CONTROL)
        if (infoCls == null || ctrlCls == null) {
            Logger.i("[pic_no_fold] protobuf class missing")
            return
        }

        val hasCtrl = method(infoCls, "hasReplyControl") ?: return logMissing("hasReplyControl")
        val getCtrl = method(infoCls, "getReplyControl") ?: return logMissing("getReplyControl")
        val setFold = method(ctrlCls, "setFoldPictures", java.lang.Boolean.TYPE)
            ?: return logMissing("setFoldPictures")
        val getReplies = method(infoCls, "getRepliesList")

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val info = param.result ?: return
                runCatching { unfold(info, hasCtrl, getCtrl, setFold, getReplies, 0) }
                    .onFailure { Logger.e("[pic_no_fold] apply failed", it) }
            }
        }

        var hooked = 0
        for (m in infoCls.declaredMethods) {
            if (m.name != "parseFrom") continue
            if (!Modifier.isStatic(m.modifiers)) continue
            if (!infoCls.isAssignableFrom(m.returnType)) continue
            runCatching {
                XposedBridge.hookMethod(m, hook)
                hooked++
            }.onFailure { Logger.e("[pic_no_fold] hook failed: ${m.parameterCount} args", it) }
        }
        Logger.i("[pic_no_fold] hooked parsers=$hooked")
    }

    private fun unfold(
        info: Any,
        hasCtrl: Method,
        getCtrl: Method,
        setFold: Method,
        getReplies: Method?,
        depth: Int,
    ) {
        if (hasCtrl.invoke(info) as? Boolean == true) {
            getCtrl.invoke(info)?.let { ctrl -> setFold.invoke(ctrl, false) }
        }
        if (depth >= MAX_DEPTH || getReplies == null) return
        val replies = runCatching { getReplies.invoke(info) as? List<*> }.getOrNull() ?: return
        for (reply in replies) {
            if (reply != null) unfold(reply, hasCtrl, getCtrl, setFold, getReplies, depth + 1)
        }
    }

    private fun method(cls: Class<*>, name: String, vararg params: Class<*>): Method? =
        runCatching { cls.getMethod(name, *params).apply { isAccessible = true } }.getOrNull()

    private fun logMissing(name: String) {
        Logger.i("[pic_no_fold] method missing: $name")
    }
}
