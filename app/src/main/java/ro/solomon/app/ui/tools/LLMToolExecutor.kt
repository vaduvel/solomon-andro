package ro.solomon.app.ui.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.ObligationConfidence
import ro.solomon.core.domain.ObligationKind
import ro.solomon.core.domain.Subscription
import ro.solomon.core.domain.CancellationDifficulty
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource

sealed class ToolResult {
    data class Ok(val message: String) : ToolResult()
    data class Error(val reason: String) : ToolResult()
}

sealed class ToolIntent {
    data class AddTransaction(val amount: Int, val category: TransactionCategory, val merchant: String?, val direction: FlowDirection) : ToolIntent()
    data class AddObligation(val name: String, val amount: Int, val dayOfMonth: Int, val kind: ObligationKind) : ToolIntent()
    data class AddGoal(val destination: String, val amount: Int, val deadline: Long?) : ToolIntent()
    data class AddSubscription(val name: String, val amount: Int) : ToolIntent()
    data class DeleteObligation(val name: String) : ToolIntent()
    data class DeleteGoal(val destination: String) : ToolIntent()
    data class DeleteSubscription(val name: String) : ToolIntent()
    object SummarizeToday : ToolIntent()
    object SummarizeWeek : ToolIntent()
    object SummarizeMonth : ToolIntent()
    data class SetBudget(val category: TransactionCategory, val amount: Int) : ToolIntent()
    object NoTool : ToolIntent()
}

object LLMToolExecutor {

    suspend fun execute(text: String): ToolResult = withContext(Dispatchers.IO) {
        when (val intent = parseIntent(text)) {
            is ToolIntent.AddTransaction -> addTransaction(intent)
            is ToolIntent.AddObligation -> addObligation(intent)
            is ToolIntent.AddGoal -> addGoal(intent)
            is ToolIntent.AddSubscription -> addSubscription(intent)
            is ToolIntent.DeleteObligation -> deleteObligation(intent.name)
            is ToolIntent.DeleteGoal -> deleteGoal(intent.destination)
            is ToolIntent.DeleteSubscription -> deleteSubscription(intent.name)
            is ToolIntent.SummarizeToday -> summarizeToday()
            is ToolIntent.SummarizeWeek -> summarizeWeek()
            is ToolIntent.SummarizeMonth -> summarizeMonth()
            is ToolIntent.SetBudget -> ToolResult.Ok("Am setat bugetul de ${intent.amount} RON pentru ${intent.category.displayNameRO}.")
            ToolIntent.NoTool -> ToolResult.Error("Nu am inteles ce trebuie sa fac. Incearca: «adauga o chirie de 1500 RON pe 5», «cat am cheltuit azi», «seteaza buget 500 RON la mancare».")
        }
    }

