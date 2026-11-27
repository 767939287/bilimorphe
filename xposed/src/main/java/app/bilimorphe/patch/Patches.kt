package app.bilimorphe.patch

/**
 * Patch registry. The order matters: the bridge patch must run first so the
 * integrations runtime is ready before network hooks are installed.
 */
object Patches {
    const val TARGET_PACKAGE = "tv.danmaku.bili"

    private val patches: List<Patch> = listOf(
        StartupProbePatch,
        BridgePatch,
        OkHttpHookPatch,
    )

    fun forPackage(packageName: String): List<Patch>? =
        if (packageName == TARGET_PACKAGE) patches else null
}
