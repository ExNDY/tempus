package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.repository.ScanRepository

class SettingViewModel(
    private val scanRepository: ScanRepository
) : ViewModel() {

    fun launchScan(callback: ScanCallback) {
        scanRepository.startScan(callback)
    }

    fun getScanStatus(callback: ScanCallback) {
        scanRepository.getScanStatus(callback)
    }
}
