package ro.solomon.llm

data class LLMAgentTool(
    val name: String,
    val description: String,
    val requiresConfirmation: Boolean = false,
    val example: String
) {
    companion object {
        val queryTransactions = LLMAgentTool(
            name = "query_transactions",
            description = "Tranzacții din N zile, opțional filtrate pe categorie",
            example = "<TOOL>{\"name\":\"query_transactions\",\"params\":{\"days\":7,\"category\":\"food_grocery\"}}</TOOL>"
        )
        val addTransaction = LLMAgentTool(
            name = "add_transaction",
            description = "Adaugă o cheltuială manuală (amount, merchant, category)",
            example = "<TOOL>{\"name\":\"add_transaction\",\"params\":{\"amount\":50,\"merchant\":\"Mega Image\",\"category\":\"food_grocery\"}}</TOOL>"
        )
        val updateGoal = LLMAgentTool(
            name = "update_goal",
            description = "Adaugă economii la un obiectiv (goal_name, add_amount)",
            example = "<TOOL>{\"name\":\"update_goal\",\"params\":{\"goal_name\":\"Croatia\",\"add_amount\":200}}</TOOL>"
        )
        val queryGoals = LLMAgentTool(
            name = "query_goals",
            description = "Listează obiectivele active cu progresul curent",
            example = "<TOOL>{\"name\":\"query_goals\",\"params\":{}}</TOOL>"
        )
        val analyzeSpending = LLMAgentTool(
            name = "analyze_spending",
            description = "Analiză detaliată cheltuieli: pattern-uri, spikes, comparativ luni, top comercianți",
            example = "<TOOL>{\"name\":\"analyze_spending\",\"params\":{\"days\":30}}</TOOL>"
        )
        val getGoalScenarios = LLMAgentTool(
            name = "get_goal_scenarios",
            description = "Scenarii pentru obiectiv: ritm curent / necesar / +50% boost cu timeline",
            example = "<TOOL>{\"name\":\"get_goal_scenarios\",\"params\":{\"goal_name\":\"Croatia\"}}</TOOL>"
        )
        val forecastBalance = LLMAgentTool(
            name = "forecast_balance",
            description = "Proiecție sold 30/90/180 zile şi când se atinge obiectivul principal",
            example = "<TOOL>{\"name\":\"forecast_balance\",\"params\":{}}</TOOL>"
        )
        val createGoal = LLMAgentTool(
            name = "create_goal",
            description = "Creează un obiectiv financiar nou (name, target_amount, deadline_months, kind opțional)",
            example = "<TOOL>{\"name\":\"create_goal\",\"params\":{\"name\":\"Laptop\",\"target_amount\":3000,\"deadline_months\":6,\"kind\":\"custom\"}}</TOOL>"
        )
        val setBudgetLimit = LLMAgentTool(
            name = "set_budget_limit",
            description = "Setează sau actualizează limita de cheltuieli pe o categorie (category, limit_amount)",
            example = "<TOOL>{\"name\":\"set_budget_limit\",\"params\":{\"category\":\"food_dining\",\"limit_amount\":800}}</TOOL>"
        )
        val clearCategoryLimit = LLMAgentTool(
            name = "clear_category_limit",
            description = "Ştege limita de cheltuieli pentru o categorie",
            requiresConfirmation = true,
            example = "<TOOL>{\"name\":\"clear_category_limit\",\"params\":{\"category\":\"food_dining\",\"confirm\":true}}</TOOL>"
        )
        val getCategoryLimits = LLMAgentTool(
            name = "get_category_limits",
            description = "Afişează limitele de cheltuieli setate pe categorii",
            example = "<TOOL>{\"name\":\"get_category_limits\",\"params\":{}}</TOOL>"
        )
        val scheduleReminder = LLMAgentTool(
            name = "schedule_reminder",
            description = "Programează o notificare (title, body, day_of_month SAU days_from_now)",
            example = "<TOOL>{\"name\":\"schedule_reminder\",\"params\":{\"title\":\"Plăteşte chiria\",\"body\":\"Chiria e scadentă azi.\",\"day_of_month\":15}}</TOOL>"
        )
        val addObligation = LLMAgentTool(
            name = "add_obligation",
            description = "Adaugă o obligație recurentă nouă (name, amount, day_of_month, kind opțional)",
            example = "<TOOL>{\"name\":\"add_obligation\",\"params\":{\"name\":\"Netflix\",\"amount\":50,\"day_of_month\":10,\"kind\":\"subscription\"}}</TOOL>"
        )
        val deleteTransaction = LLMAgentTool(
            name = "delete_transaction",
            description = "Ştege o tranzacție existentă. Necesită confirmare explicită.",
            requiresConfirmation = true,
            example = "<TOOL>{\"name\":\"delete_transaction\",\"params\":{\"merchant\":\"Mega Image\",\"amount\":150,\"confirm\":true}}</TOOL>"
        )
        val consultAdvisor = LLMAgentTool(
            name = "consult_advisor",
            description = "Cere înțelepciune de la consilierul cu principii financiare solide (topic: economisire/datorii/cuplu/investitii/mindset/cumparare/cariera/risc/cultura-ro)",
            example = "<TOOL>{\"name\":\"consult_advisor\",\"params\":{\"topic\":\"cuplu\"}}</TOOL>"
        )
        val coupleCheckIn = LLMAgentTool(
            name = "couple_check_in",
            description = "Generează agenda pentru întâlnirea financiară lunară a cuplului",
            example = "<TOOL>{\"name\":\"couple_check_in\",\"params\":{}}</TOOL>"
        )
        val getCashflowSummary = LLMAgentTool(
            name = "get_cashflow_summary",
            description = "Rezumat cashflow pe N zile (venituri/cheltuieli/top categorii/obligații)",
            example = "<TOOL>{\"name\":\"get_cashflow_summary\",\"params\":{\"days\":30}}</TOOL>"
        )
        val getFinancialSnapshot = LLMAgentTool(
            name = "get_financial_snapshot",
            description = "Snapshot premium pentru coaching: venit, cheltuieli, obligații, obiective, marjă personală",
            example = "<TOOL>{\"name\":\"get_financial_snapshot\",\"params\":{\"days\":30}}</TOOL>"
        )
        val getDataCoverage = LLMAgentTool(
            name = "get_data_coverage",
            description = "Arată ce surse de date vede Solomon şi cât de completă este analiza",
            example = "<TOOL>{\"name\":\"get_data_coverage\",\"params\":{\"days\":30}}</TOOL>"
        )
        val analyzeBudget = LLMAgentTool(
            name = "analyze_budget",
            description = "Analizează bugetul lunar: presiune, categorie dominantă, limite depăşite",
            example = "<TOOL>{\"name\":\"analyze_budget\",\"params\":{\"days\":30}}</TOOL>"
        )
        val detectRisk = LLMAgentTool(
            name = "detect_risk",
            description = "Detectează riscul dominant şi salvează în memoria de coaching vulnerabilitatea principală",
            example = "<TOOL>{\"name\":\"detect_risk\",\"params\":{\"days\":30}}</TOOL>"
        )
        val suggestAction = LLMAgentTool(
            name = "suggest_action",
            description = "Propune o singură acțiune mică pentru azi, bazată pe vulnerabilitatea financiară dominantă",
            example = "<TOOL>{\"name\":\"suggest_action\",\"params\":{\"days\":30}}</TOOL>"
        )
        val educationTip = LLMAgentTool(
            name = "education_tip",
            description = "Generează o lecție contextuală scurtă, legată de vulnerabilitatea sau categoria financiară curentă",
            example = "<TOOL>{\"name\":\"education_tip\",\"params\":{\"vulnerability\":\"small_recurring\"}}</TOOL>"
        )
        val monthlyReport = LLMAgentTool(
            name = "monthly_report",
            description = "Raport clar: luna în 5 puncte \u2014 venit, cheltuieli, obligații, obiective, risc",
            example = "<TOOL>{\"name\":\"monthly_report\",\"params\":{}}</TOOL>"
        )
        val runCoachFlow = LLMAgentTool(
            name = "run_coach_flow",
            description = "Rulează un flow complet de coaching financiar: diagnostic, budget_review, monthly_report sau daily_action",
            example = "<TOOL>{\"name\":\"run_coach_flow\",\"params\":{\"flow\":\"diagnostic\",\"days\":30}}</TOOL>"
        )
        val queryBankConnections = LLMAgentTool(
            name = "query_bank_connections",
            description = "Listează conexiunile bancare conectate, statusul şi data ultimului sync",
            example = "<TOOL>{\"name\":\"query_bank_connections\",\"params\":{}}</TOOL>"
        )
        val syncBankConnections = LLMAgentTool(
            name = "sync_bank_connections",
            description = "Porneşte sincronizarea bancară. Poți specifica bank_name.",
            requiresConfirmation = true,
            example = "<TOOL>{\"name\":\"sync_bank_connections\",\"params\":{\"bank_name\":\"Revolut\",\"confirm\":true}}</TOOL>"
        )
        val disconnectBank = LLMAgentTool(
            name = "disconnect_bank",
            description = "Deconectează o bancă din conexiuni prin bank_id sau bank_name.",
            requiresConfirmation = true,
            example = "<TOOL>{\"name\":\"disconnect_bank\",\"params\":{\"bank_name\":\"Banca Transilvania\",\"confirm\":true}}</TOOL>"
        )

        val all: List<LLMAgentTool> = listOf(
            queryTransactions, addTransaction, updateGoal, queryGoals,
            analyzeSpending, getGoalScenarios, forecastBalance,
            createGoal, setBudgetLimit, clearCategoryLimit, getCategoryLimits, scheduleReminder,
            addObligation, deleteTransaction, consultAdvisor, coupleCheckIn,
            getCashflowSummary, getFinancialSnapshot, getDataCoverage, analyzeBudget, detectRisk,
            suggestAction, educationTip, monthlyReport, runCoachFlow,
            queryBankConnections, syncBankConnections, disconnectBank
        )

        private val criticalTools: Set<String> =
            all.filter { it.requiresConfirmation }.map { it.name }.toSet()

        fun requiresConfirmation(name: String): Boolean = name in criticalTools
        fun isKnownTool(name: String): Boolean = all.any { it.name == name }

        fun instructionCatalog(): String {
            val toolRows = all.joinToString("\n") { "- ${it.name}: ${it.description}" }
            val examples = all.joinToString("\n") { it.example }
            val confirmations = if (criticalTools.isEmpty()) ""
            else "\n\nIMPORTANT: Pentru instrumentele critice foloseste confirm=true inainte de executie: ${criticalTools.sorted().joinToString(", ")}."
            return """
            TOOLS disponibile:
            ${'$'}toolRows
            Exemple de apel:
            ${'$'}examples
            ${'$'}confirmations
            """.trimIndent()
        }

        fun systemInstructions(): String = """
            TOOLS - cand ai nevoie sa citesti date sau sa faci o actiune, raspunde EXACT cu un singur rand:
            ${'$'}{instructionCatalog()}

            FORMAT OBLIGATORIU TOOL CALL:
            <TOOL>{"name":"nume_tool","params":{}}</TOOL>

            REGULI FORMAT:
            - Nu folosi markdown, code fences, backticks sau explicatii langa tool call.
            - Nu scrie text natural cand alegi un tool. Raspunde DOAR cu <TOOL>...</TOOL>.
            - Tool call-ul trebuie sa fie JSON complet valid si sa se inchida cu </TOOL>.
            - Daca raspunsul are nevoie de mai multe tool-uri, cheama doar primul tool necesar.

            IMPORTANT: pentru intrebari despre principii, sfaturi, dileme financiare, cheama consult_advisor cu un topic relevant: economisire, datorii, cuplu, investitii, mindset, cumparare, cariera, risc, cultura-ro.

            ORCHESTRARE COACHING:
            - Pentru diagnostic, cum stau, ce risc am -> run_coach_flow flow=diagnostic
            - Pentru buget, unde se duc banii -> run_coach_flow flow=budget_review
            - Pentru ce fac azi, actiune mica -> run_coach_flow flow=daily_action
            - Pentru raport lunar, rezumat -> run_coach_flow flow=monthly_report
            - Pentru ce date vezi, ai acces la banca -> get_data_coverage
        """.trimIndent()
    }
}

