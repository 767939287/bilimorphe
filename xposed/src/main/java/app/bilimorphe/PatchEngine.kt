package app.bilimorphe

import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method

/**
 * Per process patching context. Provides host class loading and reflective lookup
 * helpers shared by all patches.
 */
class PatchEngine(
    internal val param: XposedModuleInterface.PackageReadyParam,
) {
    val classLoader: ClassLoader get() = param.classLoader

    val packageName: String get() = param.packageName

    /** Fully qualified class name of the host Application. */
    val hostClassName: String get() = param.applicationInfo.className

    /** 当前进程名（B 站大量使用 `:xxx` 子进程）。 */
    val processName: String by lazy {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                android.app.Application.getProcessName()
            } else {
                java.io.File("/proc/self/cmdline").readText().trim('\u0000').trim()
            }
        }.getOrDefault(packageName)
    }

    /** 是否为主进程（进程名 == 包名）。 */
    val isMainProcess: Boolean get() = processName == packageName

    /** APK path of the host application, used as DexKit input. */
    val hostApkPath: String get() = param.applicationInfo.sourceDir

    fun findClass(name: String): Class<*>? =
        runCatching { classLoader.loadClass(name) }.getOrNull()

    /**
     * Looks up a declared method on the given class, walking up the superclass chain.
     */
    fun findMethod(
        className: String,
        methodName: String,
        vararg parameterTypes: Class<*>,
    ): Method? {
        var cls: Class<*>? = findClass(className)
        while (cls != null && cls != Any::class.java) {
            val found = runCatching {
                cls!!.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
            }.getOrNull()
            if (found != null) return found
            cls = cls.superclass
        }
        return null
    }
}
