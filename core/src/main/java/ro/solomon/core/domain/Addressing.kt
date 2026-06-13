package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Addressing {
    tu, dumneavoastra;

    val subjectPronoun: String get() = when (this) {
        tu -> "tu"
        dumneavoastra -> "dumneavoastră"
    }

    val canVerb: String get() = when (this) {
        tu -> "poți"
        dumneavoastra -> "puteți"
    }

    val imperativeSuffix: String get() = when (this) {
        tu -> "tu_form"
        dumneavoastra -> "formal"
    }
}
