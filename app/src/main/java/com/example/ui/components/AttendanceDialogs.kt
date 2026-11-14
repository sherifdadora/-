package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.EduTealPrimary
import com.example.util.AttendanceCloudSyncService
import com.example.util.WhatsAppHelper

@Composable
fun CloudSyncConfigDialog(
    currentUrl: String,
    pendingSyncCount: Int,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onExportPendingJson: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = EduTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إعداد المزامنة السحابية للحضور",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تُحفظ كشوف الحضور محليًا في الجهاز فور تسجيلها، وعند توفر الإنترنت يتم رفع الكشوف المعلقة ($pendingSyncCount كشف) تلقائيًا عبر طلب POST بصيغة JSON إلى الرابط التالي (مثل API خاص أو Google Apps Script).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        validationError = null
                    },
                    label = { Text("رابط خادم السحابة (HTTPS URL)") },
                    placeholder = { Text("https://example.com/api/attendance") },
                    isError = validationError != null,
                    supportingText = validationError?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_sync_url_input"),
                    singleLine = true
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تنسيق البيانات المرسلة: sessionId, groupName, sessionDate, sessionTime, lessonTopic, presentCount, absentCount, records[]",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedButton(
                    onClick = onExportPendingJson,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة / تصدير الكشوف المعلقة كملف JSON", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = urlInput.trim()
                    if (trimmed.isNotEmpty() && !AttendanceCloudSyncService.isValidCloudEndpoint(trimmed)) {
                        validationError = "يجب أن يبدأ الرابط بـ https:// أو http:// دون مسافات"
                        return@Button
                    }
                    onSaveUrl(trimmed)
                    Toast.makeText(
                        context,
                        if (trimmed.isEmpty()) "تم مسح رابط السحابة والاعتماد على الحفظ المحلي" else "تم حفظ رابط السحابة بنجاح",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                modifier = Modifier.testTag("save_cloud_sync_url_button")
            ) {
                Text("حفظ الإعداد")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AbsenceNoticeBatchDialog(
    absentStudents: List<StudentEntity>,
    groupName: String,
    sessionDate: String,
    lessonTopic: String,
    settings: AppSettingsEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val centerName = settings?.centerName ?: "سنتر التفوق والريادة"
    val sentMap = remember { mutableStateMapOf<String, Boolean>() }
    var targetRecipient by remember { mutableStateOf("parent") } // "parent" or "student"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال إشعارات الغياب عبر واتساب",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "سيتم إرسال إشعار الغياب التلقائي مع تفاصيل الحصة ($groupName - $sessionDate):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "إرسال إلى:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = targetRecipient == "parent",
                            onClick = { targetRecipient = "parent" },
                            label = { Text("ولي الأمر", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = targetRecipient == "student",
                            onClick = { targetRecipient = "student" },
                            label = { Text("الطالب", fontSize = 11.sp) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (absentStudents.isEmpty()) {
                    Text("لا يوجد طلاب مسجلين كغائبين في هذه الحصة.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(absentStudents) { student ->
                            val isSent = sentMap[student.id] == true
                            val phone = if (targetRecipient == "parent") {
                                if (student.parentPhone.isNotBlank()) student.parentPhone else student.phone
                            } else {
                                student.phone
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = student.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (targetRecipient == "parent") "ولي الأمر: ${student.parentPhone}" else "الطالب: ${student.phone}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val msg = WhatsAppHelper.buildAbsenceMessage(
                                                studentName = student.fullName,
                                                groupName = groupName,
                                                sessionDate = sessionDate,
                                                lessonTopic = lessonTopic,
                                                centerName = centerName,
                                                baseTemplate = settings?.absenceNoticeTemplate ?: ""
                                            )
                                            WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                                            sentMap[student.id] = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSent) EduTealPrimary else Color(0xFF25D366)
                                        )
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSent) "تم الإرسال ✓" else "إرسال إشعار",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("تم الانتهاء وإغلاق")
            }
        }
    )
}
