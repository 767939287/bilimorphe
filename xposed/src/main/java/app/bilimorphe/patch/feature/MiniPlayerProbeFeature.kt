package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge

/**
 * 小窗 / AI 系列的探针（对应 `always_mini_play`、`mini_danmaku`、`mini_subtitle`、
 * `mini_follow_full`、`ai_video_filter_enabled`）。
 *
 * 为什么是探针而不是实现：这 5 项在 1.28.6 中靠注入宿主播放器渲染 / AI 判定代码完成，
 * 宿主侧相关类名在 dex 里只是字符串常量（如 `autoMiniPlayerSettingSuppressor`
 * 出现在 classes9 / classes11 / classes35，索引中无对应类），没有稳定的类名锚点。
 * 按 `docs/roadmap.md` 9.9「findClass 观测法」，先用 DexKit 按字符串特征定位候选方法，
 * hook 后只记录「类名 / 方法签名 / 调用次数」，实机跑一次即可固化出真正的指纹，
 * 再补实现逻辑。
 *
 * 探针本身不改变宿主行为：仅当对应开关打开、且命中的候选数不为零时安装，
 * 日志前缀统一为 `[probe:<开关名>]`，便于实机过滤。
 */
object MiniPlayerProbeFeature : Patch(
    name = "Mini player probe",
    description = "Observes candidate host methods for mini-player and AI toggles to fix their fingerprints.",
) {

    /** 开关 → DexKit 字符串特征（可多个，命中即记录）。 */
    private val PROBES = listOf(
        SettingsKeys.ALWAYS_MINI_PLAY to listOf("autoMiniPlayerSettingSuppressor"),
        SettingsKeys.MINI_DANMAKU to listOf("mini_danmaku", "miniPlayerDanmaku"),
        SettingsKeys.MINI_SUBTITLE to listOf("mini_subtitle", "miniPlayerSubtitle"),
        SettingsKeys.MINI_FOLLOW_FULL to listOf("mini_follow_full", "followFullScreen"),
        SettingsKeys.AI_VIDEO_FILTER_ENABLED to listOf("ai_video_filter", "aiVideoFilter"),
    )

    /** 每个特征最多观测的候选方法数，防止一次挂太多 hook。 */
    private const val MAX_CANDIDATES = 3

    override fun applyTo(engine: PatchEngine) {
        val targets = PROBES.mapNotNull { (key, hints) ->
            if (ModulePrefs.isEnabled(key)) key to hints else null
        }
        if (targets.isEmpty()) {
            Logger.i("[probe] no toggle enabled, skip")
            return
        }

        val bridge = DexKitBridge.create(engine.hostApkPath)
        try {
            for ((key, hints) in targets) {
                var observed = 0
                for (hint in hints) {
                    val candidates = runCatching {
                        bridge.findMethod { matcher { usingStrings(hint) } }
                    }.getOrElse {
                        Logger.e("[probe:$key] dexkit query failed for '$hint'", it)
                        continue
                    }
                    for (data in candidates.take(MAX_CANDIDATES)) {
                        val method = runCatching { data.getMethodInstance(engine.classLoader) }
                            .getOrElse {
                                Logger.i("[probe:$key] cannot resolve ${data.className}#${data.methodName}")
                                continue
                            }
                        runCatching {
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    Logger.i(
                                        "[probe:$key] ${data.className}#${data.methodName}" +
                                            " args=${param.args.size} result=${param.result}"
                                    )
                                }
                            })
                            observed++
                            Logger.i("[probe:$key] watching ${data.className}#${data.methodName} (hint=$hint)")
                        }.onFailure { Logger.e("[probe:$key] hook failed: ${data.methodName}", it) }
                    }
                }
                Logger.i("[probe:$key] total watched=$observed")
            }
        } finally {
            runCatching { bridge.close() }
        }
    }
}
