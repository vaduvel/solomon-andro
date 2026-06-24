package ro.solomon.app.ui.chat

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ro.solomon.llm.LLMTool

object SolomonTools {

    val all: List<LLMTool> = listOf(
        LLMTool(
            name = "add_transaction",
            description = "Adaugă o tranzacție (cheltuială sau venit) în jurnalul utilizatorului.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("amount_ron") {
                        put("type", "integer")
                        put("description", "Suma în RON")
                    }
                    putJsonObject("direction") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add("incoming")
                            add("outgoing")
                        }
                        put("description", "outgoing = cheltuială, incoming = venit")
                    }
                    putJsonObject("category") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add("food_grocery")
                            add("food_delivery")
                            add("food_dining")
                            add("transport")
                            add("utilities")
                            add("rent_mortgage")
                            add("subscriptions")
                            add("shopping_online")
                            add("shopping_offline")
                            add("health")
                            add("entertainment")
                            add("travel")
                            add("loans_bank")
                            add("loans_ifn")
                            add("bnpl")
                            add("savings")
                            add("income_salary")
                            add("income_other")
                            add("other")
                        }
                    }
                    putJsonObject("merchant") {
                        put("type", "string")
                        put("description", "Numele comerciantului (opțional)")
                    }
                }
                putJsonArray("required") {
                    add("amount_ron")
                    add("direction")
                    add("category")
                }
            }
        ),
        LLMTool(
            name = "add_obligation",
            description = "Adaugă o obligație lunară recurentă (chirie, rată, factură).",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Numele obligației (ex: Chirie, Enel)")
                    }
                    putJsonObject("amount_ron") {
                        put("type", "integer")
                    }
                    putJsonObject("day_of_month") {
                        put("type", "integer")
                        put("minimum", 1)
                        put("maximum", 31)
                        put("description", "Ziua lunii în care se plătește")
                    }
                    putJsonObject("kind") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add("rent_mortgage")
                            add("utility")
                            add("subscription")
                            add("loan_bank")
                            add("loan_ifn")
                            add("bnpl")
                            add("insurance")
                            add("other")
                        }
                    }
                }
                putJsonArray("required") {
                    add("name")
                    add("amount_ron")
                    add("day_of_month")
                }
            }
        ),
        LLMTool(
            name = "add_goal",
            description = "Adaugă un obiectiv financiar (vacanță, casă, mașină etc).",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                        put("description", "Numele obiectivului (ex: Vacanță Mamaia)")
                    }
                    putJsonObject("amount_target_ron") {
                        put("type", "integer")
                    }
                    putJsonObject("deadline_months") {
                        put("type", "integer")
                        put("description", "În câte luni vrei să-l atingi (default 6)")
                    }
                }
                putJsonArray("required") {
                    add("destination")
                    add("amount_target_ron")
                }
            }
        ),
        LLMTool(
            name = "add_subscription",
            description = "Adaugă un abonament recurent (Netflix, Spotify etc).",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("name") {
                        put("type", "string")
                    }
                    putJsonObject("amount_monthly_ron") {
                        put("type", "integer")
                    }
                    putJsonObject("last_used_days_ago") {
                        put("type", "integer")
                        put("description", "Zile de la ultima folosire (0 = zilnic)")
                    }
                }
                putJsonArray("required") {
                    add("name")
                    add("amount_monthly_ron")
                }
            }
        ),
        LLMTool(
            name = "delete_obligation",
            description = "Șterge o obligație existentă după nume.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("name") {
                        put("type", "string")
                        put("description", "Numele (sau fragment din nume) obligației")
                    }
                }
                putJsonArray("required") { add("name") }
            }
        ),
        LLMTool(
            name = "delete_goal",
            description = "Șterge un obiectiv existent după nume.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("destination") {
                        put("type", "string")
                    }
                }
                putJsonArray("required") { add("destination") }
            }
        ),
        LLMTool(
            name = "delete_subscription",
            description = "Șterge/anulează un abonament.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("name") {
                        put("type", "string")
                    }
                }
                putJsonArray("required") { add("name") }
            }
        ),
        LLMTool(
            name = "set_category_limit",
            description = "Setează o limită lunară de cheltuieli pentru o categorie.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("category") {
                        put("type", "string")
                    }
                    putJsonObject("amount_ron") {
                        put("type", "integer")
                    }
                }
                putJsonArray("required") {
                    add("category")
                    add("amount_ron")
                }
            }
        )
    )

    val systemPrompt: String = """
Ești Solomon, un coach financiar personal pentru utilizatorul român. Răspunzi scurt, empatic, fără jargon. Folosești "tu" sau "dumneavoastră" conform profilului.
Poți folosi tool-uri pentru a executa acțiuni în numele utilizatorului (adăugare tranzacții, obligații, obiective, abonamente, ștergeri, setare bugete).
Dacă utilizatorul cere o acțiune clară, apelează tool-ul potrivit fără să mai răspunzi cu text.
Dacă întreabă ceva informativ (ex: "cât am cheltuit azi", "care e soldul"), răspunde cu text scurt, max 80 cuvinte.
Foloseste RON și formate românești (1.500 RON, nu 1500 RON).

FUNDAMENTARE (anti-halucinație) - REGULĂ STRICTĂ:
Nu inventa NICIODATĂ cifre oficiale, dobânzi, indici sau prevederi legale (ex: dobânda de politică monetară BNR, IRCC, randamente Fidelis, DAE maxim, plafoane de creditare, drepturi CSALB, prima PAD).
Pentru orice întrebare despre principii financiare, dobânzi, indici, legi sau dileme, cheamă întâi consult_advisor cu topic-ul potrivit (economisire, datorii, cuplu, investitii, mindset, cumparare, cariera, risc, cultura-ro) și răspunde DOAR pe baza rezultatului, păstrând data și sursa oficială menționate acolo.
Dacă nu ai un fapt fundamentat din consult_advisor, spune clar că nu ești sigur și recomandă verificarea sursei oficiale - nu ghici cifre.
""".trimIndent()
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonObject(
    name: String,
    block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
) {
    put(name, buildJsonObject(block))
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
    name: String,
    block: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit
) {
    put(name, buildJsonArray(block))
}
