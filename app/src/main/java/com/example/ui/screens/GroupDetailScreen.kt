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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.WhatsAppMessageComposerDialog
import com.example.ui.components.WhatsAppRecipientMode
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import com.example.util.WhatsAppHelper
import com.example.util.WhatsAppMessageTemplateType

@Composable
fun GroupDetailScreen(
    groupId: String?,
    groups: List<GroupEntity>,
    students: List<StudentEntity>,
    enrollments: List<EnrollmentEntity>,
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    sessions: List<AttendanceSessionEntity>,
    attendanceRecords: List<AttendanceRecordEntity>,
    settings: AppSettingsEntity?,
    onOpenStudentProfile: (String) -> Unit,
    onOpenAttendance: (String) -> Unit,
    onEnrollStudent: (studentId: String, groupId: String) -> Unit,
    onUnenrollStudent: (studentId: String, groupId: String) -> Unit,
    onDeleteGroup: (groupId: String, groupName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"

    val group = groups.find { it.id == groupId }
    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("المجموعة غير موجودة", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val groupEnrollments = enrollments.filter { it.groupId == group.id && it.isActive }
    val enrolledStudents = students.filter { s -> groupEnrollments.any { it.studentId == s.id } }
    val unenrolledStudents = students.filter { s -> groupEnrollments.none { it.studentId == s.id } }

    val groupSubs = subscriptions.filter { it.groupId == group.id }
    val groupPayments = payments.filter { it.groupId == group.id }
    val groupSessions = sessions.filter { it.groupId == group.id }
    val groupRecords = attendanceRecords.filter { it.groupId == group.id }

    val totalExpected = groupSubs.sumOf { it.expectedAmount }
    val totalPaid = groupPayments.sumOf { it.amountPaid }
    val totalRemaining = (totalExpected - totalPaid).coerceAtLeast(0.0)

    val presentCount = groupRecords.count { it.status == "حاضر" }
    val totalRecs = groupRecords.size
    val attendanceRate = if (totalRecs > 0) (presentCount * 100 / totalRecs) else 100

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var showGroupMessageComposer by remember { mutableStateOf(false) }
    var composerStudent by remember { mutableStateOf<StudentEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Group Header Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${group.stage} • ${group.grade} • مادة: ${group.subject}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${enrolledStudents.size} / ${group.maxStudents} طالب",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "📅 المواعيد: ${group.daysOfWeek} • الساعة ${group.sessionTime}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📍 القاعة: ${group.location}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenAttendance(group.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل الحضور للحصة", fontSize = 11.sp)
                        }

                        if (group.whatsappLink.isNotBlank()) {
                            Button(
                                onClick = { WhatsAppHelper.openWhatsAppGroup(context, group.whatsappLink) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح جروب واتساب", fontSize = 11.sp, color = Color.White)
                            }

                            IconButton(
                                onClick = { WhatsAppHelper.copyToClipboard(context, "رابط واتساب", group.whatsappLink) }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرابط", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showGroupMessageComposer = true },
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنسيق وإرسال رسالة جاهزة للمجموعة عبر واتساب", color = Color(0xFF075E54), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteGroupDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EduErrorRed),
                        border = BorderStroke(1.dp, EduErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = EduErrorRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف المجموعة من النظام", color = EduErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Financial & Attendance Stats for this Group
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
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "إيرادات المجموعة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = "${totalPaid.toInt()} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                        Text(text = "مستحق: ${totalExpected.toInt()} $currency", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "المبالغ المتأخرة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${totalRemaining.toInt()} $currency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalRemaining > 0) EduErrorRed else EduSuccessGreen
                        )
                        Text(text = "متبقي على الطلاب", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "نسبة حضور المجموعة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = "$attendanceRate%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduTealPrimary)
                        Text(text = "${groupSessions.size} حصة مسجلة", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Students in Group Header & Add Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الطلاب الملتحقون بالمجموعة (${enrolledStudents.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { showAddStudentDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة طالب للمجموعة", fontSize = 11.sp)
                }
            }
        }

        // Enrolled Students List
        if (enrolledStudents.isEmpty()) {
            item {
                Text(
                    text = "لم يتم إضافة أي طالب لهذه المجموعة بعد. اضغط \"إضافة طالب للمجموعة\" لربط الطلاب.",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(enrolledStudents) { student ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenStudentProfile(student.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = student.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "هاتف: ${student.phone} • ولي الأمر: ${student.parentPhone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { composerStudent = student }
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "رسالة واتساب للطالب", tint = Color(0xFF25D366))
                            }

                            IconButton(
                                onClick = { onUnenrollStudent(student.id, group.id) }
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "إلغاء الاشتراك من المجموعة", tint = EduErrorRed)
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

    // Add Student to Group Dialog
    if (showAddStudentDialog) {
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("إلحاق طالب بمجموعة ${group.name}", fontWeight = FontWeight.Bold) },
            text = {
                if (unenrolledStudents.isEmpty()) {
                    Text("جميع الطلاب المسجلين ملتحقون بالفعل بهذه المجموعة!")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(unenrolledStudents) { s ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onEnrollStudent(s.id, group.id)
                                    showAddStudentDialog = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = s.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = "${s.grade} • ${s.phone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = "إضافة", tint = EduTealPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showAddStudentDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showDeleteGroupDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("تأكيد حذف المجموعة", fontWeight = FontWeight.Bold, color = EduErrorRed) },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف المجموعة \"${group.name}\"؟\n\nسيتم إزالة المجموعة من قائمة المجموعات وجدول الحصص مع الاحتفاظ بالسجلات المحاسبية والمالية القديمة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteGroupDialog = false
                        onDeleteGroup(group.id, group.name)
                        Toast.makeText(context, "تم حذف المجموعة ${group.name} بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف المجموعة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showGroupMessageComposer) {
        WhatsAppMessageComposerDialog(
            group = group,
            settings = settings,
            initialTemplate = WhatsAppMessageTemplateType.CLASS_REMINDER,
            initialRecipientMode = WhatsAppRecipientMode.GROUP_LINK,
            onDismiss = { showGroupMessageComposer = false }
        )
    }

    if (composerStudent != null) {
        val st = composerStudent!!
        val stSubs = groupSubs.filter { it.studentId == st.id }
        val stPayments = groupPayments.filter { it.studentId == st.id }
        val stPaid = stPayments.sumOf { it.amountPaid }
        val stRem = (stSubs.sumOf { it.expectedAmount } - stPaid).coerceAtLeast(0.0)
        WhatsAppMessageComposerDialog(
            student = st,
            group = group,
            settings = settings,
            initialTemplate = if (stRem > 0) WhatsAppMessageTemplateType.FEE_REMINDER else WhatsAppMessageTemplateType.WELCOME,
            initialRecipientMode = if (st.parentPhone.isNotBlank()) WhatsAppRecipientMode.PARENT_PHONE else WhatsAppRecipientMode.STUDENT_PHONE,
            totalPaid = stPaid,
            totalRemaining = stRem,
            remainingAmount = if (stRem > 0) stRem else st.monthlyFee,
            onDismiss = { composerStudent = null }
        )
    }
}
