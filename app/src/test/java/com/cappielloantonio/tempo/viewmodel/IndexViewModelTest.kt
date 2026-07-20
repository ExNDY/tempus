package com.cappielloantonio.tempo.viewmodel

import com.cappielloantonio.tempo.subsonic.models.Artist
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Index
import com.cappielloantonio.tempo.subsonic.models.Indexes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IndexViewModelTest {

    @Test
    fun toIndexUiState_keepsChildrenAndIndexedArtists() {
        val child = Child(
            id = "child-folder",
            isDir = true,
            title = "Folder",
        )
        val artist = Artist(
            id = "artist-directory",
            name = "Artist",
        )
        val index = Index().apply {
            name = "A"
            artists = listOf(artist)
        }
        val indexes = Indexes().apply {
            indices = listOf(index)
            children = listOf(child)
        }

        val state = indexes.toIndexUiState(musicFolderId = "music-folder")

        assertEquals("music-folder", state.musicFolderId)
        assertEquals(listOf(index), state.indices)
        assertEquals(listOf(child), state.children)
        assertFalse(state.isLoading)
    }

    @Test
    fun toIndexUiState_keepsChildrenWhenIndicesAreEmpty() {
        val child = Child(
            id = "root-song",
            isDir = false,
            title = "Song",
        )
        val indexes = Indexes().apply {
            indices = emptyList()
            children = listOf(child)
        }

        val state = indexes.toIndexUiState(musicFolderId = "music-folder")

        assertEquals(emptyList<Index>(), state.indices)
        assertEquals(listOf(child), state.children)
        assertFalse(state.isLoading)
    }

    @Test
    fun toIndexUiState_handlesNullIndexesAsLoadedEmptyState() {
        val state = null.toIndexUiState(musicFolderId = null)

        assertEquals(emptyList<Index>(), state.indices)
        assertEquals(emptyList<Child>(), state.children)
        assertFalse(state.isLoading)
    }
}
