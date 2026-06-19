package com.cappielloantonio.tempo.ui.state

sealed interface UiEvent {
    data class ShowMessage(val message: UiText) : UiEvent
    data object CloseDialog : UiEvent
}
