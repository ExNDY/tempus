package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getIndexViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.subsonic.models.Artist
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.IndexUiState
import org.koin.core.context.GlobalContext
import java.util.ArrayList

@UnstableApi
class IndexFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val musicFolder = arguments?.getSerializable(Constants.MUSIC_FOLDER_OBJECT) as? MusicFolder

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getIndexViewModel().apply { onStart(musicFolder?.id) } }
                    val uiState by viewModel.uiState.collectAsState()

                    IndexScreen(
                        uiState = uiState,
                        title = musicFolder?.name ?: stringResource(R.string.nav_drawer_index),
                        onNavigateBack = { findNavController().navigateUp() },
                        onArtistClick = { artist ->
                            findNavController().navigate(
                                R.id.directoryFragment,
                                Bundle().apply { putString(Constants.MUSIC_DIRECTORY_ID, artist.id) },
                            )
                        },
                        onArtistPlayClick = { artist ->
                            val directoryId = artist.id
                            if (!directoryId.isNullOrEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.folder_play_collecting),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                collectAndPlayDirectorySongs(directoryId)
                            }
                        },
                    )
                }
            }
        }
    }

    private fun collectAndPlayDirectorySongs(directoryId: String) {
        val directoryRepository = GlobalContext.get().get<com.cappielloantonio.tempo.repository.DirectoryRepository>()
        val collectedSongs = linkedSetOf<Child>()

        fun loadDirectory(id: String, onComplete: () -> Unit) {
            directoryRepository.getMusicDirectory(id).observe(viewLifecycleOwner) { directory ->
                val children = directory?.children.orEmpty()
                children.forEach { child ->
                    if (!child.isDir && !child.isVideo) {
                        collectedSongs.add(child)
                    }
                }

                val subdirectories = children.filter { it.isDir && !it.id.isNullOrEmpty() }
                if (subdirectories.isEmpty()) {
                    onComplete()
                    return@observe
                }

                var remaining = subdirectories.size
                subdirectories.forEach { child ->
                    loadDirectory(child.id) {
                        remaining -= 1
                        if (remaining == 0) {
                            onComplete()
                        }
                    }
                }
            }
        }

        loadDirectory(directoryId) {
            val activity = requireActivity() as MainActivity
            if (collectedSongs.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.folder_play_no_songs),
                    Toast.LENGTH_SHORT,
                ).show()
                return@loadDirectory
            }

            MediaManager.startQueue(
                activity.mediaBrowserListenableFuture,
                ArrayList(collectedSongs),
                0,
            )
            activity.setBottomSheetInPeek(true)
            Toast.makeText(
                requireContext(),
                getString(R.string.folder_play_playing, collectedSongs.size),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun IndexScreen(
    uiState: IndexUiState,
    title: String,
    onNavigateBack: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onArtistPlayClick: (Artist) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.indices.forEach { index ->
                    item {
                        Text(
                            text = index.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(index.artists.orEmpty(), key = { artist -> artist.id.orEmpty() + artist.name.orEmpty() }) { artist ->
                        ListItem(
                            modifier = Modifier.clickable { onArtistClick(artist) },
                            headlineContent = { Text(artist.name.orEmpty()) },
                            trailingContent = {
                                IconButton(onClick = { onArtistPlayClick(artist) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
