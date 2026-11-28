package app.bilimorphe.patch.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.patch.ProcessScope
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Proxy

/**
 * 设置入口寄生（对照 1.28.6 的做法：入口开在宿主设置页里，而不是单独起一个 App）。
 *
 * 做法：hook 宿主 `BiliPreferencesActivity.onResume`，找到当前显示的
 * `PreferenceFragmentCompat`，往它的 `PreferenceScreen` 末尾插一条「模块设置」，
 * 点击后用 `FLAG_ACTIVITY_NEW_TASK` 拉起模块自己的 `SettingsActivity`。
 *
 * 为什么全程反射：宿主的 `androidx.preference` 与模块内置的是两份类（不同 ClassLoader），
 * 直接用编译期类型会 ClassCastException，所以 Preference / PreferenceScreen /
 * OnPreferenceClickListener 全部按宿主 ClassLoader 解析。
 */
object SettingsEntryFeature : Patch(
    name = "Settings entry",
    description = "Injects a module settings entry into the host preference screen.",
    scope = ProcessScope.MAIN_ONLY,
) {

    private const val HOST_PREFS_ACTIVITY = "com.bilibili.app.preferences.BiliPreferencesActivity"
    private const val MODULE_PACKAGE = "app.bilimorphe"
    private const val MODULE_ACTIVITY = "$MODULE_PACKAGE.SettingsActivity"

    private const val ENTRY_KEY = "bilimorphe_settings_entry"
    private const val ENTRY_TITLE = "bilimorphe 模块设置"
    private const val ENTRY_SUMMARY = "点击进入模块功能开关"

    override fun applyTo(engine: PatchEngine) {
        val cls = engine.findClass(HOST_PREFS_ACTIVITY)
        if (cls == null) {
            Logger.i("[settings_entry] host preferences activity missing")
            return
        }

        val onResume = cls.declaredMethods.firstOrNull {
            it.name == "onResume" && it.parameterCount == 0
        }
        if (onResume == null) {
            Logger.i("[settings_entry] onResume not found")
            return
        }

        XposedBridge.hookMethod(onResume, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as? Activity ?: return
                runCatching { injectEntry(engine, activity) }
                    .onFailure { Logger.e("[settings_entry] inject failed", it) }
            }
        })
        Logger.i("[settings_entry] armed on $HOST_PREFS_ACTIVITY")
    }

    /** 找到当前 PreferenceFragmentCompat，插入一条入口。 */
    private fun injectEntry(engine: PatchEngine, activity: Activity) {
        val fragmentManager = runCatching {
            activity.javaClass.getMethod("getSupportFragmentManager").invoke(activity)
        }.getOrNull() ?: return

        @Suppress("UNCHECKED_CAST")
        val fragments = runCatching {
            fragmentManager.javaClass.getMethod("getFragments").invoke(fragmentManager) as? List<Any>
        }.getOrNull() ?: return

        val fragmentBase = engine.findClass("androidx.preference.PreferenceFragmentCompat") ?: return
        val preferenceClass = engine.findClass("androidx.preference.Preference") ?: return
        val listenerClass = engine.findClass(
            "androidx.preference.Preference\$OnPreferenceClickListener"
        ) ?: return

        for (fragment in fragments) {
            if (!fragmentBase.isInstance(fragment)) continue
            val screen = runCatching {
                fragment.javaClass.getMethod("getPreferenceScreen").invoke(fragment)
            }.getOrNull() ?: continue

            // 已经插过就跳过
            val existing = runCatching {
                screen.javaClass.getMethod("findPreference", CharSequence::class.java)
                    .invoke(screen, ENTRY_KEY)
            }.getOrNull()
            if (existing != null) continue

            val preference = runCatching {
                preferenceClass.getConstructor(Context::class.java).newInstance(activity)
            }.getOrNull() ?: continue

            runCatching {
                preferenceClass.getMethod("setKey", String::class.java).invoke(preference, ENTRY_KEY)
                preferenceClass.getMethod("setTitle", CharSequence::class.java)
                    .invoke(preference, ENTRY_TITLE)
                preferenceClass.getMethod("setSummary", CharSequence::class.java)
                    .invoke(preference, ENTRY_SUMMARY)
                preferenceClass.getMethod("setPersistent", java.lang.Boolean.TYPE)
                    .invoke(preference, false)

                val listener = Proxy.newProxyInstance(
                    listenerClass.classLoader,
                    arrayOf(listenerClass),
                ) { _, method, _ ->
                    if (method.name == "onPreferenceClick") {
                        runCatching { launchSettings(activity) }
                            .onFailure { Logger.e("[settings_entry] launch failed", it) }
                        true
                    } else {
                        null
                    }
                }
                preferenceClass.getMethod("setOnPreferenceClickListener", listenerClass)
                    .invoke(preference, listener)

                screen.javaClass.getMethod("addPreference", preferenceClass)
                    .invoke(screen, preference)
                Logger.i("[settings_entry] entry injected into ${fragment.javaClass.name}")
            }.onFailure { Logger.e("[settings_entry] add preference failed", it) }
        }
    }

    private fun launchSettings(activity: Activity) {
        val intent = Intent().apply {
            setClassName(MODULE_PACKAGE, MODULE_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
