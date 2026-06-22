package ro.solomon.app.services

import ro.solomon.core.domain.FinancialPersonality

object SolomonAdvisor {

    fun coupleQuestions(userType: FinancialPersonality, partnerType: FinancialPersonality): String {
        val pair = listOf(userType, partnerType).sortedBy { it.name }
        val key = "${pair[0].name}-${pair[1].name}"
        return when {
            key.contains("saver") && key.contains("spender") -> """
                |\u2022 "Ce sim\u021Bi c\u00E2nd vezi c\u0103 cel\u0103lalt cheltuie diferit dec\u00E2t tine? De unde vine senza\u021Bia aceea?"
                |\u2022 "Spender-ul aduce bucurie de azi; Saver-ul aduce siguran\u021B\u0103 de m\u00E2ine. Cum balans\u0103m cele dou\u0103?"
                |\u2022 "Putem fi de acord pe un buget \u00ABliber\u00BB lunar pentru fiecare, f\u0103r\u0103 justificare?"
            """.trimMargin()
            key == "saver-saver" -> """
                |\u2022 "Riscul nostru e s\u0103 tr\u0103im prea precaut \u2014 ce ne-am dori s\u0103 facem \u00EEmpreun\u0103 dar evit\u0103m pentru c\u0103 \u00ABe scump\u00BB?"
                |\u2022 "Avem 6+ luni fond urgen\u021B\u0103? Atunci poate ar trebui s\u0103 investim mai mult, nu s\u0103 economisim mai mult."
                |\u2022 "Care e o \u00ABcheltuial\u0103 pe bucurie\u00BB pe care ne-am da voie s\u0103pt\u0103m\u00E2na asta?"
            """.trimMargin()
            key == "spender-spender" -> """
                |\u2022 "Care e cea mai important\u0103 cheltuial\u0103 pe care AM AM\u00C2NA-O o lun\u0103 ca s\u0103 str\u00E2ngem \u00EEmpreun\u0103?"
                |\u2022 "Avem fond de urgen\u021B\u0103? Dac\u0103 nu, putem \u00EEncepe cu 100 RON/lun\u0103 automat?"
                |\u2022 "Dac\u0103 nu am avea card \u0219i BNPL, cum ar ar\u0103ta luna noastr\u0103?"
            """.trimMargin()
            key.contains("avoider") -> """
                |\u2022 "Ce te face anxios c\u00E2nd deschidem subiectul banilor? Putem \u00EEncepe foarte mic?"
                |\u2022 "Ai vrea ca eu (sau Solomon) s\u0103 gestionez vizibilitatea, iar tu doar s\u0103 confirmi lunar?"
                |\u2022 "Ce-ai vrut s\u0103 afli de ani de zile despre banii no\u0219tri \u0219i nu ai \u00EEntrebat?"
            """.trimMargin()
            key.contains("monk") -> """
                |\u2022 "Ce ne-ar ajuta s\u0103 ne uit\u0103m la cont m\u0103car o dat\u0103 pe s\u0103pt\u0103m\u00E2n\u0103, f\u0103r\u0103 presiune?"
                |\u2022 "Dac\u0103 m\u00E2ine am avea o problem\u0103 financiar\u0103, am \u0219ti unde \u0219i ce avem?"
                |\u2022 "Care e cel mai mic pas s\u0103pt\u0103m\u00E2nal pe care l-am putea face \u00EEmpreun\u0103?"
            """.trimMargin()
            else -> """
                |\u2022 "Cum vorbeau p\u0103rin\u021Bii t\u0103i despre bani c\u00E2nd erai copil? Ce ai mo\u0219tenit?"
                |\u2022 "Dac\u0103 ai 10.000 EUR m\u00E2ine, ce ai face cu primii 1.000?"
                |\u2022 "Ce \u00EEnseamn\u0103 pentru tine s\u0103 fii bine financiar \u00EEn 5 ani?"
            """.trimMargin()
        }
    }

    fun wisdom(rawTopic: String): String {
        val t = rawTopic
            .replace("\u021B", "t").replace("\u0219", "s")
            .replace("\u0103", "a").replace("\u00E2", "a")
            .replace("\u00EE", "i")
        return when {
            t.contains("cupl") || t.contains("famil") || t.contains("partener") -> cupluWisdom
            t.contains("econom") || t.contains("strang") || t.contains("pun de-o parte") -> economisireWisdom
            t.contains("dator") || t.contains("credit") || t.contains("imprum") || t.contains("rat") -> datoriiWisdom
            t.contains("invest") || t.contains("etf") || t.contains("actiun") || t.contains("crypto") -> investitiiWisdom
            t.contains("mindset") || t.contains("psiholog") || t.contains("frica") || t.contains("vinov") -> mindsetWisdom
            t.contains("cumpa") || t.contains("achizitie") || t.contains("masina") || t.contains("apartament") -> cumparareWisdom
            t.contains("cari") || t.contains("salari") || t.contains("freelance") || t.contains("majorare") -> carieraWisdom
            t.contains("risc") || t.contains("asigur") || t.contains("urgent") || t.contains("forta major") -> riscWisdom
            t.contains("ro") || t.contains("anaf") || t.contains("revolut") || t.contains("ifn") || t.contains("bnpl") -> culturaROWisdom
            else -> generalWisdom
        }
    }

