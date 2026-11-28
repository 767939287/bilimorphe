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
 * 注意：该方法的宿主类名在当前 9.20.0 中为混淆名 `defpackage.vdv0`，跨版本会变。
 * 这里按「类名候选 + 方法签名」双重匹配，签名条件为：
 * 静态、返回 Bitmap、参数依次为 (Bitmap, Rect, Bitmap, Drawable, String, DisplayMetrics, boolean)。
 * 后续应替换为 DexKit 特征定位（见 docs/roadmap.md 第 8 项）。
 */
object RemoveVideoScreenshotWatermarkFeature : Patch(
    name = "Remove video screenshot watermark",
    description = "Drops the watermark drawable when the host composites screenshots.",
) {

    /**
     * 已知类名候选。宿主该类的真实描述符是默认包 `Lvdv0;`（jadx 显示为 `defpackage.vdv0`），
     * 运行时类名不带包名，因此这里写 `vdv0`。
     */
    private val CLASS_CANDIDATES = listOf(
        "vdv0",
    )

    private const val DRAWABLE_ARG_INDEX = 3

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.REMOVE_VIDEO_SCREENSHOT_WATERMARK)) {
            Logger.i("[watermark] off, skip")
            return
        }

        var hooked = 0
        for (className in CLASS_CANDIDATES) {
            val cls = engine.findClass(className) ?: continue
            val method = findCompositor(cls) ?: continue
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
                Logger.i("[watermark] hooked ${cls.name}#${method.name}")
            }.onFailure { Logger.e("[watermark] hook failed: ${cls.name}", it) }
        }

        if (hooked == 0) {
            Logger.i("[watermark] no target matched (class candidates: $CLASS_CANDIDATES)")
        } else {
            Logger.i("[watermark] hooked=$hooked")
        }
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
