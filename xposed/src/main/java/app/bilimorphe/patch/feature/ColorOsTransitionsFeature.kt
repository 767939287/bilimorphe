package app.bilimorphe.patch.feature

import android.app.Application
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 关闭 ColorOS 无缝转场（对应 1.28.6 的 `disable_coloros_transitions`）。
 *
 * 行为依据（`kofua/fei.java`，宿主 Application 子类 onCreate 内）：
 * 反射取 `android.app.viewseamless.OplusViewSeamlessRemoteManager` 实例，
 * 把字段 `mSeamlessThirdParty` 置 null，从而禁用系统对第三方应用的无缝转场。
 *
 * 本项目重写为运行时 hook：宿主 Application.onCreate 之后执行同一反射操作。
 * 非 ColorOS 设备上类不存在，仅记录日志。
 */
object ColorOsTransitionsFeature : Patch(
    name = "Disable ColorOS transitions",
    description = "Nulls out the Oplus seamless transition manager field on ColorOS devices.",
) {

    private const val MANAGER = "android.app.viewseamless.OplusViewSeamlessRemoteManager"
    private const val FIELD = "mSeamlessThirdParty"

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.DISABLE_COLOROS_TRANSITIONS)) {
            Logger.i("[coloros] off, skip")
            return
        }

        val appClass = engine.findClass(engine.hostClassName)
        val onCreate = runCatching { appClass?.getMethod("onCreate") }.getOrNull()
        if (onCreate == null) {
            Logger.i("[coloros] Application.onCreate not found")
            return
        }

        XposedBridge.hookMethod(onCreate, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.thisObject !is Application) return
                disable()
            }
        })
        Logger.i("[coloros] armed")
    }

    private fun disable() {
        runCatching {
            val cls = Class.forName(MANAGER)
            val instance = cls.getDeclaredMethod("getInstance").invoke(null)
            val field = cls.getDeclaredField(FIELD).apply { isAccessible = true }
            field.set(instance, null)
            Logger.i("[coloros] seamless transitions disabled")
        }.onFailure {
            Logger.i("[coloros] unavailable: ${it.javaClass.simpleName}")
        }
    }
}
