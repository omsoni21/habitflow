package com.example.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.CelebrationEvent
import com.example.domain.usecase.MilestoneCelebrationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CelebrationOverlayViewModel @Inject constructor(
    celebrationBus: MilestoneCelebrationBus
) : ViewModel() {

    private val _activeCelebration = MutableStateFlow<CelebrationEvent?>(null)
    val activeCelebration: StateFlow<CelebrationEvent?> = _activeCelebration.asStateFlow()

    init {
        viewModelScope.launch {
            celebrationBus.events.collect { event ->
                _activeCelebration.value = event
            }
        }
    }

    fun dismiss() {
        _activeCelebration.value = null
    }
}
