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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.OverdueItem
import com.example.ui.UpcomingPaymentItem
import com.example.ui.components.WhatsAppMessageComposerDialog
import com.example.ui.components.WhatsAppRecipientMode
import com.example.ui.theme.EduErrorContainer
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import com.example.ui.theme.EduWarningContainer
import com.example.util.WhatsAppHelper
import com.example.util.WhatsAppMessageTemplateType

@Composable
fun WhatsAppNoticesScreen(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity>,
    sessions: List<AttendanceSessionEntity>,
    attendanceRecords: List<AttendanceRecordEntity>,
    overdueItems: List<OverdueItem>,
    upcomingPayments: List<UpcomingPaymentItem> = emptyList(),
    settings: AppSettingsEntity?,
    onUpdateSettings: (AppSettingsEntity) -> Unit
) {
    val context = LocalContext.current
    val centerName = settings?.centerName ?: "سنتر التفوق والريادة"
    val currency = settings?.currency ?: "جنيه"

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "إشعارات الغياب",
        "تذكيرات الاشتراكات",
        "تذكيرات مواعيد الحصص",
        "قوالب الرسائل"
    )

    // Sent status tracker for students during this session
    val sentNoticeStudents = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مركز رسائل وتذكيرات واتساب",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "إرسال إشعارات الغياب الفورية وتذكيرات السداد ومواعيد الحصص للطلاب وأولياء الأمور",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> AbsenceNoticesTab(
                students = students,
                groups = groups,
                sessions = sessions,
                records = attendanceRecords,
                settings = settings,
                centerName = centerName,
                sentMap = sentNoticeStudents
            )
            1 -> FeeRemindersTab(
                overdueItems = overdueItems,
                upcomingPayments = upcomingPayments,
                groups = groups,
                settings = settings,
                centerName = centerName,
                currency = currency,
                sentMap = sentNoticeStudents
            )
            2 -> ClassRemindersTab(
                groups = groups,
                students = students,
                enrollments = enrollments,
                settings = settings,
                centerName = centerName
            )
            3 -> TemplatesTab(
                settings = settings,
                onUpdateSettings = onUpdateSettings
            )
        }
    }
}

/**
 * تبويب إشعارات الغياب للطلاب
 */
