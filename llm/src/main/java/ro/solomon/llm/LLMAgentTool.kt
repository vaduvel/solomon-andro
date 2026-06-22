package ro.solomon.llm

data class LLMAgentTool(
    val name: String,
    val description: String,
    val requiresConfirmation: Boolean = false,
    val example: String
) {
    companion object {
        val queryTransactions = LLMAgentTool("query_transactions", "Tranzac\u021Bii din N zile, op\u021Bional filtrate pe categorie", example = "<TOOL>{\"name\":\"query_transactions\",\"params\":{\"days\":7,\"category\":\"M\u00E2ncare\"}}</TOOL>")
        val addTransaction = LLMAgentTool("add_transaction", "Adaug\u0103 o cheltuial\u0103 manual\u0103 (amount, merchant, category)", example = "<TOOL>{\"name\":\"add_transaction\",\"params\":{\"amount\":50,\"merchant\":\"Mega Image\",\"category\":\"Cump\u0103r\u0103turi\"}}</TOOL>")
        val updateGoal = LLMAgentTool("update_goal", "Adaug\u0103 economii la un obiectiv (goal_name, add_amount)", example = "<TOOL>{\"name\":\"update_goal\",\"params\":{\"goal_name\":\"Croa\u021Bia\",\"add_amount\":200}}</TOOL>")
        val queryGoals = LLMAgentTool("query_goals", "Listeaz\u0103 obiectivele active cu progresul curent", example = "<TOOL>{\"name\":\"query_goals\",\"params\":{}}</TOOL>")
        val analyzeSpending = LLMAgentTool("analyze_spending", "Analiz\u0103 detaliat\u0103 cheltuieli: pattern-uri, spikes, comparativ luni, top comercian\u021Bi", example = "<TOOL>{\"name\":\"analyze_spending\",\"params\":{\"days\":30}}</TOOL>")
        val getGoalScenarios = LLMAgentTool("get_goal_scenarios", "Scenarii pentru obiectiv: ritm curent / necesar / +50% boost cu timeline", example = "<TOOL>{\"name\":\"get_goal_scenarios\",\"params\":{\"goal_name\":\"Croa\u021Bia\"}}</TOOL>")
        val forecastBalance = LLMAgentTool("forecast_balance", "Proiec\u021Bie sold 30/90/180 zile \u0219i c\u00E2nd se atinge obiectivul principal", example = "<TOOL>{\"name\":\"forecast_balance\",\"params\":{}}</TOOL>")
        val createGoal = LLMAgentTool("create_goal", "Creeaz\u0103 un obiectiv financiar nou (name, target_amount, deadline_months, kind op\u021Bional)", example = "<TOOL>{\"name\":\"create_goal\",\"params\":{\"name\":\"Laptop\",\"target_amount\":3000,\"deadline_months\":6,\"kind\":\"custom\"}}</TOOL>")
        val setBudgetLimit = LLMAgentTool("set_budget_limit", "Seteaz\u0103 sau actualizeaz\u0103 limita de cheltuieli pe o categorie (category, limit_amount)", example = "<TOOL>{\"name\":\"set_budget_limit\",\"params\":{\"category\":\"M\u00E2ncare\",\"limit_amount\":800}}</TOOL>")
        val clearCategoryLimit = LLMAgentTool("clear_category_limit", "\u0218terge limita de cheltuieli pentru o categorie", requiresConfirmation = true, example = "<TOOL>{\"name\":\"clear_category_limit\",\"params\":{\"category\":\"M\u00E2ncare\",\"confirm\":true}}</TOOL>")
        val getCategoryLimits = LLMAgentTool("get_category_limits", "Afi\u0219eaz\u0103 limitele de cheltuieli setate pe categorii", example = "<TOOL>{\"name\":\"get_category_limits\",\"params\":{}}</TOOL>")
        val scheduleReminder = LLMAgentTool("schedule_reminder", "Programeaz\u0103 o notificare (title, body, day_of_month SAU days_from_now)", example = "<TOOL>{\"name\":\"schedule_reminder\",\"params\":{\"title\":\"Pl\u0103te\u0219te chiria\",\"body\":\"Chiria e scadent\u0103 azi.\",\"day_of_month\":15}}</TOOL>")
        val getCashflowSummary = LLMAgentTool("get_cashflow_summary", "Rezumat cashflow pe N zile (venituri/cheltuieli/top categorii/obliga\u021Bii)", example = "<TOOL>{\"name\":\"get_cashflow_summary\",\"params\":{\"days\":30}}</TOOL>")
        val getFinancialSnapshot = LLMAgentTool("get_financial_snapshot", "Snapshot premium pentru coaching: venit, cheltuieli, obliga\u021Bii, obiective, marj\u0103 personal\u0103 \u0219i memoria ultimei vulnerabilit\u0103\u021Bi", example = "<TOOL>{\"name\":\"get_financial_snapshot\",\"params\":{\"days\":30}}</TOOL>")
        val getDataCoverage = LLMAgentTool("get_data_coverage", "Arat\u0103 ce surse de date vede Solomon \u0219i c\u00E2t de complet\u0103 este analiza", example = "<TOOL>{\"name\":\"get_data_coverage\",\"params\":{\"days\":30}}</TOOL>")
        val analyzeBudget = LLMAgentTool("analyze_budget", "Analizeaz\u0103 bugetul lunar: presiune, categorie dominant\u0103, limite dep\u0103\u0219ite \u0219i spa\u021Biu de respira\u021Bie financiar\u0103", example = "<TOOL>{\"name\":\"analyze_budget\",\"params\":{\"days\":30}}</TOOL>")
        val detectRisk = LLMAgentTool("detect_risk", "Detecteaz\u0103 riscul dominant \u0219i salveaz\u0103 \u00EEn memoria de coaching vulnerabilitatea principal\u0103", example = "<TOOL>{\"name\":\"detect_risk\",\"params\":{\"days\":30}}</TOOL>")
        val suggestAction = LLMAgentTool("suggest_action", "Propune o singur\u0103 ac\u021Biune mic\u0103 pentru azi, bazat\u0103 pe vulnerabilitatea financiar\u0103 dominant\u0103", example = "<TOOL>{\"name\":\"suggest_action\",\"params\":{\"days\":30}}</TOOL>")
        val educationTip = LLMAgentTool("education_tip", "Genereaz\u0103 o lec\u021Bie contextual\u0103 scurt\u0103, legat\u0103 de vulnerabilitatea sau categoria financiar\u0103 curent\u0103", example = "<TOOL>{\"name\":\"education_tip\",\"params\":{\"vulnerability\":\"small_recurring\"}}</TOOL>")
        val monthlyReport = LLMAgentTool("monthly_report", "Raport clar: luna \u00EEn 5 puncte \u2014 venit, cheltuieli, obliga\u021Bii, obiective, risc", example = "<TOOL>{\"name\":\"monthly_report\",\"params\":{}}</TOOL>")
        val runCoachFlow = LLMAgentTool("run_coach_flow", "Ruleaz\u0103 un flow complet de coaching financiar: diagnostic, budget_review, monthly_report sau daily_action", example = "<TOOL>{\"name\":\"run_coach_flow\",\"params\":{\"flow\":\"diagnostic\",\"days\":30}}</TOOL>")
        val addObligation = LLMAgentTool("add_obligation", "Adaug\u0103 o obliga\u021Bie recurent\u0103 nou\u0103 (name, amount, day_of_month, kind op\u021Bional)", example = "<TOOL>{\"name\":\"add_obligation\",\"params\":{\"name\":\"Netflix\",\"amount\":50,\"day_of_month\":10,\"kind\":\"subscription\"}}</TOOL>")
        val deleteTransaction = LLMAgentTool("delete_transaction", "\u0218terge o tranzac\u021Bie existent\u0103. Necesit\u0103 confirmare explicit\u0103.", requiresConfirmation = true, example = "<TOOL>{\"name\":\"delete_transaction\",\"params\":{\"merchant\":\"Mega Image\",\"amount\":150,\"confirm\":true}}</TOOL>")
        val consultAdvisor = LLMAgentTool("consult_advisor", "Cere \u00EEn\u021Belepciune de la consilierul cu principii financiare solide (topic: economisire/datorii/cuplu/investitii/mindset/cumparare-mare/cariera/risc/cultura-ro)", example = "<TOOL>{\"name\":\"consult_advisor\",\"params\":{\"topic\":\"cuplu\"}}</TOOL>")
        val coupleCheckIn = LLMAgentTool("couple_check_in", "Genereaz\u0103 agenda pentru \u00EEnt\u00E2lnirea financiar\u0103 lunar\u0103 a cuplului (doar dac\u0103 Solomon Doi e activ)", example = "<TOOL>{\"name\":\"couple_check_in\",\"params\":{}}</TOOL>")
        val queryBankConnections = LLMAgentTool("query_bank_connections", "Listeaz\u0103 conexiunile bancare conectate, statusul \u0219i data ultimului sync", example = "<TOOL>{\"name\":\"query_bank_connections\",\"params\":{}}</TOOL>")
        val syncBankConnections = LLMAgentTool("sync_bank_connections", "Porne\u0219te sincronizarea bancar\u0103. Po\u021Bi s\u0103 o faci pentru o banc\u0103 anume: bank_id sau bank_name.", requiresConfirmation = true, example = "<TOOL>{\"name\":\"sync_bank_connections\",\"params\":{\"bank_name\":\"Revolut\",\"confirm\":true}}</TOOL>")
        val disconnectBank = LLMAgentTool("disconnect_bank", "Deconecteaz\u0103 o banc\u0103 din conexiuni prin bank_id sau bank_name (se recomand\u0103 confirmare).", requiresConfirmation = true, example = "<TOOL>{\"name\":\"disconnect_bank\",\"params\":{\"bank_name\":\"Banca Transilvania\",\"confirm\":true}}</TOOL>")

        val all: List<LLMAgentTool> = listOf(
            queryTransactions, addTransaction, updateGoal, queryGoals,
            analyzeSpending, getGoalScenarios, forecastBalance,
            createGoal, setBudgetLimit, clearCategoryLimit, getCategoryLimits, scheduleReminder,
            addObligation, deleteTransaction, consultAdvisor, coupleCheckIn,
            getCashflowSummary, getFinancialSnapshot, getDataCoverage, analyzeBudget, detectRisk,
            suggestAction, educationTip, monthlyReport, runCoachFlow,
            queryBankConnections, syncBankConnections, disconnectBank
        )

        private val criticalTools: Set<String> = all.filter { it.requiresConfirmation }.map { it.name }.toSet()

        fun requiresConfirmation(name: String): Boolean = name in criticalTools
        fun isKnownTool(name: String): Boolean = all.any { it.name == name }

        fun instructionCatalog(): String {
            val toolRows = all.joinToString("\n") { "- ${it.name}: ${it.description}" }
            val examples = all.joinToString("\n") { it.example }
            val confirmations = if (criticalTools.isEmpty()) "" else "\n\nIMPORTANT: Pentru instrumentele critice folose\u0219te confirm=true \u00EEnainte de execu\u021Bie: ${criticalTools.sorted().joinToString(", ")}."
            return """
            TOOLS disponibile:
            $toolRows
            Exemplu de apel:
            $examples
            $confirmations
            """.trimIndent()
        }

        fun systemInstructions(): String = """
            TOOLS \u2014 c\u00E2nd ai nevoie s\u0103 cite\u0219ti date sau s\u0103 faci o ac\u021Biune, r\u0103spunde EXACT cu un singur r\u00E2nd (nimic altceva):
            ${instructionCatalog()}

            FORMAT OBLIGATORIU TOOL CALL:
            <TOOL>{"name":"nume_tool","params":{}}</TOOL>

            REGULI FORMAT:
            - Nu folosi markdown, code fences, backticks sau explica\u021Bii l\u00E2ng\u0103 tool call.
            - Nu scrie "first", "apoi", "voi folosi" sau text natural c\u00E2nd alegi un tool.
            - Tool call-ul trebuie s\u0103 fie JSON complet valid \u0219i s\u0103 se \u00EEnchid\u0103 cu </TOOL>.
            - Dac\u0103 r\u0103spunsul are nevoie de mai multe tool-uri, cheam\u0103 doar primul tool necesar; aplica\u021Bia \u00EE\u021Bi va da rezultatul \u0219i apoi po\u021Bi chema urm\u0103torul.

            IMPORTANT: pentru \u00EEntreb\u0103ri despre principii, sfaturi, framework-uri, dileme financiare \
            (nu cifre pure din contul utilizatorului), CHEAM\u0103 "consult_advisor" cu un topic relevant \
            \u00EEnainte s\u0103 r\u0103spunzi. Topicuri valide: economisire, datorii, cuplu, investitii, mindset, \
            cumparare-mare, cariera, risc, cultura-ro.

            ORCHESTRARE COACHING:
            - Pentru cereri mari sau ambigue, prefer\u0103 run_coach_flow \u00EEn loc s\u0103 chemi manual mai multe tool-uri.
            - Pentru "cum stau?", "diagnostic", "ce risc am?", cheam\u0103 run_coach_flow cu flow="diagnostic".
            - Pentru "buget", "unde se duc banii?", "categorie sc\u0103pat\u0103", cheam\u0103 run_coach_flow cu flow="budget_review".
            - Pentru "ce fac azi?", "ac\u021Biune mic\u0103", cheam\u0103 run_coach_flow cu flow="daily_action".
            - Pentru "raport lunar", "luna mea", "rezumat", cheam\u0103 run_coach_flow cu flow="monthly_report".
            - Pentru "ce date vezi?", "ai acces la banca?", "c\u00E2t de complet\u0103 e analiza?", cheam\u0103 get_data_coverage.
            - Pentru \u00EEntreb\u0103ri foarte punctuale po\u021Bi chema direct get_financial_snapshot, analyze_budget, detect_risk, suggest_action, education_tip sau monthly_report.
            - Pentru recomandare practic\u0103 de azi, cheam\u0103 suggest_action \u0219i r\u0103spunde cu un singur pas concret.

            DUP\u0102 TOOL RESULT:
            - Dac\u0103 rezultatul con\u021Bine "Acoperire date" sau "Vizibilitate", men\u021Bioneaz\u0103 limita \u00EEntr-o singur\u0103 propozi\u021Bie. Nu pretinde c\u0103 vezi banca complet\u0103 c\u00E2nd vezi doar manual.
            - Pentru run_coach_flow, r\u0103spunde \u00EEn 3 blocuri scurte: "Ce v\u0103d", "Riscul", "Azi faci asta".
            - \u00EEnchide cu o micro-lec\u021Bie contextual\u0103, nu cu sfaturi generale.
        """.trimIndent()
    }
}

