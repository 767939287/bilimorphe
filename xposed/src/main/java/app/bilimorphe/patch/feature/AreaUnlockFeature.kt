package app.bilimorphe.patch.feature

import android.app.Application
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.patch.ProcessScope
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Field

/**
 * 区域解锁设置同步（1.23.3 的核心能力）。
 *
 * 背景：解锁链路的实际逻辑在 integrations 的 `BangumiPlayUrlHook` 等类里，
 * 它们通过 `Settings` 对象读配置，而 `Settings` 内部用的是 `CrossProcessPreferences`
 * （宿主进程的 SharedPreferences，`BridgePatch` 已初始化）。
 *
 * 问题：设置页运行在模块进程，写的是模块私有 prefs，integrations 读不到。
 * 解决：宿主进程启动后，把模块设置里的解锁配置**回写**到 integrations 的 `Settings`，
 * 之后 Moss 通道（[MossHookFeature]）→ protobuf hook → `BangumiPlayUrlHook` 即可生效。
 *
 * 映射关系（模块键 → integrations 字段）：
 * - `main_func`       → `UnlockAreaLimit`（总开关）
 * - `tw_server`       → `TaiWanServer`
 * - `hk_server`       → `HongKongServer`
 * - `cn_server`       → `ChinaServer`
 * - `th_server`       → `ThailandServer`
 * - `upos_host`       → `UposHost`
 * - `access_key_main` → `AccessKeyMain`
 */
object AreaUnlockFeature : Patch(
    name = "Area unlock settings",
    description = "Copies module unlock settings into the integrations settings store.",
    scope = ProcessScope.MAIN_ONLY,
) {

    private const val INTEGRATIONS_SETTINGS = "app.revanced.bilibili.settings.Settings"

    /** 布尔项：模块键 → integrations 字段。 */
    private val BOOL_MAP = listOf(
        SettingsKeys.MAIN_FUNC to "UnlockAreaLimit",
        SettingsKeys.ALLOW_DOWNLOAD to "AllowDownload",
    )

    /** 字符串项：模块键 → integrations 字段。 */
    private val STRING_MAP = listOf(
        SettingsKeys.TW_SERVER to "TaiWanServer",
        SettingsKeys.HK_SERVER to "HongKongServer",
        SettingsKeys.CN_SERVER to "ChinaServer",
        SettingsKeys.TH_SERVER to "ThailandServer",
        SettingsKeys.UPOS_HOST to "UposHost",
        SettingsKeys.ACCESS_KEY_MAIN to "AccessKeyMain",
    )

    override fun applyTo(engine: PatchEngine) {
        val settingsClass = runCatching {
            Class.forName(INTEGRATIONS_SETTINGS, true, AreaUnlockFeature::class.java.classLoader)
        }.getOrNull()
        if (settingsClass == null) {
            Logger.e("[area_unlock] integrations Settings missing", null)
            return
        }

        val appClass = engine.findClass(engine.hostClassName)
        val onCreate = runCatching { appClass?.getMethod("onCreate") }.getOrNull()
        if (onCreate == null) {
            Logger.i("[area_unlock] Application.onCreate not found")
            return
        }

        XposedBridge.hookMethod(onCreate, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.thisObject !is Application) return
                runCatching { syncSettings(settingsClass) }
                    .onFailure { Logger.e("[area_unlock] sync failed", it) }
            }
        })
        Logger.i("[area_unlock] armed")
    }

    /** 把模块进程里的解锁配置写入 integrations 的 Settings。 */
    private fun syncSettings(settingsClass: Class<*>) {
        var applied = 0

        for ((ourKey, fieldName) in BOOL_MAP) {
            val value = ModulePrefs.isEnabled(ourKey)
            if (save(settingsClass, fieldName, value, java.lang.Boolean.TYPE)) applied++
        }

        for ((ourKey, fieldName) in STRING_MAP) {
            val value = runCatching {
                ModulePrefs.getString(ourKey, "")
            }.getOrDefault("")
            if (value.isEmpty()) continue
            if (save(settingsClass, fieldName, value, String::class.java)) applied++
        }

        Logger.i("[area_unlock] synced=$applied unlock=${ModulePrefs.isEnabled(SettingsKeys.MAIN_FUNC)}")
    }

    /**
     * 反射写一个 Setting：`Settings.<field>` 是 Setting 实例，调其 `save(value)`。
     *
     * @param valueType 用于挑选 `save` 重载（Kotlin 协变会在字节码里留下 Object 参数的方法，
     *                  因此这里按声明类型判断，兜底用单参数方法）
     */
    private fun save(settingsClass: Class<*>, fieldName: String, value: Any, valueType: Class<*>): Boolean {
        val field: Field = runCatching { settingsClass.getField(fieldName) }.getOrNull() ?: run {
            Logger.i("[area_unlock] field missing: $fieldName")
            return false
        }
        val setting = runCatching { field.get(null) }.getOrNull() ?: return false
        val saveMethod = setting.javaClass.methods.firstOrNull {
            it.name == "save" && it.parameterCount == 1 &&
                (it.parameterTypes[0] == valueType || it.parameterTypes[0] == Any::class.java)
        }
        if (saveMethod == null) {
            Logger.i("[area_unlock] save() missing on $fieldName")
            return false
        }
        return runCatching {
            saveMethod.invoke(setting, value)
            Logger.i("[area_unlock] $fieldName = ${if (valueType == String::class.java) "***" else value}")
            true
        }.getOrElse {
            Logger.e("[area_unlock] save $fieldName failed", it)
            false
        }
    }
}
