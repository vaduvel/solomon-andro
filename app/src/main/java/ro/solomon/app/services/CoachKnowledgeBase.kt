package ro.solomon.app.services

/**
 * Curated, dated and sourced Romanian financial knowledge base for the coach.
 *
 * Hybrid grounding model:
 * - [KnowledgeEntry.principles] are timeless and safe to state directly.
 * - [KnowledgeEntry.datedFacts] are time-sensitive (rates, prices, legal
 *   thresholds); each carries the date it was verified plus an official source,
 *   and is subject to periodic refresh.
 * - [KnowledgeEntry.microLesson] is a short behavioral takeaway for drip delivery.
 *
 * Grounding guardrail: never present a dated fact without its date and source,
 * and flag entries past the refresh horizon as needing re-verification.
 */

enum class CoachKnowledgeTopic {
    SAVING, DEBT, COUPLE, INVESTING, MINDSET, BIG_PURCHASE, CAREER, RISK, CULTURE_RO, GENERAL,
    HABITS, DEBT_PSYCHOLOGY, EMOTIONAL_MONEY, COMPOUNDING
}

data class DatedFact(
    val claim: String,
    val asOf: String,
    val source: String
)

data class KnowledgeEntry(
    val topic: CoachKnowledgeTopic,
    val title: String,
    val principles: List<String>,
    val datedFacts: List<DatedFact> = emptyList(),
    val microLesson: String? = null,
    val lastReviewed: String
)

object CoachKnowledgeBase {

    /** Entries with dated facts older than this should be re-verified before being quoted as current. */
    const val REFRESH_HORIZON_DAYS: Long = 120

    private const val REVIEWED = "2026-06-24"

