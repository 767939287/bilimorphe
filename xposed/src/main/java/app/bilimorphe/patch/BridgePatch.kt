package app.bilimorphe.patch

import android.app.Application
import android.content.Context
import app.bilimorphe.PatchEngine
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Bridges the embedded integrations runtime into the host process:
 * links the host class loader, initializes the integrations context and flags
 * the runtime as attached.
 */
object BridgePatch : Patch(
    name = "Integrations bridge",
    description = "Initializes the embedded integrations runtime inside the host process.",
) {
    private val moduleLoader: ClassLoader
        get() = BridgePatch::class.java.classLoader!!

    override fun applyTo(engine: PatchEngine) {
        val appClass = engine.classLoader.loadClass(engine.hostClassName)
        val attach = XposedHelpers.findMethodExact(appClass, "attachBaseContext", Context::class.java)
        XposedBridge.hookMethod(attach, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                initialize(param.thisObject as Application, engine)
            }
        })
    }

    private fun initialize(app: Application, engine: PatchEngine) {
        injectHostClassLoader(engine.classLoader)

        runCatching {
            val utils = Class.forName("app.revanced.bilibili.utils.Utils", true, moduleLoader)
            utils.getField("context").set(null, app)
            Logger.i("integrations context ready")
        }.onFailure { Logger.e("integrations context init failed", it) }

        runCatching {
            val delegate = Class.forName(
                "app.revanced.bilibili.patches.main.ApplicationDelegate", true, moduleLoader
            )
            delegate.getDeclaredField("attached").apply { isAccessible = true }.setBoolean(null, true)
            Logger.i("integrations attached flag set")
        }.onFailure { Logger.e("integrations attach flag failed", it) }

        runCatching {
            val prefs = Class.forName(
                "app.revanced.bilibili.utils.CrossProcessPreferences", true, moduleLoader
            )
            prefs.getDeclaredMethod("init", Context::class.java)
                .apply { isAccessible = true }
                .invoke(null, app)
            Logger.i("cross process preferences ready")
        }.onFailure { Logger.e("cross process preferences failed", it) }

        enableTemporaryDefaults()
    }

    /**
     * Temporary switch used to validate the pipeline before a settings UI exists.
     * Remove once the settings screen is ported.
     */
    private fun enableTemporaryDefaults() {
        runCatching {
            val settings = Class.forName("app.revanced.bilibili.settings.Settings", true, moduleLoader)
            for (name in listOf("UnlockAreaLimit")) {
                val setting = settings.getField(name).get(null) ?: continue
                val isDefault = setting.javaClass.getMethod("isSetToDefault").invoke(setting) as Boolean
                if (isDefault) {
                    setting.javaClass.getMethod("save", Any::class.java).invoke(setting, true)
                    Logger.i("default enabled: $name")
                }
            }
        }.onFailure { Logger.e("temporary defaults failed", it) }
    }

    private fun injectHostClassLoader(hostLoader: ClassLoader) {
        runCatching {
            val parentField = ClassLoader::class.java.getDeclaredField("parent")
            parentField.isAccessible = true
            var current: ClassLoader = moduleLoader
            while (true) {
                val parent = current.parent ?: break
                if (parent == ClassLoader.getSystemClassLoader()) break
                current = parent
            }
            if (current !== hostLoader) {
                parentField.set(current, hostLoader)
                Logger.i("host class loader linked")
            }
        }.onFailure { Logger.e("host class loader link failed", it) }
    }
}
