package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.StudentEntity
import com.example.ui.components.AbsenceNoticeBatchDialog
import com.example.ui.components.CloudSyncConfigDialog
import com.example.ui.theme.EduErrorContainer
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyContainer
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import com.example.ui.theme.EduWarningContainer
import com.example.util.AttendanceCloudSyncService
import com.example.util.ExportHelper
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AttendanceScreen(
    initialGroupId: String?,
    groups: List<GroupEntity>,
    students: List<StudentEntity>,
    enrollments: List<EnrollmentEntity>,
    sessions: List<AttendanceSessionEntity> = emptyList(),
    records: List<AttendanceRecordEntity> = emptyList(),
    settings: AppSettingsEntity?,
    isOnline: Boolean = false,
    isSyncing: Boolean = false,
    pendingSyncCount: Int = 0,
    lastSyncFeedback: String? = null,
    onSaveSession: (AttendanceSessionEntity, List<AttendanceRecordEntity>) -> Unit,
    onTriggerSync: () -> Unit = {},
    onUpdateCloudSyncUrl: (String) -> Unit = {},
    onClearSyncFeedback: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var selectedGroupId by remember {
        mutableStateOf(initialGroupId ?: groups.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(initialGroupId, groups) {
        if (!initialGroupId.isNullOrBlank() && groups.any { it.id == initialGroupId }) {
            selectedGroupId = initialGroupId
        } else if (selectedGroupId.isBlank() && groups.isNotEmpty()) {
            selectedGroupId = groups.first().id
        }
    }

    val currentGroup = groups.find { it.id == selectedGroupId }
    var editingSessionId by remember(selectedGroupId) { mutableStateOf<String?>(null) }
    var sessionDate by remember { mutableStateOf(dateFormat.format(Date())) }
    var sessionTime by remember(selectedGroupId) {
        mutableStateOf(currentGroup?.sessionTime?.ifBlank { timeFormat.format(Date()) } ?: timeFormat.format(Date()))
    }
    var lessonTopic by remember(selectedGroupId) { mutableStateOf("") }
    var teacherName by remember(settings?.teacherName) { mutableStateOf(settings?.teacherName ?: "") }

    val groupEnrollments = enrollments.filter { it.groupId == selectedGroupId && it.isActive }
    val enrolledStudents = students.filter { s -> groupEnrollments.any { it.studentId == s.id } }

    // Map of studentId -> attendance status ("حاضر", "غائب", "متأخر", "غياب بعذر")
    val attendanceMap = remember(selectedGroupId, enrolledStudents) {
        mutableStateMapOf<String, String>().apply {
            enrolledStudents.forEach { s ->
                this[s.id] = "حاضر"
            }
        }
    }

    val notesMap = remember(selectedGroupId) {
        mutableStateMapOf<String, String>()
    }

    val presentCount = enrolledStudents.count { (attendanceMap[it.id] ?: "حاضر") == "حاضر" }
    val absentCount = enrolledStudents.count { attendanceMap[it.id] == "غائب" }
    val lateCount = enrolledStudents.count { attendanceMap[it.id] == "متأخر" }
    val excusedCount = enrolledStudents.count { attendanceMap[it.id] == "غياب بعذر" }

    val groupSessions = remember(sessions, selectedGroupId) {
        sessions.filter { it.groupId == selectedGroupId }
            .sortedWith(compareByDescending<AttendanceSessionEntity> { it.sessionDate }.thenByDescending { it.createdAt })
    }
    val matchingDateSession = remember(groupSessions, sessionDate) {
        groupSessions.firstOrNull { it.sessionDate == sessionDate }
    }

    var showAbsenceDialog by remember { mutableStateOf(false) }
    var showCloudConfigDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cloud Sync & Connectivity Banner
        item {
            AttendanceCloudSyncBanner(
                isOnline = isOnline,
                isSyncing = isSyncing,
                pendingSyncCount = pendingSyncCount,
                hasCloudEndpoint = AttendanceCloudSyncService.isValidCloudEndpoint(settings?.cloudSyncUrl.orEmpty()),
                lastSyncFeedback = lastSyncFeedback,
                onTriggerSync = onTriggerSync,
                onOpenCloudConfig = { showCloudConfigDialog = true },
                onClearFeedback = onClearSyncFeedback
            )
        }

        // Group Selector Chips per Group
        item {
            Text(
                text = "اختر المجموعة الدراسية لتسجيل الحضور:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups, key = { it.id }) { g ->
                    val count = enrollments.count { it.groupId == g.id && it.isActive }
                    FilterChip(
                        selected = selectedGroupId == g.id,
                        onClick = { selectedGroupId = g.id },
                        label = { Text("${g.name} ($count)", fontSize = 12.sp) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("attendance_group_chip_${g.id}")
                    )
                }
            }

            if (currentGroup != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${currentGroup.subject} • ${currentGroup.daysOfWeek}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${currentGroup.location} • ${enrolledStudents.size} طالب مسجل",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (currentGroup.sessionTime.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentGroup.sessionTime, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Session Metadata Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingSessionId != null) "تعديل كشف الحصة المحفوظ" else "بيانات الحصة الدراسية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (matchingDateSession != null && editingSessionId != matchingDateSession.id) {
                            OutlinedButton(
                                onClick = {
                                    editingSessionId = matchingDateSession.id
                                    sessionTime = matchingDateSession.sessionTime
                                    lessonTopic = matchingDateSession.lessonTopic
                                    teacherName = matchingDateSession.teacherName
                                    val savedRecs = records.filter { it.sessionId == matchingDateSession.id }
                                    savedRecs.forEach { rec ->
                                        attendanceMap[rec.studentId] = rec.status
                                        notesMap[rec.studentId] = rec.notes
                                    }
                                    Toast.makeText(context, "تم تحميل كشف تاريخ $sessionDate للتعديل", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                            ) {
                                Text("تحميل كشف اليوم", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sessionDate,
                            onValueChange = { sessionDate = it },
                            label = { Text("تاريخ الحصة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sessionTime,
                            onValueChange = { sessionTime = it },
                            label = { Text("موعد الحصة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lessonTopic,
                        onValueChange = { lessonTopic = it },
                        label = { Text("موضوع الدرس أو محتوى الحصة") },
                        placeholder = { Text("مثال: حل مسائل الديناميكا الحرارية...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Quick Attendance Counters & "تحديد الجميع حاضرًا" button
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusCounter(text = "$presentCount حاضر", bg = EduSuccessContainer, fg = EduSuccessGreen)
                    StatusCounter(text = "$absentCount غائب", bg = EduErrorContainer, fg = EduErrorRed)
                    StatusCounter(text = "$lateCount متأخر", bg = EduWarningContainer, fg = EduWarningAmber)
                    StatusCounter(text = "$excusedCount عذر", bg = EduSkyContainer, fg = EduSkyTertiary)
                }

                Button(
                    onClick = {
                        enrolledStudents.forEach { s -> attendanceMap[s.id] = "حاضر" }
                        Toast.makeText(context, "تم تحديد جميع الطلاب (${enrolledStudents.size}) كحاضرين", Toast.LENGTH_SHORT).show()
                    },
                    enabled = enrolledStudents.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("mark_all_present_button")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديد الجميع حاضرًا", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // WhatsApp Absence Notice Banner
        if (absentCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EduErrorContainer,
                    border = BorderStroke(1.dp, EduErrorRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAbsenceDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = EduErrorRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "إرسال إشعارات الغياب عبر واتساب ($absentCount طالب غائب)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduErrorRed
                                )
                                Text(
                                    text = "اضغط لفتح نافذة إرسال رسائل الغياب الفورية لأولياء الأمور",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EduErrorRed, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Students Attendance Rows
        if (enrolledStudents.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا يوجد طلاب ملتحقون بهذه المجموعة حتى الآن. برجاء إضافة طلاب للمجموعة من قسم المجموعات.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(enrolledStudents, key = { it.id }) { student ->
                val currentStatus = attendanceMap[student.id] ?: "حاضر"
                val studentNote = notesMap[student.id] ?: ""

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    text = student.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ولي الأمر: ${student.parentPhone}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if ((currentStatus == "غائب" || currentStatus == "غياب بعذر") && student.parentPhone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val msg = WhatsAppHelper.buildAbsenceMessage(
                                            studentName = student.fullName,
                                            groupName = currentGroup?.name ?: "",
                                            sessionDate = sessionDate,
                                            lessonTopic = lessonTopic,
                                            centerName = settings?.centerName ?: "سنتر التفوق والريادة",
                                            baseTemplate = settings?.absenceNoticeTemplate ?: ""
                                        )
                                        WhatsAppHelper.openWhatsAppChat(context, student.parentPhone, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "إخطار غياب واتساب", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("إخطار واتساب", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("حاضر", EduSuccessGreen, EduSuccessContainer),
                                Triple("غائب", EduErrorRed, EduErrorContainer),
                                Triple("متأخر", EduWarningAmber, EduWarningContainer),
                                Triple("غياب بعذر", EduSkyTertiary, EduSkyContainer)
                            ).forEach { (status, color, _) ->
                                val isSelected = currentStatus == status
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clickable { attendanceMap[student.id] = status }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = status,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (currentStatus != "حاضر") {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = studentNote,
                                onValueChange = { notesMap[student.id] = it },
                                placeholder = { Text(if (currentStatus == "متأخر") "دقائق التأخير وملاحظات..." else "سبب الغياب / العذر...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Local Save & Cloud Sync Action Button
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EduSuccessContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, EduSuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetSessionId = editingSessionId ?: UUID.randomUUID().toString()
                                val session = AttendanceSessionEntity(
                                    id = targetSessionId,
                                    groupId = selectedGroupId,
                                    sessionDate = sessionDate,
                                    sessionTime = sessionTime,
                                    teacherName = teacherName,
                                    lessonTopic = lessonTopic,
                                    createdAt = System.currentTimeMillis(),
                                    isSynced = false,
                                    syncStatus = if (isOnline) "بانتظار المزامنة" else "بانتظار توفر الإنترنت"
                                )
                                val sessionRecords = enrolledStudents.map { s ->
                                    val st = attendanceMap[s.id] ?: "حاضر"
                                    AttendanceRecordEntity(
                                        id = UUID.randomUUID().toString(),
                                        sessionId = targetSessionId,
                                        studentId = s.id,
                                        groupId = selectedGroupId,
                                        status = st,
                                        notes = notesMap[s.id] ?: "",
                                        excuseReason = if (st == "غياب بعذر") notesMap[s.id] else null
                                    )
                                }
                                onSaveSession(session, sessionRecords)
                                editingSessionId = null
                                Toast.makeText(
                                    context,
                                    if (isOnline) {
                                        "تم حفظ الكشف محليًا وجاري مزامنته مع السحابة..."
                                    } else {
                                        "تم حفظ الكشف محليًا — ستتم المزامنة مع السحابة تلقائيًا عند توفر الإنترنت"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                                if (absentCount > 0) {
                                    showAbsenceDialog = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduSuccessGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("save_local_attendance_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ الكشف المحلي", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "يحفظ الكشف في قاعدة البيانات المحلية فورًا، وتتم مزامنته مع السحابة تلقائيًا عند توفر الاتصال بالإنترنت.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Saved Attendance History & Sync Queue for Selected Group
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل كشوف المجموعة وحالة المزامنة (${groupSessions.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (groupSessions.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val json = AttendanceCloudSyncService.buildBatchExportJson(
                                sessions = groupSessions,
                                allRecords = records,
                                groups = groups,
                                students = students,
                                centerName = settings?.centerName ?: "سنتر التفوق والريادة"
                            )
                            ExportHelper.shareText(
                                context,
                                "كشوف حضور ${currentGroup?.name ?: ""}",
                                json
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "تصدير كشوف المجموعة JSON",
                            tint = EduTealPrimary
                        )
                    }
                }
            }
        }

        if (groupSessions.isEmpty()) {
            item {
                Text(
                    text = "لم يتم حفظ كشوف حضور لهذه المجموعة بعد.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            items(groupSessions, key = { it.id }) { sess ->
                val sessRecords = records.filter { it.sessionId == sess.id }
                val sessPresent = sessRecords.count { it.status == "حاضر" }
                val sessAbsent = sessRecords.count { it.status == "غائب" }
                val statusColor = when {
                    sess.isSynced -> EduSuccessGreen
                    sess.syncStatus.contains("فشل") -> EduErrorRed
                    else -> EduWarningAmber
                }
                val statusBg = when {
                    sess.isSynced -> EduSuccessContainer
                    sess.syncStatus.contains("فشل") -> EduErrorContainer
                    else -> EduWarningContainer
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${sess.sessionDate} • ${sess.sessionTime}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (sess.lessonTopic.isNotBlank()) {
                                    Text(
                                        text = sess.lessonTopic,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (sess.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sess.syncStatus,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إجمالي: ${sessRecords.size} • حاضر: $sessPresent • غائب: $sessAbsent",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (!sess.isSynced) {
                                OutlinedButton(
                                    onClick = onTriggerSync,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مزامنة", fontSize = 11.sp)
                                }
                            }
                        }

                        if (!sess.syncError.isNullOrBlank()) {
                            Text(
                                text = "ملاحظة المزامنة: ${sess.syncError}",
                                fontSize = 10.sp,
                                color = EduErrorRed
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showAbsenceDialog) {
        val absentStudents = enrolledStudents.filter {
            attendanceMap[it.id] == "غائب" || attendanceMap[it.id] == "غياب بعذر"
        }
        AbsenceNoticeBatchDialog(
            absentStudents = absentStudents,
            groupName = currentGroup?.name ?: "",
            sessionDate = sessionDate,
            lessonTopic = lessonTopic,
            settings = settings,
            onDismiss = { showAbsenceDialog = false }
        )
    }

    if (showCloudConfigDialog) {
        CloudSyncConfigDialog(
            currentUrl = settings?.cloudSyncUrl.orEmpty(),
            pendingSyncCount = pendingSyncCount,
            onDismiss = { showCloudConfigDialog = false },
            onSaveUrl = onUpdateCloudSyncUrl,
            onExportPendingJson = {
                val pending = sessions.filter { !it.isSynced }
                val targetList = if (pending.isNotEmpty()) pending else groupSessions
                val json = AttendanceCloudSyncService.buildBatchExportJson(
                    sessions = targetList,
                    allRecords = records,
                    groups = groups,
                    students = students,
                    centerName = settings?.centerName ?: "سنتر التفوق والريادة"
                )
                ExportHelper.shareText(context, "كشوف الحضور المعلقة JSON", json)
            }
        )
    }
}

@Composable
private fun AttendanceCloudSyncBanner(
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingSyncCount: Int,
    hasCloudEndpoint: Boolean,
    lastSyncFeedback: String?,
    onTriggerSync: () -> Unit,
    onOpenCloudConfig: () -> Unit,
    onClearFeedback: () -> Unit
) {
    val bannerBg = when {
        pendingSyncCount > 0 -> EduWarningContainer.copy(alpha = 0.7f)
        isOnline -> EduSuccessContainer.copy(alpha = 0.65f)
        else -> EduSkyContainer.copy(alpha = 0.6f)
    }
    val iconTint = when {
        pendingSyncCount > 0 -> EduWarningAmber
        isOnline -> EduSuccessGreen
        else -> EduSkyTertiary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bannerBg,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_sync_banner")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = EduTealPrimary
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                !isOnline -> Icons.Default.CloudOff
                                pendingSyncCount > 0 -> Icons.Default.CloudQueue
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = "حالة الاتصال والمزامنة",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isOnline) "متصل بالإنترنت • مزامنة سحابية نشطة" else "غير متصل بالإنترنت • الوضع المحلي (أوفلاين)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                pendingSyncCount > 0 && !isOnline ->
                                    "يوجد $pendingSyncCount كشف محفوظ محليًا وبانتظار عودة الإنترنت للمزامنة"
                                pendingSyncCount > 0 && !hasCloudEndpoint ->
                                    "يوجد $pendingSyncCount كشف محلي — اضغط «إعداد السحابة» لإضافة رابط الرفع"
                                pendingSyncCount > 0 ->
                                    "يوجد $pendingSyncCount كشف جاهز للمزامنة السحابية"
                                else ->
                                    "جميع كشوف الحضور متزامنة ومؤمّنة بالكامل"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTriggerSync,
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("sync_now_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pendingSyncCount > 0) "مزامنة الآن ($pendingSyncCount)" else "فحص ومزامنة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenCloudConfig,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("configure_cloud_sync_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعداد السحابة", fontSize = 12.sp)
                }
            }

            if (!lastSyncFeedback.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearFeedback() }
                ) {
                    Text(
                        text = lastSyncFeedback,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCounter(text: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