    val entries: Map<CoachKnowledgeTopic, KnowledgeEntry> = mapOf(
        CoachKnowledgeTopic.SAVING to KnowledgeEntry(
            topic = CoachKnowledgeTopic.SAVING,
            title = "Economisire",
            principles = listOf(
                "Economisirea nu e renuntare, ci a da banilor o destinatie inainte sa si-o gaseasca singuri.",
                "Regula 50/30/20 adaptata la Romania: 50% nevoi fixe, 30% calitatea vietii, 20% viitor (economii, fond de urgente, investitii).",
                "Fondul de urgente vine primul: 3 luni de cheltuieli fixe, inaintea oricarui alt obiectiv.",
                "Automatizeaza transferul in ziua salariului, nu la final de luna.",
                "Economisirea mica si consecventa bate economisirea mare si sporadica.",
                "Banii tinuti doar in cash pierd valoare reala din cauza inflatiei."
            ),
            datedFacts = listOf(
                DatedFact(
                    claim = "Inflatia anuala in Romania era 9,87% (martie 2026), peste dobanzile uzuale la depozite, deci cash-ul nepus la lucru pierde valoare reala.",
                    asOf = "2026-03",
                    source = "BNR, decizii de politica monetara - bnr.ro/en/25486-2026-05-15-nbr-board-decisions-on-monetary-policy"
                ),
                DatedFact(
                    claim = "Dobanda de politica monetara BNR: 6,50% pe an (mentinuta din august 2024); facilitatea de depozit 5,50%.",
                    asOf = "2026-05-15",
                    source = "BNR - bnr.ro/en/25486-2026-05-15-nbr-board-decisions-on-monetary-policy"
                ),
                DatedFact(
                    claim = "Titlurile de stat Fidelis (emisiunea 15-22 iunie 2026) ofereau dobanzi neimpozabile de pana la 7,60% pe an in lei (10 ani) si 5,80% in euro (10 ani).",
                    asOf = "2026-06-22",
                    source = "Ministerul Finantelor, Ordin 661/2026 - mfinante.gov.ro (Fidelis)"
                )
            ),
            microLesson = "Da fiecarui leu o destinatie inainte sa-si gaseasca singur una.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.DEBT to KnowledgeEntry(
            topic = CoachKnowledgeTopic.DEBT,
            title = "Datorii si credite",
            principles = listOf(
                "Datoria nu e rea prin definitie; datoria scumpa fara scop e periculoasa.",
                "Rata totala la credite ideal sub 30% din venitul net; peste 40% inseamna presiune cronica.",
                "Metoda avalansa (platesti intai cel mai scump credit) reduce cel mai mult costul total; metoda bulgarelui de zapada (cel mai mic sold) ajuta motivatia.",
                "Semnale de alarma: platesti doar minimul la card, iei credit ca sa acoperi alt credit, nu stii exact cate datorii ai si la ce dobanzi.",
                "IFN-urile si BNPL pot fi mult mai scumpe decat creditul bancar; verifica DAE-ul real inainte sa semnezi.",
                "Refinantarea are sens doar daca economia de dobanda depaseste comisioanele."
            ),
            datedFacts = listOf(
                DatedFact(
                    claim = "DAE la creditele de consum e plafonata la 27 puncte procentuale peste dobanda Lombard a BNR, adica maximum aproximativ 34,5% pe an la o dobanda Lombard de 7,50%.",
                    asOf = "2026-06-24",
                    source = "OUG 50/2010 si analize juridice - juridice.ro/758560; legislatie.just.ro"
                ),
                DatedFact(
                    claim = "Creditul de consum e definit legal ca imprumut de maximum 100.000 lei, pe maximum 5 ani, negarantat cu ipoteca.",
                    asOf = "2024-08-12",
                    source = "Legea 243/2024 - legislatie.just.ro/public/DetaliiDocument/286989"
                ),
                DatedFact(
                    claim = "Pentru litigii cu banca sau IFN-ul poti apela GRATUIT la CSALB; solutionare in mai putin de 90 de zile, in afara instantei.",
                    asOf = "2026-06-24",
                    source = "CSALB - csalb.ro (OG 38/2015)"
                )
            ),
            microLesson = "Ataca intai dobanda cea mai scumpa; acolo pierzi cei mai multi bani in tacere.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.COUPLE to KnowledgeEntry(
            topic = CoachKnowledgeTopic.COUPLE,
            title = "Finantele in cuplu",
            principles = listOf(
                "Banii sunt printre primele surse de conflict in cuplu, nu din lipsa de bani, ci din lipsa unei reguli clare.",
                "Sistem care functioneaza: cont comun pentru cheltuieli comune plus conturi personale pentru libertate individuala.",
                "Contributie proportionala cu venitul, nu 50/50 daca veniturile sunt inegale.",
                "Prag de decizie comuna: orice cheltuiala peste un nivel agreat se discuta.",
                "Intalnirea financiara lunara de circa 20 de minute rezolva majoritatea tensiunilor inainte sa devina conflicte."
            ),
            microLesson = "20 de minute pe luna despre bani previn majoritatea conflictelor financiare.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.INVESTING to KnowledgeEntry(
            topic = CoachKnowledgeTopic.INVESTING,
            title = "Investitii",
            principles = listOf(
                "Primul pas inainte de investitii: fondul de urgente complet, altfel poti fi fortat sa vinzi la momentul gresit.",
                "Diversifica; nu pune totul intr-un singur loc.",
                "Comisioanele mici conteaza enorm pe termen lung.",
                "Investitia pe termen lung (5+ ani) reduce riscul volatilitatii.",
                "Nu investi bani de care ai putea avea nevoie in 12 luni.",
                "Optiuni accesibile in Romania: titluri de stat (Tezaur, Fidelis), ETF-uri si fonduri prin brokeri locali (BVB, BT Trade, Tradeville, XTB)."
            ),
            datedFacts = listOf(
                DatedFact(
                    claim = "Fidelis iunie 2026: dobanzi fixe neimpozabile - in lei 6,35% (2 ani) / 6,90% (4 ani) / 7,60% (10 ani); in euro 4,00% (3 ani) / 4,85% (5 ani) / 5,80% (10 ani). Subscriere minima 5.000 lei sau 1.000 euro.",
                    asOf = "2026-06-22",
                    source = "Ministerul Finantelor, Ordin 661/2026 - mfinante.gov.ro (Fidelis)"
                )
            ),
            microLesson = "Intai fondul de urgente, apoi diversificare, apoi rabdare. In aceasta ordine.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.MINDSET to KnowledgeEntry(
            topic = CoachKnowledgeTopic.MINDSET,
            title = "Psihologia banilor",
            principles = listOf(
                "Comportamentul financiar e circa 80% psihologie si 20% matematica.",
                "Cheltuitul emotional te face sa te simti mai bine circa 20 de minute; nu rezolva cauza.",
                "Anchoring: primul pret vazut nu e pretul corect.",
                "Eroarea costului scufundat: nu continua sa platesti ceva doar fiindca ai platit deja.",
                "FOMO financiar: cand toata lumea cumpara, adesea e varful ciclului.",
                "Reguli simple si automatizare bat vointa zilnica."
            ),
            microLesson = "Cei mai buni cu banii nu castiga cel mai mult; stiu unde se duc banii lor si de ce.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.BIG_PURCHASE to KnowledgeEntry(
            topic = CoachKnowledgeTopic.BIG_PURCHASE,
            title = "Achizitii mari",
            principles = listOf(
                "Regula 72 de ore: pentru orice achizitie neplanificata mare, asteapta 72 de ore; majoritatea impulsurilor dispar.",
                "Inainte de o achizitie mare verifica: ramane fondul de urgente intact? platesti cash sau pe credit (ce DAE, ce perioada)? care e costul total de proprietate? exista o alternativa buna la pret mai mic?",
                "Atentie la ratele 'fara dobanda' cu penalitati ascunse la intarziere.",
                "Upgrade-ul inutil si presiunea sociala sunt capcane frecvente."
            ),
            microLesson = "Daca dupa 72 de ore tot o vrei si raspunzi da la cele 4 intrebari, e o decizie, nu un impuls.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.CAREER to KnowledgeEntry(
            topic = CoachKnowledgeTopic.CAREER,
            title = "Cariera si venit",
            principles = listOf(
                "Optimizarea cheltuielilor are un plafon; cresterea venitului, nu.",
                "Negocierea salariului e printre cele mai bune investitii de timp pe care le poti face.",
                "Investitia in competente rentaza, la 25-35 de ani, mai mult decat orice produs financiar.",
                "Venitul pasiv vine dupa stabilitate, nu inaintea ei.",
                "In Romania, contractul de drepturi de autor, PFA vs microintreprindere si munca remote pentru companii din afara tarii pot schimba semnificativ venitul net; verifica regulile fiscale curente."
            ),
            microLesson = "O ora de negociere a salariului poate valora mai mult decat un an de taiat cheltuieli.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.RISK to KnowledgeEntry(
            topic = CoachKnowledgeTopic.RISK,
            title = "Managementul riscului",
            principles = listOf(
                "Riscul financiar nu dispare daca il ignori; creste.",
                "Riscuri majore: lipsa fondului de urgente, dependenta de un singur venit, asigurare insuficienta, datorii cu dobanda variabila.",
                "Fond de urgente = 3-6 luni de cheltuieli fixe, intr-un cont separat si lichid.",
                "Asigurarea de sanatate privata completeaza sistemul public pentru urgente majore."
            ),
            datedFacts = listOf(
                DatedFact(
                    claim = "Asigurarea obligatorie a locuintei (PAD) costa 130 lei/an pentru locuinte tip A (suma asigurata 100.000 lei) si 50 lei/an pentru tip B (50.000 lei); acopera cutremur, inundatii si alunecari de teren. Lipsa politei se sanctioneaza cu amenda 100-500 lei.",
                    asOf = "2026-06-24",
                    source = "Legea 260/2008 (modif. Legea 115/2023), PAID - padrom.ro; ghiseul.ro/paid"
                )
            ),
            microLesson = "Fondul de urgente nu e o economie, e asigurarea ta ca un soc nu devine catastrofa.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.CULTURE_RO to KnowledgeEntry(
            topic = CoachKnowledgeTopic.CULTURE_RO,
            title = "Comportamente financiare in Romania",
            principles = listOf(
                "Particularitati locale: reticenta la transparenta financiara ('sa nu stie vecinul'), creditul la IFN sau prieten care pare la indemana dar costa mai mult, economiile in cash care pierd din inflatie, proprietatea ca unic obiectiv financiar.",
                "Ce functioneaza in Romania: titlurile de stat Tezaur si Fidelis (garantate de stat, dobanzi neimpozabile), BVB si fondurile locale, plus negocierea cu bancile, mai accesibila decat cred multi."
            ),
            datedFacts = listOf(
                DatedFact(
                    claim = "Titlurile de stat Tezaur si Fidelis sunt garantate de stat si au avut dobanzi neimpozabile de pana la circa 7,60% pe an in lei in 2026.",
                    asOf = "2026-06-22",
                    source = "Ministerul Finantelor - mfinante.gov.ro (Tezaur/Fidelis)"
                )
            ),
            microLesson = "Educatia financiara e cea mai rentabila investitie pe care o poti face azi, si e gratuita.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.GENERAL to KnowledgeEntry(
            topic = CoachKnowledgeTopic.GENERAL,
            title = "Principii fundamentale",
            principles = listOf(
                "Cheltuie mai putin decat castigi; diferenta e libertatea ta financiara.",
                "Fondul de urgente e primul obiectiv.",
                "Datoriile scumpe se platesc inaintea oricarei investitii.",
                "Automatizeaza economisirea; sistemele bat vointa.",
                "Investeste in tine; competentele care iti cresc venitul rentaza cel mai mult.",
                "Urmareste progresul: un obiectiv nevizualizat ramane o dorinta."
            ),
            microLesson = "Cel mai bun moment sa incepi un plan financiar a fost acum 5 ani. Al doilea, azi.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.HABITS to KnowledgeEntry(
            topic = CoachKnowledgeTopic.HABITS,
            title = "Obiceiuri financiare",
            principles = listOf(
                "Un obicei are 3 parti: declansator, rutina, recompensa. Schimba rutina pastrand declansatorul si recompensa.",
                "Regula celor 2 minute: orice obicei nou incepe cu o versiune care dureaza sub 2 minute, ca sa fie usor de pornit.",
                "Habit stacking: leaga obiceiul nou de unul existent (dupa ce imi iau cafeaua, verific o cheltuiala).",
                "Automatizarea bate vointa: ce se intampla singur (transfer in ziua salariului) nu depinde de motivatie.",
                "Mediul conteaza mai mult decat disciplina: sterge cardul salvat din aplicatiile de shopping."
            ),
            microLesson = "Nu iti propune sa economisesti mai mult luna asta. Propune-ti un singur gest automat care se repeta singur.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.DEBT_PSYCHOLOGY to KnowledgeEntry(
            topic = CoachKnowledgeTopic.DEBT_PSYCHOLOGY,
            title = "Psihologia datoriei",
            principles = listOf(
                "Rusinea intretine datoria: cu cat eviti sa te uiti, cu atat creste. Primul pas e sa privesti cifra exacta, fara judecata.",
                "Plata minimului la card pare o usurare, dar e o capcana: dobanda mananca progresul, iar soldul aproape nu scade.",
                "Mental accounting: banii de la bonus sau cadou se cheltuie mai usor desi sunt la fel de reali. Trateaza-i la fel.",
                "O datorie are o poveste, nu doar o cifra. Intelege ce nevoie a acoperit, ca sa nu se repete.",
                "Progresul vizibil (sold care scade lunar) bate orice plan tinut doar in minte."
            ),
            microLesson = "Datoria nu spune cine esti. E doar o problema de rezolvat, cu pasi mici si vizibili.",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.EMOTIONAL_MONEY to KnowledgeEntry(
            topic = CoachKnowledgeTopic.EMOTIONAL_MONEY,
            title = "Relatia emotionala cu banii",
            principles = listOf(
                "Tiparele despre bani se formeaza in copilarie (money scripts) si ne conduc inconstient ca adulti.",
                "Cheltuitul e adesea reglare emotionala: cumperi ca sa gestionezi stres, plictiseala sau tristete, nu nevoia in sine.",
                "Comparatia sociala (si online) imprumuta dorinte care nu sunt ale tale si distorsioneaza ce inseamna suficient.",
                "Numeste emotia inainte de cumparare (sunt obosit, stresat, plictisit); de multe ori dispare nevoia de a cumpara.",
                "Banii sunt un instrument pentru valorile tale, nu un scor al valorii tale ca om."
            ),
            microLesson = "Inainte sa cumperi cand esti agitat, intreaba-te: ce simt acum si ce am nevoie de fapt?",
            lastReviewed = REVIEWED
        ),
        CoachKnowledgeTopic.COMPOUNDING to KnowledgeEntry(
            topic = CoachKnowledgeTopic.COMPOUNDING,
            title = "Dobanda compusa si timpul",
            principles = listOf(
                "Dobanda compusa inseamna ca si dobanda produce dobanda: efectul devine exponential pe termen lung.",
                "Timpul e cel mai mare avantaj: a incepe devreme bate a investi sume mari mai tarziu.",
                "Regula 72: imparti 72 la randamentul anual in procente si afli in cati ani se dubleaza banii (de exemplu 6% inseamna circa 12 ani).",
                "Acelasi principiu lucreaza si impotriva ta la datorii: dobanda compusa la card creste soldul daca platesti doar minimul.",
                "Costul amanarii e real: fiecare an de asteptare pierde tocmai anii cei mai valorosi de compunere."
            ),
            microLesson = "Nu suma conteaza cel mai mult, ci de cand incepi. Cel mai bun moment a fost ieri; al doilea, azi.",
            lastReviewed = REVIEWED
        )
    )

    fun entry(topic: CoachKnowledgeTopic): KnowledgeEntry =
        entries[topic] ?: entries.getValue(CoachKnowledgeTopic.GENERAL)

    /** The short behavioral takeaway for a topic, if any. */
    fun microLessonFor(topic: CoachKnowledgeTopic): String? = entry(topic).microLesson

    private fun epochMillisOf(isoDate: String): Long = try {
        val pattern = if (isoDate.length == 7) "yyyy-MM" else "yyyy-MM-dd"
        java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(isoDate)?.time ?: 0L
    } catch (_: Throwable) {
        0L
    }

    /** Days since the entry was last reviewed, given a clock in epoch millis. */
    fun ageDays(entry: KnowledgeEntry, nowEpochMillis: Long): Long {
        val reviewed = epochMillisOf(entry.lastReviewed)
        if (reviewed == 0L) return 0L
        return (nowEpochMillis - reviewed) / 86_400_000L
    }

    /** True when an entry carries dated facts that are past the refresh horizon. */
    fun needsRefresh(entry: KnowledgeEntry, nowEpochMillis: Long): Boolean =
        entry.datedFacts.isNotEmpty() && ageDays(entry, nowEpochMillis) > REFRESH_HORIZON_DAYS

    /** Topics whose dated facts should be re-verified before being quoted as current. */
    fun staleTopics(nowEpochMillis: Long): List<CoachKnowledgeTopic> =
        entries.values.filter { needsRefresh(it, nowEpochMillis) }.map { it.topic }

    /**
     * Render an entry to display text with a provenance footer. When [nowEpochMillis]
     * is supplied, a staleness warning is appended for dated facts past the horizon.
     */
    fun render(topic: CoachKnowledgeTopic, nowEpochMillis: Long? = null): String {
        val e = entry(topic)
        val sb = StringBuilder()
        sb.append(e.title).append("\n\n")
        e.principles.forEach { sb.append("- ").append(it).append("\n") }
        if (e.datedFacts.isNotEmpty()) {
            sb.append("\nCifre la zi (verifica intotdeauna la sursa):\n")
            e.datedFacts.forEach { f ->
                sb.append("- ").append(f.claim)
                    .append(" (valabil la ").append(f.asOf)
                    .append("; sursa: ").append(f.source).append(")\n")
            }
            if (nowEpochMillis != null && needsRefresh(e, nowEpochMillis)) {
                sb.append("\nAtentie: aceste cifre nu au mai fost verificate de peste ")
                    .append(REFRESH_HORIZON_DAYS)
                    .append(" de zile. Confirma la sursa oficiala inainte de a te baza pe ele.\n")
            }
        }
        e.microLesson?.let { sb.append("\nMicro-lectie: ").append(it).append("\n") }
        sb.append("\nUltima verificare editoriala: ").append(e.lastReviewed).append(".")
        return sb.toString().trimEnd()
    }
}
