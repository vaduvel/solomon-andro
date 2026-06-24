package ro.solomon.app.services

/**
 * Drip sequencer for behavioral micro-lessons over time.
 *
 * Picks one micro-lesson per period (typically per day) from
 * [CoachKnowledgeBase], rotating through a curated order so the user gets a
 * steady, non-repetitive behavioral nudge. Deterministic and stateless: the
 * same day index always yields the same lesson, so callers do not need to
 * persist progress.
 */
object CoachMicroLessons {

    /** Curated delivery order: behavior and education first, concrete facts later. */
    private val order: List<CoachKnowledgeTopic> = listOf(
        CoachKnowledgeTopic.MINDSET,
        CoachKnowledgeTopic.HABITS,
        CoachKnowledgeTopic.EMOTIONAL_MONEY,
        CoachKnowledgeTopic.GENERAL,
        CoachKnowledgeTopic.SAVING,
        CoachKnowledgeTopic.DEBT_PSYCHOLOGY,
        CoachKnowledgeTopic.COMPOUNDING,
        CoachKnowledgeTopic.BIG_PURCHASE,
        CoachKnowledgeTopic.DEBT,
        CoachKnowledgeTopic.RISK,
        CoachKnowledgeTopic.INVESTING,
        CoachKnowledgeTopic.CAREER,
        CoachKnowledgeTopic.CULTURE_RO,
        CoachKnowledgeTopic.COUPLE
    )

    data class MicroLesson(
        val topic: CoachKnowledgeTopic,
        val title: String,
        val text: String
    )

    /** All available micro-lessons in curated delivery order. */
    fun all(): List<MicroLesson> = order.mapNotNull { topic ->
        val entry = CoachKnowledgeBase.entry(topic)
        entry.microLesson?.let { MicroLesson(topic, entry.title, it) }
    }

    /** The micro-lesson for a 0-based day index, rotating through the sequence. */
    fun forDayIndex(dayIndex: Long): MicroLesson? {
        val lessons = all()
        if (lessons.isEmpty()) return null
        val size = lessons.size
        val i = ((dayIndex % size) + size) % size
        return lessons[i.toInt()]
    }

    /** The micro-lesson for a given clock, using days since the Unix epoch as the index. */
    fun forDate(nowEpochMillis: Long): MicroLesson? =
        forDayIndex(nowEpochMillis / 86_400_000L)

    /**
     * Adaptive selection: biases the daily rotation toward the topics most relevant
     * to the user's real weakness ([SolomonCoachVulnerability]) and dominant money
     * script ([MoneyScript]), while still cycling through everything over time.
     *
     * Relevant lessons are weighted by appearing first AND remaining in the full
     * cycle, so the user sees their weak spot roughly twice as often without losing
     * variety. Falls back to the plain [forDate] rotation when there is no context
     * signal (both null) or when no relevant lesson has content yet. Still
     * deterministic per day index.
     */
    fun forContext(
        nowEpochMillis: Long,
        vulnerability: SolomonCoachVulnerability?,
        script: MoneyScript?
    ): MicroLesson? {
        val lessons = all()
        if (lessons.isEmpty()) return null
        val priority = priorityTopicsFor(vulnerability, script)
        if (priority.isEmpty()) return forDate(nowEpochMillis)
        val prioritized = lessons.filter { it.topic in priority }
        if (prioritized.isEmpty()) return forDate(nowEpochMillis)
        val weighted = prioritized + lessons
        val size = weighted.size
        val dayIndex = nowEpochMillis / 86_400_000L
        val i = ((dayIndex % size) + size) % size
        return weighted[i.toInt()]
    }

    /**
     * Maps the user's weakness and money script to the knowledge topics worth
     * reinforcing. Both signals contribute; the union is used. Empty when we have
     * no signal, which makes [forContext] fall back to the neutral rotation.
     */
    private fun priorityTopicsFor(
        vulnerability: SolomonCoachVulnerability?,
        script: MoneyScript?
    ): Set<CoachKnowledgeTopic> {
        val set = mutableSetOf<CoachKnowledgeTopic>()
        when (vulnerability) {
            SolomonCoachVulnerability.SMALL_RECURRING -> {
                set += CoachKnowledgeTopic.HABITS
                set += CoachKnowledgeTopic.EMOTIONAL_MONEY
            }
            SolomonCoachVulnerability.HEAVY_OBLIGATIONS -> {
                set += CoachKnowledgeTopic.DEBT
                set += CoachKnowledgeTopic.DEBT_PSYCHOLOGY
            }
            SolomonCoachVulnerability.IRREGULAR_INCOME -> {
                set += CoachKnowledgeTopic.SAVING
                set += CoachKnowledgeTopic.RISK
            }
            SolomonCoachVulnerability.GOALS_WITHOUT_CONTRIBUTION -> {
                set += CoachKnowledgeTopic.SAVING
                set += CoachKnowledgeTopic.COMPOUNDING
            }
            SolomonCoachVulnerability.CASHFLOW_PRESSURE -> {
                set += CoachKnowledgeTopic.BIG_PURCHASE
                set += CoachKnowledgeTopic.SAVING
            }
            null -> {}
        }
        when (script) {
            MoneyScript.AVOIDANCE -> {
                set += CoachKnowledgeTopic.MINDSET
                set += CoachKnowledgeTopic.EMOTIONAL_MONEY
            }
            MoneyScript.WORSHIP -> {
                set += CoachKnowledgeTopic.BIG_PURCHASE
                set += CoachKnowledgeTopic.COMPOUNDING
            }
            MoneyScript.STATUS -> {
                set += CoachKnowledgeTopic.MINDSET
                set += CoachKnowledgeTopic.CULTURE_RO
            }
            MoneyScript.VIGILANCE -> {
                set += CoachKnowledgeTopic.INVESTING
                set += CoachKnowledgeTopic.RISK
            }
            null -> {}
        }
        return set
    }
}
