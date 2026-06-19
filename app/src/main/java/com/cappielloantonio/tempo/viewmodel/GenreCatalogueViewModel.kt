package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.subsonic.models.Genre

class GenreCatalogueViewModel(
    private val genreRepository: GenreRepository
) : ViewModel() {

    fun getGenreList(): LiveData<List<Genre>> = genreRepository.getGenres(false, -1)
}
