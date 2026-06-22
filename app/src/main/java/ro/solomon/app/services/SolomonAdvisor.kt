package ro.solomon.app.services

import ro.solomon.core.domain.FinancialPersonality

object SolomonAdvisor {

    fun wisdom(rawTopic: String): String {
        val topic = rawTopic.lowercase().trim()
        return when {
            topic.contains("economisire") || topic.contains("saving") || topic.contains("economi") -> economisire
            topic.contains("datori") || topic.contains("credit") || topic.contains("imprumut") -> datorii
            topic.contains("cuplu") || topic.contains("partener") || topic.contains("relatie") -> cuplu
            topic.contains("investit") || topic.contains("invest") -> investitii
            topic.contains("mindset") || topic.contains("mentalit") || topic.contains("psiholog") -> mindset
            topic.contains("cumpara") || topic.contains("achizit") || topic.contains("mare") -> cumparareMare
            topic.contains("carier") || topic.contains("job") || topic.contains("salar") -> cariera
            topic.contains("risc") || topic.contains("urgenta") || topic.contains("fond") -> risc
            topic.contains("roman") || topic.contains("cultura") -> culturaRo
            else -> general
        }
    }

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

    private val economisire = """
    [Consilier - principii despre economisire]

    Economisirea nu este despre renuntare. Este despre a da banilor tai o destinatie inainte ca ei sa-si gaseasca singuri una.

    Principii fundamentale:
    - Regula 50/30/20 adaptata la Romania: 50% nevoi fixe (chirie, facturi, mancare de baza), 30% calitate a vietii (iesiri, vacante, placeri), 20% viitor (economii, fond urgente, investitii).
    - Fondul de urgente vine primul - 3 luni de cheltuieli fixe, inainte de orice alt obiectiv.
    - Automatizeaza. Transferul in ziua salariului, nu la final de luna.
    - Economisirea mica si consecventa bate economisirea mare si sporadica.

    Pentru Romania:
    - Dobanzile la depozite in lei sunt mai bune decat Euro in prezent - ramai in lei daca nu ai nevoie de Euro.
    - ING Economii, BT Save, Libra - optiuni fara comisioane ascunse.
    - Banii sub saltea pierd 5-8% pe an din cauza inflatiei.

    Micro-lectie: Daca economisesti 10% din venit lunar timp de 10 ani si nu faci nimic cu ei, poti pierde mai mult decat daca ii puneai la un depozit simplu. Viteza conteaza mai putin decat consecventa.
    """.trimIndent()

    private val datorii = """
    [Consilier - principii despre datorii si credite]

    Datoria nu este rea prin definitie. Datoria scumpa fara scop este periculoasa.

    Principii:
    - Rata totala la credite nu trebuie sa depaseasca 30% din venitul net. Peste 40% = presiune financiara cronica.
    - Metoda avalansa: platesti mai mult la cel mai scump credit. Metoda bulgare de zapada: la cel mai mic sold. Alege ce functioneaza pentru tine.
    - IFN-urile si BNPL (rate fara dobanda la cumparare) sunt cele mai periculoase. DAE-ul real poate fi 200-400%.
    - Refinantarea are sens daca economia de dobanda depaseste comisioanele de restructurare.

    Semnal de alarma:
    - Platesti minimul la card de credit in fiecare luna.
    - Ai luat un credit ca sa acoperi alt credit.
    - Nu stii exact cate datorii ai si la ce dobanzi.

    Micro-lectie: Un credit de consum de 10.000 RON la 18% DAE, pe 5 ani, costa aproape 5.000 RON in dobanzi. Acelasi credit la 8% costa 2.200 RON. Diferenta este cina de la restaurant in fiecare saptamana timp de 3 ani.
    """.trimIndent()

