package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.Share

class ShareBottomSheetViewModel(
    private val sharingRepository: SharingRepository
) : ViewModel() {

    private var share: Share? = null

    fun getShare(): Share? = share

    fun setShare(share: Share?) {
        this.share = share
    }

    fun updateShare(description: String?, expires: Long) {
        val currentShare = share ?: return
        val shareId = currentShare.id ?: return
        sharingRepository.updateShare(shareId, description, expires)
    }

    fun deleteShare() {
        val currentShare = share ?: return
        val shareId = currentShare.id ?: return
        sharingRepository.deleteShare(shareId)
    }
}
