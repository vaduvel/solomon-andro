package ro.solomon.moments

import ro.solomon.core.moments.MomentType

sealed class OrchestratorError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object NoCandidatesAvailable : OrchestratorError("No moment candidates are available for the current snapshot")

    data class BuildFailed(
        val momentType: MomentType,
        val underlying: Throwable
    ) : OrchestratorError("Moment build failed for ${'$'}momentType: ${'$'}{underlying.message}", underlying)
}
