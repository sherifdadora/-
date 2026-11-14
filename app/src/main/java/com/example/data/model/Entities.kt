package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val phone: String,
    val parentPhone: String,
    val educationalStage: String,
    val grade: String,
    val subject: String,
    val registrationDate: String,
    val status: String = "نشط", // نشط، موقوف، منسحب
    val monthlyFee: Double = 250.0,
    val notes: String = "",
    val isArchived: Boolean = false,
    val nextPaymentDueDate: String = "" // موعد الدفع القادم (YYYY-MM-DD)
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subject: String,
    val stage: String,
    val grade: String,
    val daysOfWeek: String, // e.g. "السبت، الثلاثاء"
    val sessionTime: String, // e.g. "04:00 مساءً"
    val location: String, // e.g. "سنتر التفوق - قاعة 1"
    val defaultMonthlyFee: Double = 250.0,
    val whatsappLink: String = "",
    val maxStudents: Int = 30,
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(
    tableName = "enrollments",
    indices = [Index(value = ["studentId", "groupId"], unique = true)]
)
data class EnrollmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val groupId: String,
    val joinedDate: String,
    val customMonthlyFee: Double? = null,
    val isActive: Boolean = true
)

@Entity(
    tableName = "monthly_subscriptions",
    indices = [Index(value = ["studentId", "groupId", "monthYear", "academicYear"], unique = true)]
)
data class MonthlySubscriptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val groupId: String,
    val monthYear: String, // e.g. "يناير 2025"
    val monthSortKey: String = "", // e.g. "2025-01" for ordering
    val academicYear: String = "2024 - 2025",
    val expectedAmount: Double = 250.0,
    val status: String = "لم يدفع", // لم يدفع، دفع جزئي، مدفوع بالكامل، معفى
    val exemptionReason: String? = null,
    val nextDueDate: String = "" // موعد الدفع القادم (YYYY-MM-DD)
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class PaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subscriptionId: String,
    val studentId: String,
    val groupId: String,
    val amountPaid: Double,
    val paymentMethod: String = "نقدًا", // نقدًا، تحويل بنكي، محفظة إلكترونية، أخرى
    val paymentTimestamp: Long = System.currentTimeMillis(),
    val paymentDateFormatted: String, // YYYY-MM-DD HH:mm
    val notes: String = "",
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val recordedBy: String = "المعلم",
    val nextPaymentDueDate: String = "" // موعد الدفعة القادمة المحدد عند السداد
)

@Entity(tableName = "attendance_sessions")
data class AttendanceSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val sessionDate: String, // YYYY-MM-DD
    val sessionTime: String,
    val teacherName: String = "",
    val lessonTopic: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncStatus: String = "بانتظار المزامنة",
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["sessionId", "studentId"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val studentId: String,
    val groupId: String,
    val status: String = "حاضر", // حاضر، غائب، متأخر، غياب بعذر
    val notes: String = "",
    val excuseReason: String? = null
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val teacherName: String = "",
    val centerName: String = "سنتر التفوق والريادة التعليمي",
    val currency: String = "جنيه",
    val academicYear: String = "2024 - 2025",
    val defaultFee: Double = 250.0,
    val absenceAlertThreshold: Int = 3,
    val userRole: String = "المعلم",
    val isDarkMode: Boolean = false,
    val welcomeMessageTemplate: String = "أهلًا وسهلًا بك في مجموعتنا التعليمية، نتمنى لك التوفيق والنجاح.",
    val feeReminderTemplate: String = "نذكركم بسداد اشتراك الشهر الحالي، شكرًا لتعاونكم مع إدارة السنتر.",
    val absenceNoticeTemplate: String = "نحيطكم علمًا بغياب الطالب عن الحصة اليوم، ونرجو متابعة انتظامه في الحضور والتواصل معنا في حال وجود عذر.",
    val paymentConfirmationTemplate: String = "تم بحمد الله تسجيل سداد اشتراك الطالب بنجاح، شكرًا لتعاونكم وثقتكم بنا.",
    val cloudSyncUrl: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String,
    val description: String,
    val performedBy: String = "المدير"
)
