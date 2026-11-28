package app.bilimorphe.patch.feature

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

/**
 * 去除视频截图水印（对应 1.28.6 的 `remove_video_screenshot_watermark`）。
 *
 * 行为依据：1.28.6 在宿主截图合成方法里把水印 drawable 置空后再绘制：
 * 目标方法 `Bitmap a(Bitmap, Rect, Bitmap, Drawable, String, DisplayMetrics, boolean)`，
 * 第 4 个参数（index 3）为水印 drawable。
 *
 * 本项目重写为运行时 hook：调用前把 drawable 参数替换为 null，宿主自然跳过水印绘制。
 *
 * 定位策略：该方法所在类在默认包且名字每版都变（9.20.0 = `vdv0`，9.21.0 = `tzu0`），
 * 所以**以签名为主**：用 DexKit 匹配「7 参数、index3 为 Drawable、返回 Bitmap」的方法，
 * 类名候选仅作 DexKit 不可用时的回退。
 */
object RemoveVideoScreenshotWatermarkFeature : Patch(
    name = "Remove video screenshot watermark",
    description = "Drops the watermark drawable when the host composites screenshots.",
) {

    /** DexKit 不可用时的回退类名（逐版本追加）。 */
    private val CLASS_FALLBACKS = listOf("vdv0", "tzu0")

    private const val DRAWABLE_ARG_INDEX = 3

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.REMOVE_VIDEO_SCREENSHOT_WATERMARK)) {
            Logger.i("[watermark] off, skip")
            return
        }

        val methods = mutableListOf<Method>()

        // 1) DexKit 按签名定位（跨版本稳定）
        runCatching {
            val bridge = DexKitBridge.create(engine.hostApkPath)
            try {
                bridge.findMethod {
                    matcher {
                        returnType = "android.graphics.Bitmap"
                        paramCount = 7
                        paramTypes = listOf(
                            "android.graphics.Bitmap",
                            "android.graphics.Rect",
                            "android.graphics.Bitmap",
                            "android.graphics.drawable.Drawable",
                            "java.lang.String",
                            "android.util.DisplayMetrics",
                            "boolean",
                        )
                    }
                }.forEach { data ->
                    runCatching { data.getMethodInstance(engine.classLoader) }
                        .onSuccess { methods.add(it) }
                        .onFailure { Logger.i("[watermark] resolve failed: ${data.className}") }
                }
            } finally {
                runCatching { bridge.close() }
            }
        }.onFailure { Logger.e("[watermark] dexkit locate failed", it) }

        // 2) 回退：类名候选 + 签名二次确认
        if (methods.isEmpty()) {
            Logger.i("[watermark] dexkit found nothing, fallback to class candidates")
            for (className in CLASS_FALLBACKS) {
                val cls = engine.findClass(className) ?: continue
                findCompositor(cls)?.let { methods.add(it) }
            }
        }

        if (methods.isEmpty()) {
            Logger.i("[watermark] no target matched")
            return
        }

        var hooked = 0
        for (method in methods.distinct()) {
            runCatching {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.size > DRAWABLE_ARG_INDEX &&
                            param.args[DRAWABLE_ARG_INDEX] is Drawable
                        ) {
                            param.args[DRAWABLE_ARG_INDEX] = null
                        }
                    }
                })
                hooked++
                Logger.i("[watermark] hooked ${method.declaringClass.name}#${method.name}")
            }.onFailure { Logger.e("[watermark] hook failed", it) }
        }
        Logger.i("[watermark] hooked=$hooked")
    }

    /** 严格按签名匹配截图合成方法。 */
    private fun findCompositor(cls: Class<*>): Method? =
        cls.declaredMethods.firstOrNull { m ->
            val p = m.parameterTypes
            java.lang.reflect.Modifier.isStatic(m.modifiers) &&
                m.returnType == Bitmap::class.java &&
                p.size == 7 &&
                p[0] == Bitmap::class.java &&
                p[3] == Drawable::class.java &&
                p[5] == DisplayMetrics::class.java &&
                (p[6] == java.lang.Boolean.TYPE || p[6] == java.lang.Boolean::class.java)
        }?.apply { isAccessible = true }
}
