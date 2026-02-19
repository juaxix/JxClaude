package com.jx.claude.models

import com.google.gson.annotations.SerializedName

data class ContentBlock(
    val type: String,
    val text: String? = null,
    val source: ImageSource? = null
)

data class ImageSource(
    val type: String = "base64",
    @SerializedName("media_type")
    val mediaType: String,
    val data: String
)

data class Attachment(
    val mimeType: String,
    val base64Data: String,
    val fileName: String? = null
)