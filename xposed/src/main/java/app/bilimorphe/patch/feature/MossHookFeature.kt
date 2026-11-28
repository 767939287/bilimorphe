package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

/**
 * Moss / gRPC 报文拦截（1.23.3 的 `Moss` 补丁，基础设施，无独立开关）。
 *
 * 行为依据：1.23.3 在宿主 Moss 服务的 `blockingUnaryCall` / `asyncUnaryCall` 里注入：
 * - 调 `MossPatch.hookBlockingBefore(request)`，返回非空则替换请求；
 * - 调 `MossPatch.hookBlockingAfter(request, reply, exception)`，返回非空则替换响应。
 * 区域解锁（`BangumiPlayUrlHook`）、番剧 season 等全部依赖这一层。
 *
 * 本项目重写为运行时 hook `com.bilibili.lib.moss.api.KMossServiceImp.blockingUnaryCall`，
 * 反射调用内置 integrations 的 `MossPatch` 同名方法 —— 逻辑复用、行为与 1.23.3 一致。
 * protobuf 类型通过宿主 ClassLoader 解析，避免模块侧直接依赖 protobuf。
 */
object MossHookFeature : Patch(
    name = "Moss protobuf hook",
    description = "Routes host gRPC (Moss) calls through the embedded protobuf hook layer.",
) {

    private const val MOSS_IMPL = "com.bilibili.lib.moss.api.KMossServiceImp"
    private const val INTEGRATIONS = "app.revanced.bilibili.patches.protobuf.MossPatch"

    override fun applyTo(engine: PatchEngine) {
        val cls = engine.findClass(MOSS_IMPL)
        if (cls == null) {
            Logger.i("[moss] $MOSS_IMPL missing")
            return
        }

        val target = cls.declaredMethods.firstOrNull {
            it.name == "blockingUnaryCall" && it.parameterCount == 3
        }
        if (target == null) {
            Logger.i("[moss] blockingUnaryCall not found")
            return
        }

        val impl = runCatching {
            Class.forName(INTEGRATIONS, true, MossHookFeature::class.java.classLoader)
        }.getOrNull()
        if (impl == null) {
            Logger.e("[moss] integrations MossPatch missing", null)
            return
        }

        val before = impl.methods.firstOrNull { it.name == "hookBlockingBefore" && it.parameterCount == 1 }
        val after = impl.methods.firstOrNull { it.name == "hookBlockingAfter" && it.parameterCount == 3 }
        if (before == null || after == null) Logger.i("[moss] hook methods incomplete")

        target.isAccessible = true
        XposedBridge.hookMethod(target, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val request = param.args.getOrNull(1) ?: return
                runCatching {
                    val replaced = before?.invoke(null, request)
                    if (replaced != null) param.args[1] = replaced
                }.onFailure { Logger.e("[moss] before hook failed", it) }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val request = param.args.getOrNull(1) ?: return
                val reply = param.result
                runCatching {
                    val newReply = after?.invoke(null, request, reply, param.throwable)
                    if (newReply != null) param.result = newReply
                }.onFailure { Logger.e("[moss] after hook failed", it) }
            }
        })
        Logger.i("[moss] blockingUnaryCall hooked")
    }

    /** 供其他 Feature 复用：找到宿主 protobuf 基类，便于按类型过滤。 */
    @Suppress("unused")
    private fun generatedMessageLite(engine: PatchEngine): Class<*>? =
        engine.findClass("com.google.protobuf.GeneratedMessageLite")
}
