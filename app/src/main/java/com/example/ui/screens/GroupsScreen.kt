package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.components.WhatsAppMessageComposerDialog
import com.example.ui.components.WhatsAppRecipientMode
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.eduErrorContentColor
import com.example.ui.theme.eduSuccessContentColor
import com.example.ui.theme.eduWhatsAppTextColor
import com.example.util.WhatsAppHelper
import com.example.util.WhatsAppMessageTemplateType
import java.util.UUID

@Composable
fun GroupsScreen(
    groups: List<GroupEntity>,
    students: List<StudentEntity>,
    enrollments: List<EnrollmentEntity>,
    settings: AppSettingsEntity?,
    onOpenGroupDetail: (String) -> Unit,
    onOpenAttendance: (String) -> Unit,
    onAddGroup: (GroupEntity) -> Unit,
    onDeleteGroup: (groupId: String, groupName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"
    var showAddDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }
    var composerGroup by remember { mutableStateOf<GroupEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "المجموعات الدراسية (${groups.size} مجموعة)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "إدارة المواعيد، القاعات، روابط الواتساب، والطلاب المسجلين في كل مجموعة",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد مجموعات مسجلة بعد. اضغط + لإضافة أول مجموعة.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(groups, key = { it.id }) { group ->
                        val groupEnrollments = enrollments.filter { it.groupId == group.id && it.isActive }
                        val enrolledStudentsCount = groupEnrollments.size

                        GroupItemCard(
                            group = group,
                            studentsCount = enrolledStudentsCount,
                            currency = currency,
                            onClick = { onOpenGroupDetail(group.id) },
                            onAttendanceClick = { onOpenAttendance(group.id) },
                            onWhatsAppGroupClick = {
                                val msg = WhatsAppHelper.buildClassReminderMessage(
                                    groupName = group.name,
                                    daysOfWeek = group.daysOfWeek,
                                    sessionTime = group.sessionTime,
                                    location = group.location,
                                    centerName = settings?.centerName ?: ""
                                )
                                WhatsAppHelper.openWhatsAppGroup(context, group.whatsappLink, msg)
                            },
                            onComposeGroupMessageClick = {
                                composerGroup = group
                            },
                            onCopyLinkClick = {
                                WhatsAppHelper.copyToClipboard(context, "رابط مجموعة واتساب", group.whatsappLink)
                            },
                            onDeleteClick = {
                                groupToDelete = group
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Add Group Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة مجموعة جديدة", modifier = Modifier.size(36.dp))
        }
    }

    if (showAddDialog) {
        AddGroupDialog(
            defaultFee = settings?.defaultFee ?: 250.0,
            onDismiss = { showAddDialog = false },
            onSave = {
                onAddGroup(it)
                showAddDialog = false
            }
        )
    }

    groupToDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("تأكيد حذف المجموعة", fontWeight = FontWeight.Bold, color = EduErrorRed) },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف المجموعة \"${g.name}\"؟\n\nسيتم إزالة المجموعة من قائمة المجموعات وجدول الحصص مع الاحتفاظ بالسجلات المالية السابقة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(g.id, g.name)
                        groupToDelete = null
                        Toast.makeText(context, "تم حذف المجموعة ${g.name} بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف المجموعة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { groupToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
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

@Composable
fun GroupItemCard(
    group: GroupEntity,
    studentsCount: Int,
    currency: String,
    onClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onWhatsAppGroupClick: () -> Unit,
    onComposeGroupMessageClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${group.stage} • ${group.grade} • ${group.subject}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$studentsCount / ${group.maxStudents} طالب",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timetable & Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${group.daysOfWeek} • الساعة ${group.sessionTime}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.location,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "قيمة الاشتراك الشهري: ${group.defaultMonthlyFee.toInt()} $currency",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = eduSuccessContentColor()
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAttendanceClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الحضور", fontSize = 11.sp)
                }

                Button(
                    onClick = onComposeGroupMessageClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رسالة للجروب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (group.whatsappLink.isNotBlank()) {
                    IconButton(onClick = onWhatsAppGroupClick, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Chat, contentDescription = "فتح رابط الجروب", modifier = Modifier.size(26.dp), tint = eduWhatsAppTextColor())
                    }
                    IconButton(onClick = onCopyLinkClick, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ رابط واتساب", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف المجموعة",
                        modifier = Modifier.size(26.dp),
                        tint = eduErrorContentColor()
                    )
                }
            }
        }
    }
}

@Composable
fun AddGroupDialog(
    defaultFee: Double,
    onDismiss: () -> Unit,
    onSave: (GroupEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("المرحلة الثانوية") }
    var grade by remember { mutableStateOf("الصف الثالث الثانوي") }
    var days by remember { mutableStateOf("السبت، الثلاثاء") }
    var time by remember { mutableStateOf("04:30 م") }
    var location by remember { mutableStateOf("سنتر التفوق - قاعة 1") }
    var feeText by remember { mutableStateOf(defaultFee.toInt().toString()) }
    var whatsappLink by remember { mutableStateOf("") }
    var maxStudentsText by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إضافة مجموعة دراسية جديدة", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المجموعة *") },
                        placeholder = { Text("مثال: مجموعة التفوق - فيزياء 3ث") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("المادة الدراسية *") },
                        placeholder = { Text("فيزياء، كيمياء، أحياء...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_subject_input"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = stage,
                        onValueChange = { stage = it },
                        label = { Text("المرحلة الدراسية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("الصف الدراسي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it },
                        label = { Text("أيام الدراسة") },
                        placeholder = { Text("السبت، الثلاثاء") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("موعد الحصة") },
                        placeholder = { Text("04:30 م") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("مكان الحصة (السنتر / القاعة)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("سعر الاشتراك الشهري الافتراضي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = whatsappLink,
                        onValueChange = { whatsappLink = it },
                        label = { Text("رابط مجموعة واتساب (WhatsApp Group Link)") },
                        placeholder = { Text("https://chat.whatsapp.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = maxStudentsText,
                        onValueChange = { maxStudentsText = it },
                        label = { Text("الحد الأقصى للطلاب") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                    if (name.isNotBlank() && subject.isNotBlank()) {
                        val group = GroupEntity(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            subject = subject,
                            stage = stage,
                            grade = grade,
                            daysOfWeek = days,
                            sessionTime = time,
                            location = location,
                            defaultMonthlyFee = feeText.toDoubleOrNull() ?: defaultFee,
                            whatsappLink = whatsappLink,
                            maxStudents = maxStudentsText.toIntOrNull() ?: 30,
                            notes = notes
                        )
                        onSave(group)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                modifier = Modifier.testTag("confirm_add_group_button")
            ) {
                Text("إضافة المجموعة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
