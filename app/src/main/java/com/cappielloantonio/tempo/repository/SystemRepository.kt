package com.cappielloantonio.tempo.repository
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.github.models.LatestRelease
import com.cappielloantonio.tempo.interfaces.SystemCallback
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.cappielloantonio.tempo.subsonic.models.ResponseStatus
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class SystemRepository @JvmOverloads constructor(
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)
) {
    suspend fun ping(): SubsonicResponse? = withContext(Dispatchers.IO) {
        subsonicRepository.ping()
    }
    suspend fun getOpenSubsonicExtensions(): List<OpenSubsonicExtension>? = withContext(Dispatchers.IO) {
        subsonicRepository.getOpenSubsonicExtensions()?.openSubsonicExtensions
    }
    fun checkUserCredential(callback: SystemCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.ping()
            withContext(Dispatchers.Main) {
                if (response != null) {
                    if (response.status == ResponseStatus.FAILED) {
                        callback.onError(Exception("${response.error?.code} - ${response.error?.message}"))
                    } else if (response.status == ResponseStatus.OK) {
                        callback.onSuccess("", "", "")
                    } else {
                        callback.onError(Exception("Empty response"))
                    }
                } else {
                    callback.onError(Exception("Network error"))
                }
            }
        }
    }
    fun checkTempoUpdate(): MutableLiveData<LatestRelease?> {
        val latestRelease = MutableLiveData<LatestRelease?>(null)
        // TODO: Implement update check using Ktor when needed for the fork
        return latestRelease
    }
}