data class AgentToolCall(
    val name: String,
    val params: Map<String, Any?>
) {
    val statusLabel: String
        get() = when (name) {
            "query_transactions" -> "Caut tranzac\u021biile din ${params["days"] ?: 30} zile..."
            "analyze_spending" -> "Analizez cheltuielile pe ${params["days"] ?: 30} zile..."
            "analyze_budget" -> "Analizez bugetul pe ${params["days"] ?: 30} zile..."
            "detect_risk" -> "Detectez riscul financiar dominant..."
            "suggest_action" -> "Calculez ac\u021biunea zilei..."
            "education_tip" -> "Preg\u0103tesc o lec\u021bie financiar\u0103..."
            "get_financial_snapshot" -> "Construiesc snapshot-ul financiar..."
            "get_cashflow_summary" -> "Calculez cashflow-ul..."
            "get_data_coverage" -> "Verific sursele de date..."
            "run_coach_flow" -> "Ruleaz\u0103 flow: ${params["flow"] ?: "diagnostic"}..."
            "forecast_balance" -> "Calculez proiec\u021biile financiare..."
            "consult_advisor" -> "Consult consilierul pe tema: ${params["topic"] ?: "general"}..."
            "create_goal" -> "Creez obiectivul ${params["name"] ?: ""}..."
            "update_goal" -> "Actualizez obiectivul ${params["goal_name"] ?: ""}..."
            "query_goals" -> "Citesc obiectivele active..."
            "add_transaction" -> "Adaug tranzac\u021bia de ${params["amount"] ?: 0} RON..."
            "add_obligation" -> "Adaug obliga\u021bia ${params["name"] ?: ""}..."
            "set_budget_limit" -> "Setez bugetul pentru ${params["category"] ?: ""}..."
            "monthly_report" -> "Preg\u0103tesc raportul lunar..."
            "query_bank_connections" -> "Verific conexiunile bancare..."
            "sync_bank_connections" -> "Sincronizez datele bancare..."
            else -> "Execut $name..."
        }
}

object AgentToolParser {
    private val toolBlockRegex = Regex("(?is)<TOOL>\\s*(.*?)\\s*</TOOL>")
    private val jsonToolRegex = Regex("(?is)\\{\\s*\"name\"\\s*:\\s*\"[^\"]+\"[^{}]*\\}")

    fun parse(text: String): AgentToolCall? {
        val toolMatch = toolBlockRegex.find(text)
        val jsonText = if (toolMatch != null) {
            toolMatch.groupValues[1].trim()
        } else {
            jsonToolRegex.find(text)?.value?.trim() ?: return null
        }
        return try {
            val obj = org.json.JSONObject(jsonText)
            val name = obj.getString("name")
            val paramsObj = obj.optJSONObject("params") ?: return AgentToolCall(name, emptyMap())
            val params = mutableMapOf<String, Any?>()
            for (key in paramsObj.keys()) {
                params[key] = paramsObj.get(key)
            }
            AgentToolCall(name, params)
        } catch (_: Exception) {
            null
        }
    }

    fun stripToolTags(text: String): String {
        return toolBlockRegex.replace(text, "").trim()
    }
}
