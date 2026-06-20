package com.chatapp.domain.analyze

data class FileContent(
    val text: String,
    val pageCount: Int = 1,
    val sourceType: String = ""
)
