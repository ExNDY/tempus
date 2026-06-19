package com.cappielloantonio.tempo.viewmodel

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences

class DownloadViewModel(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val downloadedTrackSample = MutableLiveData<List<Child>?>(null)
    private val viewStack = MutableLiveData<ArrayList<DownloadStack>?>(null)
    private val refreshResult = MutableLiveData<Int>()

    init {
        initViewStack(DownloadStack(Preferences.getDefaultDownloadViewType(), null))
    }

    fun getDownloadedTracks(owner: androidx.lifecycle.LifecycleOwner): LiveData<List<Child>?> {
        downloadRepository.getLiveDownload().observe(owner) { downloads ->
            downloadedTrackSample.postValue(downloads.map { it as Child })
        }
        return downloadedTrackSample
    }

    fun getViewStack(): LiveData<ArrayList<DownloadStack>?> = viewStack

    fun getRefreshResult(): LiveData<Int> = refreshResult

    fun initViewStack(level: DownloadStack) {
        viewStack.value = arrayListOf(level)
    }

    fun pushViewStack(level: DownloadStack) {
        val stack = viewStack.value ?: arrayListOf()
        stack.add(level)
        viewStack.value = stack
    }

    fun popViewStack() {
        val stack = viewStack.value ?: return
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
            viewStack.value = stack
        }
    }

    fun refreshExternalDownloads() {
        Thread {
            val directoryUri = Preferences.getDownloadDirectoryUri()
            if (directoryUri == null) {
                refreshResult.postValue(-1)
                return@Thread
            }

            val downloads = downloadRepository.getAllDownloads()
            if (downloads.isNullOrEmpty()) {
                refreshResult.postValue(0)
                return@Thread
            }

            val toRemove = arrayListOf<Download>()

            for (download in downloads) {
                val uriString = download.downloadUri
                if (uriString.isNullOrEmpty()) continue

                val uri = Uri.parse(uriString)
                if (uri.scheme.isNullOrEmpty() || !uri.scheme.equals("content", ignoreCase = true)) continue

                val file = try {
                    DocumentFile.fromSingleUri(App.getContext(), uri)
                } catch (_: SecurityException) {
                    null
                }

                if (file == null || !file.exists()) {
                    toRemove.add(download)
                }
            }

            if (toRemove.isNotEmpty()) {
                val ids = arrayListOf<String>()
                for (download in toRemove) {
                    ids.add(download.id)
                    ExternalAudioReader.removeMetadata(download)
                }

                downloadRepository.delete(ids)
                ExternalAudioReader.refreshCache()
                refreshResult.postValue(ids.size)
            } else {
                refreshResult.postValue(0)
            }
        }.start()
    }
}
