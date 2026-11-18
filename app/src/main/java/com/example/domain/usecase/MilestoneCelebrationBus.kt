package com.example.domain.usecase

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilestoneCelebrationBus @Inject constructor() {
    private val _events = MutableSharedFlow<CelebrationEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<CelebrationEvent> = _events.asSharedFlow()

    fun emit(event: CelebrationEvent) {
        _events.tryEmit(event)
    }
}
