package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.OverdueItem
import com.example.ui.ScreenDestination
import com.example.ui.StudentAttendanceStat
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.eduErrorContainerColor
import com.example.ui.theme.eduErrorContentColor
import com.example.ui.theme.eduSuccessContainerColor
import com.example.ui.theme.eduSuccessContentColor
import com.example.ui.theme.eduWarningContentColor
import com.example.util.WhatsAppHelper

@Composable
fun DashboardScreen(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity> = emptyList(),
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    sessions: List<AttendanceSessionEntity>,
    attendanceRecords: List<AttendanceRecordEntity>,
    overdueItems: List<OverdueItem>,
    frequentAbsentees: List<StudentAttendanceStat>,
    settings: AppSettingsEntity?,
    onNavigate: (ScreenDestination) -> Unit,
    onOpenStudentProfile: (String) -> Unit,
    onOpenPaymentDialog: (MonthlySubscriptionEntity) -> Unit,
    onOpenGroupDetail: (String) -> Unit = {},
    onOpenAttendanceForGroup: (String) -> Unit = {},
    onAddGroup: (GroupEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"
    var showAddGroupDialog by remember { mutableStateOf(false) }

    // Statistics Calculations
    val totalStudents = students.size
    val activeStudents = students.count { it.status == "نشط" }
    val totalGroups = groups.size

    val totalExpectedAmount = subscriptions.sumOf { it.expectedAmount }
    val totalPaidAmount = payments.sumOf { it.amountPaid }
    val totalRemainingAmount = (totalExpectedAmount - totalPaidAmount).coerceAtLeast(0.0)

    val paidSubsCount = subscriptions.count { it.status == "مدفوع بالكامل" || it.status == "مدفوع" }
    val partialSubsCount = subscriptions.count { it.status == "دفع جزئي" }
    val unpaidSubsCount = subscriptions.count { it.status == "لم يدفع" || it.status == "غير مدفوع" }

    // Today / Recent attendance summary
    val totalPresent = attendanceRecords.count { it.status == "حاضر" }
    val totalAbsent = attendanceRecords.count { it.status == "غائب" }
    val totalLate = attendanceRecords.count { it.status == "متأخر" }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Welcome Banner Card
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "أهلاً يا مستر 👋",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val displayAcademicYear = settings?.academicYear?.trim().orEmpty()
                            if (displayAcademicYear.isNotEmpty() && displayAcademicYear != "2024 - 2025") {
                                Text(
                                    text = "العام الدراسي: $displayAcademicYear",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "لوحة التحكم الحية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "نظام متكامل لإدارة الحضور والغياب، الاشتراكات والمدفوعات بدون إنترنت مع الربط المباشر مع أولياء الأمور.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Quick Action Row
        item {
            Text(
                text = "الإجراءات السريعة",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.PersonAdd,
                    title = "إضافة طالب",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ScreenDestination.ADD_STUDENT) }
                )
                QuickActionButton(
                    icon = Icons.Default.Class,
                    title = "إضافة مجموعة",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ScreenDestination.GROUPS) }
                )
                QuickActionButton(
                    icon = Icons.Default.HowToReg,
                    title = "تسجيل حضور",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ScreenDestination.ATTENDANCE) }
                )
                QuickActionButton(
                    icon = Icons.Default.Receipt,
                    title = "الاشتراكات",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ScreenDestination.PAYMENTS) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Chat,
                    title = "تذكيرات واتساب",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ScreenDestination.WHATSAPP_NOTICES) }
                )
                QuickActionButton(
                    icon = Icons.Default.Warning,
                    title = "المتأخرون بالسداد",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(ScreenDestination.OVERDUE) }
                )
            }
        }

        // Main Study Groups Section (قائمة المجموعات الدراسية + إضافة مجموعة جديدة)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المجموعات الدراسية (${groups.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "عرض الكل",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigate(ScreenDestination.GROUPS) }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                    FilledTonalButton(
                        onClick = { showAddGroupDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dashboard_add_group_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة مجموعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_empty_groups_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Class,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد مجموعات دراسية مسجلة بعد",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ابدأ بإضافة مجموعتك الأولى لتنظيم المواعيد والطلاب والحضور",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddGroupDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة أول مجموعة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(groups, key = { it.id }) { group ->
                val groupStudentsCount = enrollments.count { it.groupId == group.id && it.isActive }
                DashboardGroupSummaryCard(
                    group = group,
                    studentsCount = groupStudentsCount,
                    currency = currency,
                    onOpenDetails = { onOpenGroupDetail(group.id) },
                    onTakeAttendance = { onOpenAttendanceForGroup(group.id) }
                )
            }
        }

        // Financial & Students KPI Stat Cards Grid
        item {
            Text(
                text = "الإحصائيات الرئيسية (نظرة عامة)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي الطلاب",
                        primaryValue = "$totalStudents طالب",
                        subtitle = "$activeStudents طالب نشط",
                        icon = Icons.Default.People,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).clickable { onNavigate(ScreenDestination.STUDENTS) }
                    )
                    StatCard(
                        title = "المجموعات الدراسية",
                        primaryValue = "$totalGroups مجموعات",
                        subtitle = "مواعيد منتظمة",
                        icon = Icons.Default.Class,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f).clickable { onNavigate(ScreenDestination.GROUPS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي التحصيل",
                        primaryValue = "${totalPaidAmount.toInt()} $currency",
                        subtitle = "مستحق: ${totalExpectedAmount.toInt()} $currency",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = eduSuccessContentColor(),
                        modifier = Modifier.weight(1f).clickable { onNavigate(ScreenDestination.FINANCIAL_REPORTS) }
                    )
                    StatCard(
                        title = "المبالغ المتأخرة",
                        primaryValue = "${totalRemainingAmount.toInt()} $currency",
                        subtitle = "${overdueItems.size} اشتراك متأخر",
                        icon = Icons.Default.MoneyOff,
                        accentColor = eduErrorContentColor(),
                        modifier = Modifier.weight(1f).clickable { onNavigate(ScreenDestination.OVERDUE) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "الحضور والغياب",
                        primaryValue = "$totalPresent حضور",
                        subtitle = "$totalAbsent غياب • $totalLate متأخر",
                        icon = Icons.Default.HowToReg,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f).clickable { onNavigate(ScreenDestination.ATTENDANCE_REPORTS) }
                    )
                    StatCard(
                        title = "حالة الاشتراكات",
                        primaryValue = "$paidSubsCount مدفوع",
                        subtitle = "$partialSubsCount جزئي • $unpaidSubsCount لم يدفع",
                        icon = Icons.Default.EventNote,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).clickable { onNavigate(ScreenDestination.PAYMENTS) }
                    )
                }
            }
        }

        // Frequent Absentees Warning Card
        if (frequentAbsentees.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = eduErrorContainerColor()),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = eduErrorContentColor(),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تنبيه غياب متكرر (${frequentAbsentees.size} طلاب)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = eduErrorContentColor()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "هؤلاء الطلاب تجاوزوا حد الغياب المحدد (${settings?.absenceAlertThreshold ?: 3} مرات). اضغط لإرسال إخطار لولي الأمر عبر واتساب:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        frequentAbsentees.take(3).forEach { stat ->
                            val student = students.find { it.id == stat.studentId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stat.studentName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "غاب ${stat.absentCount} حصص (نسبة حضور: ${stat.attendancePercentage}%)",
                                        fontSize = 11.sp,
                                        color = eduErrorContentColor()
                                    )
                                }
                                if (student != null && (student.parentPhone.isNotBlank() || student.phone.isNotBlank())) {
                                    FilledTonalButton(
                                        onClick = {
                                            val targetPhone = student.parentPhone.ifBlank { student.phone }
                                            val msg = WhatsAppHelper.buildStudentReportSummaryMessage(
                                                studentName = student.fullName,
                                                groupName = "",
                                                attendancePercent = stat.attendancePercentage,
                                                presentCount = stat.presentCount,
                                                absentCount = stat.absentCount,
                                                totalPaid = 0.0,
                                                totalRemaining = 0.0,
                                                currency = currency,
                                                centerName = settings?.centerName ?: ""
                                            )
                                            WhatsAppHelper.openWhatsAppChat(context, targetPhone, msg)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = EduErrorRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("واتساب ولي الأمر", fontSize = 11.sp)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = EduErrorRed.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        // Overdue Students Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الطلاب المتأخرون في السداد (${overdueItems.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "عرض الكل",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigate(ScreenDestination.OVERDUE) }
                )
            }
        }

        // Overdue Items list preview
        if (overdueItems.isEmpty()) {
            item {
                Surface(
                    color = eduSuccessContainerColor(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ممتاز! لا يوجد أي مبالغ متأخرة على الطلاب حالياً.",
                        color = eduSuccessContentColor(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(overdueItems.take(4)) { item ->
                OverdueRowCard(
                    item = item,
                    currency = currency,
                    onPayClick = { onOpenPaymentDialog(item.subscription) },
                    onWhatsAppReminder = {
                        val phone = item.student.parentPhone.ifBlank { item.student.phone }
                        val msg = WhatsAppHelper.buildFeeReminderMessage(
                            studentName = item.student.fullName,
                            groupName = item.group?.name ?: "",
                            monthYear = item.subscription.monthYear,
                            remainingAmount = item.remaining,
                            currency = currency,
                            centerName = settings?.centerName ?: "",
                            baseTemplate = settings?.feeReminderTemplate ?: ""
                        )
                        WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                    },
                    onProfileClick = { onOpenStudentProfile(item.student.id) }
                )
            }
        }

        // Recent Payments Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أحدث المدفوعات المسجلة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "سجل المدفوعات",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigate(ScreenDestination.PAYMENTS) }
                )
            }
        }

        if (payments.isEmpty()) {
            item {
                Text(
                    text = "لا توجد مدفوعات مسجلة بعد.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            items(payments.take(4)) { payment ->
                val student = students.find { it.id == payment.studentId }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = student?.fullName ?: "طالب غير معروف",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${payment.paymentDateFormatted} • طريقة: ${payment.paymentMethod}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (payment.notes.isNotBlank()) {
                                Text(
                                    text = payment.notes,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = eduSuccessContainerColor()
                        ) {
                            Text(
                                text = "+${payment.amountPaid.toInt()} $currency",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = eduSuccessContentColor(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button to add a new study group right from Home
        ExtendedFloatingActionButton(
            onClick = { showAddGroupDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dashboard_add_group_fab"),
            icon = {
                Icon(Icons.Default.Add, contentDescription = "إضافة مجموعة جديدة", modifier = Modifier.size(28.dp))
            },
            text = {
                Text("إضافة مجموعة جديدة", fontWeight = FontWeight.Bold)
            }
        )
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            defaultFee = settings?.defaultFee ?: 250.0,
            onDismiss = { showAddGroupDialog = false },
            onSave = { newGroup ->
                onAddGroup(newGroup)
                showAddGroupDialog = false
                Toast.makeText(context, "تمت إضافة المجموعة \"${newGroup.name}\" بنجاح ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun DashboardGroupSummaryCard(
    group: GroupEntity,
    studentsCount: Int,
    currency: String,
    onOpenDetails: () -> Unit,
    onTakeAttendance: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_group_card_${group.id}")
            .clickable { onOpenDetails() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Class,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.subject} • ${group.grade}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$studentsCount/${group.maxStudents}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.daysOfWeek} • ${group.sessionTime}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = eduSuccessContainerColor()
                ) {
                    Text(
                        text = "${group.defaultMonthlyFee.toInt()} $currency/شهر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = eduSuccessContentColor(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (group.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group.location,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("تفاصيل المجموعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onTakeAttendance,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل الحضور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = primaryValue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OverdueRowCard(
    item: OverdueItem,
    currency: String,
    onPayClick: () -> Unit,
    onWhatsAppReminder: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.clickable { onProfileClick() }) {
                    Text(
                        text = item.student.fullName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.group?.name ?: "مجموعة"} • ${item.subscription.monthYear}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "متبقي: ${item.remaining.toInt()} $currency",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = eduErrorContentColor()
                    )
                    Text(
                        text = if (item.totalPaid > 0) "دفع جزئي: ${item.totalPaid.toInt()}" else "لم يدفع",
                        fontSize = 10.sp,
                        color = if (item.totalPaid > 0) eduWarningContentColor() else eduErrorContentColor()
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduSuccessGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تسجيل دفع", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onWhatsAppReminder,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("واتساب ولي الأمر", fontSize = 11.sp)
                }
            }
        }
    }
}
