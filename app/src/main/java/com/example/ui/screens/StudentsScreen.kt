package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.components.StudentPaymentSubscriptionDialog
import com.example.ui.components.WhatsAppMessageComposerDialog
import com.example.ui.components.WhatsAppRecipientMode
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduWarningAmber
import com.example.ui.theme.eduErrorContainerColor
import com.example.ui.theme.eduErrorContentColor
import com.example.ui.theme.eduSuccessContainerColor
import com.example.ui.theme.eduSuccessContentColor
import com.example.ui.theme.eduWarningContainerColor
import com.example.ui.theme.eduWarningContentColor
import com.example.util.ExportHelper
import com.example.util.WhatsAppHelper
import com.example.util.WhatsAppMessageTemplateType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun filterStudentsByQuery(
    students: List<StudentEntity>,
    query: String,
    stageFilter: String = "الكل",
    statusFilter: String = "الكل"
): List<StudentEntity> {
    val trimmedQuery = query.trim()
    val normalizedPhoneQuery = StudentInputValidator.normalizePhoneInput(trimmedQuery)

    return students.filter { student ->
        val matchesQuery = if (trimmedQuery.isEmpty()) {
            true
        } else {
            val normalizedStudentPhone = StudentInputValidator.normalizePhoneInput(student.phone)
            val normalizedParentPhone = StudentInputValidator.normalizePhoneInput(student.parentPhone)
            student.fullName.contains(trimmedQuery, ignoreCase = true) ||
                student.phone.contains(trimmedQuery, ignoreCase = true) ||
                student.parentPhone.contains(trimmedQuery, ignoreCase = true) ||
                (normalizedPhoneQuery.isNotEmpty() && (
                    normalizedStudentPhone.contains(normalizedPhoneQuery) ||
                        normalizedParentPhone.contains(normalizedPhoneQuery)
                    ))
        }
        val matchesStage = stageFilter == "الكل" || student.educationalStage == stageFilter
        val matchesStatus = statusFilter == "الكل" || student.status == statusFilter
        matchesQuery && matchesStage && matchesStatus
    }.sortedBy { it.fullName }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity>,
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    settings: AppSettingsEntity?,
    onOpenProfile: (String) -> Unit,
    onOpenStatement: (String) -> Unit,
    onOpenAddStudent: () -> Unit = {},
    onAddStudent: (StudentEntity, String?) -> Unit,
    onUpdateStudent: (StudentEntity) -> Unit,
    onArchiveStudent: (String, String) -> Unit,
    onQuickPayMonth: (studentId: String, monthName: String) -> Unit,
    onCancelMonthPayment: (studentId: String, monthName: String) -> Unit,
    onRecordStudentSubscriptionAndPayment: (
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
        monthStatus: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currency = settings?.currency ?: "جنيه"

    var searchQuery by remember { mutableStateOf("") }
    var selectedStageFilter by remember { mutableStateOf("الكل") }
    var selectedStatusFilter by remember { mutableStateOf("الكل") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToArchive by remember { mutableStateOf<StudentEntity?>(null) }
    var composerTarget by remember { mutableStateOf<Pair<StudentEntity, GroupEntity?>?>(null) }
    var studentForPaymentForm by remember { mutableStateOf<StudentEntity?>(null) }

    // Filtering logic by student name or phone number (+ optional stage/status chips)
    val filteredStudents = remember(students, searchQuery, selectedStageFilter, selectedStatusFilter) {
        filterStudentsByQuery(
            students = students,
            query = searchQuery,
            stageFilter = selectedStageFilter,
            statusFilter = selectedStatusFilter
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Search Bar at top of student list
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("students_search_bar"),
                placeholder = { Text("ابحث باسم الطالب أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", modifier = Modifier.size(26.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("students_search_clear_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح البحث", modifier = Modifier.size(24.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stage Filters
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val stages = listOf("الكل", "المرحلة الثانوية", "المرحلة الإعدادية", "المرحلة الابتدائية")
                items(stages) { stage ->
                    FilterChip(
                        selected = selectedStageFilter == stage,
                        onClick = { selectedStageFilter = stage },
                        label = { Text(stage, fontSize = 12.sp) }
                    )
                }
            }

            // Status Filters & Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val statuses = listOf("الكل", "نشط", "موقوف", "منسحب")
                    items(statuses) { status ->
                        FilterChip(
                            selected = selectedStatusFilter == status,
                            onClick = { selectedStatusFilter = status },
                            label = { Text(status, fontSize = 11.sp) }
                        )
                    }
                }

                // Export / Share students list
                IconButton(onClick = {
                    val sb = StringBuilder()
                    sb.append("قائمة الطلاب المسجلين (${filteredStudents.size} طالب)\n")
                    sb.append("───────────────────────\n")
                    filteredStudents.forEachIndexed { i, s ->
                        sb.append("${i + 1}. ${s.fullName} - ${s.grade}\n")
                        sb.append("   هاتف: ${s.phone} | ولي الأمر: ${s.parentPhone}\n")
                    }
                    ExportHelper.shareText(context, "قائمة الطلاب", sb.toString())
                }) {
                    Icon(Icons.Default.Share, contentDescription = "تصدير القائمة", modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Count header
            Text(
                text = "عدد الطلاب: ${filteredStudents.size} من إجمالي ${students.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Students List
            if (filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (students.isEmpty()) {
                            "لا يوجد طلاب مسجلون بعد. اضغط + لإضافة أول طالب."
                        } else {
                            "لا يوجد طلاب يطابقون خيارات البحث"
                        },
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val studentEnrollments = enrollments.filter { it.studentId == student.id }
                        val enrolledGroups = groups.filter { g -> studentEnrollments.any { it.groupId == g.id } }

                        StudentItemCard(
                            student = student,
                            groups = enrolledGroups,
                            subscriptions = subscriptions,
                            payments = payments,
                            currency = currency,
                            onCardClick = { onOpenProfile(student.id) },
                            onStatementClick = { onOpenStatement(student.id) },
                            onOpenPaymentForm = { studentForPaymentForm = student },
                            onWhatsAppStudent = {
                                val msg = WhatsAppHelper.buildWelcomeMessage(
                                    studentName = student.fullName,
                                    groupName = enrolledGroups.firstOrNull()?.name ?: "",
                                    grade = student.grade,
                                    centerName = settings?.centerName ?: "",
                                    baseTemplate = settings?.welcomeMessageTemplate ?: ""
                                )
                                WhatsAppHelper.openWhatsAppChat(context, student.phone, msg)
                            },
                            onWhatsAppParent = {
                                val studentSubs = subscriptions.filter { it.studentId == student.id }
                                val studentPayments = payments.filter { it.studentId == student.id }
                                val totalPaid = studentPayments.sumOf { it.amountPaid }
                                val totalRemaining = (studentSubs.sumOf { it.expectedAmount } - totalPaid).coerceAtLeast(0.0)
                                val msg = WhatsAppHelper.buildStudentReportSummaryMessage(
                                    studentName = student.fullName,
                                    groupName = enrolledGroups.firstOrNull()?.name ?: "",
                                    attendancePercent = 100,
                                    presentCount = 0,
                                    absentCount = 0,
                                    totalPaid = totalPaid,
                                    totalRemaining = totalRemaining,
                                    currency = currency,
                                    centerName = settings?.centerName ?: ""
                                )
                                WhatsAppHelper.openWhatsAppChat(context, student.parentPhone, msg)
                            },
                            onComposeWhatsApp = {
                                composerTarget = student to enrolledGroups.firstOrNull()
                            },
                            onEditClick = { editingStudent = student },
                            onArchiveClick = { studentToArchive = student },
                            onQuickPayMonth = onQuickPayMonth,
                            onCancelMonthPayment = onCancelMonthPayment
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Add Student Floating Action Button
        FloatingActionButton(
            onClick = { onOpenAddStudent() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة طالب جديد", modifier = Modifier.size(36.dp))
        }
    }

    // Add / Edit Student Dialog
    if (showAddDialog || editingStudent != null) {
        val isEditing = editingStudent != null
        StudentFormDialog(
            initialStudent = editingStudent,
            groups = groups,
            defaultFee = settings?.defaultFee ?: 250.0,
            onDismiss = {
                showAddDialog = false
                editingStudent = null
            },
            onSave = { student, chosenGroupId ->
                if (isEditing) {
                    onUpdateStudent(student)
                } else {
                    onAddStudent(student, chosenGroupId)
                }
                showAddDialog = false
                editingStudent = null
            }
        )
    }

    // Delete Confirmation Dialog
    studentToArchive?.let { s ->
        AlertDialog(
            onDismissRequest = { studentToArchive = null },
            title = { Text("تأكيد حذف الطالب", fontWeight = FontWeight.Bold, color = EduErrorRed) },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف الطالب \"${s.fullName}\"؟\n\nسيتم إزالة الطالب من قائمة الطلاب والمجموعات الدراسية (مع الاحتفاظ بالسجلات المحاسبية والمالية القديمة للأمان المالي)."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onArchiveStudent(s.id, s.fullName)
                        studentToArchive = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف الطالب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentToArchive = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    composerTarget?.let { (targetStudent, targetGroup) ->
        val stSubs = subscriptions.filter { it.studentId == targetStudent.id }
        val stPayments = payments.filter { it.studentId == targetStudent.id }
        val totalPaid = stPayments.sumOf { it.amountPaid }
        val totalRem = (stSubs.sumOf { it.expectedAmount } - totalPaid).coerceAtLeast(0.0)
        WhatsAppMessageComposerDialog(
            student = targetStudent,
            group = targetGroup,
            settings = settings,
            initialTemplate = if (totalRem > 0) WhatsAppMessageTemplateType.FEE_REMINDER else WhatsAppMessageTemplateType.WELCOME,
            initialRecipientMode = if (targetStudent.parentPhone.isNotBlank()) WhatsAppRecipientMode.PARENT_PHONE else WhatsAppRecipientMode.STUDENT_PHONE,
            totalPaid = totalPaid,
            totalRemaining = totalRem,
            remainingAmount = if (totalRem > 0) totalRem else targetStudent.monthlyFee,
            onDismiss = { composerTarget = null }
        )
    }

    studentForPaymentForm?.let { targetStudent ->
        StudentPaymentSubscriptionDialog(
            initialStudentId = targetStudent.id,
            students = students,
            groups = groups,
            enrollments = enrollments,
            subscriptions = subscriptions,
            settings = settings,
            onDismiss = { studentForPaymentForm = null },
            onConfirm = { sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, monthStatus ->
                onRecordStudentSubscriptionAndPayment(
                    sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, monthStatus
                )
                studentForPaymentForm = null
            }
        )
    }
}

@Composable
fun StudentItemCard(
    student: StudentEntity,
    groups: List<GroupEntity>,
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    currency: String,
    onCardClick: () -> Unit,
    onStatementClick: () -> Unit,
    onOpenPaymentForm: () -> Unit,
    onWhatsAppStudent: () -> Unit,
    onWhatsAppParent: () -> Unit,
    onComposeWhatsApp: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onQuickPayMonth: (studentId: String, monthName: String) -> Unit,
    onCancelMonthPayment: (studentId: String, monthName: String) -> Unit
) {
    val context = LocalContext.current
    var monthToCancel by remember { mutableStateOf<String?>(null) }

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = student.fullName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${student.educationalStage} • ${student.grade}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (student.status) {
                        "نشط" -> eduSuccessContainerColor()
                        "موقوف" -> eduWarningContainerColor()
                        else -> eduErrorContainerColor()
                    }
                ) {
                    Text(
                        text = student.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (student.status) {
                            "نشط" -> eduSuccessContentColor()
                            "موقوف" -> eduWarningContentColor()
                            else -> eduErrorContentColor()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Months Row directly under student's name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 شهور الاشتراك (اضغط على الشهر = تم الدفع):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            val academicMonths = PaymentInputHelper.academicOrderMonths
            val studentSubs = subscriptions.filter { it.studentId == student.id }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(academicMonths) { monthName ->
                    val sub = studentSubs.find { it.monthYear.contains(monthName) }
                    val isPaid = sub?.status == "مدفوع بالكامل" || sub?.status == "مدفوع"
                    val isPartial = sub?.status == "دفع جزئي"
                    val isExempt = sub?.status == "معفى"

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isPaid -> EduSuccessGreen
                            isPartial -> EduWarningAmber
                            isExempt -> EduSkyTertiary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .testTag("student_month_chip_${student.id}_$monthName")
                            .clickable {
                                if (isPaid) {
                                    monthToCancel = monthName
                                } else {
                                    onQuickPayMonth(student.id, monthName)
                                    Toast.makeText(context, "تم تحديث حالة شهر $monthName إلى (مدفوع) ✓", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPaid) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "مدفوع",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = when {
                                    isPaid -> "$monthName (مدفوع ✓)"
                                    isPartial -> "$monthName (جزئي)"
                                    isExempt -> "$monthName (معفى)"
                                    else -> "$monthName (غير مدفوع)"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPaid || isPartial || isExempt) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            monthToCancel?.let { mName ->
                AlertDialog(
                    onDismissRequest = { monthToCancel = null },
                    title = { Text("تحديث حالة شهر $mName", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("اشتراك شهر $mName مسجل حالياً كـ \"مدفوع\" للطالب ${student.fullName}.\n\nهل ترغب في إلغاء الدفعة وتغييره إلى \"غير مدفوع\"؟")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onCancelMonthPayment(student.id, mName)
                                Toast.makeText(context, "تم تحديث شهر $mName إلى غير مدفوع", Toast.LENGTH_SHORT).show()
                                monthToCancel = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                        ) {
                            Text("تغيير إلى غير مدفوع")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { monthToCancel = null }) {
                            Text("إبقاء كمدفوع")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Groups info
            if (groups.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "المجموعات: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = groups.joinToString("، ") { it.name },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Phones info with quick copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        WhatsAppHelper.copyToClipboard(context, "هاتف الطالب", student.phone)
                    }
                ) {
                    Text(text = "طالب: ${student.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
                if (student.parentPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            WhatsAppHelper.copyToClipboard(context, "هاتف ولي الأمر", student.parentPhone)
                        }
                    ) {
                        Text(text = "ولي أمر: ${student.parentPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Payment Form Trigger Button per Student
            Button(
                onClick = onOpenPaymentForm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("student_payment_form_button_${student.id}")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نموذج تسجيل دفعة وتحديث حالة الشهر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Student
                Button(
                    onClick = onWhatsAppStudent,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 11.sp, color = Color.White)
                }

                // Ready formatted message composer
                OutlinedButton(
                    onClick = onComposeWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("رسالة جاهزة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Parent quick icon or button if available
                if (student.parentPhone.isNotBlank()) {
                    IconButton(onClick = onWhatsAppParent, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Chat, contentDescription = "واتساب ولي الأمر", modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Account Statement icon
                IconButton(onClick = onStatementClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "كشف الحساب", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                }

                // Edit
                IconButton(onClick = onEditClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
                }

                // Delete
                IconButton(onClick = onArchiveClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الطالب", modifier = Modifier.size(28.dp), tint = eduErrorContentColor())
                }
            }
        }
    }
}

@Composable
fun StudentFormDialog(
    initialStudent: StudentEntity?,
    groups: List<GroupEntity>,
    defaultFee: Double,
    onDismiss: () -> Unit,
    onSave: (StudentEntity, String?) -> Unit
) {
    val isEdit = initialStudent != null
    var name by remember { mutableStateOf(initialStudent?.fullName ?: "") }
    var phone by remember { mutableStateOf(initialStudent?.phone ?: "") }
    var parentPhone by remember { mutableStateOf(initialStudent?.parentPhone ?: "") }
    var stage by remember { mutableStateOf(initialStudent?.educationalStage ?: "المرحلة الثانوية") }
    var grade by remember { mutableStateOf(initialStudent?.grade ?: "الصف الثالث الثانوي") }
    var subject by remember { mutableStateOf(initialStudent?.subject ?: "عام") }
    var feeText by remember { mutableStateOf((initialStudent?.monthlyFee ?: defaultFee).toInt().toString()) }
    var status by remember { mutableStateOf(initialStudent?.status ?: "نشط") }
    var notes by remember { mutableStateOf(initialStudent?.notes ?: "") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id) }

    val stages = listOf("المرحلة الثانوية", "المرحلة الإعدادية", "المرحلة الابتدائية")
    val statuses = listOf("نشط", "موقوف", "منسحب")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "تعديل بيانات الطالب" else "إضافة طالب جديد",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم بالكامل *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم هاتف الطالب *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("رقم هاتف ولي الأمر *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text(text = "المرحلة الدراسية:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        stages.forEach { stg ->
                            FilterChip(
                                selected = stage == stg,
                                onClick = { stage = stg },
                                label = { Text(stg, fontSize = 10.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("الصف الدراسي (مثال: 3 ثانوي)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("المادة الدراسية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                if (!isEdit && groups.isNotEmpty()) {
                    item {
                        Text(text = "إلحاق بمجموعة مبدئياً:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(groups) { g ->
                                FilterChip(
                                    selected = selectedGroupId == g.id,
                                    onClick = { selectedGroupId = g.id },
                                    label = { Text(g.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("قيمة الاشتراك الشهري الافتراضية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text(text = "حالة الطالب:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        statuses.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val student = (initialStudent ?: StudentEntity(
                            id = UUID.randomUUID().toString(),
                            fullName = name,
                            phone = phone,
                            parentPhone = parentPhone,
                            educationalStage = stage,
                            grade = grade,
                            subject = subject,
                            registrationDate = dateFormat.format(Date()),
                            status = status,
                            monthlyFee = feeText.toDoubleOrNull() ?: defaultFee,
                            notes = notes
                        )).copy(
                            fullName = name,
                            phone = phone,
                            parentPhone = parentPhone,
                            educationalStage = stage,
                            grade = grade,
                            subject = subject,
                            status = status,
                            monthlyFee = feeText.toDoubleOrNull() ?: defaultFee,
                            notes = notes
                        )
                        onSave(student, selectedGroupId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (isEdit) "حفظ التعديلات" else "إضافة الطالب")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
