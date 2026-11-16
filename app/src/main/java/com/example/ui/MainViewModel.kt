package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.EduRepository
import com.example.util.AttendanceCloudSyncService
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

enum class ScreenDestination(val title: String) {
    DASHBOARD("لوحة التحكم"),
    STUDENTS("إدارة الطلاب"),
    ADD_STUDENT("إضافة طالب جديد"),
    STUDENT_PROFILE("ملف الطالب"),
    GROUPS("المجموعات الدراسية"),
    GROUP_DETAIL("تفاصيل المجموعة"),
    SCHEDULE("الجدول الدراسي"),
    ATTENDANCE("تسجيل الحضور والغياب"),
    ATTENDANCE_REPORTS("تقارير الحضور"),
    PAYMENTS("الاشتراكات والمدفوعات"),
    RECORD_PAYMENT("تسجيل دفعة طالب"),
    OVERDUE("الطلاب المتأخرون بالسداد"),
    STATEMENT("كشف حساب الطالب"),
    FINANCIAL_REPORTS("التقارير المالية"),
    BACKUP("النسخ الاحتياطي والاستعادة"),
    WHATSAPP_NOTICES("رسائل وتذكيرات واتساب"),
    SETTINGS("الإعدادات العامة")
}

data class UpcomingPaymentItem(
    val student: StudentEntity,
    val group: GroupEntity?,
    val dueDate: String,
    val daysRemaining: Int,
    val expectedAmount: Double,
    val statusText: String
)

data class OverdueItem(
    val student: StudentEntity,
    val group: GroupEntity?,
    val subscription: MonthlySubscriptionEntity,
    val totalPaid: Double,
    val remaining: Double
)

