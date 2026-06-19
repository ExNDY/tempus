package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.AppDatabase
import com.cappielloantonio.tempo.database.dao.RecentSearchDao
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.repository.subsonic.SubsonicRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PlaylistWithSongs
import com.cappielloantonio.tempo.subsonic.models.SearchResult2
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import com.cappielloantonio.tempo.util.Preferences
import kotlinx.coroutines.*

@UnstableApi
class SearchingRepository(
    private val recentSearchDao: RecentSearchDao,
    private val subsonicRepository: SubsonicRepository,
    private val preferences: Preferences
) {
    constructor() : this(
        AppDatabase.getInstance().recentSearchDao(),
        App.get(SubsonicRepository::class.java),
        Preferences
    )

    suspend fun search2Result(query: String): SearchResult2? {
        return subsonicRepository.search3(query, 20, 0, 20, 0, 20, 0)?.searchResult2
    }

    suspend fun search3Result(query: String): SearchResult3? {
        return subsonicRepository.search3(query, 20, 0, 20, 0, 20, 0)?.searchResult3
    }

    suspend fun searchAllSongs(query: String): PlaylistWithSongs {
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

        return PlaylistWithSongs("allsongs", allSongs).apply {
            songCount = allSongs.size
            duration = allSongs.sumOf { it.duration?.toLong() ?: 0L }
        }
    }

    suspend fun searchSuggestions(query: String): List<String> {
        val response = subsonicRepository.search3(query, 5, 0, 5, 0, 5, 0)
        val newSuggestions = mutableListOf<String>()
        response?.searchResult3?.let { sr3 ->
            sr3.artists?.forEach { it.name?.let { name -> newSuggestions.add(name) } }
            sr3.albums?.forEach { it.name?.let { name -> newSuggestions.add(name) } }
            sr3.songs?.forEach { it.title?.let { title -> newSuggestions.add(title) } }
        }
        return newSuggestions.distinct()
    }

    fun search2(query: String): LiveData<SearchResult2?> = liveData(Dispatchers.IO) {
        emit(search2Result(query))
    }

    fun search3(query: String): LiveData<SearchResult3?> = liveData(Dispatchers.IO) {
        emit(search3Result(query))
    }

    fun getSuggestions(query: String): LiveData<List<String>> = liveData(Dispatchers.IO) {
        emit(searchSuggestions(query))
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
            if (preferences.isSearchSortingChronologicallyEnabled()) {
                recentSearchDao.recent
            } else {
                recentSearchDao.alpha
            }
        }
    }
}
