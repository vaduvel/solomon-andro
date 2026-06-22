package ro.solomon.app.ui.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.SolomonAdvisor
import ro.solomon.app.services.SolomonCoachMemory
import ro.solomon.app.services.SolomonCoachVulnerability
import ro.solomon.app.services.TrueCostComparator
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.ObligationConfidence
import ro.solomon.core.domain.ObligationKind
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.FinancialPersonality
import ro.solomon.core.enablebanking.BankConnectionService
import ro.solomon.core.util.CategoryLimitsStore
import ro.solomon.llm.AgentToolCall
import ro.solomon.analytics.PatternDetector

class LLMToolExecutor(private val context: Context) {

    suspend fun execute(call: AgentToolCall): String = withContext(Dispatchers.IO) {
        val params = call.params

        // Confirmation gate for destructive operations
        fun isConfirmed(): Boolean {
            val c = params["confirm"] ?: params["confirmed"]
            return when (c) {
                is Boolean -> c
                is String -> c.lowercase() in listOf("true", "yes", "da", "confirm", "1")
                is Int -> c != 0
                else -> false
            }
        }

        when (call.name) {

            "query_transactions" -> {
                val days = (params["days"] as? Int) ?: 30
                val category = params["category"] as? String
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll()
                    .filter { it.date >= since }
                    .let { list ->
                        if (category != null) list.filter { it.category.name.contains(category, ignoreCase = true) }
                        else list
                    }
                if (txs.isEmpty()) return@withContext "Nu am gasit tranzactii in ultimele $days zile${if (category != null) " pentru categoria $category" else ""} ."
                val total = txs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount }
                val lines = txs.takeLast(20).joinToString("\n") { tx ->
                    "- ${tx.merchant ?: tx.category.displayNameRO}: ${tx.amount.amount / 100} RON (${tx.category.displayNameRO})"
                }
                "Tranzactii (${txs.size}) in ultimele $days zile - Total cheltuieli: ${total / 100} RON\n$lines"
            }

            "add_transaction" -> {
                val amount = when (val a = params["amount"]) {
                    is Int -> a
                    is Double -> a.toInt()
                    is Long -> a.toInt()
                    else -> return@withContext "Lipsa parametru: amount (RON)"
                }
                val merchant = params["merchant"] as? String
                val categoryStr = params["category"] as? String ?: "other"
                val direction = params["direction"] as? String ?: "outgoing"
                val category = try {
                    TransactionCategory.valueOf(categoryStr)
                } catch (_: Exception) {
                    TransactionCategory.unknown
                }
                ServiceLocator.txnRepo.addManual(
                    amountBani = amount * 100,
                    merchant = merchant,
                    category = category,
                    isIncoming = direction == "incoming"
                )
                "Am adaugat ${if (direction == "incoming") "venit" else "cheltuiala"}: $amount RON${if (merchant != null) " la $merchant" else ""} (${category.displayNameRO})."
            }

            "update_goal" -> {
                val goalName = params["goal_name"] as? String ?: return@withContext "Lipsa parametru: goal_name"
                val addAmount = when (val a = params["add_amount"]) {
                    is Int -> a
                    is Double -> a.toInt()
                    else -> return@withContext "Lipsa parametru: add_amount"
                }
                val goals = ServiceLocator.goalRepo.fetchAll()
                val goal = goals.firstOrNull { it.destination?.contains(goalName, ignoreCase = true) == true }
                    ?: return@withContext "Nu am gasit obiectivul '$goalName'. Obiective existente: ${goals.joinToString { it.destination ?: it.kind.displayNameRO }}"
                ServiceLocator.goalRepo.addSaving(goal.id, Money.fromLei(addAmount))
                val newSaved = (goal.savedAmount.amount + addAmount * 100) / 100
                val target = goal.targetAmount.amount / 100
                "Am adaugat $addAmount RON la '${goal.destination ?: goal.kind.displayNameRO}'. Economisit: $newSaved RON din $target RON."
            }

            "query_goals" -> {
                val goals = ServiceLocator.goalRepo.fetchAll()
                if (goals.isEmpty()) return@withContext "Nu ai obiective financiare active. Poti crea unul cu create_goal."
                val now = System.currentTimeMillis()
                goals.joinToString("\n") { g ->
                    val name = g.destination ?: g.kind.displayNameRO
                    val saved = g.savedAmount.amount / 100
                    val target = g.targetAmount.amount / 100
                    val pct = if (target > 0) (saved * 100 / target) else 0
                    val monthsLeft = if (g.deadline > now) ((g.deadline - now) / (30L * 86_400_000L)).toInt() else 0
                    "- $name: $saved RON / $target RON ($pct%) - ${if (monthsLeft > 0) "$monthsLeft luni ramase" else "scadent"}"
                }
            }

            "analyze_spending" -> {
                val days = ((params["days"] as? Int) ?: 30).coerceIn(7, 90)
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= since && it.direction.name == "outgoing" }
                if (txs.isEmpty()) return@withContext "Nu am tranzactii de cheltuieli in ultimele $days zile."
                val byCategory = txs.groupBy { it.category }
                val totalSpend = txs.sumOf { it.amount.amount } / 100
                val topCategories = byCategory.entries
                    .sortedByDescending { e -> e.value.sumOf { it.amount.amount } }
                    .take(5)
                    .joinToString("\n") { (cat, txList) ->
                        val sum = txList.sumOf { it.amount.amount } / 100
                        "  - ${cat.displayNameRO}: $sum RON (${txList.size} tranzactii)"
                    }
                val pattern = try {
                    val report = PatternDetector().detect(txs, days)
                    if (report.topCategories.isNotEmpty()) {
                        val top = report.topCategories.first()
                        "\nPattern dominant: ${top.category.displayNameRO} = ${(top.fractionOfTotal * 100).toInt()}% din total${if (top.dominantMerchant != null) " (${top.dominantMerchant})" else ""}"
                    } else ""
                } catch (_: Exception) { "" }
                "Analiza cheltuieli ($days zile) - Total: $totalSpend RON\nTop categorii:\n$topCategories$pattern"
            }

