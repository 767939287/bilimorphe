package app.bilimorphe.patch

/**
 * Patch registry. Maps host packages to the patches that should run inside them.
 */
object Patches {
    const val BILI_PACKAGE = "tv.danmaku.bili"

    private val registry: Map<String, List<Patch>> = mapOf(
        BILI_PACKAGE to listOf(
            StartupProbePatch,
        ),
    )

    fun forPackage(packageName: String): List<Patch>? = registry[packageName]
}
