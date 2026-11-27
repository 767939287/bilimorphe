package app.bilimorphe.patch

import android.util.Pair
import app.bilimorphe.PatchEngine
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.luckypray.dexkit.DexKitBridge
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Hooks the host HTTP stack and forwards requests/responses to the embedded
 * integrations runtime. The target method is the obfuscated equivalent of
 * okhttp's RealCall.getResponseWithInterceptorChain(), located with DexKit.
 */
object OkHttpHookPatch : Patch(
    name = "Network bridge",
    description = "Forwards host HTTP requests and responses to the embedded integrations runtime.",
) {
    private const val INTEGRATIONS = "app.revanced.bilibili.patches.okhttp.OkHttpPatch"
    private const val RETURN_TYPE = "okhttp3.Response"

    private lateinit var integrations: Class<*>
    private lateinit var shouldHook: Method
    private lateinit var hookResponse: Method
    private lateinit var hookRequest: Method

    override fun applyTo(engine: PatchEngine) {
        val moduleLoader = OkHttpHookPatch::class.java.classLoader!!

        val bridge: DexKitBridge = DexKitBridge.create(engine.hostApkPath)
        val target = try {
            bridge.findMethod {
                matcher {
                    returnType = RETURN_TYPE
                    paramCount = 0
                    usingStrings("Canceled")
                }
            }.firstOrNull()
        } finally {
            runCatching { bridge.close() }
        }
        if (target == null) {
            Logger.e("failed to locate host network entry point")
            return
        }
        Logger.i("network entry point: ${target.className}#${target.methodName}")

        integrations = Class.forName(INTEGRATIONS, true, moduleLoader)
        shouldHook = integrations.getMethod(
            "shouldHook", String::class.java, Int::class.javaPrimitiveType
        )
        hookResponse = integrations.getMethod(
            "hook",
            String::class.java, Int::class.javaPrimitiveType,
            String::class.java, InputStream::class.java,
            String::class.java, InputStream::class.java,
        )
        hookRequest = integrations.getMethod(
            "hookBefore", String::class.java, Array<String>::class.java
        )

        val entry = target.getMethodInstance(engine.classLoader)
        entry.isAccessible = true

        XposedBridge.hookMethod(entry, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                runCatching { rewriteRequest(param) }
                    .onFailure { Logger.e("request rewrite failed", it) }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                runCatching { rewriteResponse(param) }
                    .onFailure { Logger.e("response rewrite failed", it) }
            }
        })
        Logger.i("network bridge installed")
    }

    private fun rewriteRequest(param: XC_MethodHook.MethodHookParam) {
        val call = param.thisObject ?: return
        val requestField: Field = findRequestField(call.javaClass) ?: return
        val request = requestField.get(call) as? Request ?: return
        val url = request.url().toString()
        val headers = request.headers()
        val original = Array(headers.size()) { "${headers.name(it)}: ${headers.value(it)}" }

        @Suppress("UNCHECKED_CAST")
        val result = hookRequest.invoke(null, url, original) as? Pair<String, Array<String>> ?: return
        val newUrl = result.first
        val newHeaders = result.second

        if (newUrl == url && original.contentEquals(newHeaders)) return

        val builder = Request.Builder().url(newUrl)
        for (line in newHeaders) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            builder.header(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
        }
        if (request.method() != "GET") {
            builder.method(request.method(), request.body())
        }
        requestField.set(call, builder.build())
    }

    private fun rewriteResponse(param: XC_MethodHook.MethodHookParam) {
        val response = param.result as? Response ?: return
        val url = response.request().url().toString()
        val code = response.code()

        val wanted = shouldHook.invoke(null, url, code) as? Boolean ?: false
        if (!wanted) return

        val body = response.body() ?: return
        val bytes = body.bytes()
        val encoding = response.header("Content-Encoding")

        val rewritten = hookResponse.invoke(
            null,
            url,
            code,
            null,
            ByteArrayInputStream(ByteArray(0)),
            encoding,
            ByteArrayInputStream(bytes),
        ) as? String ?: return

        if (rewritten.isEmpty()) return

        val newResponse = response.newBuilder()
            .body(ResponseBody.create(body.contentType(), rewritten))
            .removeHeader("Content-Encoding")
            .removeHeader("Content-Length")
            .build()
        param.result = newResponse
    }

    private fun findRequestField(clazz: Class<*>): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            current.declaredFields.firstOrNull { it.type == Request::class.java }?.let {
                it.isAccessible = true
                return it
            }
            current = current.superclass
        }
        return null
    }
}