    private val cuplu = """
    [Consilier - principii pentru finantele in cuplu]

    Banii sunt primul sau al doilea subiect de conflict in cupluri. Nu pentru ca nu exista bani, ci pentru ca nu exista o regula clara.

    Sisteme care functioneaza:
    1. Cont comun pentru cheltuieli comune + conturi personale pentru libertate individuala.
    2. Contributie proportionala cu venitul, nu 50/50 daca veniturile sunt inegale.
    3. Prag de decizie comuna - orice cheltuiala peste X RON se discuta.

    Ce nu functioneaza:
    - Gestionam din mers fara regula clara.
    - Un partener stie tot, celalalt nu stie nimic.
    - Judecarea cheltuielilor personale ale partenerului.

    Intalnirea financiara lunara - 20 minute, o data pe luna - rezolva 80% din tensiunile financiare inainte sa devina conflicte.

    Micro-lectie: Cuplurile care vorbesc despre bani o data pe luna au mai putine conflicte financiare decat cele care nu vorbesc deloc, chiar daca au mai putini bani.
    """.trimIndent()

    private val investitii = """
    [Consilier - principii despre investitii pentru romani]

    Investitiile nu sunt pentru bogati. Sunt pentru oricine vrea ca banii sa lucreze in locul lor.

    Primul pas - fondul de urgente complet. Fara el, orice investitie poate fi fortat vanduta la momentul gresit.

    Optiuni accesibile in Romania:
    - Titluri de stat Fidelis sau Tezaur - garantate de stat, dobanzi bune in RON/EUR.
    - ETF-uri pe BVB sau internationale prin Tradeville, XTB - diversificare globala cu costuri mici.
    - Fonduri de investitii locale - usor de accesat, fara sa stii bursa.

    Principii de baza:
    - Diversifica. Nu pune totul intr-un singur loc.
    - Costul de intrare conteaza. Comisioanele mici fac diferenta pe termen lung.
    - Investitia pe termen lung (5+ ani) reduce riscul volatilitatii.
    - Nu investi bani de care ai putea avea nevoie in 12 luni.

    Micro-lectie: 500 RON/luna investiti constant timp de 20 de ani, cu un randament mediu de 7%, devin aproximativ 260.000 RON. Acelasi efort cu 10 ani mai devreme: 600.000 RON. Timpul este cel mai mare avantaj.
    """.trimIndent()

    private val mindset = """
    [Consilier - psihologia banilor si mentalitate financiara]

    Comportamentul financiar este 80% psihologie si 20% matematica. Daca ar fi doar matematica, toata lumea ar fi bine.

    Tipare comune:
    - Cheltuitul emotional - cumperi ca sa te simti mai bine. Functioneaza 20 de minute.
    - Anchoring - esti influentat de primul pret vazut. Primul pret nu este pretul corect.
    - Eroarea costului scufundat - continui sa platesti pentru ceva (abonament, curs) pentru ca ai platit deja.
    - FOMO financiar - investesti cand toata lumea vorbeste despre ceva. Adesea la varful ciclului.

    Ce construieste un mindset financiar sanatos:
    - Claritate: stii exact cati bani ai, ce ai de platit si ce vrei sa construiesti.
    - Reguli simple, nu vointa. Automatizarea bate disciplina zilnica.
    - Progres vizibil - un obiectiv urmarit se atinge mai des decat unul tinut in minte.

    Micro-lectie: Oamenii cu cel mai bun mindset financiar nu sunt cei cu cel mai mare venit. Sunt cei care stiu unde se duc banii lor si de ce.
    """.trimIndent()

    private val cumparareMare = """
    [Consilier - principii pentru achizitii mari]

    O achizitie mare luata impulsiv poate costa mai mult decat 3 luni de cheltuieli obisnuite.

    Regula 72 de ore: pentru orice achizitie neplanificata peste 500 RON, astepti 72 de ore. 80% din impulsuri dispar.

    Inainte de orice achizitie mare:
    1. Ai fondul de urgente intact dupa cumparare?
    2. Platesti cu bani proprii sau credit? Daca credit - ce DAE si ce perioada?
    3. Costul total de proprietate - nu doar pretul de achizitie (reparatii, asigurare, mentenanta)?
    4. Exista o alternativa la 80% din calitate si 50% din pret?

    Capcane frecvente:
    - Rate fara dobanda cu penalizari ascunse la intarziere.
    - Upgrade inutil - produsul actual functioneaza, dar noul model e mai nou.
    - Presiunea sociala - si prietenul meu si-a luat.

    Micro-lectie: Cea mai buna achizitie mare este cea la care raspunzi da la toate cele 4 intrebari de mai sus, dupa 72 de ore de gandire.
    """.trimIndent()

