package ro.solomon.app.services

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class IngestionEvent {
    data class BankNotificationIngested(val count: Int, val original: String) : IngestionEvent()
    data class ShareIntentIngested(val count: Int) : IngestionEvent()
    data class ErrorOccurred(val source: String, val message: String) : IngestionEvent()
}

object IngestionEventBus {
    private val _events = MutableSharedFlow<IngestionEvent>(replay = 0, extraBufferCapacity = 16)
    val events: SharedFlow<IngestionEvent> = _events.asSharedFlow()

    fun publish(event: IngestionEvent) {
        _events.tryEmit(event)
    }
}
