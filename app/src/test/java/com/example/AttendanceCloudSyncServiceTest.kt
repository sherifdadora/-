package com.example

import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.util.AttendanceCloudSyncService
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
class AttendanceCloudSyncServiceTest {

    @Test
    fun isValidCloudEndpoint_acceptsHttpAndHttpsUrlsOnly() {
        assertTrue(AttendanceCloudSyncService.isValidCloudEndpoint("https://example.com/api/sync"))
        assertTrue(AttendanceCloudSyncService.isValidCloudEndpoint("http://192.168.1.10:8080/attendance"))
        assertFalse(AttendanceCloudSyncService.isValidCloudEndpoint(""))
        assertFalse(AttendanceCloudSyncService.isValidCloudEndpoint("ftp://example.com/file"))
        assertFalse(AttendanceCloudSyncService.isValidCloudEndpoint("https://example.com/bad url"))
    }

    @Test
    fun buildSessionPayloadJson_includesSummaryCountsAndStudentRecords() {
        val group = GroupEntity(
            id = "g1",
            name = "مجموعة النخبة",
            subject = "فيزياء",
            stage = "ثانوي",
            grade = "3 ثانوي",
            daysOfWeek = "السبت",
            sessionTime = "04:00 م",
            location = "قاعة 1"
        )
        val s1 = StudentEntity(
            id = "s1",
            fullName = "أحمد علي",
            phone = "01011112222",
            parentPhone = "01111112222",
            educationalStage = "ثانوي",
            grade = "3 ثانوي",
            subject = "فيزياء",
            registrationDate = "2025-01-01"
        )
        val s2 = StudentEntity(
            id = "s2",
            fullName = "عمر خالد",
            phone = "01033334444",
            parentPhone = "01133334444",
            educationalStage = "ثانوي",
            grade = "3 ثانوي",
            subject = "فيزياء",
            registrationDate = "2025-01-01"
        )
        val session = AttendanceSessionEntity(
            id = "sess_1",
            groupId = "g1",
            sessionDate = "2025-03-01",
            sessionTime = "04:00 م",
            teacherName = "أ. محمود",
            lessonTopic = "قانون أوم"
        )
        val records = listOf(
            AttendanceRecordEntity(id = "r1", sessionId = "sess_1", studentId = "s1", groupId = "g1", status = "حاضر"),
            AttendanceRecordEntity(id = "r2", sessionId = "sess_1", studentId = "s2", groupId = "g1", status = "غائب", notes = "سفر")
        )

        val jsonStr = AttendanceCloudSyncService.buildSessionPayloadJson(
            session = session,
            records = records,
            group = group,
            studentMap = mapOf(s1.id to s1, s2.id to s2),
            centerName = "سنتر التفوق"
        )

        val parsed = JSONObject(jsonStr)
        assertEquals("attendance_session_sync", parsed.getString("type"))
        assertEquals("sess_1", parsed.getString("sessionId"))
        assertEquals("مجموعة النخبة", parsed.getString("groupName"))
        assertEquals(1, parsed.getInt("presentCount"))
        assertEquals(1, parsed.getInt("absentCount"))
        assertEquals(2, parsed.getInt("totalStudents"))
        assertEquals(2, parsed.getJSONArray("records").length())
    }

    @Test
    fun uploadAttendanceSession_postsJsonAndHandlesServerResponses() = runBlocking {
        var receivedBody = ""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ok") { exchange ->
            receivedBody = exchange.requestBody.bufferedReader().readText()
            val resp = """{"status":"ok"}""".toByteArray()
            exchange.sendResponseHeaders(200, resp.size.toLong())
            exchange.responseBody.use { it.write(resp) }
        }
        server.createContext("/err") { exchange ->
            val resp = """{"error":"server error"}""".toByteArray()
            exchange.sendResponseHeaders(500, resp.size.toLong())
            exchange.responseBody.use { it.write(resp) }
        }
        server.start()

        try {
            val port = server.address.port
            val service = AttendanceCloudSyncService()
            val session = AttendanceSessionEntity(
                id = "sess_upload",
                groupId = "g1",
                sessionDate = "2025-03-01",
                sessionTime = "04:00 م"
            )
            val records = listOf(
                AttendanceRecordEntity(id = "r1", sessionId = "sess_upload", studentId = "s1", groupId = "g1", status = "حاضر")
            )

            val okResult = service.uploadAttendanceSession(
                endpointUrl = "http://127.0.0.1:$port/ok",
                session = session,
                records = records,
                group = null,
                studentMap = emptyMap(),
                centerName = "سنتر التفوق"
            )
            assertTrue(okResult is AttendanceCloudSyncService.UploadResult.Success)
            assertTrue(receivedBody.contains("sess_upload"))

            val errResult = service.uploadAttendanceSession(
                endpointUrl = "http://127.0.0.1:$port/err",
                session = session,
                records = records,
                group = null,
                studentMap = emptyMap(),
                centerName = "سنتر التفوق"
            )
            assertTrue(errResult is AttendanceCloudSyncService.UploadResult.Failure)
        } finally {
            server.stop(0)
        }
    }
}
