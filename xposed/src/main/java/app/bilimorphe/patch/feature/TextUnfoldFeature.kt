package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 文本不折叠（1.23.3 的 `Text fold`）。
 *
 * 1.23.3 是「评论 / 动态各自可配置最大行数」（`TextFoldPatch.getCommentMaxLines` /
 * `getDynMaxLines`），但这部分实现的源码在开源备份里缺失。
 * 本项目改为等价效果：把两处行数限制直接放开（不折叠），开关为布尔项。
 *
 * 落点（9.21.0 实测存在）：
 * - `com.bilibili.bplus.followingcard.widget.EllipsizingTextView.setMaxLines(int)` —— 动态卡片文本
 * - `com.bapis.bilibili.main.community.reply.v1.ReplyControl.setMaxLine(int)` —— 评论正文
 */
object TextUnfoldFeature : Patch(
    name = "Text unfold",
    description = "Removes max-line limits on comment and dynamic text.",
) {

    private const val ELLIPSIZING_TEXT_VIEW = "com.bilibili.bplus.followingcard.widget.EllipsizingTextView"
    private const val REPLY_CONTROL = "com.bapis.bilibili.main.community.reply.v1.ReplyControl"

    /** 放开行数限制用的大值。 */
    private const val UNLIMITED_LINES = Int.MAX_VALUE

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.TEXT_UNFOLD)) {
            Logger.i("[text_unfold] off, skip")
            return
        }

        var hooked = 0

        engine.findClass(ELLIPSIZING_TEXT_VIEW)?.let { cls ->
            cls.declaredMethods
                .filter { it.name == "setMaxLines" && it.parameterCount == 1 }
                .forEach { method ->
                    runCatching {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.args[0] = UNLIMITED_LINES
                            }
                        })
                        hooked++
                    }.onFailure { Logger.e("[text_unfold] dynamic hook failed", it) }
                }
        } ?: Logger.i("[text_unfold] EllipsizingTextView missing")

        engine.findClass(REPLY_CONTROL)?.let { cls ->
            val method = cls.declaredMethods.firstOrNull {
                it.name == "setMaxLine" && it.parameterCount == 1
            }
            if (method != null) {
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.args[0] = UNLIMITED_LINES
                        }
                    })
                    hooked++
                }.onFailure { Logger.e("[text_unfold] comment hook failed", it) }
            }
        } ?: Logger.i("[text_unfold] ReplyControl missing")

        Logger.i("[text_unfold] hooked=$hooked")
    }
}
