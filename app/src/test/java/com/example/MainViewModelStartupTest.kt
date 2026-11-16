package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.EduDatabase
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.data.repository.EduRepository
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class MainViewModelStartupTest {

    @Test
    fun mainViewModel_initializesWithoutInitOrderNullPointerException() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        ShadowLooper.idleMainLooper()

        assertNotNull(viewModel.syncStatus.value)
        assertFalse(viewModel.isSyncing.value)
    }

    @Test
    fun navigateBack_returnsToExactPreviousScreensInOrder() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        ShadowLooper.idleMainLooper()

        assertEquals(ScreenDestination.DASHBOARD, viewModel.currentScreen.value)

        viewModel.navigateTo(ScreenDestination.STUDENTS)
        assertEquals(ScreenDestination.STUDENTS, viewModel.currentScreen.value)

        viewModel.openAddStudentScreen(ScreenDestination.STUDENTS)
        assertEquals(ScreenDestination.ADD_STUDENT, viewModel.currentScreen.value)

        assertTrue(viewModel.navigateBack())
        assertEquals(ScreenDestination.STUDENTS, viewModel.currentScreen.value)

        assertTrue(viewModel.navigateBack())
        assertEquals(ScreenDestination.DASHBOARD, viewModel.currentScreen.value)

        assertFalse(viewModel.navigateBack())
    }

    @Test
    fun checkAndSeedInitialDemoData_startsEmptyAndDeletesLegacyDemoRecords() = runBlocking(Dispatchers.IO) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, EduDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            // Insert legacy demo records + one real user-created group and student
            db.groupDao().insertGroups(
                listOf(
                    GroupEntity(
                        id = "group_physics_3sec",
                        name = "مجموعة النخبة",
                        subject = "فيزياء",
                        stage = "الثانوية",
                        grade = "3 ثانوي",
                        daysOfWeek = "السبت",
                        sessionTime = "04:00 م",
                        location = "قاعة 1"
                    ),
                    GroupEntity(
                        id = "user_custom_group_1",
                        name = "مجموعتي الخاصة",
                        subject = "رياضيات",
                        stage = "الإعدادية",
                        grade = "2 إعدادي",
                        daysOfWeek = "الأحد",
                        sessionTime = "05:00 م",
                        location = "قاعة 2"
                    )
                )
            )
            db.studentDao().insertStudents(
                listOf(
                    StudentEntity(
                        id = "s1",
                        fullName = "أحمد محمد محمود السيد",
                        phone = "01012345678",
                        parentPhone = "01123456789",
                        educationalStage = "الثانوية",
                        grade = "3 ثانوي",
                        subject = "فيزياء",
                        registrationDate = "2024-09-01"
                    ),
                    StudentEntity(
                        id = "user_custom_student_1",
                        fullName = "طالب حقيقي",
                        phone = "01000000001",
                        parentPhone = "01100000001",
                        educationalStage = "الإعدادية",
                        grade = "2 إعدادي",
                        subject = "رياضيات",
                        registrationDate = "2025-01-01"
                    )
                )
            )

            val repository = EduRepository(db)
            repository.checkAndSeedInitialDemoData()

            val remainingGroups = db.groupDao().getAllGroups().first()
            val remainingStudents = db.studentDao().getAllStudents().first()

            assertEquals(1, remainingGroups.size)
            assertEquals("user_custom_group_1", remainingGroups.first().id)

            assertEquals(1, remainingStudents.size)
            assertEquals("user_custom_student_1", remainingStudents.first().id)
            assertTrue(db.settingsDao().getSettingsDirect() != null)
        } finally {
            db.close()
        }
    }
}
