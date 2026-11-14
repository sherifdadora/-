package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.EduDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.util.AttendanceCloudSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EduRepository(
    private val db: EduDatabase,
    private val cloudSyncService: AttendanceCloudSyncService = AttendanceCloudSyncService()
) {

    private val syncMutex = Mutex()

    val allStudents: Flow<List<StudentEntity>> = db.studentDao().getAllStudents()
    val allGroups: Flow<List<GroupEntity>> = db.groupDao().getAllGroups()
    val allEnrollments: Flow<List<EnrollmentEntity>> = db.enrollmentDao().getAllEnrollments()
    val allSubscriptions: Flow<List<MonthlySubscriptionEntity>> = db.subscriptionDao().getAllSubscriptions()
    val allPayments: Flow<List<PaymentEntity>> = db.paymentDao().getAllPayments()
    val allSessions: Flow<List<AttendanceSessionEntity>> = db.attendanceDao().getAllSessions()
    val allRecords: Flow<List<AttendanceRecordEntity>> = db.attendanceDao().getAllRecords()
    val settings: Flow<AppSettingsEntity?> = db.settingsDao().getSettings()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllLogs()

    suspend fun addStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        db.studentDao().insertStudent(student)
        logAction("إضافة طالب", "تم إضافة الطالب ${student.fullName} بنجاح")
    }

    suspend fun updateStudent(student: StudentEntity) = withContext(Dispatchers.IO) {
        db.studentDao().updateStudent(student)
        logAction("تعديل طالب", "تم تعديل بيانات الطالب ${student.fullName}")
    }

    suspend fun archiveStudent(id: String, name: String) = withContext(Dispatchers.IO) {
        db.studentDao().archiveStudent(id)
        db.enrollmentDao().deleteEnrollmentsForStudent(id)
        logAction("حذف طالب", "تم حذف الطالب $name وإلغاء انتسابه لجميع المجموعات مع الاحتفاظ بسجلاته المالية والحضور")
    }

    suspend fun deleteStudent(id: String, name: String) = archiveStudent(id, name)

    suspend fun addGroup(group: GroupEntity) = withContext(Dispatchers.IO) {
        db.groupDao().insertGroup(group)
        logAction("إضافة مجموعة", "تم إضافة المجموعة ${group.name}")
    }

    suspend fun updateGroup(group: GroupEntity) = withContext(Dispatchers.IO) {
        db.groupDao().updateGroup(group)
        logAction("تعديل مجموعة", "تم تعديل بيانات المجموعة ${group.name}")
    }

    suspend fun deleteGroup(id: String, name: String) = withContext(Dispatchers.IO) {
        db.groupDao().archiveGroup(id)
        db.enrollmentDao().deleteEnrollmentsForGroup(id)
        logAction("حذف مجموعة", "تم حذف المجموعة $name بنجاح")
    }

    suspend fun enrollStudentInGroup(studentId: String, groupId: String, customFee: Double? = null) = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val enrollment = EnrollmentEntity(
            studentId = studentId,
            groupId = groupId,
            joinedDate = dateFormat.format(Date()),
            customMonthlyFee = customFee,
            isActive = true
        )
        db.enrollmentDao().insertEnrollment(enrollment)
        logAction("تسجيل طالب بمجموعة", "تم ربط الطالب بمعرف $studentId بالمجموعة $groupId")
    }

    suspend fun unenrollStudent(studentId: String, groupId: String) = withContext(Dispatchers.IO) {
        db.enrollmentDao().deleteByStudentAndGroup(studentId, groupId)
        logAction("إلغاء اشتراك بمجموعة", "تم إلغاء انتساب الطالب للمجموعة")
    }

    suspend fun recordPayment(
        subscriptionId: String,
        studentId: String,
        groupId: String,
        amount: Double,
        method: String,
        notes: String,
        operator: String
    ) = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val payment = PaymentEntity(
            subscriptionId = subscriptionId,
            studentId = studentId,
            groupId = groupId,
            amountPaid = amount,
            paymentMethod = method,
            paymentTimestamp = System.currentTimeMillis(),
            paymentDateFormatted = timeFormat.format(Date()),
            notes = notes,
            idempotencyKey = UUID.randomUUID().toString(),
            recordedBy = operator
        )
        db.paymentDao().insertPayment(payment)

        // Calculate total paid so far for this subscription using direct queries to avoid Flow race condition
        val sub = db.subscriptionDao().getSubscriptionDirect(subscriptionId)
        if (sub != null) {
            val payments = db.paymentDao().getPaymentsListForSubscription(subscriptionId)
            val totalPaid = payments.sumOf { it.amountPaid }
            val newStatus = when {
                totalPaid >= sub.expectedAmount -> "مدفوع"
                totalPaid > 0 -> "دفع جزئي"
                else -> "لم يدفع"
            }
            db.subscriptionDao().updateStatus(subscriptionId, newStatus)
        }

        logAction("تسجيل دفعة", "تم تسجيل دفعة بقيمة $amount ج.م بنجاح")
    }

    suspend fun setSubscriptionExemption(subscriptionId: String, reason: String) = withContext(Dispatchers.IO) {
        val sub = db.subscriptionDao().getSubscriptionById(subscriptionId).firstOrNull()
        if (sub != null) {
            val updated = sub.copy(status = "معفى", exemptionReason = reason)
            db.subscriptionDao().updateSubscription(updated)
            logAction("تسجيل إعفاء", "تم إعفاء الاشتراك بسبب: $reason")
        }
    }

    suspend fun saveAttendanceSession(
        session: AttendanceSessionEntity,
        records: List<AttendanceRecordEntity>
    ) = withContext(Dispatchers.IO) {
        val localQueuedSession = session.copy(
            isSynced = false,
            syncStatus = session.syncStatus.ifBlank { "بانتظار المزامنة" },
            syncError = null
        )
        db.attendanceDao().insertSession(localQueuedSession)
        db.attendanceDao().deleteRecordsForSession(localQueuedSession.id)
        db.attendanceDao().insertRecords(records)
        logAction(
            "حفظ كشف حضور محلي",
            "تم حفظ كشف حضور الحصة بتاريخ ${localQueuedSession.sessionDate} لعدد ${records.size} طالب (${localQueuedSession.syncStatus})"
        )
    }

    suspend fun syncPendingAttendanceSessions(
        isOnline: Boolean,
        cloudSyncUrlOverride: String? = null
    ): AttendanceCloudSyncService.SyncBatchResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val unsynced = db.attendanceDao().getUnsyncedSessionsList()
            if (unsynced.isEmpty()) {
                return@withLock AttendanceCloudSyncService.SyncBatchResult.NothingPending
            }

            if (!isOnline) {
                unsynced.forEach { sess ->
                    db.attendanceDao().updateSessionSyncState(
                        sessionId = sess.id,
                        isSynced = false,
                        syncStatus = "بانتظار توفر الإنترنت",
                        lastSyncedAt = sess.lastSyncedAt,
                        syncError = null
                    )
                }
                return@withLock AttendanceCloudSyncService.SyncBatchResult.OfflineQueued(unsynced.size)
            }

            val settings = db.settingsDao().getSettingsDirect()
            val endpoint = (cloudSyncUrlOverride ?: settings?.cloudSyncUrl).orEmpty().trim()
            if (!AttendanceCloudSyncService.isValidCloudEndpoint(endpoint)) {
                unsynced.forEach { sess ->
                    db.attendanceDao().updateSessionSyncState(
                        sessionId = sess.id,
                        isSynced = false,
                        syncStatus = "بانتظار ربط السحابة",
                        lastSyncedAt = sess.lastSyncedAt,
                        syncError = null
                    )
                }
                return@withLock AttendanceCloudSyncService.SyncBatchResult.EndpointNotConfigured(unsynced.size)
            }

            val groupMap = (db.groupDao().getAllGroups().firstOrNull() ?: emptyList()).associateBy { it.id }
            val studentMap = (db.studentDao().getAllStudents().firstOrNull() ?: emptyList()).associateBy { it.id }
            val centerName = settings?.centerName ?: "سنتر التفوق والريادة"

            var syncedCount = 0
            var failedCount = 0
            var lastError = ""

            for (session in unsynced) {
                db.attendanceDao().updateSessionSyncState(
                    sessionId = session.id,
                    isSynced = false,
                    syncStatus = "جاري المزامنة...",
                    lastSyncedAt = session.lastSyncedAt,
                    syncError = null
                )
                val records = db.attendanceDao().getRecordsForSessionDirect(session.id)
                when (
                    val res = cloudSyncService.uploadAttendanceSession(
                        endpointUrl = endpoint,
                        session = session,
                        records = records,
                        group = groupMap[session.groupId],
                        studentMap = studentMap,
                        centerName = centerName
                    )
                ) {
                    is AttendanceCloudSyncService.UploadResult.Success -> {
                        val syncedAt = System.currentTimeMillis()
                        db.attendanceDao().updateSessionSyncState(
                            sessionId = session.id,
                            isSynced = true,
                            syncStatus = "تمت المزامنة",
                            lastSyncedAt = syncedAt,
                            syncError = null
                        )
                        syncedCount++
                    }
                    is AttendanceCloudSyncService.UploadResult.Failure -> {
                        failedCount++
                        lastError = res.errorMessage
                        db.attendanceDao().updateSessionSyncState(
                            sessionId = session.id,
                            isSynced = false,
                            syncStatus = "فشل المزامنة",
                            lastSyncedAt = session.lastSyncedAt,
                            syncError = res.errorMessage
                        )
                    }
                }
            }

            if (syncedCount > 0) {
                logAction("مزامنة سحابية للحضور", "تمت مزامنة عدد $syncedCount كشف حضور محلي مع السحابة بنجاح")
            }

            return@withLock if (failedCount == 0) {
                AttendanceCloudSyncService.SyncBatchResult.SyncedAll(syncedCount)
            } else {
                AttendanceCloudSyncService.SyncBatchResult.PartialOrFailed(
                    syncedCount = syncedCount,
                    failedCount = failedCount,
                    lastError = lastError
                )
            }
        }
    }

    suspend fun updateCloudSyncUrl(url: String) = withContext(Dispatchers.IO) {
        val current = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()
        db.settingsDao().insertOrUpdate(current.copy(cloudSyncUrl = url.trim()))
        logAction(
            "تحديث رابط السحابة",
            if (url.isBlank()) "تم مسح رابط المزامنة السحابية" else "تم تحديث رابط المزامنة السحابية للحضور"
        )
    }

    suspend fun updateSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        db.settingsDao().insertOrUpdate(settings)
        logAction("تحديث الإعدادات", "تم تحديث إعدادات النظام")
    }

    suspend fun createMonthlySubscriptions(
        monthYear: String,
        monthSortKey: String,
        academicYear: String
    ) = withContext(Dispatchers.IO) {
        val enrollments = db.enrollmentDao().getAllEnrollments().firstOrNull() ?: emptyList()
        val groups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
        val groupMap = groups.associateBy { it.id }

        val newSubs = mutableListOf<MonthlySubscriptionEntity>()
        for (enrollment in enrollments) {
            val fee = enrollment.customMonthlyFee ?: groupMap[enrollment.groupId]?.defaultMonthlyFee ?: 250.0
            newSubs.add(
                MonthlySubscriptionEntity(
                    studentId = enrollment.studentId,
                    groupId = enrollment.groupId,
                    monthYear = monthYear,
                    monthSortKey = monthSortKey,
                    academicYear = academicYear,
                    expectedAmount = fee,
                    status = "لم يدفع"
                )
            )
        }
        if (newSubs.isNotEmpty()) {
            db.subscriptionDao().insertSubscriptions(newSubs)
            logAction("إنشاء اشتراكات شهرية", "تم إنشاء ${newSubs.size} اشتراك لشهر $monthYear")
        }
    }

    private fun calculateMonthSortKey(monthName: String): String {
        return when {
            monthName.contains("سبتمبر") -> "2024-09"
            monthName.contains("أكتوبر") || monthName.contains("اكتوبر") -> "2024-10"
            monthName.contains("نوفمبر") -> "2024-11"
            monthName.contains("ديسمبر") -> "2024-12"
            monthName.contains("يناير") -> "2025-01"
            monthName.contains("فبراير") -> "2025-02"
            monthName.contains("مارس") -> "2025-03"
            monthName.contains("أبريل") || monthName.contains("ابريل") -> "2025-04"
            monthName.contains("مايو") -> "2025-05"
            monthName.contains("يونيو") -> "2025-06"
            else -> "2025-01"
        }
    }

    suspend fun quickPayMonthForStudent(
        studentId: String,
        monthName: String,
        academicYear: String = "2024 - 2025"
    ): Boolean = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudentById(studentId).firstOrNull() ?: return@withContext false
        val enrollments = db.enrollmentDao().getEnrollmentsForStudent(studentId).firstOrNull() ?: emptyList()
        val primaryGroupId = enrollments.firstOrNull()?.groupId ?: "group_general"

        val allSubs = db.subscriptionDao().getSubscriptionsListForStudent(studentId)
        val existingSub = allSubs.find { it.monthYear.contains(monthName) }

        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val nowStr = timeFormat.format(Date())

        if (existingSub != null) {
            val payments = db.paymentDao().getPaymentsListForSubscription(existingSub.id)
            val totalPaid = payments.sumOf { it.amountPaid }
            val remaining = (existingSub.expectedAmount - totalPaid).coerceAtLeast(0.0)

            if (remaining > 0) {
                val payment = PaymentEntity(
                    subscriptionId = existingSub.id,
                    studentId = studentId,
                    groupId = existingSub.groupId,
                    amountPaid = remaining,
                    paymentMethod = "نقدًا",
                    paymentTimestamp = System.currentTimeMillis(),
                    paymentDateFormatted = nowStr,
                    notes = "سداد سريع بنقرة واحدة لشهر $monthName",
                    idempotencyKey = UUID.randomUUID().toString(),
                    recordedBy = "المستر"
                )
                db.paymentDao().insertPayment(payment)
                db.subscriptionDao().updateStatus(existingSub.id, "مدفوع")
                logAction("تسجيل سداد سريع", "تم سداد اشتراك شهر $monthName للطالب ${student.fullName}")
                return@withContext true
            }
            return@withContext false
        } else {
            val fee = enrollments.firstOrNull()?.customMonthlyFee ?: student.monthlyFee
            val newSubId = UUID.randomUUID().toString()
            val newSub = MonthlySubscriptionEntity(
                id = newSubId,
                studentId = studentId,
                groupId = primaryGroupId,
                monthYear = "$monthName 2025",
                monthSortKey = calculateMonthSortKey(monthName),
                academicYear = academicYear,
                expectedAmount = fee,
                status = "مدفوع"
            )
            db.subscriptionDao().insertSubscription(newSub)

            val payment = PaymentEntity(
                subscriptionId = newSubId,
                studentId = studentId,
                groupId = primaryGroupId,
                amountPaid = fee,
                paymentMethod = "نقدًا",
                paymentTimestamp = System.currentTimeMillis(),
                paymentDateFormatted = nowStr,
                notes = "سداد سريع بنقرة واحدة لشهر $monthName",
                idempotencyKey = UUID.randomUUID().toString(),
                recordedBy = "المستر"
            )
            db.paymentDao().insertPayment(payment)
            logAction("تسجيل سداد سريع", "تم إنشاء وسداد اشتراك شهر $monthName للطالب ${student.fullName}")
            return@withContext true
        }
    }

    suspend fun cancelMonthPaymentForStudent(
        studentId: String,
        monthName: String
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val student = db.studentDao().getStudentById(studentId).firstOrNull() ?: return@withTransaction false
            val allSubs = db.subscriptionDao().getSubscriptionsListForStudent(studentId)
            val existingSub = allSubs.find { it.monthYear.contains(monthName) } ?: return@withTransaction false

            val payments = db.paymentDao().getPaymentsListForSubscription(existingSub.id)
            for (p in payments) {
                db.paymentDao().deletePayment(p)
            }
            db.subscriptionDao().updateStatus(existingSub.id, "غير مدفوع")
            logAction("تحديث حالة الشهر", "تم تحديث اشتراك شهر $monthName للطالب ${student.fullName} إلى غير مدفوع")
            true
        }
    }

    suspend fun recordDirectStudentPayment(
        studentId: String,
        amount: Double,
        paymentDate: String,
        method: String = "نقدًا",
        notes: String = "",
        monthName: String = "فبراير",
        academicYear: String = "2024 - 2025"
    ): Boolean = withContext(Dispatchers.IO) {
        val student = db.studentDao().getStudentById(studentId).firstOrNull() ?: return@withContext false
        val enrollments = db.enrollmentDao().getEnrollmentsForStudent(studentId).firstOrNull() ?: emptyList()
        val primaryGroupId = enrollments.firstOrNull()?.groupId ?: "group_general"

        val allSubs = db.subscriptionDao().getSubscriptionsListForStudent(studentId)
        val existingSub = allSubs.find { it.monthYear.contains(monthName) }

        val subId = if (existingSub != null) {
            existingSub.id
        } else {
            val fee = enrollments.firstOrNull()?.customMonthlyFee ?: student.monthlyFee
            val newSubId = UUID.randomUUID().toString()
            val newSub = MonthlySubscriptionEntity(
                id = newSubId,
                studentId = studentId,
                groupId = primaryGroupId,
                monthYear = "$monthName 2025",
                monthSortKey = calculateMonthSortKey(monthName),
                academicYear = academicYear,
                expectedAmount = fee,
                status = "لم يدفع"
            )
            db.subscriptionDao().insertSubscription(newSub)
            newSubId
        }

        val parsedTimestamp = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(paymentDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val payment = PaymentEntity(
            subscriptionId = subId,
            studentId = studentId,
            groupId = primaryGroupId,
            amountPaid = amount,
            paymentMethod = method,
            paymentTimestamp = parsedTimestamp,
            paymentDateFormatted = paymentDate,
            notes = notes,
            idempotencyKey = UUID.randomUUID().toString(),
            recordedBy = "المعلم"
        )
        db.paymentDao().insertPayment(payment)

        val sub = db.subscriptionDao().getSubscriptionDirect(subId)
        if (sub != null) {
            val payments = db.paymentDao().getPaymentsListForSubscription(subId)
            val totalPaid = payments.sumOf { it.amountPaid }
            val newStatus = when {
                totalPaid >= sub.expectedAmount -> "مدفوع"
                totalPaid > 0 -> "دفع جزئي"
                else -> "لم يدفع"
            }
            db.subscriptionDao().updateStatus(subId, newStatus)
        }

        logAction("تسجيل دفعة طالب", "تم تسجيل دفعة بقيمة $amount ج.م بتاريخ $paymentDate للطالب ${student.fullName}")
        return@withContext true
    }

    suspend fun recordStudentSubscriptionAndPayment(
        studentId: String,
        groupId: String,
        monthName: String,
        academicYear: String,
        expectedFee: Double,
        amountPaid: Double,
        paymentDate: String,
        nextDueDate: String,
        paymentMethod: String,
        notes: String,
        operator: String = "المعلم",
        explicitMonthStatus: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val student = db.studentDao().getStudentById(studentId).firstOrNull() ?: return@withTransaction false
            val targetGroupId = if (groupId.isNotBlank()) {
                groupId
            } else {
                val enrollments = db.enrollmentDao().getEnrollmentsForStudent(studentId).firstOrNull() ?: emptyList()
                enrollments.firstOrNull()?.groupId ?: "group_general"
            }

            val allSubs = db.subscriptionDao().getSubscriptionsListForStudent(studentId)
            val existingSub = allSubs.find {
                it.monthYear.contains(monthName) && (it.groupId == targetGroupId || groupId.isBlank())
            } ?: allSubs.find { it.monthYear.contains(monthName) }

            val effectiveDueDate = if (nextDueDate.isNotBlank()) nextDueDate else (existingSub?.nextDueDate ?: "")

            val subId = if (existingSub != null) {
                db.subscriptionDao().updateSubscription(
                    existingSub.copy(
                        expectedAmount = if (expectedFee > 0) expectedFee else existingSub.expectedAmount,
                        nextDueDate = effectiveDueDate
                    )
                )
                existingSub.id
            } else {
                val newSubId = UUID.randomUUID().toString()
                val newSub = MonthlySubscriptionEntity(
                    id = newSubId,
                    studentId = studentId,
                    groupId = targetGroupId,
                    monthYear = "$monthName 2025",
                    monthSortKey = calculateMonthSortKey(monthName),
                    academicYear = academicYear,
                    expectedAmount = expectedFee,
                    status = explicitMonthStatus ?: "غير مدفوع",
                    nextDueDate = effectiveDueDate
                )
                db.subscriptionDao().insertSubscription(newSub)
                newSubId
            }

            if (nextDueDate.isNotBlank()) {
                db.studentDao().updateNextPaymentDueDate(studentId, nextDueDate)
            }

            if (explicitMonthStatus == "غير مدفوع") {
                val existingPayments = db.paymentDao().getPaymentsListForSubscription(subId)
                for (p in existingPayments) {
                    db.paymentDao().deletePayment(p)
                }
                db.subscriptionDao().updateStatus(subId, "غير مدفوع")
                logAction(
                    "تحديث حالة الشهر إلى غير مدفوع",
                    "تم تحديث حالة اشتراك شهر $monthName للطالب ${student.fullName} إلى غير مدفوع",
                    operator
                )
                return@withTransaction true
            }

            val parsedTimestamp = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(paymentDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            if (amountPaid > 0) {
                val payment = PaymentEntity(
                    subscriptionId = subId,
                    studentId = studentId,
                    groupId = targetGroupId,
                    amountPaid = amountPaid,
                    paymentMethod = paymentMethod,
                    paymentTimestamp = parsedTimestamp,
                    paymentDateFormatted = paymentDate,
                    notes = notes,
                    idempotencyKey = UUID.randomUUID().toString(),
                    recordedBy = operator,
                    nextPaymentDueDate = nextDueDate
                )
                db.paymentDao().insertPayment(payment)
            } else if (explicitMonthStatus == "مدفوع") {
                val existingPaid = db.paymentDao().getPaymentsListForSubscription(subId).sumOf { it.amountPaid }
                val subNow = db.subscriptionDao().getSubscriptionDirect(subId)
                val targetFee = subNow?.expectedAmount ?: expectedFee
                val toPay = (targetFee - existingPaid).coerceAtLeast(0.0)
                if (toPay > 0) {
                    val payment = PaymentEntity(
                        subscriptionId = subId,
                        studentId = studentId,
                        groupId = targetGroupId,
                        amountPaid = toPay,
                        paymentMethod = paymentMethod,
                        paymentTimestamp = parsedTimestamp,
                        paymentDateFormatted = paymentDate,
                        notes = notes.ifBlank { "تحديد الشهر كمدفوع" },
                        idempotencyKey = UUID.randomUUID().toString(),
                        recordedBy = operator,
                        nextPaymentDueDate = nextDueDate
                    )
                    db.paymentDao().insertPayment(payment)
                }
            }

            val sub = db.subscriptionDao().getSubscriptionDirect(subId)
            if (sub != null) {
                val payments = db.paymentDao().getPaymentsListForSubscription(subId)
                val totalPaid = payments.sumOf { it.amountPaid }
                val newStatus = when {
                    explicitMonthStatus == "مدفوع" -> "مدفوع"
                    totalPaid >= sub.expectedAmount -> "مدفوع"
                    totalPaid > 0 -> "دفع جزئي"
                    else -> "غير مدفوع"
                }
                db.subscriptionDao().updateStatus(subId, newStatus)
            }

            logAction(
                "تسجيل اشتراك ودفع",
                "تم تسجيل دفعة شهر $monthName بمبلغ $amountPaid ج.م للطالب ${student.fullName} (الحالة: ${explicitMonthStatus ?: "تلقائي"})",
                operator
            )
            true
        }
    }

    private suspend fun logAction(action: String, description: String, operator: String = "المعلم") {
        db.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = action,
                description = description,
                performedBy = operator
            )
        )
    }

    suspend fun checkAndSeedInitialDemoData() = withContext(Dispatchers.IO) {
        // Clear any old placeholder teacherName / role directly in SQLite
        db.settingsDao().clearTeacherAndRole()

        val existingStudents = db.studentDao().getAllStudents().firstOrNull()
        if (!existingStudents.isNullOrEmpty()) return@withContext

        // Initialize Settings
        val initialSettings = AppSettingsEntity(
            teacherName = "",
            centerName = "سنتر التفوق والريادة التعليمي",
            currency = "جنيه",
            academicYear = "2024 - 2025",
            defaultFee = 250.0,
            absenceAlertThreshold = 3,
            userRole = "المعلم"
        )
        db.settingsDao().insertOrUpdate(initialSettings)

        // Seed 3 Realistic Groups
        val g1 = GroupEntity(
            id = "group_physics_3sec",
            name = "مجموعة النخبة - فيزياء 3 ثانوي",
            subject = "فيزياء",
            stage = "المرحلة الثانوية",
            grade = "الصف الثالث الثانوي",
            daysOfWeek = "السبت، الثلاثاء",
            sessionTime = "04:30 م",
            location = "قاعة (أ) - سنتر التفوق",
            defaultMonthlyFee = 300.0,
            whatsappLink = "https://chat.whatsapp.com/demo-physics",
            maxStudents = 35,
            notes = "مجموعة مكثفة مع حل مسائل الامتحانات"
        )
        val g2 = GroupEntity(
            id = "group_chem_2sec",
            name = "مجموعة الأوائل - كيمياء 2 ثانوي",
            subject = "كيمياء",
            stage = "المرحلة الثانوية",
            grade = "الصف الثاني الثانوي",
            daysOfWeek = "الأحد، الأربعاء",
            sessionTime = "06:00 م",
            location = "قاعة (ب) - سنتر التفوق",
            defaultMonthlyFee = 250.0,
            whatsappLink = "https://chat.whatsapp.com/demo-chem",
            maxStudents = 30,
            notes = "مجموعة المراجعة الدورية"
        )
        val g3 = GroupEntity(
            id = "group_math_3prep",
            name = "مجموعة الرواد - رياضيات 3 إعدادي",
            subject = "رياضيات",
            stage = "المرحلة الإعدادية",
            grade = "الصف الثالث الإعدادي",
            daysOfWeek = "الإثنين، الخميس",
            sessionTime = "03:00 م",
            location = "قاعة (ج) - سنتر التفوق",
            defaultMonthlyFee = 200.0,
            whatsappLink = "https://chat.whatsapp.com/demo-math",
            maxStudents = 25,
            notes = "تأسيس هندسة وجبر"
        )
        db.groupDao().insertGroups(listOf(g1, g2, g3))

        // Seed 7 Realistic Students
        val students = listOf(
            StudentEntity(
                id = "s1",
                fullName = "أحمد محمد محمود السيد",
                phone = "01012345678",
                parentPhone = "01123456789",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثالث الثانوي",
                subject = "فيزياء",
                registrationDate = "2024-09-01",
                status = "نشط",
                monthlyFee = 300.0,
                notes = "طالب متميز - يحصل على درجات عالية في الكويزات"
            ),
            StudentEntity(
                id = "s2",
                fullName = "سارة خالد إبراهيم حسن",
                phone = "01234567890",
                parentPhone = "01098765432",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثالث الثانوي",
                subject = "فيزياء",
                registrationDate = "2024-09-01",
                status = "نشط",
                monthlyFee = 300.0,
                notes = "منتظمة في الحضور"
            ),
            StudentEntity(
                id = "s3",
                fullName = "عمر طارق عبد الفتاح",
                phone = "01511223344",
                parentPhone = "01144332211",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثالث الثانوي",
                subject = "فيزياء",
                registrationDate = "2024-09-05",
                status = "نشط",
                monthlyFee = 300.0,
                notes = "يحتاج متابعة في حل الواجبات"
            ),
            StudentEntity(
                id = "s4",
                fullName = "ياسمين تامر مصطفى",
                phone = "01055667788",
                parentPhone = "01288776655",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثاني الثانوي",
                subject = "كيمياء",
                registrationDate = "2024-09-10",
                status = "نشط",
                monthlyFee = 250.0,
                notes = "متفوقة"
            ),
            StudentEntity(
                id = "s5",
                fullName = "كريم هاني عبد العزيز",
                phone = "01199887766",
                parentPhone = "01033221144",
                educationalStage = "المرحلة الثانوية",
                grade = "الصف الثاني الثانوي",
                subject = "كيمياء",
                registrationDate = "2024-09-12",
                status = "نشط",
                monthlyFee = 250.0,
                notes = "تأخر مرتين بعذر"
            ),
            StudentEntity(
                id = "s6",
                fullName = "مريم أسامة فاروق",
                phone = "01266554433",
                parentPhone = "01522334455",
                educationalStage = "المرحلة الإعدادية",
                grade = "الصف الثالث الإعدادي",
                subject = "رياضيات",
                registrationDate = "2024-09-15",
                status = "نشط",
                monthlyFee = 200.0,
                notes = "تشارك بفاعلية في الحصة"
            ),
            StudentEntity(
                id = "s7",
                fullName = "يوسف عادل الشناوي",
                phone = "01077889900",
                parentPhone = "01188990011",
                educationalStage = "المرحلة الإعدادية",
                grade = "الصف الثالث الإعدادي",
                subject = "رياضيات",
                registrationDate = "2024-09-18",
                status = "نشط",
                monthlyFee = 200.0,
                notes = "متأخر في سداد اشتراك الشهر الأخير"
            )
        )
        db.studentDao().insertStudents(students)

        // Enrollments
        val enrollments = listOf(
            EnrollmentEntity(studentId = "s1", groupId = "group_physics_3sec", joinedDate = "2024-09-01"),
            EnrollmentEntity(studentId = "s2", groupId = "group_physics_3sec", joinedDate = "2024-09-01"),
            EnrollmentEntity(studentId = "s3", groupId = "group_physics_3sec", joinedDate = "2024-09-05"),
            EnrollmentEntity(studentId = "s4", groupId = "group_chem_2sec", joinedDate = "2024-09-10"),
            EnrollmentEntity(studentId = "s5", groupId = "group_chem_2sec", joinedDate = "2024-09-12"),
            EnrollmentEntity(studentId = "s6", groupId = "group_math_3prep", joinedDate = "2024-09-15"),
            EnrollmentEntity(studentId = "s7", groupId = "group_math_3prep", joinedDate = "2024-09-18")
        )
        db.enrollmentDao().insertEnrollments(enrollments)

        // Monthly Subscriptions (January 2025 and February 2025)
        val sub1 = MonthlySubscriptionEntity(
            id = "sub_s1_jan",
            studentId = "s1",
            groupId = "group_physics_3sec",
            monthYear = "يناير 2025",
            monthSortKey = "2025-01",
            expectedAmount = 300.0,
            status = "مدفوع بالكامل"
        )
        val sub2 = MonthlySubscriptionEntity(
            id = "sub_s1_feb",
            studentId = "s1",
            groupId = "group_physics_3sec",
            monthYear = "فبراير 2025",
            monthSortKey = "2025-02",
            expectedAmount = 300.0,
            status = "دفع جزئي"
        )
        val sub3 = MonthlySubscriptionEntity(
            id = "sub_s2_jan",
            studentId = "s2",
            groupId = "group_physics_3sec",
            monthYear = "يناير 2025",
            monthSortKey = "2025-01",
            expectedAmount = 300.0,
            status = "مدفوع بالكامل"
        )
        val sub4 = MonthlySubscriptionEntity(
            id = "sub_s2_feb",
            studentId = "s2",
            groupId = "group_physics_3sec",
            monthYear = "فبراير 2025",
            monthSortKey = "2025-02",
            expectedAmount = 300.0,
            status = "لم يدفع"
        )
        val sub5 = MonthlySubscriptionEntity(
            id = "sub_s3_feb",
            studentId = "s3",
            groupId = "group_physics_3sec",
            monthYear = "فبراير 2025",
            monthSortKey = "2025-02",
            expectedAmount = 300.0,
            status = "لم يدفع"
        )
        val sub6 = MonthlySubscriptionEntity(
            id = "sub_s4_feb",
            studentId = "s4",
            groupId = "group_chem_2sec",
            monthYear = "فبراير 2025",
            monthSortKey = "2025-02",
            expectedAmount = 250.0,
            status = "مدفوع بالكامل"
        )
        val sub7 = MonthlySubscriptionEntity(
            id = "sub_s5_feb",
            studentId = "s5",
            groupId = "group_chem_2sec",
            monthYear = "فبراير 2025",
            monthSortKey = "2025-02",
            expectedAmount = 250.0,
            status = "دفع جزئي"
        )
        val sub8 = MonthlySubscriptionEntity(
            id = "sub_s7_feb",
            studentId = "s7",
            groupId = "group_math_3prep",
            monthYear = "فبراير 2025",
            monthSortKey = "2025-02",
            expectedAmount = 200.0,
            status = "لم يدفع"
        )
        db.subscriptionDao().insertSubscriptions(listOf(sub1, sub2, sub3, sub4, sub5, sub6, sub7, sub8))

        // Payments
        val payments = listOf(
            PaymentEntity(
                subscriptionId = "sub_s1_jan",
                studentId = "s1",
                groupId = "group_physics_3sec",
                amountPaid = 300.0,
                paymentMethod = "نقدًا",
                paymentTimestamp = System.currentTimeMillis() - 25 * 86400000L,
                paymentDateFormatted = "2025-01-05 16:45",
                notes = "سداد اشتراك شهر يناير كاملاً"
            ),
            PaymentEntity(
                subscriptionId = "sub_s1_feb",
                studentId = "s1",
                groupId = "group_physics_3sec",
                amountPaid = 150.0,
                paymentMethod = "فودافون كاش",
                paymentTimestamp = System.currentTimeMillis() - 5 * 86400000L,
                paymentDateFormatted = "2025-02-02 17:10",
                notes = "دفعة أولى من اشتراك فبراير (متبقي 150 ج.م)"
            ),
            PaymentEntity(
                subscriptionId = "sub_s2_jan",
                studentId = "s2",
                groupId = "group_physics_3sec",
                amountPaid = 300.0,
                paymentMethod = "نقدًا",
                paymentTimestamp = System.currentTimeMillis() - 24 * 86400000L,
                paymentDateFormatted = "2025-01-06 17:00",
                notes = "تم السداد بواسطة ولي الأمر"
            ),
            PaymentEntity(
                subscriptionId = "sub_s4_feb",
                studentId = "s4",
                groupId = "group_chem_2sec",
                amountPaid = 250.0,
                paymentMethod = "إنستاباي",
                paymentTimestamp = System.currentTimeMillis() - 6 * 86400000L,
                paymentDateFormatted = "2025-02-01 19:30",
                notes = "سداد اشتراك الكيمياء لشهر فبراير"
            ),
            PaymentEntity(
                subscriptionId = "sub_s5_feb",
                studentId = "s5",
                groupId = "group_chem_2sec",
                amountPaid = 100.0,
                paymentMethod = "نقدًا",
                paymentTimestamp = System.currentTimeMillis() - 4 * 86400000L,
                paymentDateFormatted = "2025-02-03 18:20",
                notes = "دفعة جزئية - متبقي 150 ج.م"
            )
        )
        db.paymentDao().insertPayments(payments)

        // Seed Attendance Sessions
        val nowMs = System.currentTimeMillis()
        val sess1 = AttendanceSessionEntity(
            id = "sess_phy_1",
            groupId = "group_physics_3sec",
            sessionDate = "2025-02-01",
            sessionTime = "04:30 م",
            teacherName = "المستر",
            lessonTopic = "قانون كيرشوف الأول وتطبيقاته في الدوائر المعقدة",
            isSynced = true,
            syncStatus = "تمت المزامنة",
            lastSyncedAt = nowMs
        )
        val sess2 = AttendanceSessionEntity(
            id = "sess_phy_2",
            groupId = "group_physics_3sec",
            sessionDate = "2025-02-04",
            sessionTime = "04:30 م",
            teacherName = "المستر",
            lessonTopic = "المجال المغناطيسي لسلك مستقيم وملف دائري",
            isSynced = true,
            syncStatus = "تمت المزامنة",
            lastSyncedAt = nowMs
        )
        val sess3 = AttendanceSessionEntity(
            id = "sess_chem_1",
            groupId = "group_chem_2sec",
            sessionDate = "2025-02-02",
            sessionTime = "06:00 م",
            teacherName = "المستر",
            lessonTopic = "أعداد الكم والجدول الدوري الحديث",
            isSynced = true,
            syncStatus = "تمت المزامنة",
            lastSyncedAt = nowMs
        )
        db.attendanceDao().insertSessions(listOf(sess1, sess2, sess3))

        // Seed Attendance Records
        val records = listOf(
            AttendanceRecordEntity(sessionId = "sess_phy_1", studentId = "s1", groupId = "group_physics_3sec", status = "حاضر"),
            AttendanceRecordEntity(sessionId = "sess_phy_1", studentId = "s2", groupId = "group_physics_3sec", status = "حاضر"),
            AttendanceRecordEntity(sessionId = "sess_phy_1", studentId = "s3", groupId = "group_physics_3sec", status = "غائب", excuseReason = "وعكة صحية"),
            AttendanceRecordEntity(sessionId = "sess_phy_2", studentId = "s1", groupId = "group_physics_3sec", status = "حاضر"),
            AttendanceRecordEntity(sessionId = "sess_phy_2", studentId = "s2", groupId = "group_physics_3sec", status = "متأخر", notes = "تأخر 15 دقيقة"),
            AttendanceRecordEntity(sessionId = "sess_phy_2", studentId = "s3", groupId = "group_physics_3sec", status = "غائب"),
            AttendanceRecordEntity(sessionId = "sess_chem_1", studentId = "s4", groupId = "group_chem_2sec", status = "حاضر"),
            AttendanceRecordEntity(sessionId = "sess_chem_1", studentId = "s5", groupId = "group_chem_2sec", status = "حاضر")
        )
        db.attendanceDao().insertRecords(records)

        // Seed initial Audit logs
        logAction("بدء النظام", "تم تهيئة قاعدة البيانات المحلية وبدء النظام بنجاح")
    }
}