    fun parseIntent(text: String): ToolIntent {
        val t = text.lowercase().trim()
        if (t.isEmpty()) return ToolIntent.NoTool

        if (matchesAny(t, listOf("cât am cheltuit azi", "ce am cheltuit azi", "azi cheltuieli", "azi cât am cheltuit"))) return ToolIntent.SummarizeToday
        if (matchesAny(t, listOf("cât am cheltuit săptămâna", "săptămâna asta cheltuieli", "rezumat săptămână", "săptămâna trecută"))) return ToolIntent.SummarizeWeek
        if (matchesAny(t, listOf("cât am cheltuit luna", "rezumat lunar", "luna asta cheltuieli", "rezumat luna"))) return ToolIntent.SummarizeMonth

        if (matchesAny(t, listOf("adaugă tranzacție", "adauga tranzactie", "log tranzacție", "am cheltuit"))) {
            val amount = extractAmount(t) ?: return ToolIntent.NoTool
            val cat = extractCategory(t)
            val merchant = extractMerchant(t)
            val dir = if (t.contains("am primit") || t.contains("am încasat") || t.contains("salariu")) FlowDirection.incoming else FlowDirection.outgoing
            return ToolIntent.AddTransaction(amount, cat, merchant, dir)
        }

        if (matchesAny(t, listOf("adaugă obligație", "adauga obligatie", "adaugă o chirie", "adaugă rată", "adauga rata"))) {
            val amount = extractAmount(t) ?: return ToolIntent.NoTool
            val day = extractDayOfMonth(t) ?: 1
            val name = extractAfterKeyword(t, listOf("chirie", "rata", "abona", "factur", "asigur", "credit"))?.replaceFirstChar { it.uppercase() } ?: "Obligație"
            val kind = if (t.contains("chirie")) ObligationKind.rent_mortgage
                else if (t.contains("rata") || t.contains("credit")) ObligationKind.loan_bank
                else if (t.contains("factur") || t.contains("utilit")) ObligationKind.utility
                else if (t.contains("asigur")) ObligationKind.insurance
                else ObligationKind.other
            return ToolIntent.AddObligation(name, amount, day, kind)
        }

        if (matchesAny(t, listOf("adauga obiectiv", "vreau sa economisesc", "vreau să economisesc", "am ca obiectiv"))) {
            val now = System.currentTimeMillis() / 1000
            val amount = extractAmount(t) ?: return ToolIntent.NoTool
            val dest = extractAfterKeyword(t, listOf("vacanta", "vacanță", "casa", "casă", "masina", "mașină", "fond de", "reparat")) ?: "Obiectiv"
            val kind = if (t.contains("vacan") || t.contains("calat") || t.contains("călăt")) GoalKind.vacation
                else if (t.contains("casa") || t.contains("casă") || t.contains("apart")) GoalKind.house
                else if (t.contains("masina") || t.contains("mașin")) GoalKind.car
                else if (t.contains("repar") || t.contains("urgen")) GoalKind.emergency_fund
                else GoalKind.custom
            return ToolIntent.AddGoal(dest, amount, now + 180L * 86400)
        }

        if (matchesAny(t, listOf("adaugă abonament", "adauga abonament", "am abonament", "plătesc lunar"))) {
            val amount = extractAmount(t) ?: return ToolIntent.NoTool
            val name = extractAfterKeyword(t, listOf("netflix", "spotify", "hbo", "disney", "amazon prime", "apple", "youtube")) ?: "Abonament"
            return ToolIntent.AddSubscription(name, amount)
        }

        if (matchesAny(t, listOf("șterge obligația", "sterge obligatia", "elimină obligația", "elimina obligatia"))) {
            val name = extractAfterKeyword(t, listOf("chirie", "rata", "factur", "asigur", "credit")) ?: return ToolIntent.NoTool
            return ToolIntent.DeleteObligation(name.replaceFirstChar { it.uppercase() })
        }
        if (matchesAny(t, listOf("șterge obiectivul", "sterge obiectivul", "elimină obiectivul"))) {
            val dest = extractAfterKeyword(t, listOf("vacanță", "vacanta", "casă", "casa")) ?: return ToolIntent.NoTool
            return ToolIntent.DeleteGoal(dest)
        }
        if (matchesAny(t, listOf("șterge abonamentul", "sterge abonamentul", "anulează abonamentul", "anuleaza abonamentul"))) {
            val name = extractAfterKeyword(t, listOf("netflix", "spotify", "hbo", "disney")) ?: return ToolIntent.NoTool
            return ToolIntent.DeleteSubscription(name.replaceFirstChar { it.uppercase() })
        }

        if (matchesAny(t, listOf("setează buget", "seteaza buget", "buget lunar", "buget pentru"))) {
            val amount = extractAmount(t) ?: return ToolIntent.NoTool
            val cat = extractCategory(t)
            return ToolIntent.SetBudget(cat, amount)
        }

        return ToolIntent.NoTool
    }

    private fun matchesAny(text: String, needles: List<String>): Boolean =
        needles.any { text.contains(it) }

    private fun extractAmount(text: String): Int? {
        val m = Regex("""(\d{1,3}(?:[.,\s]\d{3})*|\d+)\s*(?:ron|lei|€)?""", RegexOption.IGNORE_CASE).find(text) ?: return null
        val raw = m.groupValues[1].replace(".", "").replace(",", ".").replace(" ", "")
        return raw.toIntOrNull()
    }

    private fun extractDayOfMonth(text: String): Int? {
        val m = Regex("""pe\s+(\d{1,2})|ziua\s+(\d{1,2})|data\s+(\d{1,2})""", RegexOption.IGNORE_CASE).find(text) ?: return null
        val day = m.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.toIntOrNull() ?: return null
        return day.takeIf { it in 1..31 }
    }

