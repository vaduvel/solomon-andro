package ro.solomon.core.util

import java.util.Calendar

data class FinancialEducationTip(
    val id: Int,
    val category: Category,
    val text: String,
    val emoji: String
) {
    enum class Category(val label: String) {
        MINDSET("Mindset"),
        ECONOMISIRE("Economisire"),
        DATORII("Datorii"),
        CHELTUIELI("Cheltuieli"),
        OBIECTIVE("Obiective"),
        PROTECTIE("Protec\u021Bie"),
        ROMANIA("Context RO"),
        INVESTITII("Investi\u021Bii")
    }

    companion object {

        val all: List<FinancialEducationTip> = listOf(
            FinancialEducationTip(1, Category.MINDSET, "\uD83E\uDDE0",
                "Banii nu sunt scopul \u2014 sunt instrumentul. Scopul e lini\u0219tea, nu avu\u021Bia."),
            FinancialEducationTip(2, Category.MINDSET, "\uD83D\uDCC5",
                "Oamenii care bugeteaz\u0103 nu sunt zg\u00E2rci\u021Bi. \u0219tiu exact unde pot cheltui f\u0103r\u0103 vinov\u0103\u021Bie."),
            FinancialEducationTip(3, Category.MINDSET, "\uD83D\uDD04",
                "O gre\u0219eal\u0103 financiar\u0103 nu e un e\u0219ec \u2014 e o lec\u021Bie cu chitan\u021B\u0103. Conteaz\u0103 ce faci dup\u0103."),
            FinancialEducationTip(4, Category.MINDSET, "\uD83C\uDFAF",
                "Cel mai periculos moment financiar e c\u00E2nd c\u00E2\u0219tigi mai mult f\u0103r\u0103 s\u0103 cheltui mai mult. Asta e avu\u021Bie real\u0103."),
            FinancialEducationTip(5, Category.MINDSET, "\uD83D\uDE24",
                "\"Nu \u00EEmi permit\" e o alegere, nu o sentin\u021B\u0103. De obicei \u00EEnseamn\u0103 \"nu e o prioritate acum\"."),
            FinancialEducationTip(6, Category.MINDSET, "\u2696\uFE0F",
                "Finan\u021Bele personale sunt 80% psihologie \u0219i 20% matematic\u0103. Cifrele sunt u\u0219oare \u2014 emo\u021Biile nu."),
            FinancialEducationTip(7, Category.MINDSET, "\uD83C\uDF0A",
                "Nu trebuie s\u0103 fii perfect. Un buget urmat 70% din timp e infinit mai bun dec\u00E2t niciunul."),
            FinancialEducationTip(8, Category.MINDSET, "\uD83E\uDEA1",
                "Fiecare cheltuial\u0103 e un vot pentru cine vrei s\u0103 fii. Votezi con\u0219tient?"),

            FinancialEducationTip(9, Category.ECONOMISIRE, "\uD83C\uDFE6",
                "Pl\u0103te\u0219te-te primul. \u00EEnainte de orice altceva, pune deoparte 10% din salariu \u00EEn ziua \u00EEn care intr\u0103."),
            FinancialEducationTip(10, Category.ECONOMISIRE, "\u26A1",
                "Un fond de urgen\u021B\u0103 de 3 luni de cheltuieli transform\u0103 o criz\u0103 \u00EEn inconvenient."),
            FinancialEducationTip(11, Category.ECONOMISIRE, "\u2615",
                "Regula de 24 de ore: pentru orice cump\u0103r\u0103tur\u0103 > 200 RON, dormi o noapte \u00EEnainte."),
            FinancialEducationTip(12, Category.ECONOMISIRE, "\uD83D\uDCC8",
                "100 RON economisi\u021Bi azi la 7% dob\u00E2nd\u0103 = 400 RON \u00EEn 20 ani. Timpul e cel mai bun investitor."),
            FinancialEducationTip(13, Category.ECONOMISIRE, "\uD83C\uDF81",
                "Cadoul cel mai bun pentru tine din viitor: un fond de urgen\u021B\u0103 bine alimentat."),
            FinancialEducationTip(14, Category.ECONOMISIRE, "\uD83D\uDD01",
                "Automatizeaz\u0103 economiile. Dac\u0103 nu trebuie s\u0103 decizi, nu po\u021Bi gre\u0219i. Transfer automat pe 1 ale lunii."),
            FinancialEducationTip(15, Category.ECONOMISIRE, "\uD83E\uDEA3",
                "Metoda \"buckets\": cont curent (cheltuieli), cont economii (urgen\u021Be), cont obiective (vacan\u021B\u0103/cas\u0103). Simplu \u0219i eficient."),
            FinancialEducationTip(16, Category.ECONOMISIRE, "\uD83D\uDCA1",
                "Dac\u0103 prime\u0219ti o m\u0103rire, m\u0103re\u0219te economiile cu jum\u0103tate din cre\u0219tere. Stilul de via\u021B\u0103 poate cre\u0219te lin."),

            FinancialEducationTip(17, Category.DATORII, "\u2744\uFE0F",
                "Metoda avalan\u0219\u0103: pl\u0103te\u0219ti mai \u00EEnt\u00E2i datoria cu dob\u00E2nda cea mai mare. Matematic optim\u0103."),
            FinancialEducationTip(18, Category.DATORII, "\uD83E\uDEA8",
                "Metoda bulg\u0103re: pl\u0103te\u0219ti mai \u00EEnt\u00E2i datoria cea mai mic\u0103. Psihologic mai motivant\u0103. Alege ce func\u021Bioneaz\u0103 pentru tine."),
            FinancialEducationTip(19, Category.DATORII, "\u26A0\uFE0F",
                "DAE (Dob\u00E2nda Anual\u0103 Efectiv\u0103) e singurul num\u0103r care conteaz\u0103 c\u00E2nd compari credite. Nu rata lunar\u0103."),
            FinancialEducationTip(20, Category.DATORII, "\uD83D\uDEA8",
                "IFN-urile practică DAE de 100-3000%. Un credit de 1.000 RON poate deveni 5.000 RON \u00EEn 2 ani."),
            FinancialEducationTip(21, Category.DATORII, "\uD83D\uDCCB",
                "BNPL (\"cump\u0103r\u0103 acum, pl\u0103te\u0219te mai t\u00E2rziu\") nu e gratuit. Stacking-ul de BNPL e una din cauzele principale ale spiralei datoriilor."),
            FinancialEducationTip(22, Category.DATORII, "\uD83E\uDD1D",
                "CSALB mediaz\u0103 gratuit \u00EEntre tine \u0219i banc\u0103/IFN dac\u0103 ai dificult\u0103\u021Bi. E un drept, nu o favoare."),
            FinancialEducationTip(23, Category.DATORII, "\uD83E\uDDEE",
                "Datoria pe card de credit la rata minim\u0103 = cel mai scump credit din via\u021Ba ta. Pl\u0103te\u0219te tot ce po\u021Bi lunar."),
            FinancialEducationTip(24, Category.DATORII, "\uD83D\uDED1",
                "Regula de aur: datoria de consum (telefoane, vacan\u021Be, haine) nu ar trebui s\u0103 dep\u0103\u0219easc\u0103 15% din venitul net."),

            FinancialEducationTip(25, Category.CHELTUIELI, "\uD83D\uDD0D",
                "Cheltuielile mici sunt cele mai periculoase. 20 RON/zi pe cafea + snacks = 7.200 RON/an."),
            FinancialEducationTip(26, Category.CHELTUIELI, "\uD83D\uDCF1",
                "Abonamentele se adun\u0103 silen\u021Bios. F\u0103 un audit lunar: c\u00E2te pl\u0103te\u0219ti, c\u00E2te folose\u0219ti cu adev\u0103rat?"),
            FinancialEducationTip(27, Category.CHELTUIELI, "\uD83D\uDED2",
                "Cump\u0103r\u0103turile cu lista fac economii reale. Impulsul de la raft cost\u0103 \u00EEn medie 30% extra fa\u021B\u0103 de planul ini\u021Bial."),
            FinancialEducationTip(28, Category.CHELTUIELI, "\uD83C\uDF55",
                "Livr\u0103rile de m\u00E2ncare cost\u0103 de 2-3x fa\u021B\u0103 de g\u0103tit. O mas\u0103 livrat\u0103 s\u0103pt\u0103m\u00E2nal = 1.500-3.000 RON/an."),
            FinancialEducationTip(29, Category.CHELTUIELI, "\uD83C\uDFF7\uFE0F",
                "Pre\u021Bul per unitate, nu pre\u021Bul total. Pachetul mare e mai ieftin pe gram, dar nu dac\u0103 expire neufolosit."),
            FinancialEducationTip(30, Category.CHELTUIELI, "\uD83D\uDD14",
                "Dezactiveaz\u0103 notific\u0103rile de la shopuri \u0219i apps de cump\u0103r\u0103turi. Fiecare notificare e un trigger de impuls."),
            FinancialEducationTip(31, Category.CHELTUIELI, "\uD83C\uDFAD",
                "Cheltuielile de \"imagine\" (haine, gadgeturi pentru a impresiona) sunt cele mai pu\u021Bin satisf\u0103c\u0103toare pe termen lung."),
            FinancialEducationTip(32, Category.CHELTUIELI, "\uD83D\uDCCA",
                "Regula 50/30/20: 50% nevoi, 30% dorin\u021Be, 20% economii. Nu perfect\u0103 pentru Rom\u00E2nia, dar e un start."),

            FinancialEducationTip(33, Category.OBIECTIVE, "\uD83D\uDDFA\uFE0F",
                "Un obiectiv f\u0103r\u0103 termen limit\u0103 e un vis. Cu dat\u0103 \u0219i sum\u0103 exact\u0103, devine un plan."),
            FinancialEducationTip(34, Category.OBIECTIVE, "\uD83C\uDFD8\uFE0F",
                "Vacan\u021Ba de vis nu cost\u0103 o mo\u0219tenire. Cost\u0103 30-50 RON/zi timp de 6-12 luni. Calculul e simplu, disciplina nu."),
            FinancialEducationTip(35, Category.OBIECTIVE, "\uD83C\uDFE0",
                "Avansul la un apartament \u00EEn Cluj/Bucure\u0219ti = 30.000-80.000 EUR. La 500 EUR/lun\u0103 economisi\u021Bi = 5-13 ani. Planific\u0103 acum."),
            FinancialEducationTip(36, Category.OBIECTIVE, "\uD83C\uDF93",
                "Investi\u021Bia \u00EEn educa\u021Bie (cursuri, c\u0103r\u021Bi, mentorat) are cel mai mare ROI din toate investi\u021Biile posibile."),
            FinancialEducationTip(37, Category.OBIECTIVE, "\uD83D\uDD22",
                "\u00CEmparte obiectivele mari \u00EEn microobiective lunare. \"Economisesc 5.000 RON\" devine \"pun 417 RON de luni viitoare\"."),
            FinancialEducationTip(38, Category.OBIECTIVE, "\uD83D\uDCF8",
                "Vizualizeaz\u0103 obiectivul zilnic: o poz\u0103 cu destina\u021Bia de vacan\u021B\u0103 ca wallpaper face decizii mai u\u0219oare."),

            FinancialEducationTip(39, Category.PROTECTIE, "\uD83D\uDEE1\uFE0F",
                "Asigurarea de s\u0103n\u0103tate privat\u0103 cost\u0103 50-150 RON/lun\u0103. O opera\u021Bie privat\u0103 poate costa 10.000-50.000 RON f\u0103r\u0103 ea."),
            FinancialEducationTip(40, Category.PROTECTIE, "\uD83C\uDFE1",
                "Asigurarea PAD (obligatorie pentru locuin\u021B\u0103) e sub 100 RON/an. F\u0103r\u0103 ea, un cutremur te poate l\u0103sa cu rate \u0219i f\u0103r\u0103 cas\u0103."),
            FinancialEducationTip(41, Category.PROTECTIE, "\uD83D\uDCC4",
                "Testamentul nu e pentru b\u0103tr\u00E2ni \u2014 e pentru oricine are bunuri sau familie. F\u0103r\u0103 testament, legea decide, nu tu."),
            FinancialEducationTip(42, Category.PROTECTIE, "\uD83D\uDD10",
                "Nu da niciodat\u0103 creden\u021Bialele bancare online \u00EEn afara aplica\u021Biei oficiale a b\u0103ncii. Nici \"suportului tehnic\"."),
            FinancialEducationTip(43, Category.PROTECTIE, "\uD83D\uDCF1",
                "Activeaz\u0103 autentificarea \u00EEn doi pa\u0219i la toate conturile financiare. E 2 minute care pot salva ani de economii."),
            FinancialEducationTip(44, Category.PROTECTIE, "\uD83D\uDD75\uFE0F",
                "Verific\u0103 extrasul de cont lunar. Tranzac\u021Biile mici recurente necunoscute sunt semn de compromis al cardului."),

            FinancialEducationTip(45, Category.ROMANIA, "\uD83D\uDCCA",
                "Infla\u021Bia RO 2024 a m\u00E2ncat ~5% din puterea de cump\u0103rare. Banii \u021Binu\u021Bi sub saltea pierd valoare garantat."),
            FinancialEducationTip(46, Category.ROMANIA, "\uD83C\uDFDB\uFE0F",
                "Pilonul II de pensii (contribu\u021Bie obligatorie) e banii t\u0103i. Verific\u0103 soldul pe platforma administratorului."),
            FinancialEducationTip(47, Category.ROMANIA, "\uD83D\uDCBC",
                "PFA vs SRL: la venituri sub 100.000 EUR/an, impozitarea difer\u0103 semnificativ. Consult\u0103 un contabil \u00EEnainte s\u0103 alegi."),
            FinancialEducationTip(48, Category.ROMANIA, "\uD83E\uDDFE",
                "Declara\u021Bia 212 (freelanceri/PFA) e o \u0219ans\u0103 s\u0103 regle\u0219ti CAS/CASS. Ignorat\u0103 = penaliz\u0103ri 0,03%/zi."),
            FinancialEducationTip(49, Category.ROMANIA, "\uD83C\uDFE6",
                "Depozitele bancare \u00EEn Rom\u00E2nia sunt garantate p\u00E2n\u0103 la 100.000 EUR prin FGDB. Distribuite la mai multe b\u0103nci dac\u0103 ai mai mult."),
            FinancialEducationTip(50, Category.ROMANIA, "\uD83D\uDCC8",
                "BVB (Bursa de Valori Bucure\u0219ti) are dividende medii de 6-8%/an, printre cele mai mari din Europa. Aproape necunoscut."),
            FinancialEducationTip(51, Category.ROMANIA, "\uD83C\uDF0D",
                "Revolut nu e banc\u0103 reglementat\u0103 complet \u00EEn RO. Nu \u021Bine acolo suma principal\u0103 de economii."),
            FinancialEducationTip(52, Category.ROMANIA, "\uD83D\uDCB3",
                "Cardul de credit cu cashback folosit inteligent = randament de 1-3% pe cheltuielile normale. Dar ZERO dac\u0103 nu pl\u0103te\u0219ti integral lunar."),

            FinancialEducationTip(53, Category.INVESTITII, "\uD83C\uDF31",
                "Investi\u021Biile nu sunt pentru boga\u021Bi \u2014 sunt pentru oricine poate pune deoparte 100 RON/lun\u0103."),
            FinancialEducationTip(54, Category.INVESTITII, "\uD83D\uDCC9",
                "Volatilitatea e normal\u0103 \u00EEn investi\u021Bii. Panica \u00EEn momentele de sc\u0103dere e cauza principal\u0103 a pierderilor reale."),
            FinancialEducationTip(55, Category.INVESTITII, "\uD83C\uDF10",
                "ETF-urile (fonduri indexate) ofer\u0103 diversificare global\u0103 cu comisioane mici. Nu necesit\u0103 expertiz\u0103 financiar\u0103."),
            FinancialEducationTip(56, Category.INVESTITII, "\u23F3",
                "Timing-ul pie\u021Bei e imposibil pe termen scurt. \"Time in the market\" bate \"timing the market\" de fiecare dat\u0103."),
            FinancialEducationTip(57, Category.INVESTITII, "\uD83D\uDD04",
                "DCA (Dollar Cost Averaging): investe\u0219ti o sum\u0103 fix\u0103 lunar, indiferent de pia\u021B\u0103. Elimini stresul \u0219i \u00EEmbun\u0103t\u0103\u021Be\u0219ti pre\u021Bul mediu."),
            FinancialEducationTip(58, Category.INVESTITII, "\uD83D\uDC8E",
                "Cel mai mare inamic al investi\u021Biilor nu e pia\u021Ba \u2014 e frica care te face s\u0103 vinzi \u00EEn sc\u0103dere \u0219i s\u0103 cumperi \u00EEn v\u00E2rf."),
            FinancialEducationTip(59, Category.INVESTITII, "\uD83C\uDFD7\uFE0F",
                "Investe\u0219te mai \u00EEnt\u00E2i \u00EEn tine: s\u0103n\u0103tate, cuno\u0219tin\u021Be, rela\u021Bii. Nici o ac\u021Biune nu te pl\u0103te\u0219te ca tine \u00EEns\u021Bi."),
            FinancialEducationTip(60, Category.INVESTITII, "\uD83C\uDFB2",
                "Crypto nu e o investi\u021Bie \u2014 e specula\u021Bie. Dac\u0103 intri, intri cu bani pe care \u021Bi-i permi\u021Bi s\u0103 \u00EE pierzi complet.")
        )

        val today: FinancialEducationTip
            get() {
                val cal = Calendar.getInstance()
                val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                val index = (dayOfYear - 1) % all.size
                return all[index]
            }

        fun randomFor(category: Category): FinancialEducationTip? {
            val pool = all.filter { it.category == category }
            return pool.randomOrNull()
        }
    }
}
