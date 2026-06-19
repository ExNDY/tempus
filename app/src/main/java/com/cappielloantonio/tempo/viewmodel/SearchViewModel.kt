package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.repository.SearchingRepository
import com.cappielloantonio.tempo.subsonic.models.PlaylistWithSongs
import com.cappielloantonio.tempo.subsonic.models.SearchResult2
import com.cappielloantonio.tempo.subsonic.models.SearchResult3
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.Dispatchers

@UnstableApi
class SearchViewModel(
    private val searchingRepository: SearchingRepository
) : ViewModel() {
    private var currentQuery: String = ""

    fun getQuery(): String = currentQuery

    fun setQuery(query: String) {
        currentQuery = query

        if (query.isNotEmpty()) {
            insertNewSearch(query)
        }
    }

    fun search2(title: String): LiveData<SearchResult2?> = liveData(Dispatchers.IO) {
        emit(searchingRepository.search2Result(title))
    }

    fun search3(title: String): LiveData<SearchResult3?> = liveData(Dispatchers.IO) {
        emit(searchingRepository.search3Result(title))
    }

    fun searchAllSongs(title: String): LiveData<PlaylistWithSongs> = liveData(Dispatchers.IO) {
        emit(searchingRepository.searchAllSongs(title))
    }

    fun insertNewSearch(search: String) {
        searchingRepository.insert(RecentSearch(search, System.currentTimeMillis() / 1000L))
    }

    fun deleteRecentSearch(search: String) {
        searchingRepository.delete(RecentSearch(search, 0))
    }

    fun getSearchSuggestion(query: String): LiveData<List<String>> = liveData(Dispatchers.IO) {
        emit(searchingRepository.searchSuggestions(query))
    }

    fun getRecentSearchSuggestion(): List<String> {
        return ArrayList(searchingRepository.getRecentSearchSuggestion())
    }
}