            "get_goal_scenarios" -> {
                val goalName = params["goal_name"] as? String ?: return@withContext "Lipsa parametru: goal_name"
                val goals = ServiceLocator.goalRepo.fetchAll()
                val goal = goals.firstOrNull { it.destination?.contains(goalName, ignoreCase = true) == true }
                    ?: return@withContext "Nu am gasit obiectivul '$goalName'."
                val now = System.currentTimeMillis()
                val remaining = (goal.targetAmount.amount - goal.savedAmount.amount) / 100
                val monthsLeft = if (goal.deadline > now) ((goal.deadline - now) / (30L * 86_400_000L)).toInt().coerceAtLeast(1) else 1
                val name = goal.destination ?: goal.kind.displayNameRO
                val monthly = remaining / monthsLeft
                val fast = (remaining / (monthsLeft * 0.67)).toInt()
                "Scenarii pentru '$name' (mai ramane $remaining RON):\n" +
                "- Ritm curent: $monthly RON/luna -> gata in $monthsLeft luni\n" +
                "- Ritm accelerat (+50%): $fast RON/luna -> gata in ${(monthsLeft * 0.67).toInt()} luni\n" +
                "- Ritm minim: ${monthly / 2} RON/luna -> gata in ${monthsLeft * 2} luni"
            }

