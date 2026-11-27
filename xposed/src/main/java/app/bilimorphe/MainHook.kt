package app.bilimorphe

import app.bilimorphe.patch.Patches
import app.bilimorphe.util.Logger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed entry point, registered through META-INF/xposed/java_init.list.
 */
class MainHook : XposedModule() {

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (!param.isFirstPackage) return

        val patches = Patches.forPackage(param.packageName) ?: return
        if (patches.isEmpty()) return

        try {
            val engine = PatchEngine(param)
            for (patch in patches) {
                runCatching { patch.applyTo(engine) }
                    .onSuccess { Logger.i("patch applied: ${patch.name}") }
                    .onFailure { Logger.e("patch failed: ${patch.name}", it) }
            }
        } catch (t: Throwable) {
            Logger.e("patch engine failed", t)
        }
    }
}
