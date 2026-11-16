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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.eduErrorContainerColor
import com.example.ui.theme.eduErrorContentColor
import com.example.ui.theme.eduSkyContainerColor
import com.example.ui.theme.eduSuccessContainerColor
import com.example.ui.theme.eduSuccessContentColor
import com.example.ui.theme.eduWarningContainerColor
import com.example.ui.theme.eduWarningContentColor
import com.example.util.AttendanceCloudSyncService
import com.example.util.ExportHelper
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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
    onClearSyncFeedback: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToGroupDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var selectedGroupId by remember {
        mutableStateOf(initialGroupId?.takeIf { id -> groups.any { it.id == id } } ?: groups.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(initialGroupId, groups) {
        if (!initialGroupId.isNullOrBlank() && groups.any { it.id == initialGroupId }) {
            selectedGroupId = initialGroupId
        } else if (selectedGroupId.isBlank() || groups.none { it.id == selectedGroupId }) {
            selectedGroupId = groups.firstOrNull()?.id ?: ""
        }
    }

    val currentGroup = groups.find { it.id == selectedGroupId }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    var sessionDate by remember { mutableStateOf(dateFormat.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    var sessionTime by remember { mutableStateOf(currentGroup?.sessionTime?.ifBlank { timeFormat.format(Date()) } ?: timeFormat.format(Date())) }
    var lessonTopic by remember { mutableStateOf("") }
    var teacherName by remember(settings?.teacherName) { mutableStateOf(settings?.teacherName ?: "") }
    var studentSearchQuery by remember { mutableStateOf("") }

    val groupEnrollments = remember(enrollments, selectedGroupId) {
        enrollments.filter { it.groupId == selectedGroupId && it.isActive }
    }
    val enrolledStudents = remember(students, groupEnrollments) {
        val studentIds = groupEnrollments.map { it.studentId }.toSet()
        students.filter { it.id in studentIds }.sortedBy { it.fullName }
    }
    val filteredStudents = remember(enrolledStudents, studentSearchQuery) {
        if (studentSearchQuery.isBlank()) {
            enrolledStudents
        } else {
            val q = studentSearchQuery.trim()
            enrolledStudents.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                    it.phone.contains(q) ||
                    it.parentPhone.contains(q)
            }
        }
    }

    val groupSessions = remember(sessions, selectedGroupId) {
        sessions.filter { it.groupId == selectedGroupId }
            .sortedWith(compareByDescending<AttendanceSessionEntity> { it.sessionDate }.thenByDescending { it.createdAt })
    }
    val matchingDateSession = remember(groupSessions, sessionDate) {
        groupSessions.firstOrNull { it.sessionDate == sessionDate }
    }

    val attendanceMap = remember { mutableStateMapOf<String, String>() }
    val notesMap = remember { mutableStateMapOf<String, String>() }
    var editingSessionId by remember { mutableStateOf<String?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var loadedSessionKey by remember { mutableStateOf<String?>(null) }

    // Synchronize form state automatically with Room records when group, date, or saved session changes
    LaunchedEffect(selectedGroupId, sessionDate, matchingDateSession?.id, records, enrolledStudents) {
        val targetKey = "$selectedGroupId|$sessionDate|${matchingDateSession?.id.orEmpty()}"
        if (targetKey != loadedSessionKey) {
            editingSessionId = matchingDateSession?.id
            attendanceMap.clear()
            notesMap.clear()

            if (matchingDateSession != null) {
                sessionTime = matchingDateSession.sessionTime.ifBlank {
                    currentGroup?.sessionTime?.ifBlank { timeFormat.format(Date()) } ?: timeFormat.format(Date())
                }
                lessonTopic = matchingDateSession.lessonTopic
                val savedRecordsByStudent = records
                    .filter { it.sessionId == matchingDateSession.id }
                    .associateBy { it.studentId }

                enrolledStudents.forEach { student ->
                    val saved = savedRecordsByStudent[student.id]
                    val normalizedStatus = when (saved?.status) {
                        "غائب", "غياب بعذر" -> "غائب"
                        else -> "حاضر"
                    }
                    attendanceMap[student.id] = normalizedStatus
                    if (!saved?.notes.isNullOrBlank()) {
                        notesMap[student.id] = saved?.notes.orEmpty()
                    }
                }
            } else {
                sessionTime = currentGroup?.sessionTime?.ifBlank { timeFormat.format(Date()) } ?: timeFormat.format(Date())
                lessonTopic = ""
                enrolledStudents.forEach { student ->
                    attendanceMap[student.id] = "حاضر"
                }
            }

            hasUnsavedChanges = false
            loadedSessionKey = targetKey
        } else {
            // Ensure any newly added student in the same group/date gets a default status without overwriting user edits
            enrolledStudents.forEach { student ->
                if (!attendanceMap.containsKey(student.id)) {
                    attendanceMap[student.id] = "حاضر"
                }
            }
        }
    }

    val presentCount = enrolledStudents.count { (attendanceMap[it.id] ?: "حاضر") != "غائب" }
    val absentCount = enrolledStudents.count { attendanceMap[it.id] == "غائب" }

    var showAbsenceDialog by remember { mutableStateOf(false) }
    var showCloudConfigDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "لا توجد مجموعات دراسية بعد",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "قم بإنشاء مجموعة دراسية وإضافة الطلاب إليها أولاً لتتمكن من تسجيل الحضور والغياب.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onNavigateToGroups,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("الذهاب إلى المجموعات", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // 1. Group Selection & Quick Info Card
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المجموعة الدراسية",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        hasUnsavedChanges -> eduWarningContainerColor()
                                        matchingDateSession != null -> eduSuccessContainerColor()
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            hasUnsavedChanges -> "تعديلات غير محفوظة"
                                            matchingDateSession != null -> "محفوظ في Room ✓"
                                            else -> "كشف جديد"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            hasUnsavedChanges -> eduWarningContentColor()
                                            matchingDateSession != null -> eduSuccessContentColor()
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = groupMenuExpanded,
                                onExpandedChange = { groupMenuExpanded = !groupMenuExpanded }
                            ) {
                                val selectedLabel = currentGroup?.let { g ->
                                    val count = enrollments.count { it.groupId == g.id && it.isActive }
                                    "${g.name} ($count طالب)"
                                } ?: "اختر المجموعة"
                                OutlinedTextField(
                                    value = selectedLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("اختر المجموعة") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("attendance_group_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = groupMenuExpanded,
                                    onDismissRequest = { groupMenuExpanded = false }
                                ) {
                                    groups.forEach { g ->
                                        val count = enrollments.count { it.groupId == g.id && it.isActive }
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "${g.name} ($count طالب)",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (g.subject.isNotBlank() || g.daysOfWeek.isNotBlank()) {
                                                        Text(
                                                            text = listOf(g.subject, g.daysOfWeek, g.sessionTime)
                                                                .filter { it.isNotBlank() }
                                                                .joinToString(" • "),
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedGroupId = g.id
                                                groupMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (groups.size > 1) {
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
                            }

                            // Session Date, Time & Topic
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = sessionDate,
                                    onValueChange = { sessionDate = it },
                                    label = { Text("تاريخ الحصة") },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(
                                                Icons.Default.CalendarToday,
                                                contentDescription = "اختيار التاريخ",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("attendance_date_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sessionTime,
                                    onValueChange = {
                                        sessionTime = it
                                        hasUnsavedChanges = true
                                    },
                                    label = { Text("الموعد") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = lessonTopic,
                                onValueChange = {
                                    lessonTopic = it
                                    hasUnsavedChanges = true
                                },
                                label = { Text("عنوان أو ملاحظات الحصة (اختياري)") },
                                placeholder = { Text("مثال: مراجعة الوحدة الأولى") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // 2. Summary Counters & Quick Bulk Actions
                if (enrolledStudents.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AttendanceStatCard(
                                    label = "إجمالي الطلاب",
                                    count = enrolledStudents.size,
                                    bg = MaterialTheme.colorScheme.primaryContainer,
                                    fg = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceStatCard(
                                    label = "حاضر",
                                    count = presentCount,
                                    bg = eduSuccessContainerColor(),
                                    fg = eduSuccessContentColor(),
                                    modifier = Modifier.weight(1f)
                                )
                                AttendanceStatCard(
                                    label = "غائب",
                                    count = absentCount,
                                    bg = eduErrorContainerColor(),
                                    fg = eduErrorContentColor(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        enrolledStudents.forEach { s -> attendanceMap[s.id] = "حاضر" }
                                        hasUnsavedChanges = true
                                        Toast.makeText(
                                            context,
                                            "تم تحديد جميع الطلاب (${enrolledStudents.size}) كحاضرين",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EduSuccessGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("mark_all_present_button")
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تحديد الكل حاضر", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        enrolledStudents.forEach { s -> attendanceMap[s.id] = "غائب" }
                                        hasUnsavedChanges = true
                                        Toast.makeText(
                                            context,
                                            "تم تحديد جميع الطلاب (${enrolledStudents.size}) كغائبين",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, EduErrorRed.copy(alpha = 0.65f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = eduErrorContentColor()),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("mark_all_absent_button")
                                ) {
                                    Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تحديد الكل غائب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (enrolledStudents.size >= 5) {
                                OutlinedTextField(
                                    value = studentSearchQuery,
                                    onValueChange = { studentSearchQuery = it },
                                    placeholder = { Text("ابحث عن طالب بالاسم أو الهاتف...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        if (studentSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { studentSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("attendance_student_search")
                                )
                            }
                        }
                    }
                }

                // 3. WhatsApp Batch Absence Alert Banner
                if (absentCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = eduErrorContainerColor(),
                            border = BorderStroke(1.dp, eduErrorContentColor().copy(alpha = 0.4f)),
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
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = eduErrorContentColor(),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "إرسال إشعار غياب لولي الأمر ($absentCount طالب)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = eduErrorContentColor()
                                        )
                                        Text(
                                            text = "اضغط لإرسال رسائل واتساب الفورية للطلاب الغائبين",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = EduErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Student List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قائمة طلاب المجموعة (${filteredStudents.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentGroup != null && currentGroup.sessionTime.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentGroup.sessionTime,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 5. Group Students List with Clear "حاضر" / "غائب" Choices
                if (enrolledStudents.isEmpty()) {
                    item {
                        OutlinedCard(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "لا يوجد طلاب مسجلون في هذه المجموعة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "أضف طلاباً إلى المجموعة أولاً لتظهر أسماؤهم هنا لتسجيل الحضور والغياب.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedGroupId.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { onNavigateToGroupDetail(selectedGroupId) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إدارة طلاب المجموعة")
                                    }
                                }
                            }
                        }
                    }
                } else if (filteredStudents.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا يوجد طالب يطابق عبارة البحث \"$studentSearchQuery\".",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    itemsIndexed(filteredStudents, key = { _, s -> s.id }) { index, student ->
                        val currentStatus = if (attendanceMap[student.id] == "غائب") "غائب" else "حاضر"
                        val isPresent = currentStatus == "حاضر"
                        val studentNote = notesMap[student.id] ?: ""

                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attendance_student_card_${student.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPresent) eduSuccessContainerColor()
                                                    else eduErrorContainerColor()
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPresent) eduSuccessContentColor() else eduErrorContentColor()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = student.fullName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val contactLine = if (student.parentPhone.isNotBlank()) {
                                                "ولي الأمر: ${student.parentPhone}"
                                            } else {
                                                "الهاتف: ${student.phone}"
                                            }
                                            Text(
                                                text = contactLine,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!isPresent && student.parentPhone.isNotBlank()) {
                                        IconButton(
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
                                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Chat,
                                                contentDescription = "إرسال إخطار غياب واتساب",
                                                tint = EduErrorRed,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // Clear "حاضر" / "غائب" Toggle Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isPresent) EduSuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (!isPresent) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                attendanceMap[student.id] = "حاضر"
                                                hasUnsavedChanges = true
                                            }
                                            .testTag("student_present_${student.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isPresent) Color.White else eduSuccessContentColor(),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "حاضر",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPresent) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!isPresent) EduErrorRed else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isPresent) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                attendanceMap[student.id] = "غائب"
                                                hasUnsavedChanges = true
                                            }
                                            .testTag("student_absent_${student.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (!isPresent) Color.White else eduErrorContentColor(),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "غائب",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isPresent) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (!isPresent) {
                                    OutlinedTextField(
                                        value = studentNote,
                                        onValueChange = {
                                            notesMap[student.id] = it
                                            hasUnsavedChanges = true
                                        },
                                        placeholder = { Text("سبب الغياب أو ملاحظة (اختياري)...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Saved Attendance Sessions History in Room
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سجل حصص المجموعة المحفوظة (${groupSessions.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                                    contentDescription = "تصدير كشوف المجموعة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (groupSessions.isEmpty()) {
                    item {
                        Text(
                            text = "لم يتم حفظ كشوف حضور لهذه المجموعة في قاعدة البيانات بعد.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(groupSessions, key = { it.id }) { sess ->
                        val sessRecords = records.filter { it.sessionId == sess.id }
                        val sessPresent = sessRecords.count { it.status != "غائب" && it.status != "غياب بعذر" }
                        val sessAbsent = sessRecords.count { it.status == "غائب" || it.status == "غياب بعذر" }
                        val isCurrentlyLoaded = sess.sessionDate == sessionDate

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyLoaded) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (isCurrentlyLoaded) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sessionDate = sess.sessionDate
                                    Toast.makeText(
                                        context,
                                        "تم عرض كشف يوم ${sess.sessionDate}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
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
                                        color = if (sess.isSynced) eduSuccessContainerColor() else eduSkyContainerColor()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (sess.isSynced) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (sess.isSynced) eduSuccessContentColor() else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (sess.isSynced) "متزامن" else "محفوظ محلياً",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sess.isSynced) eduSuccessContentColor() else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "حاضر: $sessPresent • غائب: $sessAbsent • الإجمالي: ${sessRecords.size} (اضغط للعرض أو التعديل)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 7. Compact Cloud Sync Banner
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

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }

        // Sticky Bottom Save Bar for Room Database Persistence
        if (groups.isNotEmpty() && enrolledStudents.isNotEmpty()) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حاضر: $presentCount • غائب: $absentCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "التاريخ: $sessionDate",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = {
                            val targetSessionId = editingSessionId ?: UUID.randomUUID().toString()
                            val session = AttendanceSessionEntity(
                                id = targetSessionId,
                                groupId = selectedGroupId,
                                sessionDate = sessionDate.trim(),
                                sessionTime = sessionTime.trim(),
                                teacherName = teacherName,
                                lessonTopic = lessonTopic.trim(),
                                createdAt = System.currentTimeMillis(),
                                isSynced = false,
                                syncStatus = if (isOnline) "بانتظار المزامنة" else "بانتظار توفر الإنترنت"
                            )
                            val sessionRecords = enrolledStudents.map { student ->
                                val status = if (attendanceMap[student.id] == "غائب") "غائب" else "حاضر"
                                AttendanceRecordEntity(
                                    id = UUID.randomUUID().toString(),
                                    sessionId = targetSessionId,
                                    studentId = student.id,
                                    groupId = selectedGroupId,
                                    status = status,
                                    notes = notesMap[student.id]?.trim().orEmpty(),
                                    excuseReason = null
                                )
                            }
                            onSaveSession(session, sessionRecords)
                            hasUnsavedChanges = false
                            Toast.makeText(
                                context,
                                "تم حفظ حضور وغياب المجموعة (${enrolledStudents.size} طالب) في قاعدة البيانات بنجاح",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("save_local_attendance_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingSessionId != null) {
                                "تحديث الحضور والغياب في قاعدة البيانات"
                            } else {
                                "حفظ الحضور والغياب في قاعدة البيانات"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(sessionDate) {
            try {
                val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                utcFormat.parse(sessionDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            sessionDate = utcFormat.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAbsenceDialog) {
        val absentStudents = enrolledStudents.filter { attendanceMap[it.id] == "غائب" }
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
private fun AttendanceStatCard(
    label: String,
    count: Int,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = fg
            )
        }
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
        pendingSyncCount > 0 -> eduWarningContainerColor()
        isOnline -> eduSuccessContainerColor()
        else -> eduSkyContainerColor()
    }
    val iconTint = when {
        pendingSyncCount > 0 -> eduWarningContentColor()
        isOnline -> eduSuccessContentColor()
        else -> MaterialTheme.colorScheme.tertiary
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
                            color = MaterialTheme.colorScheme.primary
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isOnline) "متصل بالإنترنت • مزامنة سحابية نشطة" else "الوضع المحلي (أوفلاين)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                pendingSyncCount > 0 && !isOnline ->
                                    "يوجد $pendingSyncCount كشف محفوظ محلياً وبانتظار الإنترنت للمزامنة"
                                pendingSyncCount > 0 && !hasCloudEndpoint ->
                                    "يوجد $pendingSyncCount كشف محفوظ محلياً في Room"
                                pendingSyncCount > 0 ->
                                    "يوجد $pendingSyncCount كشف جاهز للمزامنة السحابية"
                                else ->
                                    "جميع كشوف الحضور محفوظة ومؤمّنة في Room"
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
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
