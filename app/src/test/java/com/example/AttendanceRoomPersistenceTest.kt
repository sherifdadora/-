package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.EduDatabase
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.data.repository.EduRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttendanceRoomPersistenceTest {

    private lateinit var db: EduDatabase
    private lateinit var repository: EduRepository

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, EduDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EduRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveAttendanceSession_persistsAndUpdatesPresentAbsentInRoomWithoutDuplicates() = runBlocking(Dispatchers.IO) {
        val group = GroupEntity(
            id = "g_att_1",
            name = "مجموعة الرياضيات",
            subject = "رياضيات",
            stage = "الثانوية",
            grade = "الصف الثاني الثانوي",
            daysOfWeek = "الأحد، الأربعاء",
            sessionTime = "04:00 م",
            location = "قاعة 1"
        )
        val student1 = StudentEntity(
            id = "st_att_1",
            fullName = "أحمد علي",
            phone = "01011112222",
            parentPhone = "01111112222",
            educationalStage = "الثانوية",
            grade = "الصف الثاني الثانوي",
            subject = "رياضيات",
            registrationDate = "2026-10-01"
        )
        val student2 = StudentEntity(
            id = "st_att_2",
            fullName = "سارة حسن",
            phone = "01033334444",
            parentPhone = "01133334444",
            educationalStage = "الثانوية",
            grade = "الصف الثاني الثانوي",
            subject = "رياضيات",
            registrationDate = "2026-10-01"
        )
        repository.addGroup(group)
        repository.addStudent(student1)
        repository.addStudent(student2)
        repository.enrollStudentInGroup(student1.id, group.id)
        repository.enrollStudentInGroup(student2.id, group.id)

        val date = "2026-11-15"
        val initialSession = AttendanceSessionEntity(
            id = "sess_1",
            groupId = group.id,
            sessionDate = date,
            sessionTime = "04:00 م",
            lessonTopic = "الدوال"
        )
        val initialRecords = listOf(
            AttendanceRecordEntity(
                id = "rec_1",
                sessionId = initialSession.id,
                studentId = student1.id,
                groupId = group.id,
                status = "حاضر"
            ),
            AttendanceRecordEntity(
                id = "rec_2",
                sessionId = initialSession.id,
                studentId = student2.id,
                groupId = group.id,
                status = "غائب",
                notes = "ظرف عائلي"
            )
        )

        repository.saveAttendanceSession(initialSession, initialRecords)

        val savedSessions = db.attendanceDao().getSessionsForGroup(group.id).first()
        assertEquals(1, savedSessions.size)
        val savedRecs1 = db.attendanceDao().getRecordsForSessionDirect(savedSessions.first().id)
        assertEquals(2, savedRecs1.size)
        assertEquals("حاضر", savedRecs1.find { it.studentId == student1.id }?.status)
        assertEquals("غائب", savedRecs1.find { it.studentId == student2.id }?.status)

        // Re-save for same group + date with a different session UUID -> updates existing Room session cleanly
        val updatedSession = AttendanceSessionEntity(
            id = "sess_new_id",
            groupId = group.id,
            sessionDate = date,
            sessionTime = "04:00 م",
            lessonTopic = "الدوال - مراجعة"
        )
        val updatedRecords = listOf(
            AttendanceRecordEntity(
                id = "rec_3",
                sessionId = updatedSession.id,
                studentId = student1.id,
                groupId = group.id,
                status = "غائب"
            ),
            AttendanceRecordEntity(
                id = "rec_4",
                sessionId = updatedSession.id,
                studentId = student2.id,
                groupId = group.id,
                status = "حاضر"
            )
        )

        repository.saveAttendanceSession(updatedSession, updatedRecords)

        val allSessionsAfterUpdate = db.attendanceDao().getSessionsForGroup(group.id).first()
        assertEquals(1, allSessionsAfterUpdate.size)
        assertEquals("الدوال - مراجعة", allSessionsAfterUpdate.first().lessonTopic)

        val existingLookup = db.attendanceDao().getSessionByGroupAndDate(group.id, date)
        assertNotNull(existingLookup)

        val savedRecs2 = db.attendanceDao().getRecordsForSessionDirect(allSessionsAfterUpdate.first().id)
        assertEquals(2, savedRecs2.size)
        assertEquals("غائب", savedRecs2.find { it.studentId == student1.id }?.status)
        assertEquals("حاضر", savedRecs2.find { it.studentId == student2.id }?.status)
    }
}
