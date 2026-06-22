package ro.solomon.core.enablebanking

object EnableBankingConfig {
    val baseURL = "https://api.enablebanking.com"
    val redirectURL = "solomon://bankcallback"

    var applicationID: String? = null
    var privateKeyPEM: String? = null

    val isConfigured: Boolean
        get() = applicationID != null && privateKeyPEM != null
}
