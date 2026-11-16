package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettingsScreen(
    settings: AppSettingsEntity?,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    auditLogs: List<AuditLogEntity>,
    syncStatus: String,
    onUpdateSettings: (AppSettingsEntity) -> Unit,
    onTriggerSync: () -> Unit
) {
    val context = LocalContext.current

    var teacherName by remember(settings) { mutableStateOf(settings?.teacherName ?: "") }
    var centerName by remember(settings) { mutableStateOf(settings?.centerName ?: "سنتر التفوق والريادة") }
    var academicYear by remember(settings) { mutableStateOf(settings?.academicYear?.takeIf { it != "2024 - 2025" }.orEmpty()) }
    var defaultFeeText by remember(settings) { mutableStateOf((settings?.defaultFee ?: 250.0).toInt().toString()) }
    var absenceThresholdText by remember(settings) { mutableStateOf((settings?.absenceAlertThreshold ?: 3).toString()) }
    var userRole by remember(settings) { mutableStateOf(settings?.userRole ?: "المعلم") }

    var welcomeTemplate by remember(settings) { mutableStateOf(settings?.welcomeMessageTemplate ?: "") }
    var reminderTemplate by remember(settings) { mutableStateOf(settings?.feeReminderTemplate ?: "") }
    var absenceTemplate by remember(settings) { mutableStateOf(settings?.absenceNoticeTemplate ?: "") }
    var cloudSyncUrl by remember(settings) { mutableStateOf(settings?.cloudSyncUrl ?: "") }

    val roles = listOf("المدير", "الموظف", "مسؤول التحصيل", "مسؤول الحضور")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App & Center Identity Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "بيانات المدرس والسنتر التعليمي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("اسم المدرس") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = centerName,
                        onValueChange = { centerName = it },
                        label = { Text("اسم السنتر أو المركز التعليمي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("العام الدراسي الحالي (اختياري)") },
                        placeholder = { Text("اتركه فارغاً أو أدخل العام الدراسي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = defaultFeeText,
                        onValueChange = { defaultFeeText = it },
                        label = { Text("سعر الاشتراك الافتراضي (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = absenceThresholdText,
                        onValueChange = { absenceThresholdText = it },
                        label = { Text("حد الغياب لتنبيه المدرس (عدد الحصص)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Roles & Permissions Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "الصلاحيات ونوع المستخدم الحالي", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "اختر دور المستخدم الحالي للتحكم في الصلاحيات والإجراءات:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        roles.forEach { r ->
                            FilterChip(
                                selected = userRole == r,
                                onClick = { userRole = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // WhatsApp Templates Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "قوالب رسائل واتساب الجاهزة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = welcomeTemplate,
                        onValueChange = { welcomeTemplate = it },
                        label = { Text("رسالة الترحيب بالطالب") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = reminderTemplate,
                        onValueChange = { reminderTemplate = it },
                        label = { Text("رسالة التذكير بسداد الاشتراك") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = absenceTemplate,
                        onValueChange = { absenceTemplate = it },
                        label = { Text("رسالة إخطار الغياب لولي الأمر") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Button(
                        onClick = {
                            val updated = (settings ?: AppSettingsEntity()).copy(
                                teacherName = teacherName,
                                centerName = centerName,
                                academicYear = academicYear,
                                defaultFee = defaultFeeText.toDoubleOrNull() ?: 250.0,
                                absenceAlertThreshold = absenceThresholdText.toIntOrNull() ?: 3,
                                userRole = userRole,
                                welcomeMessageTemplate = welcomeTemplate,
                                feeReminderTemplate = reminderTemplate,
                                absenceNoticeTemplate = absenceTemplate,
                                cloudSyncUrl = cloudSyncUrl.trim()
                            )
                            onUpdateSettings(updated)
                            Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتحديث جميع الإعدادات")
                    }
                }
            }
        }

        // Backup, Export & Offline Sync Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = EduTealPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "النسخ الاحتياطي والمزامنة الأوفلاين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduTealPrimary)
                    }

                    Text(
                        text = "حالة المزامنة: $syncStatus\nالبيانات مخزنة محلياً بالكامل بصيغة SQLite آمنة ولا يتم فقدها عند انقطاع الشبكة.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = cloudSyncUrl,
                        onValueChange = { cloudSyncUrl = it },
                        label = { Text("رابط الخادم السحابي للمزامنة (API / Webhook)") },
                        placeholder = { Text("https://example.com/api/attendance-sync") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val updated = (settings ?: AppSettingsEntity()).copy(
                                cloudSyncUrl = cloudSyncUrl.trim()
                            )
                            onUpdateSettings(updated)
                            Toast.makeText(context, "تم حفظ رابط المزامنة السحابية", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ رابط السحابة", fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTriggerSync,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مزامنة يدوية", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
                                val backupJson = buildBackupJson(students, groups, subscriptions, payments)
                                ExportHelper.shareText(
                                    context,
                                    "نسخة احتياطية - نظام المجموعات ${timeFormat.format(Date())}",
                                    backupJson
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EduSuccessGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير نسخة JSON", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Audit Logs (سجل العمليات والتدقيق التاريخي)
        item {
            Text(
                text = "سجل العمليات والتدقيق التاريخي (Audit Log)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                Text("لا توجد سجلات بعد.", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
            }
        } else {
            items(auditLogs.take(15)) { log ->
                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = log.actionType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = log.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = timeFormat.format(Date(log.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun buildBackupJson(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    subs: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>
): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("  \"version\": 1,\n")
    sb.append("  \"exportDate\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\",\n")
    sb.append("  \"studentsCount\": ${students.size},\n")
    sb.append("  \"groupsCount\": ${groups.size},\n")
    sb.append("  \"subscriptionsCount\": ${subs.size},\n")
    sb.append("  \"paymentsCount\": ${payments.size},\n")
    sb.append("  \"students\": [\n")
    students.forEachIndexed { i, s ->
        sb.append("    {\"id\": \"${s.id}\", \"name\": \"${s.fullName}\", \"phone\": \"${s.phone}\", \"grade\": \"${s.grade}\"}")
        if (i < students.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ],\n")
    sb.append("  \"groups\": [\n")
    groups.forEachIndexed { i, g ->
        sb.append("    {\"id\": \"${g.id}\", \"name\": \"${g.name}\", \"subject\": \"${g.subject}\", \"days\": \"${g.daysOfWeek}\"}")
        if (i < groups.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ]\n")
    sb.append("}\n")
    return sb.toString()
}