data class AgentToolCall(
    val name: String,
    val params: Map<String, Any?>
) {
    val statusLabel: String
        get() = when (name) {
            "query_transactions" -> "Verific tranzac\u021Biile din ${params["days"] ?: 30} zile\u2026"
            "add_transaction" -> "Adaug cheltuiala la ${params["merchant"] ?: "necunoscut"}\u2026"
            "update_goal" -> "Actualizez ${params["goal_name"] ?: "obiectiv"}\u2026"
            "query_goals" -> "Verific obiectivele tale\u2026"
            "analyze_spending" -> "Analizez cheltuielile din ${params["days"] ?: 30} zile\u2026"
            "get_goal_scenarios" -> "Calculez scenarii pentru ${params["goal_name"] ?: "obiectiv"}\u2026"
            "forecast_balance" -> "Proiectez finan\u021Bele pe 6 luni\u2026"
            "create_goal" -> "Creez obiectivul ${params["name"] ?: "obiectiv"}\u2026"
            "set_budget_limit" -> "Setez limita pentru ${params["category"] ?: "categorie"}\u2026"
            "clear_category_limit" -> "\u0218terg limita de buget pentru ${params["category"] ?: "categorie"}\u2026"
            "get_category_limits" -> "Verific limitele de cheltuieli\u2026"
            "get_cashflow_summary" -> "Generez rezumatul cashflow\u2026"
            "get_financial_snapshot" -> "Construiesc snapshot-ul financiar Solomon\u2026"
            "get_data_coverage" -> "Verific ce date vede Solomon\u2026"
            "analyze_budget" -> "Analizez bugetul \u0219i presiunea lunar\u0103\u2026"
            "detect_risk" -> "Detectez riscul dominant al lunii\u2026"
            "suggest_action" -> "Aleg ac\u021Biunea mic\u0103 de azi\u2026"
            "education_tip" -> "Preg\u0103tesc lec\u021Bia financiar\u0103 potrivit\u0103\u2026"
            "monthly_report" -> "Compun raportul lunii \u00EEn 5 puncte\u2026"
            "run_coach_flow" -> "Rulez flow-ul Solomon Coach: ${params["flow"] ?: "diagnostic"}\u2026"
            "query_bank_connections" -> "Verific conexiunile bancare\u2026"
            "sync_bank_connections" -> "Porne\u0219te sincronizarea bancar\u0103\u2026"
            "disconnect_bank" -> "Deconectez ${params["bank_id"] ?: params["bank_name"] ?: "banca"}\u2026"
            "schedule_reminder" -> "Programez reminder-ul\u2026"
            "add_obligation" -> "Adaug obliga\u021Bia ${params["name"] ?: "obliga\u021Bie"}\u2026"
            "delete_transaction" -> "\u0218terg tranzac\u021Bia\u2026"
            "consult_advisor" -> "Consult consilierul cu principii financiare solide\u2026"
            "couple_check_in" -> "Preg\u0103tesc agenda \u00EEnt\u00E2lnirii financiare a cuplului\u2026"
            else -> "Solomon proceseaz\u0103\u2026"
        }
}

