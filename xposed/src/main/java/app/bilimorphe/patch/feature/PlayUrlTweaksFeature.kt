package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Modifier

/**
 * 播放地址音频增强两项（对应 1.28.6 的 `clear_audio` / `prefer_lossless`）。
 *
 * 行为依据（`kofua/UBeb.java`、`kofua/fFhb.java`）：
 * - `clear_audio`：`vodInfo.lossLessItem` 存在但没有 audio → `clearLossLessItem()`；
 *   `vodInfo.dolby` 存在但 audioList 为空 → `clearDolby()`（避免选到空音轨）；
 * - `prefer_lossless`：无损与杜比同时可用时 → `clearDolby()`，让播放器优先无损。
 *
 * 本项目重写为运行时 hook：拦截 playurl 各 `PlayViewReply` 的 `parseFrom(...)`，
 * 解析完成后修改其 `videoInfo` / `vodInfo`。只依赖 protobuf 生成的方法名。
 */
object PlayUrlTweaksFeature : Patch(
    name = "Play url tweaks",
    description = "Clears empty audio tracks and prefers lossless audio over dolby.",
) {

    private val REPLY_CLASSES = listOf(
        "com.bapis.bilibili.app.playurl.v1.PlayViewReply",
        "com.bapis.bilibili.app.playurl.v1.PlayURLReply",
        "com.bapis.bilibili.pgc.gateway.player.v2.PlayViewReply",
        "com.bapis.bilibili.pgc.gateway.player.v1.PlayViewReply",
        "com.bapis.bilibili.cheese.gateway.player.v1.PlayViewReply",
    )

    override fun applyTo(engine: PatchEngine) {
        val clearAudio = ModulePrefs.isEnabled(SettingsKeys.CLEAR_AUDIO)
        val preferLossless = ModulePrefs.isEnabled(SettingsKeys.PREFER_LOSSLESS)
        if (!clearAudio && !preferLossless) {
            Logger.i("[playurl_tweaks] off, skip")
            return
        }

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val reply = param.result ?: return
                runCatching {
                    val vodInfo = infoOf(reply) ?: return@runCatching
                    if (clearAudio) clearEmptyTracks(vodInfo)
                    if (preferLossless) dropDolbyWhenLossless(vodInfo)
                }.onFailure { Logger.e("[playurl_tweaks] apply failed", it) }
            }
        }

        var hooked = 0
        for (name in REPLY_CLASSES) {
            val cls = engine.findClass(name) ?: continue
            for (m in cls.declaredMethods) {
                if (m.name != "parseFrom" || !Modifier.isStatic(m.modifiers)) continue
                if (!cls.isAssignableFrom(m.returnType)) continue
                runCatching { XposedBridge.hookMethod(m, hook); hooked++ }
            }
        }
        Logger.i("[playurl_tweaks] hooked=$hooked clearAudio=$clearAudio preferLossless=$preferLossless")
    }

    /** PlayViewReply.videoInfo / PlayURLReply.videoInfo 等，优先取 vodInfo 字段。 */
    private fun infoOf(reply: Any): Any? {
        for (getter in listOf("getVideoInfo", "getVodInfo")) {
            val info = runCatching { reply.javaClass.getMethod(getter).invoke(reply) }.getOrNull()
            if (info != null) return info
        }
        return null
    }

    /** 空音轨清理。 */
    private fun clearEmptyTracks(info: Any) {
        runCatching {
            val lossLess = info.javaClass.getMethod("getLossLessItem").invoke(info) ?: return@runCatching
            val hasLossLess = info.javaClass.getMethod("hasLossLessItem").invoke(info) as? Boolean ?: false
            val hasAudio = lossLess.javaClass.getMethod("hasAudio").invoke(lossLess) as? Boolean ?: false
            if (hasLossLess && !hasAudio) {
                info.javaClass.getMethod("clearLossLessItem").invoke(info)
            }
        }

        runCatching {
            val hasDolby = info.javaClass.getMethod("hasDolby").invoke(info) as? Boolean ?: false
            if (!hasDolby) return@runCatching
            val dolby = info.javaClass.getMethod("getDolby").invoke(info) ?: return@runCatching
            @Suppress("UNCHECKED_CAST")
            val audios = dolby.javaClass.getMethod("getAudioList").invoke(dolby) as? List<*>
            if (audios.isNullOrEmpty()) {
                info.javaClass.getMethod("clearDolby").invoke(info)
            }
        }
    }

    /** 无损优先：两者都可播时丢掉杜比。 */
    private fun dropDolbyWhenLossless(info: Any) {
        runCatching {
            val hasLossLess = info.javaClass.getMethod("hasLossLessItem").invoke(info) as? Boolean ?: false
            if (!hasLossLess) return@runCatching
            val lossLess = info.javaClass.getMethod("getLossLessItem").invoke(info) ?: return@runCatching
            val lossLessHasAudio = lossLess.javaClass.getMethod("hasAudio").invoke(lossLess) as? Boolean ?: false
            if (!lossLessHasAudio) return@runCatching

            val hasDolby = info.javaClass.getMethod("hasDolby").invoke(info) as? Boolean ?: false
            if (!hasDolby) return@runCatching
            val dolby = info.javaClass.getMethod("getDolby").invoke(info) ?: return@runCatching
            @Suppress("UNCHECKED_CAST")
            val audios = dolby.javaClass.getMethod("getAudioList").invoke(dolby) as? List<*>
            if (!audios.isNullOrEmpty()) {
                info.javaClass.getMethod("clearDolby").invoke(info)
            }
        }
    }
}