    private fun extractCategory(text: String): TransactionCategory {
        val t = text.lowercase()
        return when {
            t.contains("mâncare") || t.contains("mancare") || t.contains("kaufland") || t.contains("lidl") || t.contains("cumpărături alimentare") -> TransactionCategory.food_grocery
            t.contains("restaurant") || t.contains("livrare") || t.contains("kfc") || t.contains("mcdonald") -> TransactionCategory.food_dining
            t.contains("transport") || t.contains("uber") || t.contains("bolt") || t.contains("benzin") -> TransactionCategory.transport
            t.contains("utilit") || t.contains("enel") || t.contains("electrica") || t.contains("apă") || t.contains("gaz") -> TransactionCategory.utilities
            t.contains("chirie") || t.contains("rată") -> TransactionCategory.rent_mortgage
            t.contains("abonament") || t.contains("netflix") || t.contains("spotify") -> TransactionCategory.subscriptions
            t.contains("emag") || t.contains("online") || t.contains("altex") -> TransactionCategory.shopping_online
            t.contains("sănătate") || t.contains("sanatate") || t.contains("farmacie") || t.contains("medic") -> TransactionCategory.health
            t.contains("distracție") || t.contains("distractie") || t.contains("cinema") || t.contains("film") -> TransactionCategory.entertainment
            t.contains("călător") || t.contains("calator") || t.contains("vacanț") || t.contains("vacant") -> TransactionCategory.travel
            t.contains("împrumut") || t.contains("imprumut") || t.contains("credit") -> TransactionCategory.loans_bank
            else -> TransactionCategory.unknown
        }
    }

