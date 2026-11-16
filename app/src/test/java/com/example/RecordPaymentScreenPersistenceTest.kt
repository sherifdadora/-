package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.EduDatabase
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.data.repository.EduRepository
import com.example.ui.screens.PaymentInputHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordPaymentScreenPersistenceTest {

    @Test
    fun paymentInputHelper_validatesAmountsAndResolvesArabicMonth() {
        assertNull(PaymentInputHelper.validateAmount("250"))
        assertNull(PaymentInputHelper.validateAmount("٣٠٠"))
        assertNotNull(PaymentInputHelper.validateAmount(""))
        assertNotNull(PaymentInputHelper.validateAmount("0"))
        assertNotNull(PaymentInputHelper.validateAmount("-50"))
        assertNotNull(PaymentInputHelper.validateAmount("abc"))

        assertEquals(300.0, PaymentInputHelper.parsePositiveAmount("٣٠٠")!!, 0.01)
        assertEquals("نوفمبر", PaymentInputHelper.arabicMonthNameForDate("2026-11-15"))
        assertEquals("مارس", PaymentInputHelper.arabicMonthNameForDate("2026-03-01"))
    }

    @Test
    fun savingStudentPayment_persistsPaymentAndSubscriptionInRoomDatabase() = runBlocking(Dispatchers.IO) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, EduDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = EduRepository(db)
            val group = GroupEntity(
                id = "grp_1",
                name = "مجموعة الرياضيات",
                subject = "رياضيات",
                stage = "المرحلة الثانوية",
                grade = "الصف الثالث الثانوي",
                daysOfWeek = "السبت",
                sessionTime = "05:00 م",
                location = "قاعة أ",
                defaultMonthlyFee = 300.0
            )
            val student = StudentEntity(
                id = "stu_1",
                fullName = "كريم محمود",
                phone = "01012345678",
                parentPhone = "01112345678",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثالث الثانوي",
                subject = "رياضيات",
                registrationDate = "2026-11-01",
                monthlyFee = 300.0
            )
            db.groupDao().insertGroup(group)
            db.studentDao().insertStudent(student)
            db.enrollmentDao().insertEnrollment(
                EnrollmentEntity(studentId = student.id, groupId = group.id, joinedDate = "2026-11-01")
            )

            val saved = repository.recordStudentSubscriptionAndPayment(
                studentId = student.id,
                groupId = group.id,
                monthName = PaymentInputHelper.arabicMonthNameForDate("2026-11-15"),
                academicYear = "2026 - 2027",
                expectedFee = 300.0,
                amountPaid = 300.0,
                paymentDate = "2026-11-15",
                nextDueDate = "",
                paymentMethod = "نقدًا",
                notes = "سداد كامل",
                operator = "المعلم",
                explicitMonthStatus = null
            )

            assertTrue(saved)

            val payments = db.paymentDao().getPaymentsForStudent(student.id).first()
            assertEquals(1, payments.size)
            assertEquals(300.0, payments.first().amountPaid, 0.01)
            assertEquals("2026-11-15", payments.first().paymentDateFormatted)
            assertEquals("نقدًا", payments.first().paymentMethod)

            val subs = db.subscriptionDao().getSubscriptionsListForStudent(student.id)
            assertEquals(1, subs.size)
            assertEquals("مدفوع", subs.first().status)
            assertEquals("نوفمبر 2026", subs.first().monthYear)
            assertEquals("2026-11", subs.first().monthSortKey)
        } finally {
            db.close()
        }
    }

    @Test
    fun quickPayMonthAndCancel_persistsImmediatelyAndCreateMonthlyDoesNotOverwritePaid() = runBlocking(Dispatchers.IO) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, EduDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = EduRepository(db)
            val group = GroupEntity(
                id = "grp_qp",
                name = "مجموعة الكيمياء",
                subject = "كيمياء",
                stage = "المرحلة الثانوية",
                grade = "الصف الثاني الثانوي",
                daysOfWeek = "الأحد",
                sessionTime = "04:00 م",
                location = "قاعة ب",
                defaultMonthlyFee = 250.0
            )
            val student = StudentEntity(
                id = "stu_qp",
                fullName = "سارة علي",
                phone = "01022223333",
                parentPhone = "01122223333",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثاني الثانوي",
                subject = "كيمياء",
                registrationDate = "2026-10-01",
                status = "نشط",
                monthlyFee = 250.0
            )
            db.groupDao().insertGroup(group)
            db.studentDao().insertStudent(student)
            db.enrollmentDao().insertEnrollment(
                EnrollmentEntity(studentId = student.id, groupId = group.id, joinedDate = "2026-10-01")
            )

            // One-tap month payment (e.g., tapping أكتوبر)
            val paidOk = repository.quickPayMonthForStudent(
                studentId = student.id,
                monthName = "أكتوبر",
                academicYear = "",
                groupId = group.id,
                amountOverride = 250.0,
                paymentDate = "2026-10-05",
                paymentMethod = "نقدًا",
                notes = "دفع بالضغط على الشهر"
            )
            assertTrue(paidOk)

            val subsAfterQuickPay = db.subscriptionDao().getSubscriptionsListForStudent(student.id)
            val octSub = subsAfterQuickPay.find { it.monthYear.contains("أكتوبر") }
            assertNotNull(octSub)
            assertEquals("مدفوع", octSub?.status)
            assertEquals("2026-10", octSub?.monthSortKey)

            val paymentsAfterQuickPay = db.paymentDao().getPaymentsListForSubscription(octSub!!.id)
            assertEquals(1, paymentsAfterQuickPay.size)
            assertEquals(250.0, paymentsAfterQuickPay.first().amountPaid, 0.01)
            assertEquals("2026-10-05", paymentsAfterQuickPay.first().paymentDateFormatted)

            // Bulk monthly subscription generation for أكتوبر must NOT overwrite existing paid subscription
            repository.createMonthlySubscriptions(
                monthYear = "أكتوبر 2026",
                monthSortKey = "2026-10",
                academicYear = ""
            )
            val subsAfterBulk = db.subscriptionDao().getSubscriptionsListForStudent(student.id)
            assertEquals(1, subsAfterBulk.size)
            assertEquals("مدفوع", subsAfterBulk.first().status)
            assertEquals(octSub.id, subsAfterBulk.first().id)

            // Canceling payment reverts status to "غير مدفوع" and deletes payment rows
            val canceledOk = repository.cancelMonthPaymentForStudent(student.id, "أكتوبر")
            assertTrue(canceledOk)
            val subsAfterCancel = db.subscriptionDao().getSubscriptionsListForStudent(student.id)
            assertEquals("غير مدفوع", subsAfterCancel.first().status)
            assertEquals(0, db.paymentDao().getPaymentsListForSubscription(octSub.id).size)
        } finally {
            db.close()
        }
    }
}
