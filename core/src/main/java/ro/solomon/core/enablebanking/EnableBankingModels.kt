package ro.solomon.core.enablebanking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ASPSP(
    val name: String,
    val country: String,
    val logo: String? = null,
    @SerialName("psu_types") val psuTypes: List<String>? = null,
    @SerialName("auth_methods") val authMethods: List<AuthMethod>? = null,
    @SerialName("maximum_consent_validity") val maximumConsentValidity: Int? = null,
    val bic: String? = null,
    val beta: Boolean? = null,
    @SerialName("required_psu_headers") val requiredPsuHeaders: List<String>? = null,
)

@Serializable
data class AuthMethod(
    val name: String,
    val title: String? = null,
    @SerialName("psu_type") val psuType: String? = null,
    val credentials: List<Credential>? = null,
)

@Serializable
data class Credential(
    val name: String,
    val title: String? = null,
    val required: Boolean? = null,
    val protected: Boolean? = null,
)

@Serializable
data class ASPSPList(
    val aspsps: List<ASPSP>,
)

@Serializable
data class AuthRequest(
    val access: AccessScope,
    val aspsp: ASPSPRef,
    val state: String,
    @SerialName("redirect_url") val redirectUrl: String,
    @SerialName("psu_type") val psuType: String,
    @SerialName("auth_method") val authMethod: String? = null,
    val credentials: Map<String, String>? = null,
    val language: String? = null,
)

@Serializable
data class AccessScope(
    @SerialName("valid_until") val validUntil: String,
)

@Serializable
data class ASPSPRef(
    val name: String,
    val country: String,
)

@Serializable
data class AuthResponse(
    val url: String,
    @SerialName("authorization_id") val authorizationId: String,
    @SerialName("psu_id_hash") val psuIdHash: String? = null,
)

@Serializable
data class SessionCreateRequest(
    val code: String,
)

@Serializable
data class SessionResponse(
    @SerialName("session_id") val sessionId: String,
    val accounts: List<Account>,
    val aspsp: ASPSPRef,
    val access: AccessScope,
)

@Serializable
data class Account(
    val uid: String,
    @SerialName("account_id") val accountId: AccountID? = null,
    val name: String? = null,
    val currency: String? = null,
    @SerialName("cash_account_type") val cashAccountType: String? = null,
    val product: String? = null,
)

@Serializable
data class AccountID(
    val iban: String? = null,
    val bban: String? = null,
    val other: AccountOther? = null,
)

@Serializable
data class AccountOther(
    val identification: String? = null,
    @SerialName("scheme_name") val schemeName: String? = null,
)

@Serializable
data class BalancesResponse(
    val balances: List<Balance>,
)

@Serializable
data class Balance(
    val name: String? = null,
    @SerialName("balance_amount") val balanceAmount: Amount,
    @SerialName("balance_type") val balanceType: String? = null,
    @SerialName("reference_date") val referenceDate: String? = null,
)

@Serializable
data class Amount(
    val currency: String,
    val amount: String,
)

@Serializable
data class TransactionsResponse(
    val transactions: List<BankTransaction>,
    @SerialName("continuation_key") val continuationKey: String? = null,
)

@Serializable
data class BankTransaction(
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("entry_reference") val entryReference: String? = null,
    @SerialName("booking_date") val bookingDate: String? = null,
    @SerialName("value_date") val valueDate: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null,
    @SerialName("transaction_amount") val transactionAmount: Amount,
    val creditor: Party? = null,
    val debtor: Party? = null,
    @SerialName("creditor_account") val creditorAccount: AccountID? = null,
    @SerialName("debtor_account") val debtorAccount: AccountID? = null,
    @SerialName("remittance_information") val remittanceInformation: List<String>? = null,
    @SerialName("bank_transaction_code") val bankTransactionCode: String? = null,
    @SerialName("merchant_category_code") val merchantCategoryCode: String? = null,
    @SerialName("transaction_status") val transactionStatus: String? = null,
)

@Serializable
data class Party(
    val name: String? = null,
)

@Serializable
data class EnableBankingAPIError(
    val error: String? = null,
    val message: String? = null,
    val code: String? = null,
)