    private fun extractMerchant(text: String): String? {
        val known = listOf("Kaufland", "Lidl", "Carrefour", "Auchan", "Mega Image", "eMAG", "Altex", "Bolt", "Uber", "Netflix", "Spotify", "HBO", "Disney", "Enel", "Electrica", "Cinema City", "KFC", "McDonald's")
        for (m in known) {
            if (text.contains(m, ignoreCase = true)) return m
        }
        return null
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String? {
        for (k in keywords) {
            val idx = text.indexOf(k, ignoreCase = true)
            if (idx >= 0) {
                val after = text.substring(idx + k.length).trim().split(Regex("""[.,;!?\n]""")).first().trim()
                if (after.isNotEmpty() && after.length < 60) return after
            }
        }
        return null
    }

    private suspend fun addTransaction(intent: ToolIntent.AddTransaction): ToolResult {
        val txn = Transaction(
            id = "txn-${System.currentTimeMillis()}",
            date = System.currentTimeMillis() / 1000,
            amount = Money(intent.amount),
            direction = intent.direction,
            category = intent.category,
            merchant = intent.merchant,
            source = TransactionSource.manual_entry,
            categorizationConfidence = 1.0
        )
        ServiceLocator.txnRepo.save(txn)
        val dirWord = if (intent.direction == FlowDirection.incoming) "intrat" else "iesit"
        return ToolResult.Ok("Am notat: ${intent.amount} RON $dirWord la ${intent.merchant ?: intent.category.displayNameRO}.")
    }

    private suspend fun addObligation(intent: ToolIntent.AddObligation): ToolResult {
        val now = System.currentTimeMillis() / 1000
        val ob = Obligation(
            id = "obl-${System.currentTimeMillis()}",
            name = intent.name,
            amount = Money(intent.amount),
            dayOfMonth = intent.dayOfMonth,
            kind = intent.kind,
            confidence = ObligationConfidence.declared,
            since = now,
            nextDueDate = now + intent.dayOfMonth * 86400L
        )
        ServiceLocator.obligationRepo.save(ob)
        return ToolResult.Ok("Am adaugat obligatia «${intent.name}» de ${intent.amount} RON pe ${intent.dayOfMonth}.")
    }

    private suspend fun addGoal(intent: ToolIntent.AddGoal): ToolResult {
        val g = Goal(
            id = "goal-${System.currentTimeMillis()}",
            kind = GoalKind.custom,
            destination = intent.destination,
            amountTarget = Money(intent.amount),
            amountSaved = Money(0),
            deadline = intent.deadline ?: (System.currentTimeMillis() / 1000 + 180L * 86400)
        )
        ServiceLocator.goalRepo.save(g)
        return ToolResult.Ok("Am adaugat obiectivul «${intent.destination}» cu tinta de ${intent.amount} RON.")
    }

    private suspend fun addSubscription(intent: ToolIntent.AddSubscription): ToolResult {
        val s = Subscription(
            id = "sub-${System.currentTimeMillis()}",
            name = intent.name,
            amountMonthly = Money(intent.amount),
            lastUsedDaysAgo = 0,
            cancellationDifficulty = CancellationDifficulty.medium,
            cancellationUrl = null,
            cancellationStepsSummary = null,
            alternativeSuggestion = null,
            cancellationWarning = null
        )
        ServiceLocator.subRepo.save(s)
        return ToolResult.Ok("Am adaugat abonamentul «${intent.name}» de ${intent.amount} RON/luna.")
    }

    private suspend fun deleteObligation(name: String): ToolResult {
        val all = ServiceLocator.obligationRepo.fetchAll()
        val match = all.firstOrNull { it.name.contains(name, ignoreCase = true) } ?: return ToolResult.Error("Nu am gasit obligatia «$name».")
        ServiceLocator.obligationRepo.delete(match.id)
        return ToolResult.Ok("Am sters obligatia «${match.name}».")
    }

    private suspend fun deleteGoal(destination: String): ToolResult {
        val all = ServiceLocator.goalRepo.fetchAll()
        val match = all.firstOrNull { it.destination?.contains(destination, ignoreCase = true) == true } ?: return ToolResult.Error("Nu am gasit obiectivul «$destination».")
        ServiceLocator.goalRepo.delete(match.id)
        return ToolResult.Ok("Am sters obiectivul «${match.destination}».")
    }

    private suspend fun deleteSubscription(name: String): ToolResult {
        val all = ServiceLocator.subRepo.fetchAll()
        val match = all.firstOrNull { it.name.contains(name, ignoreCase = true) } ?: return ToolResult.Error("Nu am gasit abonamentul «$name».")
        ServiceLocator.subRepo.delete(match.id)
        return ToolResult.Ok("Am sters abonamentul «${match.name}».")
    }

    private suspend fun summarizeToday(): ToolResult {
        val txns = ServiceLocator.txnRepo.fetchAll()
        val now = System.currentTimeMillis() / 1000
        val dayStart = now - 24 * 3600
        val today = txns.filter { it.date >= dayStart }
        val spent = today.filter { it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount }
        val income = today.filter { it.direction == FlowDirection.incoming }.sumOf { it.amount.amount }
        return ToolResult.Ok("Ultimele 24h: ${today.size} tranzactii, ${spent} RON cheltuiti, ${income} RON intrati.")
    }

    private suspend fun summarizeWeek(): ToolResult {
        val txns = ServiceLocator.txnRepo.fetchAll()
        val now = System.currentTimeMillis() / 1000
        val weekStart = now - 7L * 24 * 3600
        val week = txns.filter { it.date >= weekStart }
        val spent = week.filter { it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount }
        val income = week.filter { it.direction == FlowDirection.incoming }.sumOf { it.amount.amount }
        return ToolResult.Ok("Ultima saptamana: ${week.size} tranzactii, ${spent} RON cheltuiti, ${income} RON intrati.")
    }

    private suspend fun summarizeMonth(): ToolResult {
        val txns = ServiceLocator.txnRepo.fetchAll()
        val now = System.currentTimeMillis() / 1000
        val monthStart = now - 30L * 24 * 3600
        val month = txns.filter { it.date >= monthStart }
        val spent = month.filter { it.direction == FlowDirection.outgoing }.sumOf { it.amount.amount }
        val income = month.filter { it.direction == FlowDirection.incoming }.sumOf { it.amount.amount }
        val byCat = month.filter { it.direction == FlowDirection.outgoing }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .joinToString { (cat, amt) -> "${cat.displayNameRO} ${amt} RON" }
        return ToolResult.Ok("Ultima luna: ${month.size} tranzactii, ${spent} RON cheltuiti, ${income} RON intrati. Top: $byCat.")
    }
}
