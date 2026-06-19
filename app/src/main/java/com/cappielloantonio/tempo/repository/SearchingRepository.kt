package com.cappielloantonio.tempo.repository

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.PlaylistWithSongs
import com.cappielloantonio.tempo.subsonic.models.SearchResult2
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import com.cappielloantonio.tempo.util.Preferences
import com.cappielloantonio.tempo.ui.fragment.SearchFragment
import kotlinx.coroutines.*

@UnstableApi
class SearchingRepository {
    private val recentSearchDao = AppDatabase.getInstance().recentSearchDao()
    private val subsonicRepository: SubsonicRepository = App.get(SubsonicRepository::class.java)

    fun search2(query: String): MutableLiveData<SearchResult2?> {
        val result = MutableLiveData<SearchResult2?>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.search3(query, 20, 0, 20, 0, 20, 0)
            result.postValue(response?.searchResult2)
        }
        return result
    }

    fun search3(sf: SearchFragment, query: String): MutableLiveData<SearchResult3?> {
        val result = MutableLiveData<SearchResult3?>()

        CoroutineScope(Dispatchers.IO).launch {
            val allSongs = mutableListOf<Child>()
            var offset = 0
            val limit = 1000
            var hasMore = true

            while (hasMore) {
                val response = subsonicRepository.search3(query, limit, offset, 0, 0, 0, 0)
                val fetchedSongs = response?.searchResult3?.songs
                if (!fetchedSongs.isNullOrEmpty()) {
                    allSongs.addAll(fetchedSongs)
                    offset += fetchedSongs.size
                    hasMore = fetchedSongs.size == limit
                } else {
                    hasMore = false
                }
            }

            val pws = PlaylistWithSongs("allsongs", allSongs).apply {
                songCount = allSongs.size
                duration = allSongs.sumOf { it.duration?.toLong() ?: 0L }
            }

            withContext(Dispatchers.Main) {
                if (sf.view != null && sf.isAdded) {
                    pws.name = sf.view?.context?.getString(R.string.search_all_songs, allSongs.size.toString())
                    sf.updateUI(listOf(pws))
                }
            }

            val response = subsonicRepository.search3(query, 20, 0, 20, 0, 20, 0)
            result.postValue(response?.searchResult3)
        }

        return result
    }

    fun getSuggestions(query: String): MutableLiveData<List<String>> {
        val suggestions = MutableLiveData<List<String>>()
        CoroutineScope(Dispatchers.IO).launch {
            val response = subsonicRepository.search3(query, 5, 0, 5, 0, 5, 0)
            val newSuggestions = mutableListOf<String>()
            response?.searchResult3?.let { sr3 ->
                sr3.artists?.forEach { it.name?.let { name -> newSuggestions.add(name) } }
                sr3.albums?.forEach { it.name?.let { name -> newSuggestions.add(name) } }
                sr3.songs?.forEach { it.title?.let { title -> newSuggestions.add(title) } }
            }
            suggestions.postValue(newSuggestions.distinct())
        }
        return suggestions
    }

    fun insert(recentSearch: RecentSearch) {
        CoroutineScope(Dispatchers.IO).launch {
            recentSearchDao.insert(recentSearch)
        }
    }

    fun delete(recentSearch: RecentSearch) {
        CoroutineScope(Dispatchers.IO).launch {
            recentSearchDao.delete(recentSearch)
        }
    }

    fun getRecentSearchSuggestion(): List<String> {
        return runBlocking(Dispatchers.IO) {
            if (Preferences.isSearchSortingChronologicallyEnabled()) {
                recentSearchDao.recent
            } else {
                recentSearchDao.alpha
            }
        }
    }
}
