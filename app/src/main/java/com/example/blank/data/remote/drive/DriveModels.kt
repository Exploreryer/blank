package com.example.blank.data.remote.drive

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriveFileListResponse(
    val files: List<DriveFileMetadata> = emptyList()
)

@Serializable
data class DriveFileMetadata(
    val id: String,
    val name: String,
    @SerialName("modifiedTime")
    val modifiedTime: String? = null,
    @SerialName("appProperties")
    val appProperties: Map<String, String> = emptyMap()
)

@Serializable
data class DriveCreateFileResponse(
    val id: String
)

data class RemoteNote(
    val noteId: String,
    val fileId: String,
    val content: String,
    val updatedAt: Long,
    val contentHash: String
)
