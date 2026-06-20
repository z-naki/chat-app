package com.chatapp.domain.analyze

import com.chatapp.domain.model.Attachment

interface MultimodalAnalyzer {
    suspend fun analyze(file: Attachment): Result<FileContent>
}
