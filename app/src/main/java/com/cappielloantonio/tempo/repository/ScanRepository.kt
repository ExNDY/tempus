package com.cappielloantonio.tempo.repository
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class ScanRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)
    fun startScan(callback: ScanCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.startScan()
            withContext(Dispatchers.Main) {
                if (response != null) {
                    if (response.error != null) {
                        callback.onError(Exception(response.error?.message))
                    } else if (response.scanStatus != null) {
                        callback.onSuccess(response.scanStatus?.isScanning ?: false, response.scanStatus?.count ?: 0)
                    }
                } else {
                    callback.onError(Exception("Empty response"))
                }
            }
        }
    }
    fun getScanStatus(callback: ScanCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getScanStatus()
            withContext(Dispatchers.Main) {
                if (response != null) {
                    if (response.error != null) {
                        callback.onError(Exception(response.error?.message))
                    } else if (response.scanStatus != null) {
                        callback.onSuccess(response.scanStatus?.isScanning ?: false, response.scanStatus?.count ?: 0)
                    }
                } else {
                    callback.onError(Exception("Empty response"))
                }
            }
        }
    }
}
