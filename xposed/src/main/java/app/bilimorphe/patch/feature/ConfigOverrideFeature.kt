package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

/**
 * 云控开关覆盖（AB / 配置键强制）。
 *
 * 行为依据：1.28.6 在宿主 `ConfigManager` 的各实现点外包一层覆盖表
 * （`kofua/Acbb.java` 的 `Tggb`/`zKn`），先查表、未命中再走原实现。
 * 本项目重写为运行时 hook：直接拦最终出口 `ConfigManager.isHitFF(String)`
 * 与 `ConfigManager.getConfig(String, String)`，命中覆盖表即改返回值。
 *
 * 覆盖表对应关系（键 → 强制值）来自 1.28.6 的行为规格，开关各自独立：
 * - `force_old_comments`      → 评论新版相关 13 个键置 false
 * - `release_audio_focus`     → `ff_player_audio_focus_gain` 置 false
 * - `block_play_pause_ad`     → `dd_ad_pausedpage_enable` 置 false
 * - `force_tablet_mode`       → `dd_screen_adjust_xiaomi_864` 置 "large"
 */
object ConfigOverrideFeature : Patch(
    name = "Config override",
    description = "Forces host AB/config keys for the old-comments, audio focus, paused ad and tablet toggles.",
) {

    private const val CONFIG_MANAGER = "com.bilibili.lib.blconfig.ConfigManager"

    /** 旧版评论：关掉评论区新版/实验相关开关。 */
    private val OLD_COMMENT_KEYS = listOf(
        "following.opus_detail_use_cmp",
        "comment.kntr.enabled",
        "comment.kntr.route.enabled",
        "comment.kntr.story.enabled",
        "comment.kntr.landscape.enabled",
        "comment.next_appearance",
        "comment.v3",
        "comment.next_appearance_experiment_2",
        "comment.next_appearance_experiment_2_1",
        "comment.next_appearance_experiment_3",
        "comment.next_appearance_experiment_4",
        "comment.next_appearance_experiment_5",
    )

    override fun applyTo(engine: PatchEngine) {
        val boolTable = HashMap<String, Boolean>()
        val stringTable = HashMap<String, String>()

        if (ModulePrefs.isEnabled(SettingsKeys.FORCE_OLD_COMMENTS)) {
            OLD_COMMENT_KEYS.forEach { boolTable[it] = false }
        }
        if (ModulePrefs.isEnabled(SettingsKeys.RELEASE_AUDIO_FOCUS)) {
            boolTable["ff_player_audio_focus_gain"] = false
        }
        if (ModulePrefs.isEnabled(SettingsKeys.BLOCK_PLAY_PAUSE_AD)) {
            boolTable["dd_ad_pausedpage_enable"] = false
        }
        if (ModulePrefs.isEnabled(SettingsKeys.FORCE_TABLET_MODE)) {
            stringTable["dd_screen_adjust_xiaomi_864"] = "large"
        }

        if (boolTable.isEmpty() && stringTable.isEmpty()) {
            Logger.i("[config_override] no rule enabled, skip")
            return
        }

        val cls = engine.findClass(CONFIG_MANAGER)
        if (cls == null) {
            Logger.i("[config_override] $CONFIG_MANAGER missing")
            return
        }

        var hooked = 0

        if (boolTable.isNotEmpty()) {
            findMethod(cls, "isHitFF", String::class.java, java.lang.Boolean.TYPE)?.let { m ->
                runCatching {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args.getOrNull(0) as? String ?: return
                            boolTable[key]?.let { param.result = it }
                        }
                    })
                    hooked++
                }.onFailure { Logger.e("[config_override] isHitFF hook failed", it) }
            }
        }

        if (stringTable.isNotEmpty()) {
            findMethod(cls, "getConfig")?.let { m ->
                runCatching {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args.getOrNull(0) as? String ?: return
                            stringTable[key]?.let { param.result = it }
                        }
                    })
                    hooked++
                }.onFailure { Logger.e("[config_override] getConfig hook failed", it) }
            }
        }

        Logger.i(
            "[config_override] rules bool=${boolTable.size} string=${stringTable.size} hooked=$hooked"
        )
    }

    /** 按名字+参数签名找方法，兼容 `isHitFF` 的布尔返回与 `getConfig` 的多种重载。 */
    private fun findMethod(cls: Class<*>, name: String, vararg params: Class<*>): Method? {
        if (params.isNotEmpty()) {
            return runCatching { cls.getMethod(name, *params).apply { isAccessible = true } }.getOrNull()
        }
        return cls.declaredMethods.firstOrNull {
            it.name == name && it.parameterCount == 2 &&
                it.parameterTypes[0] == String::class.java
        }?.apply { isAccessible = true }
    }
}
