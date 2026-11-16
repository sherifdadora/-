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
import java.util.Calendar
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
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
        db.withTransaction {
            val existingByDate = db.attendanceDao().getSessionByGroupAndDate(
                groupId = session.groupId,
                sessionDate = session.sessionDate
            )
            val resolvedSessionId = existingByDate?.id ?: session.id
            val localQueuedSession = session.copy(
                id = resolvedSessionId,
                isSynced = false,
                syncStatus = session.syncStatus.ifBlank { "بانتظار المزامنة" },
                syncError = null
            )
            val resolvedRecords = records.map { record ->
                record.copy(
                    sessionId = resolvedSessionId,
                    groupId = localQueuedSession.groupId
                )
            }
            db.attendanceDao().insertSession(localQueuedSession)
            db.attendanceDao().deleteRecordsForSession(resolvedSessionId)
            db.attendanceDao().insertRecords(resolvedRecords)
            logAction(
                "حفظ كشف حضور محلي",
                "تم حفظ كشف حضور الحصة بتاريخ ${localQueuedSession.sessionDate} لعدد ${resolvedRecords.size} طالب (${localQueuedSession.syncStatus})"
            )
        }
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
        db.withTransaction {
            val enrollments = db.enrollmentDao().getAllEnrollments().firstOrNull() ?: emptyList()
            val groups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
            val existingSubs = db.subscriptionDao().getAllSubscriptions().firstOrNull() ?: emptyList()
            val groupMap = groups.associateBy { it.id }

            val existingKeys = existingSubs.mapTo(HashSet()) {
                "${it.studentId}|${it.groupId}|${it.monthYear}|${it.academicYear}"
            }

            val newSubs = mutableListOf<MonthlySubscriptionEntity>()
            for (enrollment in enrollments) {
                val key = "${enrollment.studentId}|${enrollment.groupId}|$monthYear|$academicYear"
                if (key in existingKeys) continue

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
                existingKeys.add(key)
            }
            if (newSubs.isNotEmpty()) {
                db.subscriptionDao().insertSubscriptions(newSubs)
                logAction("إنشاء اشتراكات شهرية", "تم إنشاء ${newSubs.size} اشتراك لشهر $monthYear")
            }
        }
    }

    private fun calculateMonthSortKey(
        monthName: String,
        yearHint: String = Calendar.getInstance().get(Calendar.YEAR).toString()
    ): String {
        val monthNum = when {
            monthName.contains("يناير") -> "01"
            monthName.contains("فبراير") -> "02"
            monthName.contains("مارس") -> "03"
            monthName.contains("أبريل") || monthName.contains("ابريل") -> "04"
            monthName.contains("مايو") -> "05"
            monthName.contains("يونيو") -> "06"
            monthName.contains("يوليو") -> "07"
            monthName.contains("أغسطس") || monthName.contains("اغسطس") -> "08"
            monthName.contains("سبتمبر") -> "09"
            monthName.contains("أكتوبر") || monthName.contains("اكتوبر") -> "10"
            monthName.contains("نوفمبر") -> "11"
            monthName.contains("ديسمبر") -> "12"
            else -> "01"
        }
        return "$yearHint-$monthNum"
    }

    suspend fun quickPayMonthForStudent(
        studentId: String,
        monthName: String,
        academicYear: String = "",
        groupId: String = "",
        amountOverride: Double? = null,
        paymentDate: String? = null,
        paymentMethod: String = "نقدًا",
        notes: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val student = db.studentDao().getStudentById(studentId).firstOrNull() ?: return@withTransaction false
            val enrollments = db.enrollmentDao().getEnrollmentsForStudent(studentId).firstOrNull() ?: emptyList()
            val groups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
            val groupMap = groups.associateBy { it.id }

            val primaryEnrollment = if (groupId.isNotBlank()) {
                enrollments.find { it.groupId == groupId } ?: enrollments.firstOrNull()
            } else {
                enrollments.firstOrNull()
            }
            val primaryGroupId = groupId.ifBlank { primaryEnrollment?.groupId ?: "group_general" }

            val allSubs = db.subscriptionDao().getSubscriptionsListForStudent(studentId)
            val existingSub = allSubs.find { it.monthYear.contains(monthName) && (groupId.isBlank() || it.groupId == primaryGroupId) }
                ?: allSubs.find { it.monthYear.contains(monthName) }

            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = paymentDate?.takeIf { it.isNotBlank() } ?: timeFormat.format(Date())
            val parsedTimestamp = paymentDate?.takeIf { it.isNotBlank() }?.let {
                try {
                    dateFormat.parse(it)?.time
                } catch (_: Exception) {
                    null
                }
            } ?: System.currentTimeMillis()
            val noteText = notes?.takeIf { it.isNotBlank() } ?: "سداد سريع لشهر $monthName"

            if (existingSub != null) {
                val payments = db.paymentDao().getPaymentsListForSubscription(existingSub.id)
                val totalPaid = payments.sumOf { it.amountPaid }
                val remaining = (existingSub.expectedAmount - totalPaid).coerceAtLeast(0.0)
                val alreadyPaid = existingSub.status == "مدفوع" || existingSub.status == "مدفوع بالكامل"
                if (alreadyPaid && remaining <= 0.0) {
                    return@withTransaction true
                }
                val amountToRecord = amountOverride?.takeIf { it > 0.0 } ?: remaining

                if (amountToRecord > 0.0) {
                    val payment = PaymentEntity(
                        subscriptionId = existingSub.id,
                        studentId = studentId,
                        groupId = existingSub.groupId,
                        amountPaid = amountToRecord,
                        paymentMethod = paymentMethod,
                        paymentTimestamp = parsedTimestamp,
                        paymentDateFormatted = dateStr,
                        notes = noteText,
                        idempotencyKey = UUID.randomUUID().toString(),
                        recordedBy = "المستر"
                    )
                    db.paymentDao().insertPayment(payment)
                }
                if (!alreadyPaid) {
                    db.subscriptionDao().updateStatus(existingSub.id, "مدفوع")
                }
                logAction("تسجيل سداد سريع", "تم سداد اشتراك شهر $monthName للطالب ${student.fullName}")
                true
            } else {
                val defaultFee = primaryEnrollment?.customMonthlyFee
                    ?: groupMap[primaryGroupId]?.defaultMonthlyFee
                    ?: student.monthlyFee
                val effectiveFee = amountOverride?.takeIf { it > 0.0 } ?: defaultFee
                val newSubId = UUID.randomUUID().toString()
                val yearStr = paymentDate?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
                    ?: Calendar.getInstance().get(Calendar.YEAR).toString()
                val newSub = MonthlySubscriptionEntity(
                    id = newSubId,
                    studentId = studentId,
                    groupId = primaryGroupId,
                    monthYear = "$monthName $yearStr",
                    monthSortKey = calculateMonthSortKey(monthName, yearStr),
                    academicYear = academicYear,
                    expectedAmount = effectiveFee,
                    status = "مدفوع"
                )
                db.subscriptionDao().insertSubscription(newSub)

                val payment = PaymentEntity(
                    subscriptionId = newSubId,
                    studentId = studentId,
                    groupId = primaryGroupId,
                    amountPaid = effectiveFee,
                    paymentMethod = paymentMethod,
                    paymentTimestamp = parsedTimestamp,
                    paymentDateFormatted = dateStr,
                    notes = noteText,
                    idempotencyKey = UUID.randomUUID().toString(),
                    recordedBy = "المستر"
                )
                db.paymentDao().insertPayment(payment)
                logAction("تسجيل سداد سريع", "تم إنشاء وسداد اشتراك شهر $monthName للطالب ${student.fullName}")
                true
            }
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
        academicYear: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val student = db.studentDao().getStudentById(studentId).firstOrNull() ?: return@withTransaction false
            val enrollments = db.enrollmentDao().getEnrollmentsForStudent(studentId).firstOrNull() ?: emptyList()
            val primaryGroupId = enrollments.firstOrNull()?.groupId ?: "group_general"

            val allSubs = db.subscriptionDao().getSubscriptionsListForStudent(studentId)
            val existingSub = allSubs.find { it.monthYear.contains(monthName) }

            val subId = if (existingSub != null) {
                existingSub.id
            } else {
                val fee = enrollments.firstOrNull()?.customMonthlyFee ?: student.monthlyFee
                val newSubId = UUID.randomUUID().toString()
                val currentYear = paymentDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }
                    ?: Calendar.getInstance().get(Calendar.YEAR).toString()
                val newSub = MonthlySubscriptionEntity(
                    id = newSubId,
                    studentId = studentId,
                    groupId = primaryGroupId,
                    monthYear = "$monthName $currentYear",
                    monthSortKey = calculateMonthSortKey(monthName, currentYear),
                    academicYear = academicYear,
                    expectedAmount = fee,
                    status = "لم يدفع"
                )
                db.subscriptionDao().insertSubscription(newSub)
                newSubId
            }

            val parsedTimestamp = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
            true
        }
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
                val yearFromDate = paymentDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }
                    ?: Calendar.getInstance().get(Calendar.YEAR).toString()
                val newSub = MonthlySubscriptionEntity(
                    id = newSubId,
                    studentId = studentId,
                    groupId = targetGroupId,
                    monthYear = "$monthName $yearFromDate",
                    monthSortKey = calculateMonthSortKey(monthName, yearFromDate),
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
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
        // Clear any old placeholder teacherName / role / legacy academicYear directly in SQLite
        db.settingsDao().clearTeacherAndRole()
        db.settingsDao().clearLegacyAcademicYear()

        // Ensure default app settings row exists without inserting sample groups or students
        if (db.settingsDao().getSettingsDirect() == null) {
            db.settingsDao().insertOrUpdate(
                AppSettingsEntity(
                    teacherName = "",
                    centerName = "سنتر التفوق والريادة التعليمي",
                    currency = "جنيه",
                    academicYear = "",
                    defaultFee = 250.0,
                    absenceAlertThreshold = 3,
                    userRole = "المعلم"
                )
            )
        }

        // Clean up any pre-seeded demo groups, students, and related demo records from earlier installs
        val demoGroupIds = listOf("group_physics_3sec", "group_chem_2sec", "group_math_3prep")
        val demoStudentIds = listOf("s1", "s2", "s3", "s4", "s5", "s6", "s7")
        val demoSubscriptionIds = listOf(
            "sub_s1_jan", "sub_s1_feb", "sub_s2_jan", "sub_s2_feb",
            "sub_s3_feb", "sub_s4_feb", "sub_s5_feb", "sub_s7_feb"
        )
        val demoSessionIds = listOf("sess_phy_1", "sess_phy_2", "sess_chem_1")

        db.withTransaction {
            db.attendanceDao().deleteRecordsByDemoIds(demoSessionIds, demoStudentIds, demoGroupIds)
            db.attendanceDao().deleteSessionsByDemoIds(demoSessionIds, demoGroupIds)
            db.paymentDao().deletePaymentsByDemoIds(demoStudentIds, demoGroupIds)
            db.subscriptionDao().deleteSubscriptionsByDemoIds(demoSubscriptionIds, demoStudentIds, demoGroupIds)
            db.enrollmentDao().deleteEnrollmentsByDemoIds(demoStudentIds, demoGroupIds)
            db.studentDao().deleteStudentsByIds(demoStudentIds)
            db.groupDao().deleteGroupsByIds(demoGroupIds)
        }
    }
}
