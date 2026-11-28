package app.bilimorphe.patch.feature

import android.app.Activity
import android.content.Context
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 自定义 DPI / 显示尺寸（1.23.3 的 `Dpi`）。
 *
 * 行为依据：1.23.3 在宿主 Activity 基类的 `attachBaseContext` 与 `onConfigurationChanged`
 * 里调用 `ApplicationDelegate.onActivityPreAttachBaseContext(activity, base)`，
 * 由它按 `Settings.DisplaySize`（键 `display_size`）改写 `Configuration` 后返回新的 Context。
 *
 * 实现：hook 系统 `android.app.Activity.attachBaseContext(Context)`，
 * 把结果交给 integrations 的同名方法处理。DPI 未设置时该方法原样返回 base，无副作用。
 * 设置项在 1.23.3 的「显示尺寸」页面里（`biliroaming_setting_display_size.xml`）。
 */
object DpiFeature : Patch(
    name = "Custom DPI",
    description = "Applies the configured display size by wrapping each activity base context.",
) {

    private const val APPLICATION_DELEGATE =
        "app.revanced.bilibili.patches.main.ApplicationDelegate"

    override fun applyTo(engine: PatchEngine) {
        val delegate = runCatching {
            Class.forName(APPLICATION_DELEGATE, true, DpiFeature::class.java.classLoader)
        }.getOrNull()
        if (delegate == null) {
            Logger.e("[dpi] ApplicationDelegate missing", null)
            return
        }

        val preAttach = delegate.declaredMethods.firstOrNull {
            it.name == "onActivityPreAttachBaseContext" && it.parameterCount == 2
        }
        if (preAttach == null) {
            Logger.i("[dpi] onActivityPreAttachBaseContext missing")
            return
        }
        preAttach.isAccessible = true

        val activityClass = runCatching { Class.forName("android.app.Activity") }.getOrNull() ?: return
        val attach = activityClass.declaredMethods.firstOrNull {
            it.name == "attachBaseContext" && it.parameterCount == 1
        }
        if (attach == null) {
            Logger.i("[dpi] Activity.attachBaseContext missing")
            return
        }

        runCatching {
            XposedBridge.hookMethod(attach, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    val base = param.args.getOrNull(0) as? Context ?: return
                    runCatching {
                        val replaced = preAttach.invoke(null, activity, base)
                        if (replaced is Context) param.args[0] = replaced
                    }.onFailure { Logger.e("[dpi] wrap context failed", it) }
                }
            })
            Logger.i("[dpi] hooked Activity.attachBaseContext")
        }.onFailure { Logger.e("[dpi] hook failed", it) }
    }
}
