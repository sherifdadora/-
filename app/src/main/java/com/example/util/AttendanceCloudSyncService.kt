package com.example.util

import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Real HTTP/REST cloud sync client for queued local attendance sheets.
 * Sends structured JSON payloads via OkHttp to the configured cloud sync endpoint.
 */
class AttendanceCloudSyncService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    sealed interface UploadResult {
        data class Success(val statusCode: Int, val responseBody: String) : UploadResult
        data class Failure(val errorMessage: String) : UploadResult
    }

    sealed interface SyncBatchResult {
        data object NothingPending : SyncBatchResult
        data class SyncedAll(val syncedCount: Int) : SyncBatchResult
        data class OfflineQueued(val pendingCount: Int) : SyncBatchResult
        data class EndpointNotConfigured(val pendingCount: Int) : SyncBatchResult
        data class PartialOrFailed(
            val syncedCount: Int,
            val failedCount: Int,
            val lastError: String
        ) : SyncBatchResult
    }

    suspend fun uploadAttendanceSession(
        endpointUrl: String,
        session: AttendanceSessionEntity,
        records: List<AttendanceRecordEntity>,
        group: GroupEntity?,
        studentMap: Map<String, StudentEntity>,
        centerName: String
    ): UploadResult = withContext(Dispatchers.IO) {
        val normalizedUrl = endpointUrl.trim()
        if (!isValidCloudEndpoint(normalizedUrl)) {
            return@withContext UploadResult.Failure("رابط المزامنة السحابية غير صالح (يجب أن يبدأ بـ https:// أو http://)")
        }

        val payloadJson = buildSessionPayloadJson(
            session = session,
            records = records,
            group = group,
            studentMap = studentMap,
            centerName = centerName
        )

        return@withContext try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payloadJson.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(normalizedUrl)
                .post(body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    UploadResult.Success(response.code, responseText)
                } else {
                    UploadResult.Failure("استجابة الخادم ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            UploadResult.Failure(e.localizedMessage ?: "تعذر الاتصال بالخادم السحابي")
        }
    }

    companion object {
        fun isValidCloudEndpoint(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isBlank()) return false
            return (trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true)) &&
                trimmed.length > 10 &&
                !trimmed.contains(" ")
        }

        fun buildSessionPayloadJson(
            session: AttendanceSessionEntity,
            records: List<AttendanceRecordEntity>,
            group: GroupEntity?,
            studentMap: Map<String, StudentEntity>,
            centerName: String
        ): String {
            val recordsArray = JSONArray()
            records.forEach { rec ->
                val student = studentMap[rec.studentId]
                val recObj = JSONObject().apply {
                    put("recordId", rec.id)
                    put("studentId", rec.studentId)
                    put("studentName", student?.fullName ?: rec.studentId)
                    put("studentPhone", student?.phone ?: "")
                    put("parentPhone", student?.parentPhone ?: "")
                    put("status", rec.status)
                    put("notes", rec.notes)
                    if (!rec.excuseReason.isNullOrBlank()) {
                        put("excuseReason", rec.excuseReason)
                    }
                }
                recordsArray.put(recObj)
            }

            val root = JSONObject().apply {
                put("type", "attendance_session_sync")
                put("sessionId", session.id)
                put("centerName", centerName)
                put("groupId", session.groupId)
                put("groupName", group?.name ?: "")
                put("subject", group?.subject ?: "")
                put("sessionDate", session.sessionDate)
                put("sessionTime", session.sessionTime)
                put("teacherName", session.teacherName)
                put("lessonTopic", session.lessonTopic)
                put("notes", session.notes)
                put("createdAt", session.createdAt)
                put("presentCount", records.count { it.status == "حاضر" })
                put("absentCount", records.count { it.status == "غائب" })
                put("lateCount", records.count { it.status == "متأخر" })
                put("excusedCount", records.count { it.status == "غياب بعذر" })
                put("totalStudents", records.size)
                put("records", recordsArray)
            }
            return root.toString(2)
        }

        fun buildBatchExportJson(
            sessions: List<AttendanceSessionEntity>,
            allRecords: List<AttendanceRecordEntity>,
            groups: List<GroupEntity>,
            students: List<StudentEntity>,
            centerName: String
        ): String {
            val groupMap = groups.associateBy { it.id }
            val studentMap = students.associateBy { it.id }
            val recordsBySession = allRecords.groupBy { it.sessionId }

            val sessionsArray = JSONArray()
            sessions.forEach { session ->
                val sessionRecords = recordsBySession[session.id] ?: emptyList()
                val sessionJsonStr = buildSessionPayloadJson(
                    session = session,
                    records = sessionRecords,
                    group = groupMap[session.groupId],
                    studentMap = studentMap,
                    centerName = centerName
                )
                sessionsArray.put(JSONObject(sessionJsonStr))
            }

            return JSONObject().apply {
                put("type", "attendance_batch_export")
                put("centerName", centerName)
                put("exportedAt", System.currentTimeMillis())
                put("sessionsCount", sessions.size)
                put("sessions", sessionsArray)
            }.toString(2)
        }
    }
}
