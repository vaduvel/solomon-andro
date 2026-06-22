package ro.solomon.app.ui.tools

import android.content.Context
import org.json.JSONObject
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.SolomonAdvisor
import ro.solomon.app.services.SolomonCoachMemory
import ro.solomon.app.services.SolomonCoachVulnerability
import ro.solomon.app.services.TrueCostComparator
import ro.solomon.app.services.CategoryLimitsStore
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.ObligationConfidence
import ro.solomon.core.domain.ObligationKind
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.enablebanking.BankConnectionService
import ro.solomon.analytics.PatternDetector
import java.util.Calendar
import java.util.UUID

class LLMToolExecutor(private val context: Context) {

    suspend fun execute(call: ro.solomon.llm.AgentToolCall): String {
        if (ro.solomon.llm.LLMAgentTool.requiresConfirmation(call.name) && !isToolCallConfirmed(call.params)) {
            return "[Eroare] Ac\u021Biunea \"${call.name}\" este critic\u0103 \u0219i necesit\u0103 confirmare explicit\u0103. Re\u00EEncearc-o cu \"confirm\": true."
        }
        return when (call.name) {
            "query_transactions" -> queryTransactions(call.params)
            "add_transaction" -> addTransaction(call.params)
            "update_goal" -> updateGoal(call.params)
            "query_goals" -> queryGoals()
            "analyze_spending" -> analyzeSpending(call.params)
            "get_goal_scenarios" -> getGoalScenarios(call.params)
            "forecast_balance" -> forecastBalance()
            "create_goal" -> createGoal(call.params)
            "set_budget_limit" -> setBudgetLimit(call.params)
            "get_category_limits" -> getCategoryLimits()
            "clear_category_limit" -> clearCategoryLimit(call.params)
            "get_cashflow_summary" -> getCashflowSummary(call.params)
            "get_financial_snapshot" -> getFinancialSnapshot(call.params)
            "get_data_coverage" -> getDataCoverage(call.params)
            "analyze_budget" -> analyzeBudget(call.params)
            "detect_risk" -> detectRisk(call.params)
            "suggest_action" -> suggestAction(call.params)
            "education_tip" -> educationTip(call.params)
            "monthly_report" -> monthlyReport(call.params)
            "run_coach_flow" -> runCoachFlow(call.params)
            "query_bank_connections" -> queryBankConnections()
            "sync_bank_connections" -> syncBankConnections(call.params)
            "disconnect_bank" -> disconnectBank(call.params)
            "schedule_reminder" -> "[Rezultat schedule_reminder] Reminder-ul necesit\u0103 implementare pe platforma Android (AlarmManager)."
            "add_obligation" -> addObligation(call.params)
            "delete_transaction" -> deleteTransactionSafe(call.params)
            "consult_advisor" -> consultAdvisor(call.params)
            "couple_check_in" -> coupleCheckIn()
            else -> "Tool necunoscut: ${call.name}"
        }
    }

    private fun isToolCallConfirmed(params: Map<String, Any?>): Boolean {
        val v = params["confirm"] ?: params["confirmed"] ?: return false
        return when (v) {
            is Boolean -> v
            is String -> v.lowercase().trim() in listOf("true", "da", "yes", "confirm")
            is Int -> v != 0
            else -> false
        }
    }

