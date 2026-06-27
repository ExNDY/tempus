package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.di.getDirectoryViewModel
import com.cappielloantonio.tempo.di.getViewModel
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import com.cappielloantonio.tempo.util.Constants
import com.google.common.util.concurrent.ListenableFuture
import java.util.ArrayList

@UnstableApi
class DirectoryFragment : Fragment() {
    private lateinit var activity: MainActivity
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        activity = requireActivity() as MainActivity
        val directoryId = arguments?.getString(Constants.MUSIC_DIRECTORY_ID) ?: return ComposeView(requireContext())
        val directoryName = arguments?.getString(Constants.MUSIC_DIRECTORY_NAME)
        val breadcrumb = arguments?.getString(Constants.MUSIC_DIRECTORY_BREADCRUMB)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TempusTheme {
                    val viewModel = getViewModel { getDirectoryViewModel().apply { onStart(directoryId) } }
                    val uiState by viewModel.uiState.collectAsState()
                    val currentTitle = directoryName ?: uiState.directory?.name ?: getString(R.string.settings_music_directory)
                    val currentBreadcrumb = breadcrumb ?: currentTitle
                    val playableChildren = uiState.children.filter { !it.isDir }

                    DirectoryScreen(
                        title = currentTitle,
                        breadcrumb = currentBreadcrumb,
                        children = uiState.children,
                        isLoading = uiState.isLoading,
                        onNavigateBack = { findNavController().navigateUp() },
                        onItemClick = { child ->
                            if (child.isDir) {
                                findNavController().navigate(
                                    R.id.directoryFragment,
                                    Bundle().apply {
                                        putString(Constants.MUSIC_DIRECTORY_ID, child.id)
                                        putString(Constants.MUSIC_DIRECTORY_NAME, child.title)
                                        putString(
                                            Constants.MUSIC_DIRECTORY_BREADCRUMB,
                                            listOf(currentBreadcrumb, child.title.orEmpty())
                                                .filter { it.isNotBlank() }
                                                .joinToString(" / ")
                                        )
                                    }
                                )
                            } else {
                                MediaManager.startQueue(
                                    mediaBrowserListenableFuture,
                                    ArrayList(playableChildren),
                                    playableChildren.indexOfFirst { it.id == child.id }
                                )
                                activity.setBottomSheetInPeek(true)
                            }
                        },
                        onItemLongClick = { child ->
                            if (!child.isDir) {
                                findNavController().navigate(
                                    R.id.songBottomSheetDialog,
                                    Bundle().apply { putSerializable(Constants.TRACK_OBJECT, child) }
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(requireContext(), ComponentName(requireContext(), MediaService::class.java))
        ).buildAsync()
    }

    override fun onStop() {
        mediaBrowserListenableFuture?.let { MediaBrowser.releaseFuture(it) }
        super.onStop()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DirectoryScreen(
    title: String,
    breadcrumb: String,
    children: List<Child>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onItemClick: (Child) -> Unit,
    onItemLongClick: (Child) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues()
            ) {
                item {
                    Text(
                        text = breadcrumb,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                itemsIndexed(children, key = { _, child -> child.id }) { index, child ->
                    ListItem(
                        modifier = Modifier.combinedClickable(
                            onClick = { onItemClick(child) },
                            onLongClick = { onItemLongClick(child) },
                        ),
                        headlineContent = { Text(child.title.orEmpty()) },
                        leadingContent = {
                            Icon(
                                if (child.isDir) Icons.Default.Folder else Icons.Default.MusicNote,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}
