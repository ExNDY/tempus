package com.cappielloantonio.tempo.repository
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class SharingRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)
    fun getShares(): MutableLiveData<List<Share>> {
        val shares = MutableLiveData<List<Share>>(ArrayList())
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getShares()
            shares.postValue(response?.shares?.shares ?: emptyList())
        }
        return shares
    }
    fun createShare(id: String, description: String?, expires: Long?): MutableLiveData<Share?> {
        val share = MutableLiveData<Share?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.createShare(id, description, expires)
            val sharesList = response?.shares?.shares
            if (!sharesList.isNullOrEmpty()) {
                share.postValue(sharesList[0])
            } else {
                share.postValue(null)
            }
        }
        return share
    }
    fun updateShare(id: String, description: String?, expires: Long?) {
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.updateShare(id, description, expires)
        }
    }
    fun deleteShare(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            subsonicRepository.deleteShare(id)
        }
    }
}
