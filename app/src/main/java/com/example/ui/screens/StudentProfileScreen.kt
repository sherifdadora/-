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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.CheckCircle
import com.example.data.model.AppSettingsEntity
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import com.example.ui.UpcomingPaymentItem
import com.example.ui.components.StudentPaymentSubscriptionDialog
import com.example.ui.components.WhatsAppMessageComposerDialog
import com.example.ui.components.WhatsAppRecipientMode
import com.example.util.ExportHelper
import com.example.util.WhatsAppHelper
import com.example.util.WhatsAppMessageTemplateType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StudentProfileScreen(
    studentId: String?,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity>,
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    sessions: List<AttendanceSessionEntity>,
    records: List<AttendanceRecordEntity>,
    upcomingPayments: List<UpcomingPaymentItem> = emptyList(),
    settings: AppSettingsEntity?,
    onOpenStatement: (String) -> Unit,
    onOpenPaymentDialog: (MonthlySubscriptionEntity) -> Unit,
    onQuickPayMonth: (studentId: String, monthName: String) -> Unit = { _, _ -> },
    onCancelMonthPayment: (studentId: String, monthName: String) -> Unit = { _, _ -> },
    onDeleteStudent: (studentId: String, studentName: String) -> Unit = { _, _ -> },
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
    var monthToCancel by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSubscriptionPaymentDialog by remember { mutableStateOf(false) }
    var showWhatsAppComposer by remember { mutableStateOf(false) }
    val currency = settings?.currency ?: "جنيه"

    val student = students.find { it.id == studentId }
    if (student == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على الطالب المحدد", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val studentEnrollments = enrollments.filter { it.studentId == student.id }
    val enrolledGroups = groups.filter { g -> studentEnrollments.any { it.groupId == g.id } }
    val studentSubs = subscriptions.filter { it.studentId == student.id }.sortedByDescending { it.monthSortKey }
    val studentPayments = payments.filter { it.studentId == student.id }.sortedByDescending { it.paymentTimestamp }
    val studentRecords = records.filter { it.studentId == student.id }
    val sessionMap = sessions.associateBy { it.id }

    // Totals
    val totalExpected = studentSubs.sumOf { it.expectedAmount }
    val totalPaid = studentPayments.sumOf { it.amountPaid }
    val totalRemaining = (totalExpected - totalPaid).coerceAtLeast(0.0)

    val presentCount = studentRecords.count { it.status == "حاضر" }
    val absentCount = studentRecords.count { it.status == "غائب" }
    val lateCount = studentRecords.count { it.status == "متأخر" }
    val excusedCount = studentRecords.count { it.status == "غياب بعذر" }
    val totalSessions = studentRecords.size
    val attendancePercent = if (totalSessions > 0) ((presentCount + lateCount) * 100 / totalSessions) else 100

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("الاشتراكات والمدفوعات", "سجل الحضور والغياب", "المجموعات المشترك بها")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EduTealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.fullName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${student.educationalStage} • ${student.grade} • ${student.subject}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "تاريخ التسجيل: ${student.registrationDate} • الحالة: ${student.status}",
                                fontSize = 11.sp,
                                color = if (student.status == "نشط") EduSuccessGreen else EduErrorRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Contact Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val msg = WhatsAppHelper.buildWelcomeMessage(
                                    studentName = student.fullName,
                                    groupName = enrolledGroups.firstOrNull()?.name ?: "",
                                    grade = student.grade,
                                    centerName = settings?.centerName ?: "",
                                    baseTemplate = settings?.welcomeMessageTemplate ?: ""
                                )
                                WhatsAppHelper.openWhatsAppChat(context, student.phone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب الطالب", fontSize = 11.sp, color = Color.White)
                        }
                        if (student.parentPhone.isNotBlank()) {
                            Button(
                                onClick = {
                                    val msg = WhatsAppHelper.buildStudentReportSummaryMessage(
                                        studentName = student.fullName,
                                        groupName = enrolledGroups.firstOrNull()?.name ?: "",
                                        attendancePercent = attendancePercent,
                                        presentCount = presentCount,
                                        absentCount = absentCount,
                                        totalPaid = totalPaid,
                                        totalRemaining = totalRemaining,
                                        currency = currency,
                                        centerName = settings?.centerName ?: ""
                                    )
                                    WhatsAppHelper.openWhatsAppChat(context, student.parentPhone, msg)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب ولي الأمر", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showWhatsAppComposer = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنسيق وإرسال رسالة واتساب جاهزة", color = Color(0xFF075E54), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOpenStatement(student.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح وتصدير كشف حساب الطالب الكامل")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EduErrorRed),
                        border = BorderStroke(1.dp, EduErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = EduErrorRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف الطالب من النظام", color = EduErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Month Payment Row under Student Name
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📅 شهور الاشتراك (اضغط على الشهر = تم الدفع):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val academicMonths = listOf("أكتوبر", "نوفمبر", "ديسمبر", "يناير", "فبراير", "مارس", "أبريل", "مايو")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(academicMonths) { monthName ->
                            val sub = studentSubs.find { it.monthYear.contains(monthName) }
                            val isPaid = sub?.status == "مدفوع بالكامل" || sub?.status == "مدفوع"
                            val isPartial = sub?.status == "دفع جزئي"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isPaid -> EduSuccessGreen
                                    isPartial -> EduWarningAmber
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.clickable {
                                    if (isPaid) {
                                        monthToCancel = monthName
                                    } else {
                                        onQuickPayMonth(student.id, monthName)
                                        Toast.makeText(context, "تم سداد شهر $monthName بنجاح! ✅", Toast.LENGTH_SHORT).show()
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
                                            else -> "$monthName (غير مدفوع)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPaid || isPartial) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (monthToCancel != null) {
                        val mName = monthToCancel!!
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

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("تأكيد حذف الطالب", fontWeight = FontWeight.Bold, color = EduErrorRed) },
                            text = {
                                Text("هل أنت متأكد من رغبتك في حذف الطالب \"${student.fullName}\"؟\n\nسيتم إزالة الطالب من قائمة الطلاب والمجموعات الدراسية (مع الاحتفاظ بالسجلات المحاسبية والمالية القديمة للأمان المالي).")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteDialog = false
                                        onDeleteStudent(student.id, student.fullName)
                                        Toast.makeText(context, "تم حذف الطالب ${student.fullName} بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حذف الطالب", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showDeleteDialog = false }) {
                                    Text("إلغاء")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Upcoming Payment Due Date & Reminder Card
        item {
            val matchingUpcoming = upcomingPayments.find { it.student.id == student.id }
            val nextDueDateStr = if (!student.nextPaymentDueDate.isNullOrBlank()) {
                student.nextPaymentDueDate
            } else if (matchingUpcoming != null) {
                matchingUpcoming.dueDate
            } else {
                "غير محدد بعد"
            }

            val daysRemaining = matchingUpcoming?.daysRemaining ?: run {
                if (!student.nextPaymentDueDate.isNullOrBlank()) {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val d = sdf.parse(student.nextPaymentDueDate)
                        if (d != null) ((d.time - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() else 30
                    } catch (e: Exception) { 30 }
                } else 30
            }

            val statusText = when {
                daysRemaining < 0 -> "متأخر منذ ${-daysRemaining} يوم ⚠️"
                daysRemaining == 0 -> "مستحق الدفع اليوم ⏰"
                daysRemaining == 1 -> "مستحق غداً ⏳"
                else -> "متبقي $daysRemaining يوم"
            }

            val statusColor = when {
                daysRemaining < 0 -> EduErrorRed
                daysRemaining == 0 -> EduWarningAmber
                daysRemaining <= 5 -> EduWarningAmber
                else -> EduSuccessGreen
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "موعد الدفع القادم والتذكير",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "تاريخ الاستحقاق المحدد:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = nextDueDateStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "قيمة الاشتراك الشهري:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "${student.monthlyFee.toInt()} $currency",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduTealPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSubscriptionPaymentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل اشتراك / دفعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val recipientPhone = if (student.parentPhone.isNotBlank()) student.parentPhone else student.phone
                                val primaryGroupName = enrolledGroups.firstOrNull()?.name ?: "المجموعة العامة"
                                val msg = WhatsAppHelper.buildUpcomingPaymentReminderMessage(
                                    studentName = student.fullName,
                                    groupName = primaryGroupName,
                                    dueDate = nextDueDateStr,
                                    daysRemaining = daysRemaining,
                                    expectedAmount = student.monthlyFee,
                                    currency = currency,
                                    centerName = settings?.centerName ?: ""
                                )
                                WhatsAppHelper.openWhatsAppChat(context, recipientPhone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تذكير واتساب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Summary Statistics Row (Finances & Attendance)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "إجمالي المدفوع", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = "${totalPaid.toInt()} $currency", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                        Text(text = "من أصل ${totalExpected.toInt()} $currency", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "المبلغ المتبقي", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${totalRemaining.toInt()} $currency",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalRemaining > 0) EduErrorRed else EduSuccessGreen
                        )
                        Text(text = if (totalRemaining > 0) "غير مسدد" else "مسدد بالكامل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "نسبة الحضور", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = "$attendancePercent%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EduTealPrimary)
                        Text(text = "$presentCount حاضر • $absentCount غياب", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // Subscriptions & Payments Tab
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جدول الاشتراكات الشهرية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showSubscriptionPaymentDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل اشتراك / دفعة", fontSize = 11.sp)
                        }
                    }
                }
                if (studentSubs.isEmpty()) {
                    item {
                        Text("لا توجد اشتراكات مسجلة لهذا الطالب", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                } else {
                    items(studentSubs) { sub ->
                        val subPayments = studentPayments.filter { it.subscriptionId == sub.id }
                        val paid = subPayments.sumOf { it.amountPaid }
                        val rem = (sub.expectedAmount - paid).coerceAtLeast(0.0)

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = sub.monthYear, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (sub.status) {
                                            "مدفوع بالكامل", "مدفوع" -> EduSuccessContainer
                                            "دفع جزئي" -> EduWarningAmber.copy(alpha = 0.2f)
                                            "معفى" -> EduSkyTertiary.copy(alpha = 0.2f)
                                            else -> EduErrorRed.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = sub.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (sub.status) {
                                                "مدفوع بالكامل", "مدفوع" -> EduSuccessGreen
                                                "دفع جزئي" -> EduWarningAmber
                                                "معفى" -> EduSkyTertiary
                                                else -> EduErrorRed
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "قيمة الاشتراك: ${sub.expectedAmount.toInt()} $currency", fontSize = 12.sp)
                                    Text(text = "المدفوع: ${paid.toInt()} $currency", fontSize = 12.sp, color = EduSuccessGreen)
                                    Text(text = "المتبقي: ${rem.toInt()} $currency", fontSize = 12.sp, color = if (rem > 0) EduErrorRed else EduSuccessGreen, fontWeight = FontWeight.Bold)
                                }
                                if (rem > 0 && sub.status != "معفى" && sub.status != "مدفوع" && sub.status != "مدفوع بالكامل") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onOpenPaymentDialog(sub) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EduSuccessGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("تسجيل دفع لهذا الشهر (${rem.toInt()} $currency)")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "سجل العمليات المالية المنفذة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (studentPayments.isEmpty()) {
                    item {
                        Text("لا توجد عمليات دفع مسجلة بعد", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                } else {
                    items(studentPayments) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "+${p.amountPaid.toInt()} $currency", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                                    Text(text = "${p.paymentDateFormatted} • طريقة: ${p.paymentMethod}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    if (p.notes.isNotBlank()) {
                                        Text(text = p.notes, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.CheckCircle, contentDescription = "تم السداد", tint = EduSuccessGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            1 -> {
                // Attendance Records Tab
                item {
                    Text(
                        text = "تفاصيل الحصص والحضور والغياب",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (studentRecords.isEmpty()) {
                    item {
                        Text("لا توجد حصص مسجلة لهذا الطالب حتى الآن", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                } else {
                    items(studentRecords) { record ->
                        val session = sessionMap[record.sessionId]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "تاريخ الحصة: ${session?.sessionDate ?: "غير محدد"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!session?.lessonTopic.isNullOrBlank()) {
                                        Text(
                                            text = "الموضوع: ${session?.lessonTopic}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (record.notes.isNotBlank()) {
                                        Text(text = "ملاحظة: ${record.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (record.status) {
                                        "حاضر" -> EduSuccessContainer
                                        "متأخر" -> EduWarningAmber.copy(alpha = 0.2f)
                                        "غياب بعذر" -> EduSkyTertiary.copy(alpha = 0.2f)
                                        else -> EduErrorRed.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = record.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (record.status) {
                                            "حاضر" -> EduSuccessGreen
                                            "متأخر" -> EduWarningAmber
                                            "غياب بعذر" -> EduSkyTertiary
                                            else -> EduErrorRed
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Enrolled Groups Tab
                item {
                    Text(
                        text = "المجموعات المشترك بها الطالب",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (enrolledGroups.isEmpty()) {
                    item {
                        Text("الطالب غير ملتحق بأي مجموعة حالياً", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                } else {
                    items(enrolledGroups) { g ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = g.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${g.subject} • ${g.daysOfWeek} • ${g.sessionTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "المكان: ${g.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                if (g.whatsappLink.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { WhatsAppHelper.openWhatsAppGroup(context, g.whatsappLink) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("فتح جروب واتساب للمجموعة", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showSubscriptionPaymentDialog) {
        StudentPaymentSubscriptionDialog(
            initialStudentId = student.id,
            students = students,
            groups = groups,
            enrollments = enrollments,
            subscriptions = studentSubs,
            settings = settings,
            onDismiss = { showSubscriptionPaymentDialog = false },
            onConfirm = { sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, monthStatus ->
                showSubscriptionPaymentDialog = false
                onRecordStudentSubscriptionAndPayment(
                    sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, monthStatus
                )
            }
        )
    }

    if (showWhatsAppComposer) {
        WhatsAppMessageComposerDialog(
            student = student,
            group = enrolledGroups.firstOrNull(),
            settings = settings,
            initialTemplate = if (totalRemaining > 0) WhatsAppMessageTemplateType.FEE_REMINDER else WhatsAppMessageTemplateType.STUDENT_REPORT,
            initialRecipientMode = if (student.parentPhone.isNotBlank()) WhatsAppRecipientMode.PARENT_PHONE else WhatsAppRecipientMode.STUDENT_PHONE,
            attendancePercent = attendancePercent,
            presentCount = presentCount,
            absentCount = absentCount,
            totalPaid = totalPaid,
            totalRemaining = totalRemaining,
            remainingAmount = if (totalRemaining > 0) totalRemaining else student.monthlyFee,
            onDismiss = { showWhatsAppComposer = false }
        )
    }
}
