package app.bilimorphe.patch.feature

import android.os.Bundle
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge

/**
 * 时间点空降（1.23.3 的 `Force comment time navigable`，键名沿用 `time_airborne`）。
 *
 * 行为依据（1.23.3 补丁与 integrations 实现）：
 * 1. `PrimaryCommentMainFragment.onCreate` 里给 arguments 塞 `enableTimeParser = true`，
 *    使评论里的时间点可解析、可点击；
 * 2. 评论配置类里「seekEnabled」的布尔开关被强制为 `true`。
 *
 * 本项目重写为运行时 hook：
 * - 第 1 处用稳定类名（`com.bilibili.app.comm.comment2.comments.view.PrimaryCommentMainFragment`）定位；
 * - 第 2 处该类为混淆名，用 DexKit 按字符串 `CommentConfig(themeMode=` 定位所属类，
 *   再把其中「无参、返回 boolean、方法名含 seek」的方法改为返回 true。
 */
object TimeAirborneFeature : Patch(
    name = "Time airborne",
    description = "Makes timestamps in danmaku, multi-part and interactive comments clickable.",
) {

    private const val COMMENT_FRAGMENT =
        "com.bilibili.app.comm.comment2.comments.view.PrimaryCommentMainFragment"

    /** 评论配置 toString 的特征串（1.23.3 指纹同款）。 */
    private const val CONFIG_HINT = "CommentConfig(themeMode="

    /** 宿主识别的 arguments 键。 */
    private const val ARG_ENABLE_TIME_PARSER = "enableTimeParser"

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.TIME_AIRBORNE)) {
            Logger.i("[time_airborne] off, skip")
            return
        }

        var hooked = 0

        // 1) 评论主 Fragment：打开时间点解析
        runCatching {
            val cls = engine.findClass(COMMENT_FRAGMENT)
            if (cls == null) {
                Logger.i("[time_airborne] comment fragment missing")
                return@runCatching
            }
            val onCreate = cls.declaredMethods.firstOrNull {
                it.name == "onCreate" && it.parameterCount == 1 &&
                    it.parameterTypes[0] == Bundle::class.java
            }
            if (onCreate == null) {
                Logger.i("[time_airborne] onCreate not found")
                return@runCatching
            }
            XposedBridge.hookMethod(onCreate, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    runCatching {
                        val getArguments = param.thisObject.javaClass.methods
                            .firstOrNull { it.name == "getArguments" && it.parameterCount == 0 }
                        val args = getArguments?.invoke(param.thisObject) as? Bundle ?: return@runCatching
                        args.putBoolean(ARG_ENABLE_TIME_PARSER, true)
                    }.onFailure { Logger.e("[time_airborne] set argument failed", it) }
                }
            })
            hooked++
            Logger.i("[time_airborne] hooked comment fragment")
        }.onFailure { Logger.e("[time_airborne] fragment hook failed", it) }

        // 2) 评论配置的 seekEnabled 开关强制为 true
        runCatching {
            val bridge = DexKitBridge.create(engine.hostApkPath)
            try {
                // 先用方法级字符串匹配拿到所属类名（与 OkHttpHookPatch 相同的用法）
                val classNames = bridge.findMethod { matcher { usingStrings(CONFIG_HINT) } }
                    .mapNotNull { it.className }
                    .distinct()
                Logger.i("[time_airborne] config classes: $classNames")
                for (className in classNames) {
                    val cls = engine.findClass(className) ?: continue
                    for (method in cls.declaredMethods) {
                        if (method.parameterCount != 0) continue
                        if (method.returnType != java.lang.Boolean.TYPE) continue
                        if (!method.name.contains("seek", ignoreCase = true)) continue
                        runCatching {
                            method.isAccessible = true
                            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    param.result = true
                                }
                            })
                            hooked++
                            Logger.i("[time_airborne] hooked ${cls.name}#${method.name}")
                        }.onFailure { Logger.e("[time_airborne] seek flag hook failed", it) }
                    }
                }
            } finally {
                runCatching { bridge.close() }
            }
        }.onFailure { Logger.e("[time_airborne] dexkit locate failed", it) }

        Logger.i("[time_airborne] hooked=$hooked")
    }
}
