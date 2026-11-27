package app.bilimorphe.patch

import app.bilimorphe.PatchEngine

/**
 * A patch is a self contained unit of runtime modification.
 * Each patch is applied once per process and must be idempotent safe.
 */
abstract class Patch(
    val name: String,
    val description: String = "",
) {
    abstract fun applyTo(engine: PatchEngine)
}
