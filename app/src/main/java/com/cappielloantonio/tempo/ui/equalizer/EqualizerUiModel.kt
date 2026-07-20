package com.cappielloantonio.tempo.ui.equalizer

data class EqualizerUiState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBandUiModel> = emptyList(),
    val isSupported: Boolean = true,
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500
)

data class EqualizerBandUiModel(
    val id: Short,
    val frequency: Int,
    val level: Int
)
