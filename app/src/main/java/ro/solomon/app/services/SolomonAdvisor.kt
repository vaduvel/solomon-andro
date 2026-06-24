package ro.solomon.app.services

import ro.solomon.core.domain.FinancialPersonality

object SolomonAdvisor {

    /** Map a free-text topic to a knowledge-base topic. */
    fun topicFor(rawTopic: String): CoachKnowledgeTopic {
        val topic = rawTopic.lowercase().trim()
        return when {
            topic.contains("economisire") || topic.contains("saving") || topic.contains("economi") -> CoachKnowledgeTopic.SAVING
            topic.contains("datori") || topic.contains("credit") || topic.contains("imprumut") -> CoachKnowledgeTopic.DEBT
            topic.contains("cuplu") || topic.contains("partener") || topic.contains("relatie") -> CoachKnowledgeTopic.COUPLE
            topic.contains("investit") || topic.contains("invest") -> CoachKnowledgeTopic.INVESTING
            topic.contains("mindset") || topic.contains("mentalit") || topic.contains("psiholog") -> CoachKnowledgeTopic.MINDSET
            topic.contains("cumpara") || topic.contains("achizit") || topic.contains("mare") -> CoachKnowledgeTopic.BIG_PURCHASE
            topic.contains("carier") || topic.contains("job") || topic.contains("salar") -> CoachKnowledgeTopic.CAREER
            topic.contains("risc") || topic.contains("urgenta") || topic.contains("fond") -> CoachKnowledgeTopic.RISK
            topic.contains("roman") || topic.contains("cultura") -> CoachKnowledgeTopic.CULTURE_RO
            else -> CoachKnowledgeTopic.GENERAL
        }
    }

    /**
     * Curated, dated and sourced wisdom for a topic. Backed by [CoachKnowledgeBase]:
     * dated facts are rendered with their date and official source, plus a staleness
     * guardrail relative to [nowEpochMillis]. Signature stays compatible with callers
     * that pass only a topic.
     */
    fun wisdom(rawTopic: String, nowEpochMillis: Long = System.currentTimeMillis()): String =
        CoachKnowledgeBase.render(topicFor(rawTopic), nowEpochMillis)

    fun coupleQuestions(userType: FinancialPersonality, partnerType: FinancialPersonality): String {
        return """
        Agenda intalnirilor financiare lunare Solomon Doi:

        1. Ce ne-a mers bine luna asta cu banii?
        2. Unde am depasit bugetul fara sa discutam?
        3. Cheltuielile comune - cine a platit ce si suntem ok cu asta?
        4. Progresul spre obiectivul comun principal?
        5. O decizie financiara mare de luat impreuna luna viitoare?

        ${coupleInsight(userType, partnerType)}
        """.trimIndent()
    }

    private fun coupleInsight(u: FinancialPersonality, p: FinancialPersonality): String {
        return when {
            u == FinancialPersonality.spender && p == FinancialPersonality.saver ->
                "Tensiune clasica cheltuitor-economisitor: stabiliti o suma de cheltuieli personale fara justificare pentru fiecare."
            u == FinancialPersonality.saver && p == FinancialPersonality.spender ->
                "Partenerul tau are nevoie de libertate; tu ai nevoie de predictibilitate. Contul comun cu regula clara rezolva 80% din conflicte."
            u == FinancialPersonality.avoider || p == FinancialPersonality.avoider ->
                "Un partener evitant face conversatiile financiare stresante. Mergeti direct la cifre, fara judecata, maxim 20 de minute."
            u == FinancialPersonality.monk && p == FinancialPersonality.monk ->
                "Amandoi minimalisti - atentie sa nu taiati si placerile mici care tin relatia vie."
            else -> "Tipurile voastre financiare se pot echilibra bine daca stabiliti o regula clara pentru cheltuielile comune."
        }
    }
}
