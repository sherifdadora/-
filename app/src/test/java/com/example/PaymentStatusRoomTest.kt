package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.EduDatabase
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.data.repository.EduRepository
import com.example.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class PaymentStatusRoomTest {

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
    fun repository_recordPaymentAndTogglePaidUnpaid_persistsInRoom() = runBlocking(Dispatchers.IO) {
        val group = GroupEntity(
            id = "g_test",
            name = "مجموعة الفيزياء",
            subject = "فيزياء",
            stage = "المرحلة الثانوية",
            grade = "الصف الثالث الثانوي",
            daysOfWeek = "السبت، الثلاثاء",
            sessionTime = "05:00 م",
            location = "قاعة أ",
            defaultMonthlyFee = 300.0
        )
        val student = StudentEntity(
            id = "st_test_1",
            fullName = "عمر خالد",
            phone = "01011112222",
            parentPhone = "01033334444",
            educationalStage = "المرحلة الثانوية",
            grade = "الصف الثالث الثانوي",
            subject = "فيزياء",
            registrationDate = "2025-01-01",
            status = "نشط",
            monthlyFee = 300.0
        )
        repository.addGroup(group)
        repository.addStudent(student)
        repository.enrollStudentInGroup(student.id, group.id, 300.0)

        // 1. Mark March as Paid with payment
        val paidResult = repository.recordStudentSubscriptionAndPayment(
            studentId = student.id,
            groupId = group.id,
            monthName = "مارس",
            academicYear = "2024 - 2025",
            expectedFee = 300.0,
            amountPaid = 300.0,
            paymentDate = "2025-03-02",
            nextDueDate = "2025-04-01",
            paymentMethod = "نقدًا",
            notes = "سداد كامل",
            explicitMonthStatus = "مدفوع"
        )
        assertTrue(paidResult)

        val subsAfterPaid = db.subscriptionDao().getSubscriptionsListForStudent(student.id)
        val marchSubPaid = subsAfterPaid.find { it.monthYear.contains("مارس") }
        assertNotNull(marchSubPaid)
        assertEquals("مدفوع", marchSubPaid?.status)

        val paymentsAfterPaid = db.paymentDao().getPaymentsListForSubscription(marchSubPaid!!.id)
        assertEquals(300.0, paymentsAfterPaid.sumOf { it.amountPaid }, 0.01)

        // 2. Update the same month to Unpaid ("غير مدفوع")
        val unpaidResult = repository.recordStudentSubscriptionAndPayment(
            studentId = student.id,
            groupId = group.id,
            monthName = "مارس",
            academicYear = "2024 - 2025",
            expectedFee = 300.0,
            amountPaid = 0.0,
            paymentDate = "2025-03-02",
            nextDueDate = "2025-04-01",
            paymentMethod = "نقدًا",
            notes = "تغيير الحالة إلى غير مدفوع",
            explicitMonthStatus = "غير مدفوع"
        )
        assertTrue(unpaidResult)

        val subsAfterUnpaid = db.subscriptionDao().getSubscriptionsListForStudent(student.id)
        val marchSubUnpaid = subsAfterUnpaid.find { it.monthYear.contains("مارس") }
        assertNotNull(marchSubUnpaid)
        assertEquals("غير مدفوع", marchSubUnpaid?.status)

        val paymentsAfterUnpaid = db.paymentDao().getPaymentsListForSubscription(marchSubUnpaid!!.id)
        assertEquals(0.0, paymentsAfterUnpaid.sumOf { it.amountPaid }, 0.01)

        val updatedStudent = db.studentDao().getStudentById(student.id).first()
        assertEquals("2025-04-01", updatedStudent?.nextPaymentDueDate)
    }

    @Test
    fun mainViewModel_recordPaymentAndMarkUnpaid_updatesSubscriptionsFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        val collectorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job = collectorScope.launch {
            viewModel.subscriptions.collect {}
        }

        try {
            ShadowLooper.idleMainLooper()

            val testStudent = StudentEntity(
                id = "vm_st_1",
                fullName = "زياد محمود",
                phone = "01055556666",
                parentPhone = "01077778888",
                educationalStage = "المرحلة الإعدادية",
                grade = "الصف الثالث الإعدادي",
                subject = "رياضيات",
                registrationDate = "2025-02-01",
                status = "نشط",
                monthlyFee = 200.0
            )
            viewModel.addStudent(testStudent, null)

            repeat(40) {
                ShadowLooper.idleMainLooper()
                Thread.sleep(25)
            }

            // 1. Mark April Paid -> UI StateFlow must reflect "مدفوع" immediately
            viewModel.recordStudentSubscriptionAndPayment(
                studentId = testStudent.id,
                groupId = "",
                monthName = "أبريل",
                academicYear = "2024 - 2025",
                expectedFee = 200.0,
                amountPaid = 200.0,
                paymentDate = "2025-04-01",
                nextDueDate = "2025-05-01",
                paymentMethod = "نقدًا",
                notes = "",
                explicitMonthStatus = "مدفوع"
            )

            var aprilStatusPaid: String? = null
            repeat(60) {
                ShadowLooper.idleMainLooper()
                aprilStatusPaid = viewModel.subscriptions.value
                    .find { it.studentId == testStudent.id && it.monthYear.contains("أبريل") }
                    ?.status
                if (aprilStatusPaid == "مدفوع") return@repeat
                Thread.sleep(25)
            }
            assertEquals("مدفوع", aprilStatusPaid)

            // Wait briefly for Room write completion, then verify DB row directly
            Thread.sleep(200)
            ShadowLooper.idleMainLooper()

            // 2. Toggle April to Unpaid ("غير مدفوع") -> UI StateFlow must reflect "غير مدفوع" immediately
            viewModel.recordStudentSubscriptionAndPayment(
                studentId = testStudent.id,
                groupId = "",
                monthName = "أبريل",
                academicYear = "2024 - 2025",
                expectedFee = 200.0,
                amountPaid = 0.0,
                paymentDate = "2025-04-01",
                nextDueDate = "2025-05-01",
                paymentMethod = "نقدًا",
                notes = "",
                explicitMonthStatus = "غير مدفوع"
            )

            var aprilStatusUnpaid: String? = null
            repeat(60) {
                ShadowLooper.idleMainLooper()
                aprilStatusUnpaid = viewModel.subscriptions.value
                    .find { it.studentId == testStudent.id && it.monthYear.contains("أبريل") }
                    ?.status
                if (aprilStatusUnpaid == "غير مدفوع") return@repeat
                Thread.sleep(25)
            }
            assertEquals("غير مدفوع", aprilStatusUnpaid)
        } finally {
            job.cancel()
            collectorScope.cancel()
        }
    }
}
