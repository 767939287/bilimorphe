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

    /**
     * 9.21.0 实测存在的小窗容器类（`kntr.common.miniplayer` 包）。
     * 该包方法名全部混淆，因此改为直接观测整条调用链：
     * 实机跑一次即可看出 弹幕 / 字幕 / 跟随全屏 分别走哪个方法。
     */
    private val MINI_CONTAINER_CLASSES = listOf(
        "kntr.common.miniplayer.container.MiniPlayerContainer",
        "com.bilibili.mini.player.biz.DefaultMiniPlayerBizManager",
    )

    /** 每个特征最多观测的候选方法数，防止一次挂太多 hook。 */
    private const val MAX_CANDIDATES = 3

    /** 每个类最多观测的方法数（容器类方法很多，限制上限）。 */
    private const val MAX_CLASS_METHODS = 12

    override fun applyTo(engine: PatchEngine) {
        val targets = PROBES.mapNotNull { (key, hints) ->
            if (ModulePrefs.isEnabled(key)) key to hints else null
        }
        val miniEnabled = ModulePrefs.isEnabled(SettingsKeys.ALWAYS_MINI_PLAY) ||
            ModulePrefs.isEnabled(SettingsKeys.MINI_DANMAKU) ||
            ModulePrefs.isEnabled(SettingsKeys.MINI_SUBTITLE) ||
            ModulePrefs.isEnabled(SettingsKeys.MINI_FOLLOW_FULL)

        if (targets.isEmpty() && !miniEnabled) {
            Logger.i("[probe] no toggle enabled, skip")
            return
        }

        // 1) 小窗容器整链观测（方法名混淆，只能靠调用序列定位）
        if (miniEnabled) probeMiniContainer(engine)

        if (targets.isEmpty()) return

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

    /**
     * 监测小窗容器的调用链。
     *
     * 9.21.0 的 `kntr.common.miniplayer.container.MiniPlayerContainer` 方法名全部混淆
     * （`A`/`b`/`c`……），无法从名字判断哪个是弹幕 / 字幕 / 跟随全屏。
     * 这里直接观测整个类的方法调用序列，实机一次即可定位。
     */
    private fun probeMiniContainer(engine: PatchEngine) {
        var hooked = 0
        for (className in MINI_CONTAINER_CLASSES) {
            val cls = engine.findClass(className)
            if (cls == null) {
                Logger.i("[probe:mini] class missing: $className")
                continue
            }
            val methods = cls.declaredMethods
                .filter { !java.lang.reflect.Modifier.isAbstract(it.modifiers) }
                .take(MAX_CLASS_METHODS)
            for (method in methods) {
                runCatching {
                    method.isAccessible = true
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            Logger.i(
                                "[probe:mini] ${className.substringAfterLast('.')}#${param.method.name}" +
                                    " args=${param.args.size} result=${param.result}"
                            )
                        }
                    })
                    hooked++
                }.onFailure { Logger.e("[probe:mini] hook failed: ${method.name}", it) }
            }
        }
        Logger.i("[probe:mini] watching $hooked methods")
    }
}
