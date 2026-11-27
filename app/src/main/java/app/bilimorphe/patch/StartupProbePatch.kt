package app.bilimorphe.patch

import android.app.Application
import app.bilimorphe.PatchEngine
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * Verification patch: logs module and host version information when the host
 * application starts. It does not alter any behaviour and is used to confirm the
 * runtime pipeline is working.
 */
object StartupProbePatch : Patch(
    name = "Startup probe",
    description = "Logs host package and version at process start.",
) {
    override fun applyTo(engine: PatchEngine) {
        val hostClass = engine.classLoader.loadClass(engine.hostClassName)
        val onCreate = hostClass.getMethod("onCreate")

        XposedBridge.hookMethod(onCreate, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val app = param.thisObject as Application
                val info = runCatching {
                    app.packageManager.getPackageInfo(app.packageName, 0)
                }.getOrNull()

                Logger.i(
                    "active in ${app.packageName} " +
                        "version=${info?.versionName} code=${info?.longVersionCode}"
                )
            }
        })

        Logger.i("startup probe installed")
    }
}