object AgentToolParser {
    private val toolBlockRegex = Regex("(?is)<TOOL>\\s*(.*?)\\s*</TOOL>")
    private val jsonToolRegex = Regex("(?is)\\{\\s*\"name\"\\s*:\\s*\"[^\"]+\"[^{}]*\\}")
    private val codeFenceRegex = Regex("(?is)```[a-zA-Z]*\\s*([\\s\\S]*?)```")

    fun parse(text: String): AgentToolCall? {
        firstToolPayload(text)?.let { payload ->
            parseToolPayload(payload)?.let { return it }
        }
        val normalized = removeCodeFences(text)
        if (normalized != null) {
            firstToolPayload(normalized)?.let { payload ->
                parseToolPayload(payload)?.let { return it }
            }
            parseLooseJSONPayload(normalized)?.let { payload ->
                parseToolPayload(payload)?.let { return it }
            }
        }
        return null
    }

    fun stripToolTags(text: String): String {
        var result = text
        toolBlockRegex.findAll(result).toList().reversed().forEach { match ->
            result = result.removeRange(match.range)
        }
        return result.trim()
    }

    private fun firstToolPayload(text: String): String? {
        val match = toolBlockRegex.find(text) ?: return null
        val payload = match.groupValues.getOrNull(1)?.trim()
        return if (payload.isNullOrEmpty()) null else payload
    }

    private fun parseToolPayload(payload: String): AgentToolCall? {
        val json = try {
            org.json.JSONObject(payload)
        } catch (_: Exception) {
            return null
        }
        val name = json.optString("name").trim()
        if (name.isEmpty()) return null
        val paramsJson = json.optJSONObject("params")
        val params = mutableMapOf<String, Any?>()
        if (paramsJson != null) {
            for (key in paramsJson.keys()) {
                params[key] = paramsJson.get(key)
            }
        }
        return AgentToolCall(name = name, params = params)
    }

    private fun parseLooseJSONPayload(text: String): String? {
        val cleaned = removeCodeFences(text) ?: text
        val trimmed = cleaned.trim()
        jsonToolRegex.find(trimmed)?.let { match ->
            val candidate = match.value.trim()
            if (candidate.isNotEmpty()) return candidate
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        return null
    }

    private fun removeCodeFences(text: String): String? {
        val cleaned = codeFenceRegex.replace(text) { it.groupValues.getOrNull(1) ?: "" }.trim()
        return if (cleaned.isEmpty()) null else cleaned
    }
}
