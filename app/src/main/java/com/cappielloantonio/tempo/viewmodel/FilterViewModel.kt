package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre

class FilterViewModel(
    private val genreRepository: GenreRepository
) : ViewModel() {

    private val selectedFiltersID = arrayListOf<String>()
    private val selectedFilters = arrayListOf<String>()

    fun getGenreList(): LiveData<List<Genre>> = genreRepository.getGenres(false, -1)

    fun addFilter(filterID: String, filterName: String) {
        selectedFiltersID.add(filterID)
        selectedFilters.add(filterName)
    }

    fun removeFilter(filterID: String, filterName: String) {
        selectedFiltersID.remove(filterID)
        selectedFilters.remove(filterName)
    }

    fun getFilters(): ArrayList<String> = selectedFiltersID

    fun getFilterNames(): ArrayList<String> = selectedFilters
}
