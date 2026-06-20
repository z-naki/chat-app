package com.chatapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val id: String,
    val type: AttachmentType,
    val name: String,
    val mimeType: String,
    val dataBase64: String = "",
    val localPath: String = ""
)

@Serializable
enum class AttachmentType {
    IMAGE, FILE
}
