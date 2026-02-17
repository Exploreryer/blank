package com.example.quickdraft.data.remote.drive

import com.example.quickdraft.data.local.NoteEntity
import com.example.quickdraft.data.repository.sha256
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.format.DateTimeParseException

class DriveClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun fetchAllNotes(accessToken: String): List<RemoteNote> {
        val query = "trashed=false and 'appDataFolder' in parents and name contains '.md'"
        val url =
            "https://www.googleapis.com/drive/v3/files?q=${query.encode()}&spaces=appDataFolder&fields=files(id,name,modifiedTime,appProperties)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string().orEmpty()
        val files = json.decodeFromString<DriveFileListResponse>(body).files

        return files.mapNotNull { metadata ->
            val noteId = metadata.appProperties["noteId"] ?: metadata.name.removeSuffix(".md")
            val content = fetchFileContent(accessToken, metadata.id) ?: return@mapNotNull null
            val updatedAt = metadata.appProperties["updatedAt"]?.toLongOrNull()
                ?: metadata.modifiedTime.toEpochMilliOrNow()
            val hash = metadata.appProperties["hash"] ?: sha256(content)
            RemoteNote(
                noteId = noteId,
                fileId = metadata.id,
                content = content,
                updatedAt = updatedAt,
                contentHash = hash
            )
        }
    }

    suspend fun upsertNote(accessToken: String, note: NoteEntity): String? {
        val existing = findFileByNoteId(accessToken, note.id)
        return if (existing != null) {
            updateFileContent(accessToken, existing.id, note)
            existing.id
        } else {
            createFile(accessToken, note)
        }
    }

    private fun findFileByNoteId(accessToken: String, noteId: String): DriveFileMetadata? {
        val query = "trashed=false and 'appDataFolder' in parents and name='${noteId}.md'"
        val url =
            "https://www.googleapis.com/drive/v3/files?q=${query.encode()}&spaces=appDataFolder&fields=files(id,name,modifiedTime,appProperties)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string().orEmpty()
        return json.decodeFromString<DriveFileListResponse>(body).files.firstOrNull()
    }

    private fun createFile(accessToken: String, note: NoteEntity): String? {
        val boundary = "quickdraft-boundary"
        val metadataJson = json.encodeToString(
            mapOf(
                "name" to "${note.id}.md",
                "parents" to listOf("appDataFolder"),
                "appProperties" to mapOf(
                    "noteId" to note.id,
                    "updatedAt" to note.updatedAt.toString(),
                    "hash" to note.contentHash
                )
            )
        )

        val multipart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: text/markdown\r\n\r\n")
            append(note.content)
            append("\r\n--$boundary--")
        }

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "multipart/related; boundary=$boundary")
            .post(multipart.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string().orEmpty()
        return json.decodeFromString<DriveCreateFileResponse>(body).id
    }

    private fun updateFileContent(accessToken: String, fileId: String, note: NoteEntity) {
        val metadataJson = json.encodeToString(
            mapOf(
                "appProperties" to mapOf(
                    "noteId" to note.id,
                    "updatedAt" to note.updatedAt.toString(),
                    "hash" to note.contentHash
                )
            )
        )
        val patchRequest = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .patch(metadataJson.toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(patchRequest).execute().close()

        val contentRequest = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "text/markdown")
            .patch(note.content.toRequestBody("text/markdown".toMediaType()))
            .build()
        httpClient.newCall(contentRequest).execute().close()
    }

    private fun fetchFileContent(accessToken: String, fileId: String): String? {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        return response.body?.string().orEmpty()
    }

    private fun String.encode(): String {
        return java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
    }

    private fun String?.toEpochMilliOrNow(): Long {
        if (this.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(this).toEpochMilli()
        } catch (_: DateTimeParseException) {
            System.currentTimeMillis()
        }
    }
}
