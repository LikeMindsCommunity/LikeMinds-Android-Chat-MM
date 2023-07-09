package com.likeminds.chatmm.media.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class MediaPickerExtras private constructor(
    val senderName: String?,
    @InternalMediaType val mediaTypes: List<String>,
    val allowMultipleSelect: Boolean,
) : Parcelable {

    class Builder {
        private var senderName: String? = null

        @InternalMediaType
        private var mediaTypes: List<String> = emptyList()
        var allowMultipleSelect: Boolean = true

        fun senderName(senderName: String?) = apply { this.senderName = senderName }
        fun mediaTypes(@InternalMediaType mediaTypes: List<String>) =
            apply { this.mediaTypes = mediaTypes }

        fun allowMultipleSelect(allowMultipleSelect: Boolean) =
            apply { this.allowMultipleSelect = allowMultipleSelect }

        fun build() = MediaPickerExtras(senderName, mediaTypes, allowMultipleSelect)
    }

    fun toBuilder(): Builder {
        return Builder().senderName(senderName)
            .allowMultipleSelect(allowMultipleSelect)
            .mediaTypes(mediaTypes)
    }
}