data class StudentAttendanceStat(
    val studentId: String,
    val studentName: String,
    val totalSessions: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val attendancePercentage: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EduRepository(EduDatabase.getDatabase(application))
    private val connectivityObserver = NetworkConnectivityObserver(application)

    // Navigation & Selected states
    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<String?>(null)
    val selectedStudentId: StateFlow<String?> = _selectedStudentId.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncFeedback = MutableStateFlow<String?>(null)
    val lastSyncFeedback: StateFlow<String?> = _lastSyncFeedback.asStateFlow()

    // Data streams from Repository
    val students: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enrollments: StateFlow<List<EnrollmentEntity>> = repository.allEnrollments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Optimistic status overrides for instant 0ms UI responsiveness
    private val _optimisticStatuses = MutableStateFlow<Map<String, String>>(emptyMap())

    val subscriptions: StateFlow<List<MonthlySubscriptionEntity>> = combine(
        repository.allSubscriptions,
        _optimisticStatuses
    ) { subs, optMap ->
        val monthEntries = optMap.entries.filter { it.key.contains("__MONTH__") }
        val result = subs.map { sub ->
            val optById = optMap[sub.id]
            val optByMonth = monthEntries.firstOrNull { entry ->
                val parts = entry.key.split("__MONTH__")
                parts.size == 2 && sub.studentId == parts[0] && sub.monthYear.contains(parts[1])
            }?.value
            val effectiveOpt = optById ?: optByMonth
            if (effectiveOpt != null) sub.copy(status = effectiveOpt) else sub
        }.toMutableList()

        monthEntries.forEach { (key, status) ->
            val parts = key.split("__MONTH__")
            if (parts.size == 2) {
                val studentId = parts[0]
                val monthName = parts[1]
                val exists = result.any { it.studentId == studentId && it.monthYear.contains(monthName) }
                if (!exists) {
                    val defaultFee = students.value.find { it.id == studentId }?.monthlyFee ?: 250.0
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val monthNum = when {
                        monthName.contains("يناير") -> "01"
                        monthName.contains("فبراير") -> "02"
                        monthName.contains("مارس") -> "03"
                        monthName.contains("أبريل") -> "04"
                        monthName.contains("مايو") -> "05"
                        monthName.contains("يونيو") -> "06"
                        monthName.contains("يوليو") -> "07"
                        monthName.contains("أغسطس") -> "08"
                        monthName.contains("سبتمبر") -> "09"
                        monthName.contains("أكتوبر") -> "10"
                        monthName.contains("نوفمبر") -> "11"
                        monthName.contains("ديسمبر") -> "12"
                        else -> "01"
                    }
                    result.add(
                        MonthlySubscriptionEntity(
                            id = "temp_${studentId}_$monthName",
                            studentId = studentId,
                            groupId = enrollments.value.find { it.studentId == studentId }?.groupId ?: "group_general",
                            monthYear = "$monthName $currentYear",
                            monthSortKey = "$currentYear-$monthNum",
                            academicYear = settings.value?.academicYear.orEmpty(),
                            expectedAmount = defaultFee,
                            status = status
                        )
                    )
                }
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceSessions: StateFlow<List<AttendanceSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = attendanceSessions
        .map { list -> list.count { !it.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncStatus: StateFlow<String> = combine(
        isOnline,
        _isSyncing,
        pendingSyncCount,
        settings
    ) { online, syncing, pending, appSettings ->
        when {
            syncing -> "جاري مزامنة الكشوف مع السحابة..."
            pending > 0 && !online -> "محلي أوفلاين • $pending كشف بانتظار الإنترنت"
            pending > 0 && online && appSettings?.cloudSyncUrl.isNullOrBlank() ->
                "متصل • $pending كشف محلي بانتظار رابط السحابة"
            pending > 0 && online -> "متصل • $pending كشف قيد المزامنة"
            online -> "متصل • كافة الكشوف متزامنة ✓"
            else -> "محلي أوفلاين • البيانات محفوظة"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "محلي أوفلاين • البيانات محفوظة")

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialDemoData()
        }
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                if (online) {
                    syncPendingAttendanceSessions(autoTriggered = true)
                }
            }
        }
    }

    // Overdue items calculated state
    val overdueItems: StateFlow<List<OverdueItem>> = combine(
        students, groups, subscriptions, payments
    ) { studentList, groupList, subList, paymentList ->
        val studentMap = studentList.associateBy { it.id }
        val groupMap = groupList.associateBy { it.id }
        val paymentsBySub = paymentList.groupBy { it.subscriptionId }

        subList.mapNotNull { sub ->
            if (sub.status == "مدفوع بالكامل" || sub.status == "مدفوع" || sub.status == "معفى") return@mapNotNull null
            val student = studentMap[sub.studentId] ?: return@mapNotNull null
            val group = groupMap[sub.groupId]
            val paid = paymentsBySub[sub.id]?.sumOf { it.amountPaid } ?: 0.0
            val remaining = (sub.expectedAmount - paid).coerceAtLeast(0.0)
            if (remaining > 0) {
                OverdueItem(
                    student = student,
                    group = group,
                    subscription = sub,
                    totalPaid = paid,
                    remaining = remaining
                )
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upcoming payments & next due date reminders
    val upcomingPayments: StateFlow<List<UpcomingPaymentItem>> = combine(
        students, groups, enrollments, subscriptions
    ) { studentList, groupList, enrollList, subList ->
        val groupMap = groupList.associateBy { it.id }
        val enrollMap = enrollList.groupBy { it.studentId }
        val subsByStudent = subList.groupBy { it.studentId }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMs = todayCal.timeInMillis

        // Default next month 1st date if none is set
        val defaultNextMonthDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val defaultDueDateStr = sdf.format(defaultNextMonthDate.time)

        studentList.filter { it.status != "مؤرشف" }.map { student ->
            val primaryGroupId = enrollMap[student.id]?.firstOrNull()?.groupId
            val group = if (primaryGroupId != null) groupMap[primaryGroupId] else null
            val studentSubs = subsByStudent[student.id] ?: emptyList()

            val targetDueDate = when {
                student.nextPaymentDueDate.isNotBlank() -> student.nextPaymentDueDate
                studentSubs.any { it.nextDueDate.isNotBlank() } -> {
                    studentSubs.filter { it.nextDueDate.isNotBlank() }.maxByOrNull { it.nextDueDate }?.nextDueDate ?: defaultDueDateStr
                }
                else -> defaultDueDateStr
            }

            val dueParsed = try {
                sdf.parse(targetDueDate)
            } catch (e: Exception) {
                null
            }

            val daysRemaining = if (dueParsed != null) {
                val diff = dueParsed.time - todayMs
                (diff / (1000 * 60 * 60 * 24)).toInt()
            } else {
                30
            }

            val statusText = when {
                daysRemaining < 0 -> "متأخر منذ ${-daysRemaining} يوم ⚠️"
                daysRemaining == 0 -> "مستحق الدفع اليوم ⏰"
                daysRemaining == 1 -> "مستحق غداً ⏳"
                else -> "متبقي $daysRemaining يوم"
            }

            val expectedFee = enrollMap[student.id]?.firstOrNull()?.customMonthlyFee ?: student.monthlyFee

            UpcomingPaymentItem(
                student = student,
                group = group,
                dueDate = targetDueDate,
                daysRemaining = daysRemaining,
                expectedAmount = expectedFee,
                statusText = statusText
            )
        }.sortedBy { it.daysRemaining }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // High absence students (Frequent Absentees)
    val frequentAbsentees: StateFlow<List<StudentAttendanceStat>> = combine(
        students, attendanceRecords, settings
    ) { studentList, records, appSettings ->
        val threshold = appSettings?.absenceAlertThreshold ?: 3
        val recordsByStudent = records.groupBy { it.studentId }

        studentList.mapNotNull { student ->
            val studentRecs = recordsByStudent[student.id] ?: emptyList()
            val total = studentRecs.size
            if (total == 0) return@mapNotNull null

            val present = studentRecs.count { it.status == "حاضر" }
            val absent = studentRecs.count { it.status == "غائب" }
            val late = studentRecs.count { it.status == "متأخر" }
            val excused = studentRecs.count { it.status == "غياب بعذر" }
            val percent = if (total > 0) ((present + late) * 100 / total) else 100

            if (absent >= threshold) {
                StudentAttendanceStat(
                    studentId = student.id,
                    studentName = student.fullName,
                    totalSessions = total,
                    presentCount = present,
                    absentCount = absent,
                    lateCount = late,
                    excusedCount = excused,
                    attendancePercentage = percent
                )
            } else null
        }.sortedByDescending { it.absentCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val navigationStack = mutableListOf(ScreenDestination.DASHBOARD)

    // Navigation methods
    fun navigateTo(destination: ScreenDestination) {
        if (destination == _currentScreen.value) return
        if (destination == ScreenDestination.DASHBOARD) {
            navigationStack.clear()
            navigationStack.add(ScreenDestination.DASHBOARD)
            _currentScreen.value = ScreenDestination.DASHBOARD
            return
        }
        if (navigationStack.lastOrNull() != destination) {
            navigationStack.add(destination)
        }
        _currentScreen.value = destination
    }

    fun openAddStudentScreen(fromScreen: ScreenDestination = _currentScreen.value) {
        if (fromScreen != ScreenDestination.ADD_STUDENT && navigationStack.lastOrNull() != fromScreen) {
            navigationStack.add(fromScreen)
        }
        navigateTo(ScreenDestination.ADD_STUDENT)
    }

    fun closeAddStudentScreen() {
        navigateBack()
    }

    fun navigateBack(): Boolean {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.lastIndex)
            _currentScreen.value = navigationStack.last()
            return true
        }
        if (_currentScreen.value != ScreenDestination.DASHBOARD) {
            navigationStack.clear()
            navigationStack.add(ScreenDestination.DASHBOARD)
            _currentScreen.value = ScreenDestination.DASHBOARD
            return true
        }
        return false
    }

    fun navigateAfterStudentAdded() {
        if (navigationStack.lastOrNull() == ScreenDestination.ADD_STUDENT) {
            navigationStack.removeAt(navigationStack.lastIndex)
        }
        if (navigationStack.lastOrNull() != ScreenDestination.STUDENTS) {
            navigationStack.add(ScreenDestination.STUDENTS)
        }
        _currentScreen.value = ScreenDestination.STUDENTS
    }

    fun openRecordPaymentScreen(studentId: String? = null) {
        if (studentId != null) {
            _selectedStudentId.value = studentId
        }
        navigateTo(ScreenDestination.RECORD_PAYMENT)
    }

    fun navigateAfterPaymentRecorded() {
        if (navigationStack.lastOrNull() == ScreenDestination.RECORD_PAYMENT) {
            navigationStack.removeAt(navigationStack.lastIndex)
        }
        if (navigationStack.lastOrNull() != ScreenDestination.PAYMENTS) {
            navigationStack.add(ScreenDestination.PAYMENTS)
        }
        _currentScreen.value = ScreenDestination.PAYMENTS
    }

    fun openStudentProfile(studentId: String) {
        _selectedStudentId.value = studentId
        navigateTo(ScreenDestination.STUDENT_PROFILE)
    }

    fun openStudentStatement(studentId: String) {
        _selectedStudentId.value = studentId
        navigateTo(ScreenDestination.STATEMENT)
    }

    fun openGroupDetail(groupId: String) {
        _selectedGroupId.value = groupId
        navigateTo(ScreenDestination.GROUP_DETAIL)
    }

    fun openAttendanceForGroup(groupId: String) {
        _selectedGroupId.value = groupId
        navigateTo(ScreenDestination.ATTENDANCE)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Database Actions
    fun addStudent(student: StudentEntity, initialGroupId: String? = null) {
        viewModelScope.launch {
            repository.addStudent(student)
            if (!initialGroupId.isNullOrBlank()) {
                repository.enrollStudentInGroup(student.id, initialGroupId, student.monthlyFee)
            }
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun archiveStudent(studentId: String, studentName: String) {
        viewModelScope.launch {
            repository.archiveStudent(studentId, studentName)
            if (_selectedStudentId.value == studentId) {
                _selectedStudentId.value = null
                if (_currentScreen.value == ScreenDestination.STUDENT_PROFILE) {
                    navigateBack()
                }
            }
        }
    }

    fun deleteStudent(studentId: String, studentName: String) {
        archiveStudent(studentId, studentName)
    }

    fun addGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.addGroup(group)
        }
    }

    fun updateGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun deleteGroup(groupId: String, groupName: String) {
        viewModelScope.launch {
            repository.deleteGroup(groupId, groupName)
            if (_selectedGroupId.value == groupId) {
                _selectedGroupId.value = null
                if (_currentScreen.value == ScreenDestination.GROUP_DETAIL) {
                    navigateBack()
                }
            }
        }
    }

    fun enrollStudentInGroup(studentId: String, groupId: String, customFee: Double? = null) {
        viewModelScope.launch {
            repository.enrollStudentInGroup(studentId, groupId, customFee)
        }
    }

    fun unenrollStudent(studentId: String, groupId: String) {
        viewModelScope.launch {
            repository.unenrollStudent(studentId, groupId)
        }
    }

    fun recordPayment(
        subscriptionId: String,
        studentId: String,
        groupId: String,
        amount: Double,
        method: String,
        notes: String
    ) {
        val sub = subscriptions.value.find { it.id == subscriptionId }
        val currentPaid = payments.value.filter { it.subscriptionId == subscriptionId }.sumOf { it.amountPaid }
        val newPaid = currentPaid + amount
        val optStatus = if (sub != null && newPaid >= sub.expectedAmount) "مدفوع" else if (sub != null && newPaid > 0) "دفع جزئي" else null
        if (optStatus != null) {
            _optimisticStatuses.value = _optimisticStatuses.value + (subscriptionId to optStatus)
        }
        viewModelScope.launch {
            try {
                val role = settings.value?.userRole ?: "المعلم"
                repository.recordPayment(subscriptionId, studentId, groupId, amount, method, notes, role)
            } finally {
                _optimisticStatuses.value = _optimisticStatuses.value - subscriptionId
            }
        }
    }

    fun recordDirectStudentPayment(
        studentId: String,
        amount: Double,
        paymentDate: String,
        method: String,
        notes: String,
        monthName: String = "فبراير"
    ) {
        val optKey = "${studentId}__MONTH__${monthName}"
        _optimisticStatuses.value = _optimisticStatuses.value + (optKey to "مدفوع")
        viewModelScope.launch {
            try {
                val year = settings.value?.academicYear.orEmpty()
                repository.recordDirectStudentPayment(
                    studentId = studentId,
                    amount = amount,
                    paymentDate = paymentDate,
                    method = method,
                    notes = notes,
                    monthName = monthName,
                    academicYear = year
                )
            } finally {
                _optimisticStatuses.value = _optimisticStatuses.value - optKey
            }
        }
    }

    fun recordStudentSubscriptionAndPayment(
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
        explicitMonthStatus: String? = null
    ) {
        val optStatus = explicitMonthStatus ?: when {
            amountPaid >= expectedFee && expectedFee > 0 -> "مدفوع"
            amountPaid > 0 -> "دفع جزئي"
            else -> "غير مدفوع"
        }
        val monthKey = "${studentId}__MONTH__${monthName}"
        val existingSub = subscriptions.value.find { it.studentId == studentId && it.monthYear.contains(monthName) }
        val addedKeys = buildMap {
            put(monthKey, optStatus)
            if (existingSub != null) put(existingSub.id, optStatus)
        }
        _optimisticStatuses.value = _optimisticStatuses.value + addedKeys

        viewModelScope.launch {
            try {
                val role = settings.value?.userRole ?: "المعلم"
                repository.recordStudentSubscriptionAndPayment(
                    studentId = studentId,
                    groupId = groupId,
                    monthName = monthName,
                    academicYear = academicYear,
                    expectedFee = expectedFee,
                    amountPaid = amountPaid,
                    paymentDate = paymentDate,
                    nextDueDate = nextDueDate,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    operator = role,
                    explicitMonthStatus = explicitMonthStatus
                )
            } finally {
                _optimisticStatuses.value = _optimisticStatuses.value - addedKeys.keys
            }
        }
    }

    fun quickPayMonth(
        studentId: String,
        monthName: String,
        groupId: String = "",
        amount: Double? = null,
        paymentDate: String? = null,
        paymentMethod: String = "نقدًا",
        notes: String? = null
    ) {
        val existing = subscriptions.value.find { it.studentId == studentId && it.monthYear.contains(monthName) }
        val monthKey = "${studentId}__MONTH__${monthName}"
        val keys = if (existing != null) mapOf(monthKey to "مدفوع", existing.id to "مدفوع") else mapOf(monthKey to "مدفوع")
        _optimisticStatuses.value = _optimisticStatuses.value + keys
        viewModelScope.launch {
            try {
                val year = settings.value?.academicYear.orEmpty()
                repository.quickPayMonthForStudent(
                    studentId = studentId,
                    monthName = monthName,
                    academicYear = year,
                    groupId = groupId,
                    amountOverride = amount,
                    paymentDate = paymentDate,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
            } finally {
                _optimisticStatuses.value = _optimisticStatuses.value - keys.keys
            }
        }
    }

    fun cancelMonthPayment(studentId: String, monthName: String) {
        val existing = subscriptions.value.find { it.studentId == studentId && it.monthYear.contains(monthName) }
        val monthKey = "${studentId}__MONTH__${monthName}"
        val keys = if (existing != null) mapOf(monthKey to "غير مدفوع", existing.id to "غير مدفوع") else mapOf(monthKey to "غير مدفوع")
        _optimisticStatuses.value = _optimisticStatuses.value + keys
        viewModelScope.launch {
            try {
                repository.cancelMonthPaymentForStudent(studentId, monthName)
            } finally {
                _optimisticStatuses.value = _optimisticStatuses.value - keys.keys
            }
        }
    }

    fun recordExemption(subscriptionId: String, reason: String) {
        viewModelScope.launch {
            repository.setSubscriptionExemption(subscriptionId, reason)
        }
    }

    fun saveAttendanceSession(
        session: AttendanceSessionEntity,
        records: List<AttendanceRecordEntity>
    ) {
        viewModelScope.launch {
            val onlineNow = connectivityObserver.isCurrentlyOnline()
            val hasEndpoint = AttendanceCloudSyncService.isValidCloudEndpoint(settings.value?.cloudSyncUrl.orEmpty())
            val initialStatus = when {
                !onlineNow -> "بانتظار توفر الإنترنت"
                !hasEndpoint -> "بانتظار ربط السحابة"
                else -> "بانتظار المزامنة"
            }
            repository.saveAttendanceSession(
                session = session.copy(isSynced = false, syncStatus = initialStatus),
                records = records
            )
            if (onlineNow && hasEndpoint) {
                syncPendingAttendanceSessions(autoTriggered = true)
            } else if (!onlineNow) {
                _lastSyncFeedback.value = "تم حفظ الكشف محليًا وسيتم رفعه للسحابة تلقائيًا فور توفر الإنترنت"
            } else {
                _lastSyncFeedback.value = "تم حفظ الكشف محليًا — أضف رابط السحابة للمزامنة التلقائية"
            }
        }
    }

    fun updateCloudSyncUrl(url: String) {
        viewModelScope.launch {
            repository.updateCloudSyncUrl(url)
            if (AttendanceCloudSyncService.isValidCloudEndpoint(url) && connectivityObserver.isCurrentlyOnline()) {
                syncPendingAttendanceSessions(autoTriggered = false)
            }
        }
    }

    fun clearSyncFeedback() {
        _lastSyncFeedback.value = null
    }

    fun createMonthlySubscriptions(monthYear: String, monthSortKey: String, academicYear: String) {
        viewModelScope.launch {
            repository.createMonthlySubscriptions(monthYear, monthSortKey, academicYear)
        }
    }

    fun updateSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun triggerManualSync() {
        syncPendingAttendanceSessions(autoTriggered = false)
    }

    fun syncPendingAttendanceSessions(autoTriggered: Boolean = false) {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            val onlineNow = connectivityObserver.isCurrentlyOnline()
            if (autoTriggered && !onlineNow) {
                return@launch
            }
            _isSyncing.value = true
            try {
                when (val result = repository.syncPendingAttendanceSessions(onlineNow)) {
                    is AttendanceCloudSyncService.SyncBatchResult.NothingPending -> {
                        if (!autoTriggered) {
                            _lastSyncFeedback.value = "جميع كشوف الحضور متزامنة ولا توجد بيانات معلقة"
                        }
                    }
                    is AttendanceCloudSyncService.SyncBatchResult.SyncedAll -> {
                        _lastSyncFeedback.value = "تمت مزامنة ${result.syncedCount} كشف حضور مع السحابة بنجاح ✓"
                    }
                    is AttendanceCloudSyncService.SyncBatchResult.OfflineQueued -> {
                        if (!autoTriggered) {
                            _lastSyncFeedback.value = "لا يوجد اتصال بالإنترنت حاليًا — ${result.pendingCount} كشف محفوظ محليًا وبانتظار الشبكة"
                        }
                    }
                    is AttendanceCloudSyncService.SyncBatchResult.EndpointNotConfigured -> {
                        if (!autoTriggered) {
                            _lastSyncFeedback.value = "${result.pendingCount} كشف محفوظ محليًا — يرجى إعداد رابط السحابة (API/Webhook) للمزامنة"
                        }
                    }
                    is AttendanceCloudSyncService.SyncBatchResult.PartialOrFailed -> {
                        _lastSyncFeedback.value = "تمت مزامنة ${result.syncedCount} وفشل ${result.failedCount}: ${result.lastError}"
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityObserver.unregister()
    }
}
