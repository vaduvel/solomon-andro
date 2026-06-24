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
}