            "forecast_balance" -> {
                val txs = ServiceLocator.txnRepo.fetchAll()
                val since30 = System.currentTimeMillis() - 30L * 24L * 3600L * 1000L
                val recentTxs = txs.filter { it.date >= since30 }
                val incomeAvg = recentTxs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                val spendAvg = recentTxs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount } / 100
                val monthlyBalance = incomeAvg - spendAvg
                "Proiectie sold (pe baza ultimelor 30 zile - venit $incomeAvg RON, cheltuieli $spendAvg RON):\n" +
                "- 30 zile: +/- $monthlyBalance RON\n" +
                "- 90 zile: +/- ${monthlyBalance * 3} RON\n" +
                "- 180 zile: +/- ${monthlyBalance * 6} RON\n" +
                if (monthlyBalance > 0) "Marja pozitiva - continua sa economisesti consistent." else "Atentie: cheltuielile depasesc veniturile. Revizuieste bugetul."
            }

            "create_goal" -> {
                val name = params["name"] as? String ?: return@withContext "Lipsa parametru: name"
                val targetAmount = when (val a = params["target_amount"]) {
                    is Int -> a
                    is Double -> a.toInt()
                    else -> return@withContext "Lipsa parametru: target_amount"
                }
                val deadlineMonths = (params["deadline_months"] as? Int) ?: 6
                val kindStr = params["kind"] as? String ?: "custom"
                val kind = try { GoalKind.valueOf(kindStr) } catch (_: Exception) { GoalKind.custom }
                val deadline = System.currentTimeMillis() + deadlineMonths * 30L * 86_400_000L
                ServiceLocator.goalRepo.create(
                    destination = name,
                    targetAmount = Money.fromLei(targetAmount),
                    deadline = deadline,
                    kind = kind
                )
                val monthly = targetAmount / deadlineMonths
                "Obiectiv creat: '$name' - $targetAmount RON in $deadlineMonths luni. Trebuie sa economisesti ~$monthly RON/luna."
            }

            "set_budget_limit" -> {
                val categoryStr = params["category"] as? String ?: return@withContext "Lipsa parametru: category"
                val limitAmount = when (val a = params["limit_amount"]) {
                    is Int -> a
                    is Double -> a.toInt()
                    else -> return@withContext "Lipsa parametru: limit_amount"
                }
                val category = try { TransactionCategory.valueOf(categoryStr) } catch (_: Exception) { TransactionCategory.unknown }
                CategoryLimitsStore.set(context, category, limitAmount)
                "Limita setata: ${category.displayNameRO} = $limitAmount RON/luna."
            }

            "clear_category_limit" -> {
                if (!isConfirmed()) return@withContext "Confirma stergerea limitei adaugand confirm=true in params."
                val categoryStr = params["category"] as? String ?: return@withContext "Lipsa parametru: category"
                val category = try { TransactionCategory.valueOf(categoryStr) } catch (_: Exception) { TransactionCategory.unknown }
                CategoryLimitsStore.remove(context, category)
                "Limita pentru ${category.displayNameRO} a fost stearsa."
            }

            "get_category_limits" -> {
                val limits = CategoryLimitsStore.getAll(context)
                if (limits.isEmpty()) return@withContext "Nu ai limite de cheltuieli setate pe categorii."
                limits.entries.joinToString("\n") { (cat, amount) ->
                    "- ${cat.displayNameRO}: $amount RON/luna"
                }
            }

            "schedule_reminder" -> {
                val title = params["title"] as? String ?: return@withContext "Lipsa parametru: title"
                val body = params["body"] as? String ?: ""
                val dayOfMonth = params["day_of_month"] as? Int
                val daysFromNow = params["days_from_now"] as? Int
                val when_ = when {
                    dayOfMonth != null -> "ziua $dayOfMonth a lunii"
                    daysFromNow != null -> "peste $daysFromNow zile"
                    else -> "curand"
                }
                "Reminder programat: '$title' - $when_. ($body)"
            }

            "add_obligation" -> {
                val name = params["name"] as? String ?: return@withContext "Lipsa parametru: name"
                val amount = when (val a = params["amount"]) {
                    is Int -> a
                    is Double -> a.toInt()
                    else -> return@withContext "Lipsa parametru: amount"
                }
                val dayOfMonth = (params["day_of_month"] as? Int) ?: 1
                val kindStr = params["kind"] as? String ?: "other"
                val kind = try { ObligationKind.valueOf(kindStr) } catch (_: Exception) { ObligationKind.other }
                ServiceLocator.obligationRepo.add(
                    name = name,
                    amount = Money.fromLei(amount),
                    dayOfMonth = dayOfMonth,
                    kind = kind,
                    confidence = ObligationConfidence.declared
                )
                "Obligatie adaugata: '$name' $amount RON/luna, scadenta in ziua $dayOfMonth."
            }

            "delete_transaction" -> {
                if (!isConfirmed()) return@withContext "Confirma stergerea adaugand confirm=true in params."
                val merchant = params["merchant"] as? String
                val amount = when (val a = params["amount"]) {
                    is Int -> a
                    is Double -> a.toInt()
                    else -> null
                }
                val txs = ServiceLocator.txnRepo.fetchAll()
                val match = txs.lastOrNull { tx ->
                    (merchant == null || tx.merchant?.contains(merchant, ignoreCase = true) == true) &&
                    (amount == null || Math.abs(tx.amount.amount / 100 - amount) < 2)
                } ?: return@withContext "Nu am gasit tranzactia${if (merchant != null) " de la $merchant" else ""}${if (amount != null) " de $amount RON" else ""}."
                ServiceLocator.txnRepo.delete(match.id)
                "Tranzactie stearsa: ${match.merchant ?: match.category.displayNameRO} ${match.amount.amount / 100} RON."
            }

            "consult_advisor" -> {
                val topic = params["topic"] as? String ?: "general"
                SolomonAdvisor.wisdom(topic)
            }

            "couple_check_in" -> {
                val profile = ServiceLocator.userRepo.fetchProfile()
                val userPersonality = profile?.financialPersonality ?: FinancialPersonality.saver
                val partnerPersonality = profile?.partner?.financialPersonality ?: FinancialPersonality.spender
                SolomonAdvisor.coupleQuestions(userPersonality, partnerPersonality)
            }

            "get_cashflow_summary" -> {
                val days = (params["days"] as? Int) ?: 30
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= since }
                val income = txs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                val spend = txs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount } / 100
                val obligs = ServiceLocator.obligationRepo.fetchAll()
                val obligTotal = obligs.sumOf { it.amount.amount } / 100
                val topCat = txs.filter { it.direction.name == "outgoing" }
                    .groupBy { it.category }
                    .maxByOrNull { e -> e.value.sumOf { it.amount.amount } }
                val balance = income - spend
                "Cashflow $days zile:\n" +
                "- Venituri: $income RON\n" +
                "- Cheltuieli: $spend RON\n" +
                "- Sold: ${if (balance >= 0) "+" else ""}$balance RON\n" +
                "- Obligatii lunare fixe: $obligTotal RON (${obligs.size})\n" +
                (if (topCat != null) "- Categorie dominanta: ${topCat.key.displayNameRO} (${topCat.value.sumOf { it.amount.amount } / 100} RON)" else "")
            }

            "get_financial_snapshot" -> {
                val days = (params["days"] as? Int) ?: 30
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll()
                val recentTxs = txs.filter { it.date >= since }
                val income = recentTxs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                val spend = recentTxs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount } / 100
                val obligs = ServiceLocator.obligationRepo.fetchAll()
                val obligTotal = obligs.sumOf { it.amount.amount } / 100
                val goals = ServiceLocator.goalRepo.fetchAll()
                val subs = ServiceLocator.subRepo.fetchAll()
                val subsTotal = subs.sumOf { it.amountMonthly.amount } / 100
                val personalMargin = income - obligTotal - subsTotal
                val vulnerability = SolomonCoachMemory.vulnerability(context)
                "=== Snapshot Financiar ($days zile) ===\n" +
                "Venituri: $income RON | Cheltuieli: $spend RON\n" +
                "Obligatii fixe: $obligTotal RON | Abonamente: $subsTotal RON\n" +
                "Marja personala: $personalMargin RON\n" +
                "Obiective active: ${goals.size}\n" +
                "Tranzactii totale in baza: ${txs.size}\n" +
                "Vulnerabilitate memorata: ${vulnerability?.title ?: "necunoscuta"}"
            }

            "get_data_coverage" -> {
                val days = (params["days"] as? Int) ?: 30
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll()
                val recentTxs = txs.filter { it.date >= since }
                val manualCount = recentTxs.count { it.source.name == "manual_entry" }
                val smsCount = recentTxs.count { it.source.name == "sms_parsed" }
                val bankCount = recentTxs.count { it.source.name == "bank_connection" }
                val shareCount = recentTxs.count { it.source.name == "share_intent_parsed" }
                val connections = try { BankConnectionService.allConnections } catch (_: Exception) { emptyList() }
                "Acoperire date ($days zile - ${recentTxs.size} tranzactii):\n" +
                "- Manuale: $manualCount\n" +
                "- SMS parsate: $smsCount\n" +
                "- Banca conectata: $bankCount\n" +
                "- Share intent: $shareCount\n" +
                "- Conexiuni bancare active: ${connections.size}\n" +
                if (bankCount == 0 && connections.isEmpty()) "\nAtentie: Solomon nu are acces direct la contul bancii. Adauga tranzactii manual sau conecteaza o banca pentru analiza completa." else ""
            }

            "analyze_budget" -> {
                val days = (params["days"] as? Int) ?: 30
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= since && it.direction.name == "outgoing" }
                val limits = CategoryLimitsStore.getAll(context)
                val totalSpend = txs.sumOf { it.amount.amount } / 100
                val byCategory = txs.groupBy { it.category }
                val exceeded = limits.entries.filter { (cat, limit) ->
                    val spent = byCategory[cat]?.sumOf { it.amount.amount }?.div(100) ?: 0
                    spent > limit
                }
                val dominated = byCategory.entries.maxByOrNull { e -> e.value.sumOf { it.amount.amount } }
                "Analiza buget ($days zile) - Total cheltuit: $totalSpend RON\n" +
                (if (dominated != null) "Categorie dominanta: ${dominated.key.displayNameRO} (${dominated.value.sumOf { it.amount.amount } / 100} RON)\n" else "") +
                (if (exceeded.isNotEmpty()) "Limite depasite: ${exceeded.joinToString { (cat, limit) -> "${cat.displayNameRO} (limita $limit RON)" }}\n" else "Toate limitele respectate.\n") +
                "Presiune buget: ${if (totalSpend > 3000) "ridicata" else if (totalSpend > 1500) "medie" else "scazuta"}"
            }

            "detect_risk" -> {
                val days = ((params["days"] as? Int) ?: 30).coerceIn(7, 90)
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= since }
                val obligs = ServiceLocator.obligationRepo.fetchAll()
                val income = txs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                val spend = txs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount } / 100
                val obligTotal = obligs.sumOf { it.amount.amount } / 100
                val goals = ServiceLocator.goalRepo.fetchAll()

                // Detect dominant vulnerability
                val vulnerability = when {
                    obligTotal > 0 && income > 0 && obligTotal.toDouble() / income > 0.4 ->
                        SolomonCoachVulnerability.HEAVY_OBLIGATIONS
                    income < 100 ->
                        SolomonCoachVulnerability.IRREGULAR_INCOME
                    goals.isNotEmpty() && goals.all { it.savedAmount.amount == 0 } ->
                        SolomonCoachVulnerability.GOALS_WITHOUT_CONTRIBUTION
                    income > 0 && spend.toDouble() / income > 0.9 ->
                        SolomonCoachVulnerability.CASHFLOW_PRESSURE
                    else -> {
                        try {
                            val report = PatternDetector().detect(txs, days)
                            if (report.topCategories.isNotEmpty() && report.topCategories.first().fractionOfTotal > 0.3)
                                SolomonCoachVulnerability.SMALL_RECURRING
                            else SolomonCoachVulnerability.CASHFLOW_PRESSURE
                        } catch (_: Exception) {
                            SolomonCoachVulnerability.CASHFLOW_PRESSURE
                        }
                    }
                }
                SolomonCoachMemory.save(context, vulnerability)
                "Vulnerabilitate detectata: ${vulnerability.title}\n" +
                "Lectie: ${vulnerability.lesson}\n" +
                "Actiune recomandata: ${vulnerability.action}"
            }

            "suggest_action" -> {
                val vulnerability = SolomonCoachMemory.vulnerability(context)
                    ?: run {
                        // run detect_risk first if no memory
                        val days = (params["days"] as? Int) ?: 30
                        val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                        val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= since }
                        val obligs = ServiceLocator.obligationRepo.fetchAll()
                        val income = txs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                        val obligTotal = obligs.sumOf { it.amount.amount } / 100
                        val v = if (obligTotal > 0 && income > 0 && obligTotal.toDouble() / income > 0.4)
                            SolomonCoachVulnerability.HEAVY_OBLIGATIONS
                        else SolomonCoachVulnerability.CASHFLOW_PRESSURE
                        SolomonCoachMemory.save(context, v)
                        v
                    }
                "Actiunea zilei (bazata pe ${vulnerability.title}):\n${vulnerability.action}\n\nTip: ${vulnerability.contextualTip}"
            }

            "education_tip" -> {
                val vulnerabilityStr = params["vulnerability"] as? String
                val vulnerability = if (vulnerabilityStr != null) {
                    SolomonCoachVulnerability.from(vulnerabilityStr) ?: SolomonCoachMemory.vulnerability(context)
                } else {
                    SolomonCoachMemory.vulnerability(context)
                }
                if (vulnerability != null) {
                    "Lectie financiara (${vulnerability.title}):\n${vulnerability.lesson}\n\nTip practic: ${vulnerability.contextualTip}"
                } else {
                    SolomonAdvisor.wisdom("general")
                }
            }

            "monthly_report" -> {
                val since = System.currentTimeMillis() - 30L * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll().filter { it.date >= since }
                val income = txs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                val spend = txs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount } / 100
                val obligs = ServiceLocator.obligationRepo.fetchAll()
                val obligTotal = obligs.sumOf { it.amount.amount } / 100
                val goals = ServiceLocator.goalRepo.fetchAll()
                val activeGoals = goals.filter { it.savedAmount.amount > 0 }
                val vulnerability = SolomonCoachMemory.vulnerability(context)
                "=== Raport lunar Solomon ===\n" +
                "1. Venituri: $income RON\n" +
                "2. Cheltuieli: $spend RON (${if (income > 0) "${spend * 100 / income}%" else "?"} din venit)\n" +
                "3. Obligatii fixe: $obligTotal RON/luna (${obligs.size} obligatii)\n" +
                "4. Obiective: ${activeGoals.size}/${goals.size} cu economii active\n" +
                "5. Risc dominant: ${vulnerability?.title ?: "nedetectat - ruleaza detect_risk"}\n" +
                if (income > 0) "\nMarja personala: ${income - obligTotal} RON dupa obligatii fixe." else ""
            }

            "run_coach_flow" -> {
                val flow = params["flow"] as? String ?: "diagnostic"
                val days = (params["days"] as? Int) ?: 30
                val since = System.currentTimeMillis() - days * 24L * 3600L * 1000L
                val txs = ServiceLocator.txnRepo.fetchAll()
                val recentTxs = txs.filter { it.date >= since }
                val income = recentTxs.filter { it.direction.name == "incoming" }.sumOf { it.amount.amount } / 100
                val spend = recentTxs.filter { it.direction.name == "outgoing" }.sumOf { it.amount.amount } / 100
                val obligs = ServiceLocator.obligationRepo.fetchAll()
                val obligTotal = obligs.sumOf { it.amount.amount } / 100
                val goals = ServiceLocator.goalRepo.fetchAll()
                val subs = ServiceLocator.subRepo.fetchAll()

                // Detect and save vulnerability
                val vulnerability = when {
                    obligTotal > 0 && income > 0 && obligTotal.toDouble() / income > 0.4 -> SolomonCoachVulnerability.HEAVY_OBLIGATIONS
                    income < 100 -> SolomonCoachVulnerability.IRREGULAR_INCOME
                    goals.isNotEmpty() && goals.all { it.savedAmount.amount == 0 } -> SolomonCoachVulnerability.GOALS_WITHOUT_CONTRIBUTION
                    income > 0 && spend.toDouble() / income > 0.9 -> SolomonCoachVulnerability.CASHFLOW_PRESSURE
                    else -> SolomonCoachVulnerability.SMALL_RECURRING
                }
                SolomonCoachMemory.save(context, vulnerability)

                val annualSpendByCategory = recentTxs.filter { it.direction.name == "outgoing" }
                    .groupBy { it.category }
                    .mapValues { e -> e.value.sumOf { it.amount.amount } / 100 * 12 / (days / 30).coerceAtLeast(1) }
                val trueCostLine = annualSpendByCategory.entries
                    .maxByOrNull { it.value }
                    ?.let { (cat, annual) -> TrueCostComparator.compare(annual, cat) }

                when (flow) {
                    "diagnostic" -> {
                        "=== Diagnostic Solomon ($days zile) ===\n\n" +
                        "CE VAD:\n" +
                        "- Venituri: $income RON | Cheltuieli: $spend RON\n" +
                        "- Obligatii: $obligTotal RON/luna (${obligs.size}) | Abonamente: ${subs.size}\n" +
                        "- Obiective: ${goals.size} (${goals.count { it.savedAmount.amount > 0 }} cu economii)\n" +
                        "- Tranzactii: ${txs.size} total, ${recentTxs.size} recente\n\n" +
                        "RISCUL DOMINANT: ${vulnerability.title}\n" +
                        "${vulnerability.lesson}\n\n" +
                        "AZI FACI ASTA:\n${vulnerability.action}\n" +
                        (if (trueCostLine != null) "\nPerspectiva: $trueCostLine" else "")
                    }
                    "budget_review" -> {
                        val limits = CategoryLimitsStore.getAll(context)
                        val byCategory = recentTxs.filter { it.direction.name == "outgoing" }.groupBy { it.category }
                        val topCategories = byCategory.entries
                            .sortedByDescending { e -> e.value.sumOf { it.amount.amount } }
                            .take(4)
                            .joinToString("\n") { (cat, txList) ->
                                val sum = txList.sumOf { it.amount.amount } / 100
                                val limit = limits[cat]
                                "- ${cat.displayNameRO}: $sum RON${if (limit != null) " (limita $limit RON${if (sum > limit) " DEPASITA" else ""})" else ""}"
                            }
                        "=== Analiza Buget ($days zile) ===\n\n" +
                        "CE VAD:\nTotal cheltuit: $spend RON\nTop categorii:\n$topCategories\n\n" +
                        "RISCUL DOMINANT: ${vulnerability.title}\n\n" +
                        "AZI FACI ASTA:\n${vulnerability.action}"
                    }
                    "monthly_report" -> {
                        "=== Raport Lunar Solomon ===\n\n" +
                        "1. Venituri: $income RON\n" +
                        "2. Cheltuieli: $spend RON\n" +
                        "3. Obligatii fixe: $obligTotal RON/luna\n" +
                        "4. Obiective: ${goals.size} (${goals.count { it.savedAmount.amount > 0 }} active)\n" +
                        "5. Risc: ${vulnerability.title}\n\n" +
                        "Actiune recomandata: ${vulnerability.action}"
                    }
                    "daily_action" -> {
                        "Actiunea ta de azi:\n${vulnerability.action}\n\nDe ce conteaza:\n${vulnerability.contextualTip}"
                    }
                    else -> "Flow necunoscut: $flow. Disponibile: diagnostic, budget_review, monthly_report, daily_action."
                }
            }

            "query_bank_connections" -> {
                val connections = try { BankConnectionService.allConnections } catch (_: Exception) { emptyList() }
                if (connections.isEmpty()) return@withContext "Nu ai conexiuni bancare active. Conecteaza o banca din sectiunea Conturi."
                connections.joinToString("\n") { conn ->
                    "- ${conn.aspspName}: ${if (conn.isExpired) "expirata" else "activa"} | ${conn.accounts.size} conturi"
                }
            }

            "sync_bank_connections" -> {
                if (!isConfirmed()) return@withContext "Confirma sincronizarea adaugand confirm=true in params."
                return@withContext try {
                    val synced = BankConnectionService.syncAll()
                    "Sincronizare bancara finalizata. $synced tranzactii noi importate."
                } catch (e: Exception) {
                    "Eroare la sincronizare: ${e.message ?: "necunoscuta"}"
                }
            }

            "disconnect_bank" -> {
                if (!isConfirmed()) return@withContext "Confirma deconectarea adaugand confirm=true in params."
                val bankName = params["bank_name"] as? String
                val connections = try { BankConnectionService.allConnections } catch (_: Exception) { emptyList() }
                val conn = if (bankName != null)
                    connections.firstOrNull { it.aspspName.contains(bankName, ignoreCase = true) }
                else connections.firstOrNull()
                if (conn == null) return@withContext "Nu am gasit conexiunea bancara${if (bankName != null) " pentru $bankName" else ""}."
                try {
                    BankConnectionService.disconnect(conn)
                    "Banca '${conn.aspspName}' deconectata."
                } catch (e: Exception) {
                    "Eroare la deconectare: ${e.message ?: "necunoscuta"}"
                }
            }

            else -> "Tool necunoscut: ${call.name}. Disponibile: ${ro.solomon.llm.LLMAgentTool.all.joinToString { it.name }}"
        }
    }
}
