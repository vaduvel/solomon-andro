package ro.solomon.app.services

/**
 * Demo / seed data has been removed.
 *
 * Solomon operates exclusively on real, user-owned data ingested from:
 *  - bank connections (Enable Banking / Open Banking)
 *  - the bank notification listener
 *  - share-intent parsing
 *  - email transaction parsing
 *  - manual entry / chat tools
 *
 * This object is retained only as a no-op so any lingering reference keeps
 * compiling; it never fabricates transactions, obligations, goals or
 * subscriptions.
 */
@Deprecated("Demo data removed — Solomon uses only real ingested data.")
object DemoDataGenerator {
    /** No-op. Kept for source compatibility; intentionally generates nothing. */
    suspend fun seedIfEmpty() {
        // Intentionally empty: no demo / fake data is ever created.
    }
}