@Composable
private fun AbsenceNoticesTab(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    sessions: List<AttendanceSessionEntity>,
    records: List<AttendanceRecordEntity>,
    settings: AppSettingsEntity?,
    centerName: String,
    sentMap: MutableMap<String, Boolean>
) {
    val context = LocalContext.current
    var selectedGroupId by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var targetPhoneRecipient by remember { mutableStateOf("parent") } // "parent" or "student"

    val studentMap = remember(students) { students.associateBy { it.id } }
    val groupMap = remember(groups) { groups.associateBy { it.id } }
    val sessionMap = remember(sessions) { sessions.associateBy { it.id } }

    // Filter absent records
    val absentRecords = remember(records, sessions, selectedGroupId, searchQuery) {
        records.filter { it.status == "غائب" || it.status == "غياب بعذر" }
            .filter { rec ->
                val session = sessionMap[rec.sessionId]
                val student = studentMap[rec.studentId]
                val matchesGroup = selectedGroupId == "الكل" || session?.groupId == selectedGroupId || rec.groupId == selectedGroupId
                val matchesSearch = searchQuery.isBlank() ||
                        student?.fullName?.contains(searchQuery, ignoreCase = true) == true ||
                        student?.phone?.contains(searchQuery) == true
                matchesGroup && matchesSearch
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Controls: Group Filter & Recipient Toggle
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "فلترة حسب المجموعة الدراسية:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedGroupId == "الكل",
                                onClick = { selectedGroupId = "الكل" },
                                label = { Text("جميع المجموعات", fontSize = 11.sp) }
                            )
                        }
                        items(groups) { g ->
                            FilterChip(
                                selected = selectedGroupId == g.id,
                                onClick = { selectedGroupId = g.id },
                                label = { Text(g.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إرسال الإشعار إلى:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = targetPhoneRecipient == "parent",
                                onClick = { targetPhoneRecipient = "parent" },
                                label = { Text("ولي الأمر (موصى به)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = targetPhoneRecipient == "student",
                                onClick = { targetPhoneRecipient = "student" },
                                label = { Text("الطالب", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Summary Bar
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (absentRecords.isNotEmpty()) EduErrorContainer else EduSuccessContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (absentRecords.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (absentRecords.isNotEmpty()) EduErrorRed else EduSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (absentRecords.isNotEmpty())
                                "إجمالي حالات الغياب المسجلة: ${absentRecords.size} طالب"
                            else "لا توجد حالات غياب مسجلة مطابقة للبحث",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (absentRecords.isNotEmpty()) EduErrorRed else EduSuccessGreen
                        )
                    }
                }
            }
        }

        // Absent Students List
        if (absentRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ممتاز! لا يوجد طلاب غائبون مسجلون.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(absentRecords, key = { it.id }) { rec ->
                val student = studentMap[rec.studentId]
                val session = sessionMap[rec.sessionId]
                val group = groupMap[session?.groupId ?: rec.groupId]
                val isSent = sentMap[rec.id] == true

                val phoneToSend = if (targetPhoneRecipient == "parent") {
                    if (student?.parentPhone.isNullOrBlank()) student?.phone ?: "" else student?.parentPhone ?: ""
                } else {
                    student?.phone ?: ""
                }

                val sessionDateStr = session?.sessionDate ?: "تاريخ الحصة"
                val topicStr = session?.lessonTopic ?: ""
                val groupNameStr = group?.name ?: "المجموعة التعليمية"

                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student?.fullName ?: "طالب",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$groupNameStr • تاريخ: $sessionDateStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (topicStr.isNotBlank()) {
                                    Text(
                                        text = "موضوع الدرس: $topicStr",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSent) EduSuccessContainer else EduErrorContainer
                            ) {
                                Text(
                                    text = if (isSent) "تم الإرسال ✓" else "غائب",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSent) EduSuccessGreen else EduErrorRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "رقم الإرسال: $phoneToSend",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (targetPhoneRecipient == "parent") "هاتف ولي الأمر" else "هاتف الطالب",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Button(
                                onClick = {
                                    val formattedMsg = WhatsAppHelper.buildAbsenceMessage(
                                        studentName = student?.fullName ?: "الطالب",
                                        groupName = groupNameStr,
                                        sessionDate = sessionDateStr,
                                        lessonTopic = topicStr,
                                        centerName = centerName,
                                        baseTemplate = settings?.absenceNoticeTemplate ?: ""
                                    )
                                    WhatsAppHelper.openWhatsAppChat(context, phoneToSend, formattedMsg)
                                    sentMap[rec.id] = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSent) EduTealPrimary else Color(0xFF25D366)
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSent) "إعادة الإرسال" else "إرسال إشعار غياب",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * تبويب تذكيرات سداد الاشتراكات الشهرية
 */
@Composable
private fun FeeRemindersTab(
    overdueItems: List<OverdueItem>,
    upcomingPayments: List<UpcomingPaymentItem>,
    groups: List<GroupEntity>,
    settings: AppSettingsEntity?,
    centerName: String,
    currency: String,
    sentMap: MutableMap<String, Boolean>
) {
    val context = LocalContext.current
    var selectedGroupFilter by remember { mutableStateOf("الكل") }
    var recipientType by remember { mutableStateOf("parent") }

    val filteredItems = remember(overdueItems, selectedGroupFilter) {
        if (selectedGroupFilter == "الكل") overdueItems
        else overdueItems.filter { it.group?.id == selectedGroupFilter }
    }

    val filteredUpcoming = remember(upcomingPayments, selectedGroupFilter) {
        val base = if (selectedGroupFilter == "الكل") upcomingPayments
        else upcomingPayments.filter { it.group?.id == selectedGroupFilter }
        base.filter { it.daysRemaining <= 14 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "فلترة الطلاب المتأخرين بالسداد:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedGroupFilter == "الكل",
                                onClick = { selectedGroupFilter = "الكل" },
                                label = { Text("الكل (${overdueItems.size})", fontSize = 11.sp) }
                            )
                        }
                        items(groups) { g ->
                            val count = overdueItems.count { it.group?.id == g.id }
                            FilterChip(
                                selected = selectedGroupFilter == g.id,
                                onClick = { selectedGroupFilter = g.id },
                                label = { Text("${g.name} ($count)", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إرسال التذكير إلى:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = recipientType == "parent",
                                onClick = { recipientType = "parent" },
                                label = { Text("ولي الأمر", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = recipientType == "student",
                                onClick = { recipientType = "student" },
                                label = { Text("الطالب", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty() && filteredUpcoming.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد اشتراكات متأخرة أو مواعيد مستحقة قريباً.", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        if (filteredItems.isNotEmpty()) {
            items(filteredItems, key = { it.subscription.id }) { item ->
                val isSent = sentMap[item.subscription.id] == true
                val phone = if (recipientType == "parent") {
                    if (item.student.parentPhone.isNotBlank()) item.student.parentPhone else item.student.phone
                } else {
                    item.student.phone
                }

                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.student.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.group?.name ?: "المجموعة"} • اشتراك شهر: ${item.subscription.monthYear}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.remaining.toInt()} $currency",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduErrorRed
                                )
                                Text(
                                    text = "مستحق السداد",
                                    fontSize = 10.sp,
                                    color = EduErrorRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الهاتف: $phone",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    val msg = WhatsAppHelper.buildFeeReminderMessage(
                                        studentName = item.student.fullName,
                                        groupName = item.group?.name ?: "",
                                        monthYear = item.subscription.monthYear,
                                        remainingAmount = item.remaining,
                                        currency = currency,
                                        centerName = centerName,
                                        baseTemplate = settings?.feeReminderTemplate ?: ""
                                    )
                                    WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                                    sentMap[item.subscription.id] = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSent) EduTealPrimary else Color(0xFF25D366)
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSent) "تم التذكير ✓" else "إرسال تذكير بالسداد",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredUpcoming.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔔 تذكيرات المواعيد القادمة للدفع (خلال 14 يوماً):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(filteredUpcoming, key = { "upcoming_${it.student.id}" }) { up ->
                    val sentKey = "upcoming_${up.student.id}"
                    val isSent = sentMap[sentKey] == true
                    val phone = if (recipientType == "parent") {
                        up.student.parentPhone.ifBlank { up.student.phone }
                    } else {
                        up.student.phone
                    }
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = up.student.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${up.group?.name ?: "عام"} • الاستحقاق: ${up.dueDate}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${up.statusText} • ${up.expectedAmount.toInt()} $currency",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (up.daysRemaining <= 0) EduErrorRed else EduWarningAmber
                                )
                            }
                            Button(
                                onClick = {
                                    val msg = WhatsAppHelper.buildUpcomingPaymentReminderMessage(
                                        studentName = up.student.fullName,
                                        groupName = up.group?.name ?: "",
                                        dueDate = up.dueDate,
                                        daysRemaining = up.daysRemaining,
                                        expectedAmount = up.expectedAmount,
                                        currency = currency,
                                        centerName = centerName
                                    )
                                    WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                                    sentMap[sentKey] = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSent) EduTealPrimary else Color(0xFF25D366)
                                )
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSent) "تم ✓" else "تذكير بالموعد",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
    }
}

/**
 * تبويب تذكيرات مواعيد الحصص
 */
@Composable
private fun ClassRemindersTab(
    groups: List<GroupEntity>,
    students: List<StudentEntity>,
    enrollments: List<EnrollmentEntity>,
    settings: AppSettingsEntity?,
    centerName: String
) {
    val context = LocalContext.current
    var composerGroup by remember { mutableStateOf<GroupEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "إرسال تذكير بمواعيد الحصص القادمة للمجموعات:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (groups.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد مجموعات مسجلة.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(groups) { group ->
                val enrolledCount = enrollments.count { it.groupId == group.id && it.isActive }

                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = group.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${group.stage} • ${group.grade} • مادة: ${group.subject}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$enrolledCount طالب",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⏰ الموعد: ${group.daysOfWeek} • الساعة ${group.sessionTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "📍 القاعة / المكان: ${group.location}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (group.whatsappLink.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val msg = WhatsAppHelper.buildClassReminderMessage(
                                            groupName = group.name,
                                            daysOfWeek = group.daysOfWeek,
                                            sessionTime = group.sessionTime,
                                            location = group.location,
                                            centerName = centerName
                                        )
                                        WhatsAppHelper.openWhatsAppGroup(context, group.whatsappLink, msg)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح الجروب بالتذكير", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            OutlinedButton(
                                onClick = { composerGroup = group },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تخصيص وإرسال", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (composerGroup != null) {
        WhatsAppMessageComposerDialog(
            group = composerGroup,
            settings = settings,
            initialTemplate = WhatsAppMessageTemplateType.CLASS_REMINDER,
            initialRecipientMode = WhatsAppRecipientMode.GROUP_LINK,
            onDismiss = { composerGroup = null }
        )
    }
}

/**
 * تبويب إعدادات وتخصيص قوالب الرسائل
 */
@Composable
private fun TemplatesTab(
    settings: AppSettingsEntity?,
    onUpdateSettings: (AppSettingsEntity) -> Unit
) {
    val context = LocalContext.current
    var absenceTemplate by remember(settings) {
        mutableStateOf(settings?.absenceNoticeTemplate ?: "نحيطكم علمًا بغياب الطالب عن الحصة، ونرجو متابعة انتظامه في الحضور والتواصل معنا في حال وجود عذر.")
    }
    var feeReminderTemplate by remember(settings) {
        mutableStateOf(settings?.feeReminderTemplate ?: "نذكركم بسداد اشتراك الشهر الحالي، شكرًا لتعاونكم مع إدارة السنتر.")
    }
    var welcomeTemplate by remember(settings) {
        mutableStateOf(settings?.welcomeMessageTemplate ?: "أهلًا وسهلًا بك في مجموعتنا التعليمية، نتمنى لك التوفيق والنجاح.")
    }
    var paymentConfirmTemplate by remember(settings) {
        mutableStateOf(settings?.paymentConfirmationTemplate ?: "تم بحمد الله تسجيل سداد اشتراك الطالب بنجاح، شكرًا لتعاونكم وثقتكم بنا.")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "تخصيص نصوص الرسائل التلقائية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "يمكنك تعديل نصوص القوالب الافتراضية المستخدمة في رسائل الواتساب:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. قالب إشعار الغياب",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduErrorRed
                    )
                    OutlinedTextField(
                        value = absenceTemplate,
                        onValueChange = { absenceTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. قالب تذكير سداد الاشتراك",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduTealPrimary
                    )
                    OutlinedTextField(
                        value = feeReminderTemplate,
                        onValueChange = { feeReminderTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3. قالب الترحيب بالطلاب الجدد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EduSuccessGreen
                    )
                    OutlinedTextField(
                        value = welcomeTemplate,
                        onValueChange = { welcomeTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "4. قالب إشعار تأكيد الدفع",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = paymentConfirmTemplate,
                        onValueChange = { paymentConfirmTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val current = settings ?: AppSettingsEntity()
                            val updated = current.copy(
                                absenceNoticeTemplate = absenceTemplate.trim(),
                                feeReminderTemplate = feeReminderTemplate.trim(),
                                welcomeMessageTemplate = welcomeTemplate.trim(),
                                paymentConfirmationTemplate = paymentConfirmTemplate.trim()
                            )
                            onUpdateSettings(updated)
                            Toast.makeText(context, "تم حفظ قوالب رسائل الواتساب بنجاح! ✅", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التعديلات على القوالب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
