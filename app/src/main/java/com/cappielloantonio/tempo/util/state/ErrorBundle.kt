package com.cappielloantonio.tempo.util.state

import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.desc.StringDesc

/**
 * Error data container that supports title, description and icon for error
 */
data class ErrorBundle(
    val title: StringDesc,
    val message: StringDesc,
    val icon: ImageResource,
)
