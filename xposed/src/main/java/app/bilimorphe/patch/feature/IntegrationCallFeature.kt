package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 1.23.3 功能的统一运行时接入层。
 *
 * 背景：1.23.3 的补丁都是「在宿主方法里插一行 invoke-static 调 integrations」，
 * 逻辑本身已经在 `integrations/` 里（如 `CopyEnhancePatch`、`DpiPatch` 等）。
 * 运行时不需要重写逻辑，只要**找到同一个宿主方法**并反射调用同名 integrations 方法即可。
 *
 * 本引擎把这件事配置化：每条 [Rule] 描述
 * 「设置键 + 宿主方法定位条件 + integrations 类与方法 + 调用方式」，
 * 命中后按 [Mode] 处理参数或返回值。所有规则独立 try-catch，单条失败不影响其他。
 */
object IntegrationCallFeature : Patch(
    name = "Integration call bridge",
    description = "Configurable bridge that calls embedded integrations methods from host hook points.",
) {

    /** 调用方式。 */
    enum class Mode {
        /** 调用前把返回值写回某个参数（用于返回 Context / View 的包装方法）。 */
        REPLACE_ARG,

        /** 调用后替换方法返回值。 */
        REPLACE_RESULT,

        /** 强制返回 false（开关型 getter）。 */
        FORCE_FALSE,

        /** 强制返回 null。 */
        FORCE_NULL,

        /** 用指定参数调用 integrations，结果写回该参数（如构造函数参数修正）。 */
        REPLACE_ARG_WITH_SELF,

        /** 调 integrations 无参方法，返回 true 时把方法结果置为 false（弹窗屏蔽类）。 */
        CONDITIONAL_FALSE,

        /** 用 this 调 integrations 的方法，不关心返回值（就地修改对象）。 */
        CALL_WITH_THIS,

        /** 同 [CALL_WITH_THIS]，但在宿主方法执行完毕后再调（需 View 已创建的重）。 */
        CALL_WITH_THIS_AFTER,

        /** integrations 无参返回 true 时，方法直接返回某个入参（如分享去追踪）。 */
        RETURN_ARG_CONDITIONAL,

        /** 用指定参数调 integrations，返回 true 时不执行原方法（如屏蔽 toast）。 */
        ARG_CONDITIONAL_NULL,

        /** 用指定参数调 integrations，忽略返回值（如就地修改 Bundle）。 */
        CALL_ARG,
    }

    /**
     * 一条接入规则。
     *
     * @param key 设置键；为 `null` 表示由 integrations 侧自行判断开关，始终安装
     * @param hostClass 宿主类全名；为空时用 DexKit 按 [methodName] / [strings] 搜索
     * @param methodName 宿主方法名（混淆类可留空，改用 [strings]）
     * @param paramCount 参数个数（可空表示不限）
     * @param returnType DexKit 定位时的返回类型（Java 类型名，如 `int`）
     * @param strings DexKit 定位时的字符串特征
     * @param integrationsClass integrations 侧类全名
     * @param integrationsMethod integrations 侧静态方法名
     * @param mode 调用方式
     * @param argIndex [Mode.REPLACE_ARG] 时被替换的参数下标
     */
    data class Rule(
        val key: String?,
        val hostClass: String? = null,
        val methodName: String? = null,
        val paramCount: Int? = null,
        val returnType: String? = null,
        val strings: List<String> = emptyList(),
        val integrationsClass: String,
        val integrationsMethod: String,
        val mode: Mode,
        val argIndex: Int = 0,
    )

    /**
     * 规则表：逐项补充，空表时整个 Feature 不安装。
     *
     * 填表步骤：
     * 1. 从 `docs/data/patch-status-1.23.3.tsv` 选一项待移植功能；
     * 2. 在 1.23.3 的补丁里找到它注入的「宿主类 + 方法 + 参数个数」；
     * 3. 在 9.21.0 索引（`/tmp/index921.tsv` 或重建）里核实该方法仍存在、签名未变；
     * 4. 按下面格式补一条 [Rule]，开关关闭时不会安装。
     */
    private val RULES: List<Rule> = listOf(
        // 视频默认画质（1.23.3 VideoQualityPatch）：宿主 PlayerSettingHelper 的取画质方法
        // 开头插入 getMatchedFullScreenQuality()，非 0 则直接返回它。
        Rule(
            key = null, // 由 integrations 侧按自身设置判断
            methodName = null,
            returnType = "int",
            strings = listOf("PlayerSettingHelper", "get free data failed"),
            integrationsClass = "app.revanced.bilibili.patches.VideoQualityPatch",
            integrationsMethod = "getMatchedFullScreenQuality",
            mode = Mode.REPLACE_RESULT,
        ),

        // 青少年模式弹窗（1.23.3 TeenagerModePatch）：disableTeenagerDialog() 为 true 时直接返回 false。
        Rule(
            key = null,
            returnType = "boolean",
            strings = listOf(
                "restrictedmode.teenagers_alert_api_config",
                "teenagers_mode_show_dialog_interval",
            ),
            integrationsClass = "app.revanced.bilibili.patches.SettingsTransfer",
            integrationsMethod = "disableTeenagerDialog",
            mode = Mode.CONDITIONAL_FALSE,
        ),

        // 禁止自动选集（1.23.3 PublishToFollowingPatch）：构造函数第 4 个 boolean 参数过一遍开关。
        Rule(
            key = null,
            methodName = "<init>",
            paramCount = 4,
            strings = listOf("PublishToFollowingConfig(visible="),
            integrationsClass = "app.revanced.bilibili.patches.SettingsTransfer",
            integrationsMethod = "shouldAutoSelectOnce",
            mode = Mode.REPLACE_ARG_WITH_SELF,
            argIndex = 3,
        ),

        // 评论词搜索 URL（1.23.3 CommentReplyUrlPatch）：取 urls 前先过滤。
        Rule(
            key = null,
            hostClass = "com.bapis.bilibili.main.community.reply.v1.Content",
            methodName = "internalGetUrls",
            paramCount = 0,
            integrationsClass = "app.revanced.bilibili.patches.CommentReplyUrlPatch",
            integrationsMethod = "filterUrls",
            mode = Mode.CALL_WITH_THIS,
        ),

        // 保存评论图片（1.23.3 SaveCommentImagePatch）：ImageFragment 视图创建后绑定长按。
        Rule(
            key = null,
            hostClass = "com.bilibili.lib.imageviewer.fragment.ImageFragment",
            methodName = "onViewCreated",
            integrationsClass = "app.revanced.bilibili.patches.CommentImagePatch",
            integrationsMethod = "bindClickListener",
            mode = Mode.CALL_WITH_THIS_AFTER,
        ),

        // 分享去除追踪参数（1.23.3 SharePatch）：disableAppendTrackingInfo() 为 true 时返回原 url。
        Rule(
            key = null,
            returnType = "java.lang.String",
            paramCount = 2,
            strings = listOf("share_medium", "share_source", "bbid", "ts"),
            integrationsClass = "app.revanced.bilibili.patches.SettingsTransfer",
            integrationsMethod = "disableAppendTrackingInfo",
            mode = Mode.RETURN_ARG_CONDITIONAL,
            argIndex = 0,
        ),

        // 播放器 Toast（1.23.3 PlayerToastPatch）：onShow(toast) 为 true 则不显示。
        // 注意：9.21.0 的实际字符串带尾随空格（`player show toast: `）。
        Rule(
            key = null,
            strings = listOf("player show toast: "),
            paramCount = 1,
            integrationsClass = "app.revanced.bilibili.patches.PlayerToastPatch",
            integrationsMethod = "onShow",
            mode = Mode.ARG_CONDITIONAL_NULL,
            argIndex = 0,
        ),

        // 强制硬解（1.23.3 HwCodecPatch）：改 ijk 的 option bundle。
        // 9.21.0 里特征串只剩 `enable-use-https-when-forbidden`（`enable-decoder-switch` 已改名），
        // 因此改为直接钩 ijk 的 setOptionBundle 入口，取第 2 个参数（Bundle）。
        Rule(
            key = null,
            hostClass = "tv.danmaku.ijk.media.player.IIjkMediaPlayerItem\$Stub\$Proxy",
            methodName = "setOptionBundle",
            paramCount = 2,
            integrationsClass = "app.revanced.bilibili.patches.HwCodecPatch",
            integrationsMethod = "printOptionBundle",
            mode = Mode.CALL_ARG,
            argIndex = 1,
        ),
    )

    override fun applyTo(engine: PatchEngine) {
        val active = RULES.filter { it.key == null || ModulePrefs.isEnabled(it.key!!) }
        if (active.isEmpty()) {
            Logger.i("[integration_call] no rule enabled, skip")
            return
        }

        val moduleLoader = IntegrationCallFeature::class.java.classLoader!!
        val pendingGlobal = active.filter { it.hostClass == null }
        val pendingClass = active.filter { it.hostClass != null }

        var hooked = 0

        // 1) 指定类名的规则：直接定位
        for (rule in pendingClass) {
            val cls = engine.findClass(rule.hostClass!!)
            if (cls == null) {
                Logger.i("[integration_call] ${rule.key}: class missing ${rule.hostClass}")
                continue
            }
            val method = cls.declaredMethods.firstOrNull {
                it.name == rule.methodName && (rule.paramCount == null || it.parameterCount == rule.paramCount)
            }
            if (method == null) {
                Logger.i("[integration_call] ${rule.key}: method missing ${rule.methodName}")
                continue
            }
            if (install(rule, method, moduleLoader)) hooked++
        }

        // 2) 未指定类名的规则：DexKit 按方法名全量搜索
        if (pendingGlobal.isNotEmpty()) {
            runCatching {
                val bridge = DexKitBridge.create(engine.hostApkPath)
                try {
                    for (rule in pendingGlobal) {
                        val hits = bridge.findMethod {
                            matcher {
                                rule.methodName?.let { name = it }
                                rule.paramCount?.let { paramCount = it }
                                rule.returnType?.let { returnType = it }
                                if (rule.strings.isNotEmpty()) {
                                    usingStrings(*rule.strings.toTypedArray())
                                }
                            }
                        }
                        for (data in hits) {
                            val method = runCatching { data.getMethodInstance(engine.classLoader) }
                                .getOrNull() ?: continue
                            if (install(rule, method, moduleLoader)) hooked++
                        }
                    }
                } finally {
                    runCatching { bridge.close() }
                }
            }.onFailure { Logger.e("[integration_call] dexkit search failed", it) }
        }

        Logger.i("[integration_call] active=${active.size} hooked=$hooked")
    }

    private fun install(rule: Rule, method: Method, moduleLoader: ClassLoader): Boolean {
        val implClass = runCatching {
            Class.forName(rule.integrationsClass, true, moduleLoader)
        }.getOrNull()
        if (implClass == null) {
            Logger.i("[integration_call] ${rule.key}: integrations class missing")
            return false
        }
        val implMethod = implClass.declaredMethods.firstOrNull {
            it.name == rule.integrationsMethod && Modifier.isStatic(it.modifiers)
        }
        if (implMethod == null) {
            Logger.i("[integration_call] ${rule.key}: integrations method missing ${rule.integrationsMethod}")
            return false
        }
        implMethod.isAccessible = true
        method.isAccessible = true

        return runCatching {
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    when (rule.mode) {
                        Mode.FORCE_FALSE -> param.result = false
                        Mode.FORCE_NULL -> param.result = null
                        Mode.REPLACE_ARG -> runCatching {
                            val args = param.args.toMutableList()
                            val callArgs = args.withIndex()
                                .filter { it.index != rule.argIndex }
                                .map { it.value }
                                .toTypedArray()
                            val replaced = if (callArgs.isEmpty()) {
                                implMethod.invoke(null)
                            } else {
                                implMethod.invoke(null, *callArgs)
                            }
                            if (replaced != null) param.args[rule.argIndex] = replaced
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: arg replace failed", it) }

                        Mode.REPLACE_ARG_WITH_SELF -> runCatching {
                            val current = param.args.getOrNull(rule.argIndex) ?: return@runCatching
                            val replaced = implMethod.invoke(null, current)
                            if (replaced != null) param.args[rule.argIndex] = replaced
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: self arg replace failed", it) }

                        Mode.CONDITIONAL_FALSE -> runCatching {
                            val flag = implMethod.invoke(null) as? Boolean ?: false
                            if (flag) param.result = false
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: conditional false failed", it) }

                        Mode.CALL_WITH_THIS -> runCatching {
                            val self = param.thisObject ?: return@runCatching
                            implMethod.invoke(null, self)
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: this call failed", it) }

                        Mode.RETURN_ARG_CONDITIONAL -> runCatching {
                            val flag = implMethod.invoke(null) as? Boolean ?: false
                            if (flag) param.result = param.args.getOrNull(rule.argIndex)
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: arg conditional failed", it) }

                        Mode.ARG_CONDITIONAL_NULL -> runCatching {
                            val arg = param.args.getOrNull(rule.argIndex) ?: return@runCatching
                            val block = implMethod.invoke(null, arg) as? Boolean ?: false
                            if (block) param.result = null
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: arg conditional null failed", it) }

                        Mode.CALL_ARG -> runCatching {
                            val arg = param.args.getOrNull(rule.argIndex) ?: return@runCatching
                            implMethod.invoke(null, arg)
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: arg call failed", it) }

                        Mode.CALL_WITH_THIS_AFTER -> Unit

                        Mode.REPLACE_RESULT -> Unit
                    }
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    if (rule.mode == Mode.CALL_WITH_THIS_AFTER) {
                        runCatching {
                            val self = param.thisObject ?: return@runCatching
                            implMethod.invoke(null, self)
                        }.onFailure { Logger.e("[integration_call] ${rule.key}: after this call failed", it) }
                        return
                    }
                    if (rule.mode != Mode.REPLACE_RESULT) return
                    runCatching {
                        // integrations 方法无参时不传宿主参数；有参则按位置传
                        val args = if (implMethod.parameterCount == 0) {
                            emptyArray()
                        } else {
                            param.args.filterNotNull().toTypedArray()
                        }
                        val replaced = implMethod.invoke(null, *args)
                        if (replaced != null) param.result = replaced
                    }.onFailure { Logger.e("[integration_call] ${rule.key}: result replace failed", it) }
                }
            })
            Logger.i("[integration_call] ${rule.key}: hooked ${method.declaringClass.name}#${method.name}")
            true
        }.getOrElse {
            Logger.e("[integration_call] ${rule.key}: hook failed", it)
            false
        }
    }
}