    private suspend fun queryTransactions(params: Map<String, Any?>): String {
        val days = (params["days"] as? Number)?.toInt() ?: 30
        val catQuery = (params["category"] as? String)?.lowercase()
        val cal = Calendar.getInstance()
        val from = cal.apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
        val all = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from }
        val txs = if (catQuery != null && catQuery.isNotEmpty()) {
            all.filter { it.direction == FlowDirection.outgoing && (matches(it.category.displayNameRO, catQuery) || matches(it.merchant, catQuery)) }
        } else {
            all.filter { it.direction == FlowDirection.outgoing }
        }
        val total = txs.sumOf { it.amount.amount }
        val byMerchant = txs.groupBy { it.merchant ?: it.category.displayNameRO }
            .mapValues { it.value.sumOf { t -> t.amount.amount } }
            .entries.sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${it.key}: ${it.value} RON" }
        return """
            [Rezultat query_transactions]
            Perioad\u0103: $days zile
            Total cheltuieli: $total RON
            Num\u0103r tranzac\u021Bii: ${txs.size}
            Top: ${if (byMerchant.isEmpty()) "niciuna" else byMerchant}
        """.trimIndent()
    }

    private suspend fun addTransaction(params: Map<String, Any?>): String {
        val amountRaw = (params["amount"] as? Number)?.toInt()
            ?: (params["amount"] as? String)?.toIntOrNull()
            ?: return "[Eroare add_transaction] Parametri lips\u0103: amount \u0219i merchant sunt obligatorii."
        val merchant = params["merchant"] as? String
            ?: return "[Eroare add_transaction] Parametri lips\u0103: amount \u0219i merchant sunt obligatorii."
        if (amountRaw <= 0) return "[Eroare add_transaction] Suma trebuie s\u0103 fie pozitiv\u0103."
        val catStr = (params["category"] as? String) ?: ""
        val category = TransactionCategory.entries.firstOrNull {
            it.displayNameRO.lowercase().contains(catStr.lowercase())
        } ?: TransactionCategory.unknown
        val tx = Transaction(
            id = "chat-${UUID.randomUUID()}",
            date = System.currentTimeMillis(),
            amount = Money.fromLei(amountRaw),
            direction = FlowDirection.outgoing,
            category = category,
            merchant = merchant,
            source = TransactionSource.manual_entry,
            categorizationConfidence = 0.9
        )
        ServiceLocator.txnRepo.save(tx)
        return "[Rezultat add_transaction] Ad\u0103ugat: $amountRaw RON la $merchant (${category.displayNameRO})."
    }

    private suspend fun updateGoal(params: Map<String, Any?>): String {
        val goalName = params["goal_name"] as? String
            ?: return "[Eroare update_goal] Parametri lips\u0103: goal_name \u0219i add_amount."
        val addAmount = (params["add_amount"] as? Number)?.toInt()
            ?: return "[Eroare update_goal] Parametri lips\u0103: goal_name \u0219i add_amount."
        val goals = ServiceLocator.goalRepo.fetchAll()
        val goal = goals.firstOrNull {
            (it.destination?.lowercase()?.contains(goalName.lowercase()) == true) ||
            it.kind.displayNameRO.lowercase().contains(goalName.lowercase())
        } ?: return "[Eroare update_goal] Obiectivul '$goalName' nu exist\u0103. Disponibile: ${goals.joinToString { it.destination ?: it.kind.displayNameRO }}"
        val newSaved = goal.amountSaved.amount + Money.fromLei(addAmount).amount
        val updated = goal.copy(amountSaved = Money.fromBani(newSaved))
        ServiceLocator.goalRepo.save(updated)
        val pct = (updated.progressFraction * 100).toInt()
        return "[Rezultat update_goal] ${updated.destination ?: updated.kind.displayNameRO}: +$addAmount RON ad\u0103uga\u021Bi. Total salvat: ${newSaved / 100} RON / ${updated.amountTarget.amount / 100} RON ($pct%)."
    }

    private suspend fun queryGoals(): String {
        val goals = ServiceLocator.goalRepo.fetchAll()
        if (goals.isEmpty()) return "[Rezultat query_goals] Nu ai obiective active."
        val lines = goals.joinToString("\n") { g ->
            val name = g.destination ?: g.kind.displayNameRO
            val saved = g.amountSaved.amount / 100
            val target = g.amountTarget.amount / 100
            val pct = (g.progressFraction * 100).toInt()
            "$name: $saved/$target RON ($pct%) \u2014 mai lipsesc ${target - saved} RON"
        }
        return "[Rezultat query_goals]\n$lines"
    }

    private suspend fun analyzeSpending(params: Map<String, Any?>): String {
        val days = (params["days"] as? Number)?.toInt() ?: 30
        val cal = Calendar.getInstance()
        val from = cal.apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
        val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from && it.direction == FlowDirection.outgoing }
        val report = PatternDetector().detect(txs, days)
        val lines = mutableListOf("[Analiz\u0103 cheltuieli $days zile]")
        val topCats = report.topCategories.take(5).joinToString(" | ") {
            "${it.category.displayNameRO}: ${it.totalAmount.amount} RON (${(it.fractionOfTotal * 100).toInt()}%)" +
            (it.dominantMerchant?.let { m -> " \u2014 top: $m" } ?: "")
        }
        lines.add("Top categorii: $topCats")
        if (report.topCategories.isNotEmpty()) {
            val topCat = report.topCategories.first()
            val annual = topCat.totalAmount.amount * 12
            TrueCostComparator.compare(annual, topCat.category)?.let { lines.add("Cost anual estimat: $annual RON \u2248 $it") }
        }
        return lines.joinToString("\n")
    }

    private suspend fun getGoalScenarios(params: Map<String, Any?>): String {
        val goalName = params["goal_name"] as? String ?: ""
        val goals = ServiceLocator.goalRepo.fetchAll()
        val goal = goals.firstOrNull {
            (it.destination?.lowercase()?.contains(goalName.lowercase()) == true) ||
            it.kind.displayNameRO.lowercase().contains(goalName.lowercase()) || goalName.isEmpty()
        } ?: return "[Eroare] Obiectivul '$goalName' nu exist\u0103."
        val dest = goal.destination ?: goal.kind.displayNameRO
        val target = goal.amountTarget.amount
        val saved = goal.amountSaved.amount
        val remaining = target - saved
        val cal = Calendar.getInstance()
        val months = maxOf(1, ((goal.deadline - cal.timeInMillis) / (30L * 86_400_000L)).toInt())
        val required = maxOf(1, remaining / months)
        return """
            [Scenarii obiectiv: $dest]
            \u021Aint\u0103: ${target / 100} RON | Str\u00E2ns: ${saved / 100} RON | R\u0103m\u00E2n: ${remaining / 100} RON
            Deadline: $months luni
            \u2014 Ritm necesar: ${required / 100} RON/lun\u0103 \u2192 atingi \u00EEn $months luni
        """.trimIndent()
    }

    private suspend fun forecastBalance(): String {
        val obligations = ServiceLocator.obligationRepo.fetchAll()
        val goals = ServiceLocator.goalRepo.fetchAll().filter { it.amountSaved.amount < it.amountTarget.amount }
        val monthlyObl = obligations.sumOf { it.amount.amount }
        val cal = Calendar.getInstance()
        val from60 = cal.apply { add(Calendar.DAY_OF_YEAR, -60) }.timeInMillis
        val txs60 = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from60 && it.direction == FlowDirection.outgoing }
        val avgMonthly = if (txs60.isEmpty()) 0 else txs60.sumOf { it.amount.amount } / 2
        val net = avgMonthly - monthlyObl
        val lines = mutableListOf("[Proiec\u021Bie sold]")
        lines.add("Cheltuieli medii: ${avgMonthly / 100} RON/lun\u0103 | Obliga\u021Bii: ${monthlyObl / 100} RON/lun\u0103")
        for (m in listOf(1, 3, 6)) {
            val projBalance = net * m
            val riskFlag = if (projBalance <= 0) " \u26A0\uFE0F risc major" else if (projBalance < monthlyObl * m) " \u26A0\uFE0F risc" else ""
            lines.add("Luna +$m: sold estimat ~${projBalance / 100} RON$riskFlag")
        }
        return lines.joinToString("\n")
    }

    private suspend fun createGoal(params: Map<String, Any?>): String {
        val name = params["name"] as? String
            ?: return "[Eroare create_goal] Parametri lips\u0103: name \u0219i target_amount."
        val targetRaw = (params["target_amount"] as? Number)?.toInt()
            ?: return "[Eroare create_goal] Parametri lips\u0103: name \u0219i target_amount."
        val deadlineMonths = (params["deadline_months"] as? Number)?.toInt() ?: 12
        val kindStr = (params["kind"] as? String ?: "custom").lowercase()
        val kind = GoalKind.entries.firstOrNull {
            it.name.lowercase() == kindStr || it.displayNameRO.lowercase().contains(kindStr)
        } ?: GoalKind.custom
        val deadline = System.currentTimeMillis() + deadlineMonths * 30L * 86_400_000L
        val goal = Goal(
            id = UUID.randomUUID().toString(),
            kind = kind,
            destination = name,
            amountTarget = Money.fromLei(targetRaw),
            amountSaved = Money.zero,
            deadline = deadline
        )
        ServiceLocator.goalRepo.save(goal)
        val required = if (deadlineMonths > 0) targetRaw / deadlineMonths else targetRaw
        return "[Rezultat create_goal] Obiectiv creat: \"$name\" \u2014 \u021Bint\u0103 $targetRaw RON \u00EEn $deadlineMonths luni (~$required RON/lun\u0103)."
    }

    private fun setBudgetLimit(params: Map<String, Any?>): String {
        val catStr = params["category"] as? String
            ?: return "[Eroare set_budget_limit] Parametri lips\u0103: category \u0219i limit_amount."
        val limitRaw = (params["limit_amount"] as? Number)?.toInt()
            ?: return "[Eroare set_budget_limit] Parametri lips\u0103: category \u0219i limit_amount."
        val category = resolveCategory(catStr)
            ?: return "[Eroare set_budget_limit] Categorie necunoscut\u0103: \"$catStr\"."
        CategoryLimitsStore.set(context, category, limitRaw)
        return "[Rezultat set_budget_limit] Limit\u0103 setat\u0103: ${category.displayNameRO} \u2192 $limitRaw RON/lun\u0103."
    }

    private fun getCategoryLimits(): String {
        val limits = CategoryLimitsStore.getAll(context)
        if (limits.isEmpty()) return "[Rezultat get_category_limits] Nu ai setate limite pe categorii."
        val lines = limits.entries.joinToString("\n") { "${it.key.displayNameRO}: ${it.value} RON/lun\u0103" }
        return "[Rezultat get_category_limits]\n$lines"
    }

    private fun clearCategoryLimit(params: Map<String, Any?>): String {
        val catStr = params["category"] as? String
            ?: return "[Eroare clear_category_limit] Parametrul category este obligatoriu."
        val category = resolveCategory(catStr)
            ?: return "[Eroare clear_category_limit] Categorie necunoscut\u0103: \"$catStr\"."
        CategoryLimitsStore.remove(context, category)
        return "[Rezultat clear_category_limit] Limita pentru ${category.displayNameRO} a fost \u0219tears\u0103."
    }

    private suspend fun getCashflowSummary(params: Map<String, Any?>): String {
        val days = maxOf(1, (params["days"] as? Number)?.toInt() ?: 30)
        val cal = Calendar.getInstance()
        val from = cal.apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
        val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from }
        val incoming = txs.filter { it.direction == FlowDirection.incoming }
        val outgoing = txs.filter { it.direction == FlowDirection.outgoing }
        val totalIncome = incoming.sumOf { it.amount.amount }
        val totalExpense = outgoing.sumOf { it.amount.amount }
        val net = totalIncome - totalExpense
        val topCats = outgoing.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount.amount } }
            .entries.sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${it.key.displayNameRO}: ${it.value} RON" }
        val topMerchants = outgoing.groupBy { it.merchant ?: it.category.displayNameRO }
            .mapValues { it.value.sumOf { t -> t.amount.amount } }
            .entries.sortedByDescending { it.value }.take(3)
            .joinToString(", ") { "${it.key}: ${it.value} RON" }
        return """
            [Rezumat cashflow pe $days zile]
            Venituri: ${totalIncome / 100} RON
            Cheltuieli: ${totalExpense / 100} RON
            Net: ${net / 100} RON
            Top categorii: ${if (topCats.isEmpty()) "niciuna" else topCats}
            Top comercian\u021Bi: ${if (topMerchants.isEmpty()) "niciunul" else topMerchants}
        """.trimIndent()
    }

    private suspend fun getFinancialSnapshot(params: Map<String, Any?>): String {
        val snapshot = coachSnapshot(params)
        val topCat = snapshot.topCategory?.let { "${it.displayNameRO}: ${snapshot.topCategoryAmount} RON" } ?: "neclar\u0103"
        val goalsLine = if (snapshot.goals.isEmpty()) "niciun obiectiv activ"
            else "${snapshot.goals.size} active \u00B7 ${snapshot.goalsMonthlySaving} RON/lun\u0103 ritm necesar"
        return """
            [Snapshot financiar Solomon]
            Perioad\u0103: ultimele ${snapshot.days} zile
            Venit estimat lunar: ${snapshot.monthlyIncome} RON
            Cheltuieli: ${snapshot.spent} RON
            Obliga\u021Bii lunare: ${snapshot.obligationsMonthly} RON
            Obiective: $goalsLine
            Categorie dominant\u0103: $topCat
            Marj\u0103 personal\u0103 estimat\u0103: ${snapshot.safeMargin} RON
            Presiune lunar\u0103: ${(snapshot.pressureRatio * 100).toInt()}%
            Memorie coaching: vulnerabilitatea dominant\u0103 anterioar\u0103 este ${SolomonCoachMemory.vulnerability(context)?.title ?: "\u00EEnc\u0103 neclar\u0103"}
            Acoperire date: ${snapshot.dataCoverageLabel}
            Surse incluse: ${snapshot.dataSourceBreakdown}
        """.trimIndent()
    }

    private suspend fun getDataCoverage(params: Map<String, Any?>): String {
        val snapshot = coachSnapshot(params)
        return """
            [Acoperire date Solomon]
            Nivel: ${snapshot.dataCoverageLabel}
            Surse v\u0103zute \u00EEn ultimele ${snapshot.days} zile: ${snapshot.dataSourceBreakdown}
            Ce \u00EEnseamn\u0103: ${snapshot.dataCoverageDetail}
            Regula de r\u0103spuns: fii onest; dac\u0103 nu exist\u0103 bank sync, spune clar c\u0103 Solomon nu vede soldul real sau toate transferurile.
        """.trimIndent()
    }

    private suspend fun analyzeBudget(params: Map<String, Any?>): String {
        val snapshot = coachSnapshot(params)
        val categoryLine = if (snapshot.topCategory != null) {
            "${snapshot.topCategory.displayNameRO}: ${snapshot.topCategoryAmount} RON"
        } else {
            "Nu exist\u0103 destule tranzac\u021Bii pentru categorie dominant\u0103."
        }
        val verdict = when {
            snapshot.pressureRatio >= 1 -> "Presiune critic\u0103: luna consum\u0103 mai mult dec\u00E2t venitul estimat."
            snapshot.pressureRatio >= 0.85 -> "Presiune ridicat\u0103: spa\u021Biul de respira\u021Bie financiar\u0103 e \u00EEngust."
            snapshot.pressureRatio >= 0.65 -> "Ritm controlabil: exist\u0103 marj\u0103, dar trebuie protejat\u0103."
            else -> "Ritm bun: luna are spa\u021Biu de respira\u021Bie financiar\u0103."
        }
        return """
            [Analiz\u0103 buget Solomon]
            Verdict: $verdict
            Unde se duc banii: $categoryLine
            Marj\u0103 personal\u0103 estimat\u0103: ${snapshot.safeMargin} RON
            Presiune lunar\u0103: ${(snapshot.pressureRatio * 100).toInt()}%
            Acoperire date: ${snapshot.dataCoverageLabel} \u2014 ${snapshot.dataCoverageDetail}
        """.trimIndent()
    }

    private suspend fun detectRisk(params: Map<String, Any?>): String {
        val snapshot = coachSnapshot(params)
        val vulnerability = dominantVulnerability(snapshot)
        SolomonCoachMemory.save(context, vulnerability)
        return """
            [Risc Solomon]
            Vulnerabilitate dominant\u0103: ${vulnerability.title}
            Lec\u021Bie: ${vulnerability.lesson}
            Semnale: presiune ${(snapshot.pressureRatio * 100).toInt()}%, cheltuieli mici ${snapshot.smallExpenseCount}x / ${snapshot.smallExpenseTotal} RON.
            Memorie actualizat\u0103: Solomon va \u021Bine minte c\u0103 vulnerabilitatea dominant\u0103 este ${vulnerability.title}.
        """.trimIndent()
    }

    private suspend fun suggestAction(params: Map<String, Any?>): String {
        val snapshot = coachSnapshot(params)
        val vulnerability = SolomonCoachMemory.vulnerability(context) ?: dominantVulnerability(snapshot)
        return """
            [Ac\u021Biunea de azi Solomon]
            F\u0103 doar asta: ${vulnerability.action}
            De ce: protejeaz\u0103 marja personal\u0103 \u0219i reduce presiunea lunar\u0103 f\u0103r\u0103 o schimbare mare.
        """.trimIndent()
    }

    private suspend fun educationTip(params: Map<String, Any?>): String {
        val requested = SolomonCoachVulnerability.from(params["vulnerability"] as? String)
        val snapshot = coachSnapshot(params)
        val vulnerability = requested ?: SolomonCoachMemory.vulnerability(context) ?: dominantVulnerability(snapshot)
        return """
            [Educa\u021Bie contextual\u0103 Solomon]
            Tema: ${vulnerability.title}
            Lec\u021Bia de azi: ${vulnerability.lesson}
            Aplicare: ${vulnerability.action}
            Not\u0103 educa\u021Bional\u0103: ${vulnerability.contextualTip}
        """.trimIndent()
    }

    private suspend fun monthlyReport(params: Map<String, Any?>): String {
        val snapshot = coachSnapshot(params)
        val vulnerability = dominantVulnerability(snapshot)
        val goalsLine = if (snapshot.goals.isEmpty()) "niciun obiectiv activ"
            else "${snapshot.goals.size} active \u00B7 ${snapshot.goalsMonthlySaving} RON/lun\u0103 necesari"
        return """
            [Raport lunar Solomon \u2014 luna ta \u00EEn 5 puncte]
            Acoperire date: ${snapshot.dataCoverageLabel} \u2014 ${snapshot.dataCoverageDetail}
            1. Venit: ${if (snapshot.monthlyIncome > 0) "${snapshot.monthlyIncome} RON estimat" else "neclar"}
            2. Cheltuieli: ${snapshot.spent} RON \u00EEn ultimele ${snapshot.days} zile
            3. Obliga\u021Bii: ${snapshot.obligationsMonthly} RON/lun\u0103
            4. Obiective: $goalsLine
            5. Risc: ${vulnerability.title} \u2014 ${(snapshot.pressureRatio * 100).toInt()}% presiune lunar\u0103
        """.trimIndent()
    }

    private suspend fun runCoachFlow(params: Map<String, Any?>): String {
        val rawFlow = (params["flow"] as? String ?: "diagnostic").replace("-", "_").replace(" ", "_")
        val days = (params["days"] as? Number)?.toInt() ?: 30
        val flowParams = mapOf<String, Any?>("days" to days)
        return when (rawFlow) {
            "diagnostic", "diagnostic_financiar", "financial_diagnostic", "cum_stau", "risc" -> listOf(
                "[Flow Solomon Coach: diagnostic]",
                getFinancialSnapshot(flowParams),
                detectRisk(flowParams),
                suggestAction(flowParams),
                educationTip(flowParams)
            ).joinToString("\n\n")
            "budget_review", "buget", "analiza_buget", "unde_se_duc_banii" -> listOf(
                "[Flow Solomon Coach: budget_review]",
                analyzeBudget(flowParams),
                detectRisk(flowParams),
                suggestAction(flowParams),
                educationTip(flowParams)
            ).joinToString("\n\n")
            "monthly_report", "raport_lunar", "luna_mea", "rezumat_lunar" -> listOf(
                "[Flow Solomon Coach: monthly_report]",
                monthlyReport(flowParams),
                detectRisk(flowParams),
                educationTip(flowParams),
                suggestAction(flowParams)
            ).joinToString("\n\n")
            "daily_action", "actiune", "actiunea_de_azi", "ce_fac_azi" -> listOf(
                "[Flow Solomon Coach: daily_action]",
                detectRisk(flowParams),
                suggestAction(flowParams),
                educationTip(flowParams)
            ).joinToString("\n\n")
            else -> "[Eroare run_coach_flow] Flow necunoscut: $rawFlow. Folose\u0219te diagnostic, budget_review, monthly_report sau daily_action."
        }
    }

    private fun queryBankConnections(): String {
        val connections = BankConnectionService.allConnections
        if (connections.isEmpty()) return "[Rezultat query_bank_connections] Nu ai nicio conexiune bancar\u0103."
        val rows = connections.joinToString(" | ") { c ->
            val state = if (c.isExpired) "Expirat\u0103" else "Activ\u0103"
            "${c.aspspName} | $state | conturi:${c.accounts.size}"
        }
        return "[Rezultat query_bank_connections] $rows"
    }

    private suspend fun syncBankConnections(params: Map<String, Any?>): String {
        if (BankConnectionService.allConnections.isEmpty())
            return "[Eroare sync_bank_connections] Nu ai conexiuni bancare. Conecteaz\u0103 mai \u00EEnt\u00E2i o banc\u0103."
        val count = BankConnectionService.syncAll()
        return "[Rezultat sync_bank_connections] Sincronizate ${BankConnectionService.allConnections.size} conexiuni. Tranzac\u021Bii noi ingestate: $count."
    }

    private suspend fun disconnectBank(params: Map<String, Any?>): String {
        val bankName = params["bank_name"] as? String
        val connections = BankConnectionService.allConnections
        val target = connections.firstOrNull {
            it.aspspName.contains(bankName ?: "", ignoreCase = true)
        } ?: return "[Eroare disconnect_bank] Nu exist\u0103 potrivire pentru '$bankName'."
        BankConnectionService.disconnect(target)
        return "[Rezultat disconnect_bank] Conexiune deconectat\u0103: ${target.aspspName}."
    }

    private suspend fun addObligation(params: Map<String, Any?>): String {
        val name = params["name"] as? String
            ?: return "[Eroare add_obligation] Parametri lips\u0103: name, amount \u0219i day_of_month."
        val amountRaw = (params["amount"] as? Number)?.toInt()
            ?: return "[Eroare add_obligation] Parametri lips\u0103: name, amount \u0219i day_of_month."
        val dayRaw = (params["day_of_month"] as? Number)?.toInt()
            ?: return "[Eroare add_obligation] Parametri lips\u0103: name, amount \u0219i day_of_month."
        if (dayRaw !in 1..31) return "[Eroare add_obligation] day_of_month trebuie s\u0103 fie \u00EEntre 1 \u0219i 31."
        val kindStr = (params["kind"] as? String ?: "other").lowercase()
        val kind = ObligationKind.entries.firstOrNull {
            it.name.lowercase() == kindStr || it.displayNameRO.lowercase().contains(kindStr)
        } ?: ObligationKind.other
        val obligation = Obligation(
            id = UUID.randomUUID().toString(),
            name = name,
            amount = Money.fromLei(amountRaw),
            dayOfMonth = dayRaw,
            kind = kind,
            confidence = ObligationConfidence.declared,
            since = System.currentTimeMillis()
        )
        ServiceLocator.obligationRepo.save(obligation)
        return "[Rezultat add_obligation] Obliga\u021Bie ad\u0103ugat\u0103: \"$name\" \u2014 $amountRaw RON \u00EEn fiecare lun\u0103 pe data de $dayRaw (${kind.displayNameRO})."
    }

    private suspend fun deleteTransactionSafe(params: Map<String, Any?>): String {
        val merchantQuery = (params["merchant"] as? String)?.trim()
        val amountQuery = (params["amount"] as? Number)?.toInt()
        val cal = Calendar.getInstance()
        val from = cal.apply { add(Calendar.DAY_OF_YEAR, -14) }.timeInMillis
        val outgoing = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from && it.direction == FlowDirection.outgoing }
        val candidates = outgoing.filter { tx ->
            val merchantOK = merchantQuery == null || matches(tx.merchant, merchantQuery)
            val amountOK = amountQuery == null || kotlin.math.abs(amountQuery - tx.amount.amount / 100) <= 5
            merchantOK && amountOK
        }
        return when (candidates.size) {
            0 -> "[Eroare delete_transaction] Nicio tranzac\u021Bie potrivit\u0103 \u00EEn ultimele 14 zile."
            1 -> {
                val tx = candidates.first()
                ServiceLocator.txnRepo.delete(tx.id)
                "[Rezultat delete_transaction] \u0218tears\u0103: ${tx.amount.amount / 100} RON la ${tx.merchant ?: tx.category.displayNameRO}."
            }
            else -> "[Eroare delete_transaction] Sunt prea multe potriviri: ${candidates.size}. Specific\u0103 merchant + amount."
        }
    }

    private fun consultAdvisor(params: Map<String, Any?>): String {
        val topic = (params["topic"] as? String ?: "").lowercase()
        return SolomonAdvisor.wisdom(topic)
    }

    private suspend fun coupleCheckIn(): String {
        val profile = ServiceLocator.userRepo.fetchProfile()
        val partner = profile?.partner
            ?: return "[Eroare couple_check_in] Solomon Doi nu e activat. User-ul trebuie s\u0103-l activeze din Set\u0103ri \u2192 Solomon Doi."
        val cal = Calendar.getInstance()
        val from30 = cal.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from30 && it.direction == FlowDirection.outgoing }
        val totalSpent = txs.sumOf { it.amount.amount } / 100
        val aboveThreshold = txs.filter { it.amount.amount / 100 >= partner.sharedThresholdRON }
        val goals = ServiceLocator.goalRepo.fetchAll()
        val goalLines = goals.take(2).joinToString("\n") { g ->
            val dest = g.destination ?: g.kind.displayNameRO
            val pct = (g.progressFraction * 100).toInt()
            "\u2022 $dest: $pct% (${g.amountSaved.amount / 100}/${g.amountTarget.amount / 100} RON)"
        }
        return """
            [Agenda \u00EEnt\u00E2lnirii financiare Solomon Doi \u2014 ${profile.demographics.name} & ${partner.name}]
            REVIEW LUNA ANTERIOAR\u0102 (30 zile):
            - Total cheltuit: $totalSpent RON
            - Achizi\u021Bii peste prag (${partner.sharedThresholdRON} RON): ${aboveThreshold.size} tranzac\u021Bii (${aboveThreshold.sumOf { it.amount.amount / 100 }} RON)
            OBIECTIVE COMUNE:
            ${if (goalLines.isEmpty()) "\u2022 Nicio \u021Bint\u0103 comun\u0103 setat\u0103." else goalLines}
            REGULA DE AUR (Asoltanie): Conversa\u021Biile despre bani sunt despre VIITORUL VOSTRU COMUN, nu despre vinov\u0103\u021Bii trecute.
        """.trimIndent()
    }

    // MARK: - Coach Snapshot

    private data class CoachSnapshot(
        val days: Int,
        val monthlyIncome: Int,
        val spent: Int,
        val obligationsMonthly: Int,
        val goals: List<Goal>,
        val goalsMonthlySaving: Int,
        val topCategory: TransactionCategory?,
        val topCategoryAmount: Int,
        val topMerchant: String?,
        val smallExpenseCount: Int,
        val smallExpenseTotal: Int,
        val safeMargin: Int,
        val pressureRatio: Double,
        val dataCoverageLabel: String,
        val dataCoverageDetail: String,
        val dataSourceBreakdown: String
    )

    private suspend fun coachSnapshot(params: Map<String, Any?>): CoachSnapshot {
        val days = minOf(maxOf(7, (params["days"] as? Number)?.toInt() ?: 30), 90)
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val from = cal.apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
        val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= from }
        val outgoing = txs.filter { it.direction == FlowDirection.outgoing }
        val incoming = txs.filter { it.direction == FlowDirection.incoming }
        val notifCount = txs.count { it.source == TransactionSource.sms_parsed || it.source == TransactionSource.share_intent_parsed }
        val bankCount = txs.count { it.source == TransactionSource.bank_connection }
        val manualCount = txs.count { it.source == TransactionSource.manual_entry }
        val otherCount = maxOf(0, txs.size - notifCount - bankCount - manualCount)
        val hasBank = BankConnectionService.allConnections.any { !it.isExpired }
        val dataSourceBreakdown = "Notific\u0103ri: $notifCount, manual: $manualCount, bank sync: $bankCount, alte: $otherCount"
        val dataCoverage = when {
            bankCount > 0 || hasBank -> "ridicat\u0103" to "Solomon are semnale din banc\u0103/Open Banking; analiza poate include solduri \u0219i tranzac\u021Bii mai complete."
            notifCount > 0 && manualCount > 0 -> "par\u021Bial\u0103 bun\u0103" to "Analiza se bazeaz\u0103 pe notific\u0103ri \u0219i intr\u0103ri manuale; nu include sigur cash, transferuri, sold real."
            notifCount > 0 -> "par\u021Bial\u0103 notific\u0103ri" to "Solomon vede mai ales tranzac\u021Biile capturate prin notific\u0103ri; nu vede soldul real sau cash."
            manualCount > 0 -> "manual\u0103" to "Analiza se bazeaz\u0103 pe ce ai introdus manual."
            else -> "insuficient\u0103" to "Nu exist\u0103 \u00EEnc\u0103 destule tranzac\u021Bii recente."
        }
        val spent = outgoing.sumOf { it.amount.amount } / 100
        val incomeInWindow = incoming.sumOf { it.amount.amount } / 100
        val byCategory = outgoing.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount.amount } }
        val topCategoryPair = byCategory.entries.sortedByDescending { it.value }.firstOrNull()
        val topMerchant = outgoing.groupBy { it.merchant ?: it.category.displayNameRO }
            .mapValues { it.value.sumOf { t -> t.amount.amount } }
            .entries.maxByOrNull { it.value }?.key
        val smallExpenses = outgoing.filter { it.amount.amount / 100 <= 50 }
        val smallExpenseTotal = smallExpenses.sumOf { it.amount.amount } / 100
        val profile = ServiceLocator.userRepo.fetchProfile()
        val declaredIncome = (profile?.financials?.salaryRange?.midpointRON ?: 0) +
            (if (profile?.financials?.hasSecondaryIncome == true) (profile.financials.secondaryIncomeAvg?.amount?.let { it / 100 } ?: 0) else 0)
        val monthlyIncome = if (declaredIncome > 0) declaredIncome else incomeInWindow
        val obligations = ServiceLocator.obligationRepo.fetchAll()
        val obligationsMonthly = obligations.sumOf { it.amount.amount } / 100
        val goals = ServiceLocator.goalRepo.fetchAll()
        val goalsMonthlySaving = 0
        val safeMargin = monthlyIncome - spent - obligationsMonthly - goalsMonthlySaving
        val pressureBase = spent + obligationsMonthly + goalsMonthlySaving
        val pressureRatio = if (monthlyIncome > 0) pressureBase.toDouble() / monthlyIncome else 1.0
        return CoachSnapshot(
            days = days, monthlyIncome = monthlyIncome, spent = spent,
            obligationsMonthly = obligationsMonthly, goals = goals, goalsMonthlySaving = goalsMonthlySaving,
            topCategory = topCategoryPair?.key, topCategoryAmount = (topCategoryPair?.value ?: 0) / 100,
            topMerchant = topMerchant, smallExpenseCount = smallExpenses.size, smallExpenseTotal = smallExpenseTotal,
            safeMargin = safeMargin, pressureRatio = pressureRatio,
            dataCoverageLabel = dataCoverage.first, dataCoverageDetail = dataCoverage.second,
            dataSourceBreakdown = dataSourceBreakdown
        )
    }

    private fun dominantVulnerability(snapshot: CoachSnapshot): SolomonCoachVulnerability {
        if (snapshot.monthlyIncome <= 0) return SolomonCoachVulnerability.IRREGULAR_INCOME
        if (snapshot.obligationsMonthly.toDouble() / snapshot.monthlyIncome >= 0.40) return SolomonCoachVulnerability.HEAVY_OBLIGATIONS
        val totalGoalSaved = snapshot.goals.sumOf { it.amountSaved.amount / 100 }
        if (snapshot.goals.isNotEmpty() && (totalGoalSaved == 0 || snapshot.goalsMonthlySaving > maxOf(0, snapshot.safeMargin)))
            return SolomonCoachVulnerability.GOALS_WITHOUT_CONTRIBUTION
        if (snapshot.smallExpenseCount >= 8 && snapshot.smallExpenseTotal >= maxOf(250, snapshot.spent / 5))
            return SolomonCoachVulnerability.SMALL_RECURRING
        if (snapshot.pressureRatio >= 0.85 || snapshot.safeMargin < maxOf(150, snapshot.monthlyIncome / 10))
            return SolomonCoachVulnerability.CASHFLOW_PRESSURE
        return SolomonCoachVulnerability.SMALL_RECURRING
    }

    private fun resolveCategory(raw: String): TransactionCategory? {
        val query = normalize(raw)
        val candidates = TransactionCategory.entries.filter { c ->
            matches(c.displayNameRO, query) || matches(c.name, query)
        }
        return if (candidates.size == 1) candidates.first() else candidates.firstOrNull { matches(it.displayNameRO, query) }
    }

    private fun matches(value: String?, query: String): Boolean {
        if (value == null) return false
        val source = normalize(value)
        return source == query || source.contains(query) || query.contains(source)
    }

    private fun normalize(value: String): String = value.lowercase().trim()
}
