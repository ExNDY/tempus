package com.cappielloantonio.tempo.repository

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.InternetRadioStationCache
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse
import com.cappielloantonio.tempo.util.RadioCoverArtDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@UnstableApi
class RadioRepository {
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun getInternetRadioStations(): MutableLiveData<List<InternetRadioStation>> {
        val radioStation = MutableLiveData<List<InternetRadioStation>>(ArrayList())

        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.getInternetRadioStations()
            val stations = response?.internetRadioStations?.internetRadioStations
            if (stations != null) {
                cacheSubsonicStations(stations)
                mergeWithLocal(radioStation, stations)
            } else {
                fallbackToCache(radioStation)
            }
        }

        return radioStation
    }

    private fun cacheSubsonicStations(stations: List<InternetRadioStation>) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance()
            db.internetRadioStationDao().deleteSubsonic()
            val cacheList = stations.map { InternetRadioStationCache(it) }
            db.internetRadioStationDao().insertAll(cacheList)

            for (cache in cacheList) {
                if (!cache.coverArtUrl.isNullOrEmpty()) {
                    RadioCoverArtDownloader.downloadCoverArt(cache.id, cache.coverArtUrl)
                }
            }
        }
    }

    private fun mergeWithLocal(liveData: MutableLiveData<List<InternetRadioStation>>, subsonicStations: List<InternetRadioStation>) {
        CoroutineScope(Dispatchers.IO).launch {
            val localCaches = AppDatabase.getInstance().internetRadioStationDao().local
            val localStations = localCaches.map { it.toInternetRadioStation() }

            val merged = ArrayList(subsonicStations)
            merged.addAll(localStations)
            sortByName(merged)
            liveData.postValue(merged)
        }
    }

    private fun fallbackToCache(liveData: MutableLiveData<List<InternetRadioStation>>) {
        CoroutineScope(Dispatchers.IO).launch {
            val cached = AppDatabase.getInstance().internetRadioStationDao().all.map { it.toInternetRadioStation() }.toMutableList()
            if (cached.isNotEmpty()) {
                sortByName(cached)
                liveData.postValue(cached)
            }
        }
    }

    private fun sortByName(stations: MutableList<InternetRadioStation>) {
        stations.sortWith(Comparator.comparing(
            { station -> station.name ?: "" },
            String.CASE_INSENSITIVE_ORDER
        ))
    }

    fun createLocalStation(name: String, streamUrl: String, homepageUrl: String?, coverArtUrl: String?, onComplete: Runnable?) {
        CoroutineScope(Dispatchers.IO).launch {
            val id = "local_" + UUID.randomUUID().toString()
            val cache = InternetRadioStationCache().apply {
                this.id = id
                this.name = name
                this.streamUrl = streamUrl
                this.homePageUrl = homepageUrl
                this.source = InternetRadioStationCache.SOURCE_LOCAL
                this.coverArtUrl = coverArtUrl
            }

            AppDatabase.getInstance().internetRadioStationDao().insert(cache)

            if (!coverArtUrl.isNullOrEmpty()) {
                RadioCoverArtDownloader.downloadCoverArt(id, coverArtUrl)
            }

            onComplete?.let { 
                withContext(Dispatchers.Main) {
                    it.run()
                }
            }
        }
    }

    fun updateLocalStation(id: String, name: String, streamUrl: String, homepageUrl: String?, coverArtUrl: String?, onComplete: Runnable?) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance()
            val cache = db.internetRadioStationDao().getById(id)
            if (cache != null) {
                cache.name = name
                cache.streamUrl = streamUrl
                cache.homePageUrl = homepageUrl
                cache.coverArtUrl = coverArtUrl
                db.internetRadioStationDao().update(cache)

                if (!coverArtUrl.isNullOrEmpty()) {
                    RadioCoverArtDownloader.downloadCoverArt(id, coverArtUrl)
                }
            }

            onComplete?.let { 
                withContext(Dispatchers.Main) {
                    it.run()
                }
            }
        }
    }

    fun deleteLocalStation(id: String, onComplete: Runnable?) {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance().internetRadioStationDao().deleteById(id)
            RadioCoverArtDownloader.deleteCoverArt(id)
            onComplete?.let { 
                withContext(Dispatchers.Main) {
                    it.run()
                }
            }
        }
    }

    fun isLocalStation(stationId: String?): Boolean {
        return stationId != null && stationId.startsWith("local_")
    }

    suspend fun createInternetRadioStation(name: String, streamURL: String, homepageURL: String?): SubsonicResponse? {
        return subsonicRepository.createInternetRadioStation(streamURL, name, homepageURL)
    }

    suspend fun updateInternetRadioStation(id: String, name: String, streamURL: String, homepageURL: String?): SubsonicResponse? {
        return subsonicRepository.updateInternetRadioStation(id, streamURL, name, homepageURL)
    }

    suspend fun deleteInternetRadioStation(id: String): SubsonicResponse? {
        return subsonicRepository.deleteInternetRadioStation(id)
    }
}