    private val cupluWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: bani \u00EEn cuplu \u2014 sintez\u0103 "Drag\u0103, unde-s banii?" de Adrian Asoltanie]

        Banii sunt prima cauz\u0103 de conflict \u00EEn cuplurile rom\u00E2ne. Adrian Asoltanie a scris "Drag\u0103, unde-s banii?" \
        (Curtea Veche, 2021) \u2014 prima carte de educa\u021Bie financiar\u0103 pentru cupluri scris\u0103 de un rom\u00E2n. \
        Teza central\u0103: educa\u021Bia financiar\u0103 e despre VIA\u021A\u0102, nu despre bani. Banii sunt cristalizarea timpului t\u0103u.

        CELE 4 TIPURI FINANCIARE (Asoltanie):
        - SPENDER (cheltuitorul): tr\u0103ie\u0219te pentru azi, cheltuie u\u0219or
        - SAVER (economul): pune deoparte, planific\u0103, uneori prea precaut
        - AVOIDER (evitantul): refuz\u0103 s\u0103 discute despre bani, anxietate
        - MONK (c\u0103lug\u0103rul): indiferent, nu se uit\u0103 niciodat\u0103 \u00EEn cont
        Recunoa\u0219te-\u021Bi tipologia \u0219i pe cea a partenerului \u2014 f\u0103r\u0103 judecat\u0103. Un cuplu Saver-Spender func\u021Bioneaz\u0103 \
        DAC\u0102 exist\u0103 respect: "pentru o l\u0103m\u00E2ie delicioas\u0103 trebuie zeam\u0103 de l\u0103m\u00E2ie \u0218I miere".

        STRUCTURA RECOMANDAT\u0102 (Asoltanie + Iancu Guda):
        1. CONT COMUN pentru cheltuielile familiei + 2 carduri legate
        2. CONT PERSONAL "f\u0103r\u0103 justificare" pentru fiecare \u2014 sum\u0103 X/lun\u0103, libertate total\u0103
        3. PRAGUL DE ACHIZI\u021AIE COMUN\u0102: peste 300-500 RON, decizie comun\u0103; sub prag, libertate
        4. \u00CENT\u00C2LNIRE FINANCIAR\u0102 LUNAR\u0102: loc neutru (cafenea, nu pat), max 1 or\u0103, 3 sec\u021Biuni: \
           (a) ce s-a \u00EEnt\u00E2mplat luna trecut\u0103, (b) ce vine luna asta, (c) un obiectiv pe 3 luni
        5. PARTENERUL CUNOA\u0218TE TOATE conturile, PIN-urile, parolele \u2014 \u00EEn caz de incapacitate/deces

        \u00CENTREB\u0102RI PUTERNICE PENTRU PARTENER:
        - "Cum vorbeau p\u0103rin\u021Bii t\u0103i despre bani c\u00E2nd erai copil \u2014 cu certuri, \u00EEn \u0219oapt\u0103, relaxat?"
        - "Care e prima ta amintire despre bani?"
        - "Ce ar \u00EEnsemna libertatea financiar\u0103 pentru tine concret? La 65 ani, ce vei fi m\u00E2ndru c\u0103 ai f\u0103cut?"
        - "Dac\u0103 am avea brusc 10.000 EUR \u00EEn plus m\u00E2ine, ce ai face cu primii \u2014 \u0219i de ce?"
        - "Cum vor cre\u0219te copiii no\u0219tri vorbind despre bani?"

        GRE\u0218ELI FRECVENTE \u00CEN CUPLU (RO):
        - Unul gestioneaz\u0103 tot, cel\u0103lalt nu \u0219tie nici parolele
        - "Banii mei vs banii t\u0103i" f\u0103r\u0103 categorie "ai no\u0219tri" definit\u0103 \u2192 resentimente
        - Cadouri/ajutor c\u0103tre familia extins\u0103 (p\u0103rin\u021Bi, fra\u021Bi) nediscutate \u2014 surs\u0103 #1 tensiune ascuns\u0103
        - Discu\u021Bii despre bani doar c\u00E2nd izbucne\u0219te criza, niciodat\u0103 preventiv

        CITAT-STINDARD ASOLTANIE: "Banii vin c\u00E2nd oferi valoare altcuiva. Banii au grij\u0103 de cei care au grij\u0103 de ei."
    """.trimIndent()

    private val economisireWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: economisire \u2014 sintez\u0103 Asoltanie + Iancu Guda]

        PRINCIPIUL #1 (Asoltanie, citat-stindard): "Pl\u0103te\u0219te-te pe tine primul. Imediat ce ai luat salariul, \
        pune deoparte 10-15%, apoi pl\u0103te\u0219te facturile \u0219i datoriile." Banii r\u0103ma\u0219i la sf\u00E2r\u0219it sunt cei pe \
        care ii DECIDEM la \u00EEnceput, nu cei care "r\u0103m\u00E2n" (nu r\u0103m\u00E2ne nimic).

        REALITATEA RO (statistici verificate):
        - 76% din rom\u00E2ni NU r\u0103m\u00E2n cu bani de la o lun\u0103 la alta, indiferent de venit
        - Doar 0,4% din veniturile rom\u00E2nilor merg c\u0103tre investi\u021Bii
        - Rom\u00E2nia locul 128 mondial la \u00EEn\u021Belegerea principiilor banilor
        Dac\u0103 tu ai economii reale, e\u0219ti deja \u00EEn top 24%.

        FORMULA 20/50/30 (Asoltanie + Guda):
        - 20% ECONOMII & INVESTI\u021AII (\u00EEn ordine strict\u0103)
        - 50% NEVOI vitale (chirie/rat\u0103 MAX 25%, alimenta\u021Bie, utilit\u0103\u021Bi, transport, asigurare)
        - 30% DORIN\u021AE (restaurante, vacan\u021Be, abonamente, hobby, haine)

        PIRAMIDA ECONOMIILOR (ordine strict\u0103, Asoltanie + practic\u0103 RO):
        Nivel 0: 1.000-2.000 RON CASH \u00EEn cas\u0103 (primul pas absolut)
        Nivel 1: Fond URGEN\u021A\u0102 3-6 luni cheltuieli (cont lichid separat, NU depozit blocat)
        Nivel 2: Fond SIGURAN\u021A\u0102 9-12 luni (boal\u0103, divor\u021B, criz\u0103 sectorial\u0103) \u2014 pentru antreprenori e prioritar
        Nivel 3: Obiective pe 1-5 ani (cont separat per scop: cas\u0103, vacan\u021B\u0103, copii)
        Nivel 4: Pilonul III pensii \u2014 400 EUR/an deducere fiscal\u0103 pe care nimeni nu o acceseaz\u0103
        Nivel 5: ETF-uri globale DCA lunar (XTB, eToro, IBKR)
        Nivel 6: Generozitate & mo\u0219tenire

        REGULA DE 24h: pentru orice cump\u0103r\u0103tur\u0103 peste 500 RON neplanificat\u0103, dormi o noapte. \
        Pentru cea peste 2.000 RON, a\u0219teapt\u0103 o s\u0103pt\u0103m\u00E2n\u0103. Impulsul moare; nevoia real\u0103 r\u0103m\u00E2ne.

        CITAT ASOLTANIE: "Trimite-\u021Bi banii \u00EEn viitor pentru a-i g\u0103si c\u00E2nd \u00EE\u021Bi sunt necesari."
    """.trimIndent()

    private val datoriiWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: datorii]

        CITAT-STINDARD ASOLTANIE: "Cea mai bun\u0103 metod\u0103 de a sc\u0103pa de datorii este s\u0103 nu le faci."

        PRINCIPIUL ABSOLUT: Singurul num\u0103r care conteaz\u0103 c\u00E2nd compari credite e DAE (Dob\u00E2nda Anual\u0103 \
        Efectiv\u0103), nu rata lunar\u0103. Rata mic\u0103 = perioad\u0103 mai lung\u0103 = mai mult\u0103 dob\u00E2nd\u0103 total\u0103.

        IERARHIA DATORIILOR DUP\u0102 TOXICITATE (\u00EEn RO 2026):
        1. CREDIT IFN (Provident, IuteCredit): DAE 100-3000%. Drogul financiar. Pl\u0103tit primul, mereu.
        2. CARD DE CREDIT cu rata minim\u0103: 20-30% DAE. La rata minim\u0103 datoria nu scade aproape niciodat\u0103.
        3. BNPL STACKING (Pago, TBI Pay, Klarna, Revolut Pay Later, eMAG rate): "0% dob\u00E2nd\u0103" mascheaz\u0103 \
           comisioane reale + lips\u0103 de tracking + nu apar la Biroul de Credit \u2192 cumulare periculoas\u0103.
        4. Credit nevoi personale bancar: 8-15% DAE. Negociabil cu istoric bun.
        5. Credit ipotecar: 4-8% DAE. SINGURA datorie "s\u0103n\u0103toas\u0103" \u2014 sub 25% din venit pe rat\u0103. (Asoltanie)

        METODA AVALAN\u0218\u0102 (matematic optim\u0103): pl\u0103te\u0219ti agresiv datoria cu DAE-ul cel mai mare; restul, minim.
        METODA BULG\u0102RE (psihologic motivant\u0103): pl\u0103te\u0219ti agresiv datoria cu soldul cel mai MIC; victorii \
        rapide care \u021Bin motiva\u021Bia. \u00CEn practic\u0103, bulg\u0103rele bate avalan\u0219a pentru majoritatea oamenilor.

        REGULI DE AUR:
        - Datoria total\u0103 (f\u0103r\u0103 ipotec\u0103) max 15% din venit net.
        - Cu ipotec\u0103 inclus\u0103, max 30-40% (limita BNR). Ideal sub 25%.
        - "Am \u021Binut-o din IFN \u00EEn IFN" e semnal de spiral\u0103 \u2014 STOP imediat \u0219i mergi la CSALB.

        CSALB (Centrul de Solu\u021Bionare Alternativ\u0103 a Litigiilor Bancare): mediaz\u0103 GRATUIT renegocierea cu \
        b\u0103ncile \u0219i IFN-urile. csalb.ro. E un DREPT, nu o favoare. Folosit prea pu\u021Bin \u00EEn RO.

        CITAT IANCU GUDA: "Rom\u00E2nii care au cump\u0103rat un telefon nou pe rate 0% nu au \u00EEn\u021Beles c\u0103 au pl\u0103tit, \
        de fapt, comisioane mascate prin BNPL."
    """.trimIndent()

    private val investitiiWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: investi\u021Bii]

        ADEV\u0102RURI INCONFORTABILE:
        1. Investi\u021Biile nu sunt pentru cei boga\u021Bi. Sunt pentru oricine poate pune 100 RON/lun\u0103 deoparte CONSTANT 10+ ani.
        2. Nimeni nu poate prezice pia\u021Ba pe termen scurt. "Time in the market" bate "timing the market" \u2014 dovedit empiric.
        3. ETF-urile (fonduri indexate globale) c\u00E2\u0219tig\u0103 pe termen lung de fa\u021B\u0103 orice strategie activ\u0103. Comisioane 0.1-0.3%/an vs 1-2% la fonduri active.
        4. Cumperi LUNAR aceea\u0219i sum\u0103, \u00EEn aceea\u0219i zi, indiferent de pia\u021B\u0103. Asta se nume\u0219te DCA \u2014 Dollar Cost Averaging.

        ORDINEA S\u0102N\u0102TOAS\u0102:
        1. Fond urgen\u021B\u0103 COMPLET \u00EEnainte de orice investi\u021Bie.
        2. Datoriile cu DAE > 8% \u2014 pl\u0103tite \u00EEnainte de investit (orice ai investi, dob\u00E2nda datoriei te bate).
        3. Pilonul III pensii \u2014 deductibil fiscal p\u00E2n\u0103 la 400 EUR/an. Bani "gratis" din impozit.
        4. ETF-uri prin broker european (XTB, eToro, Interactive Brokers) \u2014 VUAA, VWCE pentru indexare global\u0103.

        BVB (Bursa de Valori Bucure\u0219ti) are dividende medii 6-8% \u2014 printre cele mai mari din Europa. \
        Companii ca BRD, Romgaz, Transelectrica pl\u0103tesc dividende solide. Aproape necunoscut publicului RO.

        PERICOLE SPECIFICE RO:
        - "Investi\u021Bii" \u00EEn cripto promovate de influenceri RO \u2014 99% sunt scheme.
        - "Investi\u021Bii" imobiliare cu randament 8% promis \u00EEn 6 luni \u2014 schem\u0103 piramidal\u0103 clasic\u0103.
        - Forex / op\u021Biuni binare \u2014 toate sunt jocuri de noroc deghizate. Brokerii c\u00E2\u0219tig\u0103 pe pierderile clien\u021Bilor.

        Nu investi \u00EEn ce nu \u00EEn\u021Belegi. Dac\u0103 cineva \u00EE\u021Bi promite garantat peste 7%/an \u2014 minte sau \u00EEn\u0219eal\u0103.
    """.trimIndent()

    private val mindsetWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: mindset financiar]

        "Finan\u021Bele personale sunt 80% psihologie \u0219i 20% matematic\u0103." Adrian Asoltanie spune-o \u00EEnc\u0103 mai direct: \
        "Educa\u021Bia financiar\u0103 e despre VIA\u021A\u0102, nu despre bani." Banii sunt cristalizarea timpului t\u0103u de munc\u0103.

        MO\u0218TENIREA INVIZIBIL\u0102 (contextul rom\u00E2nesc):
        - 45-50 ani de comunism au ucis spiritul antreprenorial \u0219i g\u00E2ndirea pe termen lung
        - Anii '90 au ad\u0103ugat traume: Caritas (35-50% din gospod\u0103rii au pierdut!), FNI (300.000 victime), \
          SAFI, BankCoop, Bancorex, Albina \u2014 toate falimentate
        - Rezultat: mentalitate de victim\u0103 ("d\u0103-mi", "guvernul e de vin\u0103"), ne\u00EEncredere \u00EEn b\u0103nci, \
          gratificare imediat\u0103, refuzul planific\u0103rii pe termen lung
        Con\u0219tientizarea acestor mo\u0219teniri e PRIMUL PAS.

        CELE 4 TIPURI FINANCIARE (Asoltanie \u2014 f\u0103r\u0103 judecat\u0103, doar con\u0219tientizare):
        - SPENDER: cheltuie u\u0219or, tr\u0103ie\u0219te pentru azi \u2192 are nevoie de structuri automate
        - SAVER: economise\u0219te instinctiv \u2192 uneori prea precaut, rateaz\u0103 oportunit\u0103\u021Bi
        - AVOIDER: evit\u0103 subiectul, anxietate \u2192 are nevoie de baby steps
        - MONK: indiferent, nu se uit\u0103 \u2192 are nevoie de provocare bl\u00E2nd\u0103 s\u0103-\u021Bi \u00EEn\u021Beleag\u0103 via\u021Ba

        BIAS-URI COMUNE:
        1. AVERSIUNEA LA PIERDERE: durerea de a pierde 100 RON e dubl\u0103 fa\u021B\u0103 de pl\u0103cerea de a c\u00E2\u0219tiga 100 RON
        2. LIFESTYLE INFLATION: c\u00E2nd c\u00E2\u0219tigi mai mult, cheltui mai mult \u2014 m\u0103rirea evaporeaz\u0103 \u00EEn 2-3 luni
        3. ANCORARE: pre\u021Bul "vechi" t\u0103iat cu ro\u0219u te face s\u0103 crezi c\u0103 reducerea e real\u0103
        4. NORMALIZAREA DATORIEI: "to\u021Bi au rate, e normal" \u2014 nu, doar pare normal
        5. CONFUNDAREA NEVOII CU DORIN\u021AA: "avem nevoie de 4 camere" \u2014 de fapt vrem lini\u0219te, nu camere (Asoltanie)

        \u00CENTREB\u0102RI REFLECTIVE PUTERNICE:
        - "Cum vorbeau p\u0103rin\u021Bii t\u0103i despre bani c\u00E2nd erai copil \u2014 cu certuri, \u00EEn \u0219oapt\u0103, relaxat?"
        - "Care e prima ta amintire despre bani?"
        - "Ce credin\u021B\u0103 despre bani ai mo\u0219tenit pe care \u00EEnc\u0103 o crezi ast\u0103zi?"
        - "Banii sunt pentru tine surs\u0103 de libertate sau de stres? De ce?"
        - "Cum te face s\u0103 te sim\u021Bi s\u0103 te ui\u021Bi \u00EEn cont la final de lun\u0103?"
        - "Dac\u0103 te-ai \u00EEnt\u00E2lni cu tine de la 25 ani, ce sfat financiar \u021Bi-ai da?"
        - "Care e cump\u0103r\u0103tura care te face cel mai fericit retroactiv? Dar cea mai regretat\u0103?"

        CITATE ASOLTANIE:
        - "Oamenii s\u0103raci g\u00E2ndesc zilnic; boga\u021Bii g\u00E2ndesc pe zeci de ani."
        - "Banii, ca alcoolul, sunt pl\u0103cu\u021Bi s\u0103-i consumi dar durero\u0219i s\u0103-i recuperezi."
        - "Banii nu rezolv\u0103 problemele emo\u021Bionale \u2014 confundarea celor dou\u0103 duce la cheltuieli compulsive."
    """.trimIndent()

    private val cumparareWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: cump\u0103r\u0103turi mari (ma\u0219in\u0103, apartament, electronice)]

        APARTAMENT:
        1. Pre\u021Bul casei + 7-10% costuri ascunse: notar, comision, mobilare, mutare, repara\u021Bii neprev\u0103zute.
        2. Rata ipotecii + utilit\u0103\u021Bi + asigurare PAD + \u00EEntre\u021Binere = NU trebuie s\u0103 dep\u0103\u0219e\u0219ti 30% din venitul net.
        3. Avans 15-20% NU \u00EEnseamn\u0103 tot ce ai. Las\u0103 \u00EEnc\u0103 3-6 luni cheltuieli rezerv\u0103 post-cump\u0103rare.
        4. Apartamentul "investi\u021Bie" \u2014 calcule cu chirie poten\u021Bial\u0103 vs rat\u0103, NU "imobiliarele cresc mereu". \u00CEn RO 2008-2012 pre\u021Burile au sc\u0103zut 60%.

        MA\u0218IN\u0102:
        Regula 20/4/10: 20% avans, max 4 ani credit, max 10% din venit pe rata total\u0103 + asigur\u0103ri + combustibil.
        O ma\u0219in\u0103 nou\u0103 pierde 20-30% din valoare \u00EEn primul an. Ma\u0219ina de 3 ani, second-hand verificat\u0103, e \
        99% din func\u021Bionalitate la 60% din pre\u021B.

        ELECTRONICE (telefon, laptop, electrocasnice):
        \u00CEntrebarea cheie: "\u00CEn c\u00E2te zile de munc\u0103 pl\u0103tesc asta?" Telefonul de 5000 RON la salariu net de \
        5000 RON = 1 lun\u0103 \u00EEntreag\u0103. Merit\u0103?

        FRAMEWORK DECIZIONAL UNIVERSAL pentru cump\u0103r\u0103turi mari:
        1. Pl\u0103tesc cu bani lichizi pe care ii am acum? Dac\u0103 DA \u2192 poate fi OK.
        2. Trebuie s\u0103 iau credit? Care e DAE-ul TOTAL pl\u0103tit p\u00E2n\u0103 la final?
        3. Dac\u0103 m\u00E2ine pierd jobul, pot continua pl\u0103\u021Bile 6 luni din rezerv\u0103?
        4. Exist\u0103 o variant\u0103 cu 30-50% mai ieftin\u0103 care \u00EEmi serve\u0219te 90% din nevoia real\u0103?
        5. Cum m\u0103 voi sim\u021Bi despre asta peste 5 ani? Voi mai dori? O voi mai folosi?
    """.trimIndent()

    private val carieraWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: carier\u0103, venit, freelancing]

        PRINCIPIUL "VENITUL E LEVIERUL CEL MAI MARE":
        Po\u021Bi economisi maxim 30% din salariu. Dar po\u021Bi s\u0103-\u021Bi dublezi salariul. Matematica e clar\u0103: \
        energia merit\u0103 pus\u0103 mai mult \u00EEn cre\u0219terea venitului dec\u00E2t \u00EEn t\u0103ierea ultimei cheltuieli mici.

        MAJORARE DE SALARIU \u2014 cerere structurat\u0103:
        1. Documenteaz\u0103 contribu\u021Bia (cifre, proiecte, KPI atin\u0219i).
        2. Compar\u0103 cu pia\u021Ba (LinkedIn Salary, Glassdoor, Repcoder pentru RO).
        3. Cere o \u00EEnt\u00E2lnire dedicat\u0103 \u2014 nu men\u021Biona la cafea sau pe drum.
        4. Cere o sum\u0103 specific\u0103, nu "ceva mai mult".
        5. Dac\u0103 "nu se poate acum", \u00EEntreab\u0103 "ce ar trebui s\u0103 fac \u00EEn 6 luni ca s\u0103 se poat\u0103".

        FREELANCING / PFA \u00EEn RO:
        - CAS + CASS la PFA cu venituri peste 12 salarii minime \u2014 poate consuma 20-30% din venit.
        - SRL devine mai eficient fiscal la venituri peste ~80.000 EUR/an.
        - 30% din factur\u0103 SE pune deoparte pentru taxe \u0219i asigur\u0103ri \u2014 IMEDIAT la \u00EEncasare.
        - Fond de urgen\u021B\u0103 la freelancer = 6-9 luni de cheltuieli (vs 3 la angajat). Veniturile fluctueaz\u0103.

        VENIT SECUNDAR (side hustle):
        Cel mai bun side hustle e cel care folose\u0219te deja ce \u0219tii s\u0103 faci, pl\u0103tit per or\u0103, f\u0103r\u0103 overhead \
        (consultan\u021B\u0103, traduceri, design freelance). Cel mai prost e cel care \u00EE\u021Bi consum\u0103 weekend-urile cu \
        randament sub salariul t\u0103u orar de la job.

        \u00CENTREBARE PUTERNIC\u0102: "Dac\u0103 m\u00E2ine ai pierde jobul, c\u00E2t timp ai supravie\u021Bui financiar la nivelul actual?" \
        R\u0103spunsul t\u0103u dicteaz\u0103 nivelul de risc tolerabil \u00EEn alte decizii financiare.
    """.trimIndent()

    private val riscWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: risc, asigur\u0103ri, evenimente neprev\u0103zute]

        PIRAMIDA RISCURILOR (de la "\u00EEmi distruge via\u021Ba" la "incomod"):
        1. INCAPACITATE DE MUNC\u0102 permanent\u0103 (boal\u0103, accident grav) \u2014 asigurare de via\u021B\u0103 + invaliditate.
        2. DECES cu copii sau credit ipotecar \u2014 asigurare de via\u021B\u0103 term life pe sum\u0103 cel pu\u021Bin egal\u0103 cu ipoteca.
        3. PIERDEREA JOBULUI \u2014 fond de urgen\u021B\u0103 3-6 luni.
        4. SPITALIZARE major\u0103 \u2014 asigurare privat\u0103 s\u0103n\u0103tate (50-150 RON/lun\u0103) sau abonament Regina Maria/MedLife.
        5. PAGUBE LOCUIN\u021A\u0102 \u2014 asigurarea PAD (obligatorie, sub 100 RON/an) + facultativ\u0103 pentru con\u021Binut.
        6. AUTO \u2014 RCA obligatoriu + Casco la ma\u0219ini noi.

        REGULA DE BAZ\u0102: asigur\u0103-te \u00EEmpotriva a ce nu \u00EE\u021Bi po\u021Bi permite s\u0103 pl\u0103te\u0219ti din economii. NU asigura \
        telefonul, laptopul, vacan\u021Ba \u2014 costurile asigur\u0103rii dep\u0103\u0219esc beneficiul.

        GRE\u0218ELI FRECVENTE \u00CEN RO:
        - Asigurare de via\u021B\u0103 "cu economisire" (unit-linked) \u2014 v\u00E2ndut\u0103 agresiv, comisioane mari, randament prost. \
          Mai bine: term life pur\u0103 + ETF separat.
        - Asigurare auto Casco la ma\u0219ini de 10+ ani \u2014 primele dep\u0103\u0219esc valoarea rezidual\u0103 a ma\u0219inii.
        - Lipsa testamentului \u2014 \u00EEn RO, f\u0103r\u0103 testament mo\u0219tenirea se face dup\u0103 lege, NU dup\u0103 dorin\u021Ba ta.

        SCENARIU MINTAL: "Dac\u0103 m\u00E2ine eu nu pot lucra 6 luni, ce se \u00EEnt\u00E2mpl\u0103 cu familia mea?" R\u0103spunsul \
        onest dicteaz\u0103 nivelul de protec\u021Bie necesar.
    """.trimIndent()

    private val culturaROWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 topic: cultur\u0103 financiar\u0103 rom\u00E2neasc\u0103]

        ISTORIA TRAUMELOR FINANCIARE RO (de ce sunt rom\u00E2nii reticen\u021Bi):
        - 1992-1994: CARITAS \u2014 35-50% din gospod\u0103riile rom\u00E2ne\u0219ti au pierdut economiile
        - Mid-90s: FNI \u2014 300.000 victime, sum\u0103 echivalent\u0103 cu un PIB lunar
        - Tot anii '90: SAFI, BankCoop, Bancorex, Albina \u2014 b\u0103nci falimentate
        - 2014-2016: OneCoin \u2014 schem\u0103 mondial\u0103, ANAF doar pe RO a confiscat 400k EUR
        - Aceste traume sunt MO\u0218TENIRE COLECTIV\u0102. Con\u0219tientizeaz\u0103 c\u00E2nd propui ceva nou (ETF, broker european).

        CAPCANE SPECIFIC ROM\u00C2NE\u0218TI (2026):

        1. IFN-uri (Provident, IuteCredit, Salt Bank credite, etc.): DAE 100-3000%. Legale, etic pred\u0103toare. \
           Refinan\u021Bare repetat\u0103 "din IFN \u00EEn IFN" = spiral\u0103 garantat\u0103. Solomon le detecteaz\u0103 automat.

        2. BNPL STACKING (Pago, TBI Pay, Klarna, Tazz Pay, Revolut Pay Later, eMAG rate): cumulare INVIZIBIL\u0102 \
           f\u0103r\u0103 raportare la Biroul de Credit. 5 cump\u0103r\u0103turi mici "\u00EEn 3 rate" = obliga\u021Bie lunar\u0103 mai mare ca o rat\u0103 bancar\u0103.

        3. REVOLUT OVERSPEND: interfa\u021Ba gamificat\u0103, Pockets, lipsa "senza\u021Biei de bani reali" \u2192 cheltuieli +30-40% \
           fa\u021B\u0103 de carduri normale. Studii UK confirm\u0103. RO: 4.8M utilizatori, sub microscop.

        4. CRIPTO PROMOVAT DE INFLUENCERI ROM\u00C2NI: 99% scheme piramidale sau rug pulls. Regul\u0103 absolut\u0103: \
           dac\u0103 cineva \u00EE\u021Bi promite GARANTAT peste 7%/an, fugi.

        5. "INVESTI\u021AII IMOBILIARE" RANDAMENT 8% \u00CEN 6 LUNI: 100% schem\u0103. Imobiliarele REALE au 3-5% chirie + 0-3% \
           apreciere/an. Orice promisiune peste = \u00EEn\u0219el\u0103ciune. 2008-2012 pre\u021Burile au sc\u0103zut 50-60% \u00EEn RO.

        INSTITU\u021AII UTILE (pu\u021Bin \u0219tiute):
        - CSALB \u2014 mediere gratuit\u0103 cu b\u0103ncile/IFN-urile (csalb.ro). DREPT, nu favoare.
        - ANPC \u2014 reclama\u021Bii consumator.
        - FGDB \u2014 Fondul de Garantare a Depozitelor. 100.000 EUR garanta\u021Bi per persoan\u0103 per banc\u0103. Distribuie sume mari.
        - ASF \u2014 Autoritatea de Supraveghere Financiar\u0103 (verifici brokeri, asigur\u0103tori).
        - Biroul de Credit \u2014 verific\u0103 ce datorii sunt \u00EEnregistrate pe numele t\u0103u.

        INFLA\u021AIA RO 2024-2026 = 5-10%/an. Banii la saltea sau \u00EEn cont curent f\u0103r\u0103 dob\u00E2nd\u0103 PIERD valoare garantat.
    """.trimIndent()

    private val generalWisdom = """
        [Consilier \u2014 principii financiare solide \u2014 principii universale aplicate \u00EEn RO]

        CELE 10 PRINCIPII FUNDAMENTALE (sintez\u0103 Asoltanie + Guda):

        1. PL\u0102TE\u0218TE-TE PRIMUL. 10-15% din salariu, automat, IMEDIAT dup\u0103 salariu, \u00CENAINTE de facturi. (Asoltanie)
        2. BANII SUNT TIMPUL T\u0102U. \u00EEnainte s\u0103 cheltui X, \u00EEntreab\u0103: "C\u00E2te ore am muncit pentru asta?" (Asoltanie)
        3. FOND DE URGEN\u021A\u0102 3-6 LUNI \u00CENAINTE de orice investi\u021Bie. La antreprenor: 9-12 luni.
        4. RATA LOCUIN\u021AII MAX 25% din venit. Ma\u0219ina max 4-6 salarii lunare. (Asoltanie)
        5. NICIODAT\u0102 CREDIT pentru consum. Credit doar pentru active care produc valoare. (Asoltanie)
        6. CHELTUIELILE MICI RECURENTE \u00D7 365 = \u0219ocant. 20 RON/zi pe cafea = 7.300 RON/an \u2248 1.5 salarii medii.
        7. CONFUNDAREA NEVOII CU DORIN\u021AA = cauza #1 a falimentului personal. (Guda)
        8. LIFESTYLE INFLATION = capcana celor cu venit \u00EEn cre\u0219tere. Din m\u0103rire: 50% economii, 50% stil de via\u021B\u0103.
        9. NU INVESTI \u00CEN CE NU \u00CEN\u021AELEGI. Dac\u0103 cineva \u00EE\u021Bi promite garantat >7%/an = minte sau \u00EEn\u0219eal\u0103.
        10. CALCULEAZ\u0102 BUGETUL ANUAL, NU LUNAR. Vei vedea 50+ linii pe care le ignori. (Asoltanie)

        25 \u00CENTREB\u0102RI PUTERNICE pe care le pun clien\u021Bilor (alege contextul):

        R\u0102D\u0102CINI (psihologie):
        - "Cum vorbeau p\u0103rin\u021Bii t\u0103i despre bani c\u00E2nd erai copil?"
        - "Care e prima ta amintire despre bani?"
        - "Ce credin\u021B\u0103 despre bani ai mo\u0219tenit pe care \u00EEnc\u0103 o crezi?"
        - "Banii sunt pentru tine surs\u0103 de libertate sau de stres? De ce?"
        - "Cum te sim\u021Bi s\u0103 te ui\u021Bi \u00EEn cont la final de lun\u0103?"

        PREZENT (clarificare):
        - "Dac\u0103 m\u00E2ine ai pierde jobul, c\u00E2t ai supravie\u021Bui f\u0103r\u0103 s\u0103 te \u00EEmprumu\u021Bi?"
        - "\u0218tii exact c\u00E2\u021Bi bani ai cheltuit luna trecut\u0103? Pe ce categorii?"
        - "Care e cel mai mic gest financiar zilnic care, \u00D7 365, te-ar \u0219oca?"
        - "De ce ai cump\u0103rat ultimul lucru peste 500 RON? Nevoie sau emo\u021Bie?"

        VIITOR (viziune):
        - "Ce ar \u00EEnsemna libertatea financiar\u0103 pentru TINE concret? Sum\u0103? Stil via\u021B\u0103?"
        - "Dac\u0103 ai avea 10.000 EUR \u00EEn plus m\u00E2ine, ce ai face cu ei? De ce?"
        - "La 65 ani, ce vei fi m\u00E2ndru c\u0103 ai f\u0103cut cu banii t\u0103i?"
        - "Care e cel mai mic pas s\u0103pt\u0103m\u00E2na asta pentru obiectivul de peste 10 ani?"

        CITATE DIN "DRAG\u0102, UNDE-S BANII?" (Adrian Asoltanie, Curtea Veche 2021):
        - "Educa\u021Bia financiar\u0103 e despre VIA\u021A\u0102, nu despre bani."
        - "Banii vin atunci c\u00E2nd oferi valoare ALTCUIVA."
        - "Banii au grij\u0103 de cei care au grij\u0103 de ei."
        - "Trimite-\u021Bi banii \u00EEn viitor pentru a-i g\u0103si c\u00E2nd \u00EE\u021Bi sunt necesari."
        - "Cea mai bun\u0103 metod\u0103 de a sc\u0103pa de datorii este s\u0103 nu le faci."
        - "Oamenii s\u0103raci g\u00E2ndesc zilnic; boga\u021Bii g\u00E2ndesc pe zeci de ani."
    """.trimIndent()
}
