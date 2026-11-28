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
 * 评论增强三项（对应 1.28.6 的 `block_comment_op_search` / `block_comment_rating` / `show_sex`）。
 *
 * 行为依据（`kofua/hFw.java`、`kofua/QOmb.java`）：
 * - `block_comment_op_search`：`MainListReply` 的 `operationV2.type == SEARCH` 时 `clearOperationV2()`，
 *   并移除 `subjectTopCards` 中类型为 SEARCH 的卡片；
 * - `block_comment_rating`：`clearEsportsGradeCard()`，并移除对应卡片；
 * - `show_sex`：把评论者性别拼到 IP 属地后面（`location + " " + sex`）。
 *
 * 本项目重写为运行时 hook：拦截评论列表 `MainListReply.parseFrom(...)`，解析完成后就地修改。
 */
object CommentTweaksFeature : Patch(
    name = "Comment tweaks",
    description = "Blocks comment search/rating cards and appends reviewer gender to the IP location line.",
) {

    private const val MAIN_LIST_REPLY = "com.bapis.bilibili.main.community.reply.v1.MainListReply"

    override fun applyTo(engine: PatchEngine) {
        val blockSearch = ModulePrefs.isEnabled(SettingsKeys.BLOCK_COMMENT_OP_SEARCH)
        val blockRating = ModulePrefs.isEnabled(SettingsKeys.BLOCK_COMMENT_RATING)
        val showSex = ModulePrefs.isEnabled(SettingsKeys.SHOW_SEX)
        if (!blockSearch && !blockRating && !showSex) {
            Logger.i("[comment_tweaks] off, skip")
            return
        }

        val cls = engine.findClass(MAIN_LIST_REPLY)
        if (cls == null) {
            Logger.i("[comment_tweaks] $MAIN_LIST_REPLY missing")
            return
        }

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val reply = param.result ?: return
                runCatching {
                    if (blockSearch) blockSearchCards(reply)
                    if (blockRating) blockRatingCards(reply)
                    if (showSex) appendGender(reply)
                }.onFailure { Logger.e("[comment_tweaks] apply failed", it) }
            }
        }

        var hooked = 0
        for (m in cls.declaredMethods) {
            if (m.name != "parseFrom" || !Modifier.isStatic(m.modifiers)) continue
            if (!cls.isAssignableFrom(m.returnType)) continue
            runCatching { XposedBridge.hookMethod(m, hook); hooked++ }
        }
        Logger.i("[comment_tweaks] hooked parsers=$hooked search=$blockSearch rating=$blockRating sex=$showSex")
    }

    /** 去掉「按关键词搜索」入口：清 operationV2 + 移除 SEARCH 卡片。 */
    private fun blockSearchCards(reply: Any) {
        val type = call(reply, "getOperationV2")?.let { call(it, "getType") }
        if (type != null && type.toString().contains("SEARCH")) {
            callVoid(reply, "clearOperationV2")
        }
        removeCardsByType(reply, "SEARCH")
    }

    /** 去掉「电竞赛事评分」卡片。 */
    private fun blockRatingCards(reply: Any) {
        callVoid(reply, "clearEsportsGradeCard")
        removeCardsByType(reply, "ESPORTS")
    }

    private fun removeCardsByType(reply: Any, marker: String) {
        val cards = call(reply, "getSubjectTopCardsList") as? List<*> ?: return
        val doomed = cards.indices.filter { idx ->
            val card = cards[idx] ?: return@filter false
            val type = call(card, "getOperation")?.let { call(it, "getType") }
            type != null && type.toString().contains(marker)
        }
        // 逆序删除，避免索引位移
        doomed.sortedDescending().forEach { idx ->
            runCatching { callVoidArgs(reply, "removeSubjectTopCards", intArrayOf(idx)) }
        }
    }

    /** 把性别拼到 IP 属地末尾（已有则跳过）。 */
    private fun appendGender(reply: Any) {
        val replies = call(reply, "getRepliesList") as? List<*> ?: return
        for (item in replies) {
            if (item == null) continue
            runCatching {
                val control = call(item, "getReplyControl") ?: return@runCatching
                val member = call(item, "getMember") ?: return@runCatching
                val sex = call(member, "getSex") as? String ?: return@runCatching
                if (sex.isEmpty()) return@runCatching
                val location = (call(control, "getLocation") as? String).orEmpty()
                if (location.endsWith(sex)) return@runCatching
                val setLocation = control.javaClass.getMethod("setLocation", String::class.java)
                setLocation.invoke(control, "$location $sex")
            }
        }
    }

    private fun call(target: Any, name: String): Any? =
        runCatching {
            target.javaClass.getMethod(name).apply { isAccessible = true }.invoke(target)
        }.getOrNull()

    private fun callVoid(target: Any, name: String) {
        runCatching {
            target.javaClass.getMethod(name).apply { isAccessible = true }.invoke(target)
        }
    }

    private fun callVoidArgs(target: Any, name: String, args: IntArray) {
        runCatching {
            val m: Method = target.javaClass.getMethod(name, java.lang.Integer.TYPE)
            m.isAccessible = true
            m.invoke(target, args[0])
        }
    }
}