    private val cariera = """
    [Consilier - cariera, venit si crestere financiara]

    Venitul este fundamentul oricarui plan financiar. Optimizarea cheltuielilor are un plafon. Cresterea venitului nu.

    Principii:
    - Negocierea salariului este cel mai mare ROI pe ora pe care il poti face. O negociere de 30 de minute poate valora 10.000+ RON pe an.
    - Venitul pasiv nu inlocuieste venitul activ pana nu esti financiar stabil. Mai intai stabilitate, apoi diversificare.
    - Competentele sunt cel mai bun activ pe termen lung. Investitia in tine insuti renteaza mai mult decat orice produs financiar la varsta de 25-35 de ani.

    Pentru Romania:
    - Contractul de drepturi de autor - avantaj fiscal real pentru anumite profesii.
    - Microintreprindere vs. PFA - depinde de venit si activitate.
    - Remote pentru companii din afara Romaniei - oportunitate reala de multiplicare a venitului.

    Micro-lectie: O crestere salariala de 15% are acelasi efect financiar ca reducerea cheltuielilor cu 15%, dar efectul de compounding al venitului mai mare dureaza toata cariera.
    """.trimIndent()

    private val risc = """
    [Consilier - managementul riscului financiar personal]

    Riscul financiar nu dispare daca nu te gandesti la el. Creste.

    Riscuri principale pentru un roman cu venituri medii:
    1. Lipsa fondului de urgente - orice soc (job pierdut, reparatie mare, urgenta medicala) se transforma in datorie.
    2. Supraexpunere pe un singur venit - daca acel venit dispare, tot planul se prabuseste.
    3. Asigurare insuficienta - apartamentul, sanatatea, viata.
    4. Datorie cu dobanda variabila - expunere la cresterea ROBOR.

    Primul nivel de protectie:
    - Fond urgente = 3-6 luni cheltuieli fixe, in cont separat, lichid.
    - Asigurare de locuinta - obligatorie (PAD) + facultativa pentru risc real.
    - Asigurare de sanatate privata - spitalele publice sunt insuficiente pentru urgente majore.

    Micro-lectie: Fondul de urgente nu este o economie. Este costul asigurarii tale ca un eveniment neprevazut nu devine o catastrofa financiara.
    """.trimIndent()

    private val culturaRo = """
    [Consilier - comportamente financiare specifice Romaniei]

    Contextul romanesc are particularitatii care nu se regasesc in sfaturile financiare occidentale standard.

    Comportamente comune in Romania:
    - Sa nu stie vecinul - reticenta fata de transparenta financiara, inclusiv in cuplu.
    - Creditul la IFN sau prieten - adesea costa mai mult decat creditul bancar, dar pare mai la indemana.
    - Economii in cash sau valuta sub saltea - pierdere reala din inflatie.
    - Proprietatea ca unic obiectiv financiar - mai intai casa, restul dupa.

    Ce functioneaza in Romania:
    - Titlurile de stat Tezaur si Fidelis - randament bun, garantate de stat, fara comisioane de intermediar.
    - BVB si fondurile de investitii locale - subevaluate fata de potential.
    - Negocierea cu bancile - mai accesibila decat cred cei mai multi clienti.

    Micro-lectie: Romania are o rata de economisire a gospodariilor in crestere, dar majority banii stau in depozite la termen cu randament real negativ. Educatia financiara este cel mai rentabil lucru pe care il poti face azi.
    """.trimIndent()

    private val general = """
    [Consilier - principii financiare fundamentale]

    Indiferent de situatie, cateva principii raman valide:

    1. Cheltuie mai putin decat castigi. Diferenta este libertatea ta financiara.
    2. Fondul de urgente este primul obiectiv. Fara el, orice plan este fragil.
    3. Datoriile scumpe se platesc inainte de orice investitie.
    4. Automatizeaza economisirea. Vointa are limite; sistemele nu.
    5. Investeste in tine. Competentele care iti cresc venitul renteaza mai mult decat orice produs financiar.
    6. Urmareste progresul. Un obiectiv nevizualizat este o dorinta.

    Micro-lectie: Cel mai bun moment sa incepi un plan financiar a fost acum 5 ani. Al doilea cel mai bun moment este azi.
    """.trimIndent()
}
