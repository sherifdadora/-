package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.eduWhatsAppTextColor
import com.example.util.WhatsAppHelper
import com.example.util.WhatsAppMessageTemplateType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WhatsAppRecipientMode(val label: String) {
    STUDENT_PHONE("رقم الطالب"),
    PARENT_PHONE("رقم ولي الأمر"),
    GROUP_LINK("رابط المجموعة")
}

@Composable
fun WhatsAppMessageComposerDialog(
    student: StudentEntity? = null,
    group: GroupEntity? = null,
    settings: AppSettingsEntity? = null,
    initialTemplate: WhatsAppMessageTemplateType = if (student != null) WhatsAppMessageTemplateType.WELCOME else WhatsAppMessageTemplateType.CLASS_REMINDER,
    initialRecipientMode: WhatsAppRecipientMode = if (student != null) {
        if (student.parentPhone.isNotBlank()) WhatsAppRecipientMode.PARENT_PHONE else WhatsAppRecipientMode.STUDENT_PHONE
    } else {
        WhatsAppRecipientMode.GROUP_LINK
    },
    monthYear: String = "الشهر الحالي",
    remainingAmount: Double? = null,
    attendancePercent: Int = 100,
    presentCount: Int = 0,
    absentCount: Int = 0,
    totalPaid: Double = 0.0,
    totalRemaining: Double = 0.0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val centerName = settings?.centerName ?: "السنتر التعليمي"
    val currency = settings?.currency ?: "جنيه"
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val availableRecipients = remember(student, group) {
        buildList {
            if (student != null) {
                if (student.parentPhone.isNotBlank()) add(WhatsAppRecipientMode.PARENT_PHONE)
                if (student.phone.isNotBlank()) add(WhatsAppRecipientMode.STUDENT_PHONE)
            }
            if (group != null) {
                add(WhatsAppRecipientMode.GROUP_LINK)
            }
        }
    }

    var recipientMode by remember {
        mutableStateOf(
            if (availableRecipients.contains(initialRecipientMode)) initialRecipientMode
            else availableRecipients.firstOrNull() ?: WhatsAppRecipientMode.STUDENT_PHONE
        )
    }

    val availableTemplates = remember(student, group) {
        if (student != null) {
            listOf(
                WhatsAppMessageTemplateType.WELCOME,
                WhatsAppMessageTemplateType.UPCOMING_DUE,
                WhatsAppMessageTemplateType.FEE_REMINDER,
                WhatsAppMessageTemplateType.PAYMENT_CONFIRMATION,
                WhatsAppMessageTemplateType.ABSENCE_NOTICE,
                WhatsAppMessageTemplateType.STUDENT_REPORT,
                WhatsAppMessageTemplateType.CLASS_REMINDER,
                WhatsAppMessageTemplateType.CUSTOM
            )
        } else {
            listOf(
                WhatsAppMessageTemplateType.CLASS_REMINDER,
                WhatsAppMessageTemplateType.GROUP_ANNOUNCEMENT,
                WhatsAppMessageTemplateType.CUSTOM
            )
        }
    }

    var selectedTemplate by remember {
        mutableStateOf(
            if (availableTemplates.contains(initialTemplate)) initialTemplate
            else availableTemplates.first()
        )
    }

    fun generateFormattedMessage(type: WhatsAppMessageTemplateType): String {
        val stName = student?.fullName ?: "الطالب"
        val grpName = group?.name ?: ""
        val dueAmount = remainingAmount ?: student?.monthlyFee ?: group?.defaultMonthlyFee ?: 250.0
        val nextDue = student?.nextPaymentDueDate?.ifBlank { "أول الشهر القادم" } ?: "أول الشهر القادم"

        return when (type) {
            WhatsAppMessageTemplateType.WELCOME -> WhatsAppHelper.buildWelcomeMessage(
                studentName = stName,
                groupName = grpName,
                grade = student?.grade ?: group?.grade ?: "",
                centerName = centerName,
                baseTemplate = settings?.welcomeMessageTemplate ?: ""
            )
            WhatsAppMessageTemplateType.CLASS_REMINDER -> WhatsAppHelper.buildClassReminderMessage(
                groupName = grpName.ifBlank { "المجموعة الدراسية" },
                daysOfWeek = group?.daysOfWeek ?: "",
                sessionTime = group?.sessionTime ?: "",
                location = group?.location ?: "",
                centerName = centerName
            )
            WhatsAppMessageTemplateType.FEE_REMINDER -> WhatsAppHelper.buildFeeReminderMessage(
                studentName = stName,
                groupName = grpName,
                monthYear = monthYear,
                remainingAmount = dueAmount,
                currency = currency,
                centerName = centerName,
                baseTemplate = settings?.feeReminderTemplate ?: ""
            )
            WhatsAppMessageTemplateType.UPCOMING_DUE -> WhatsAppHelper.buildUpcomingPaymentReminderMessage(
                studentName = stName,
                groupName = grpName,
                dueDate = nextDue,
                daysRemaining = 7,
                expectedAmount = student?.monthlyFee ?: dueAmount,
                currency = currency,
                centerName = centerName
            )
            WhatsAppMessageTemplateType.PAYMENT_CONFIRMATION -> WhatsAppHelper.buildPaymentConfirmationMessage(
                studentName = stName,
                groupName = grpName,
                monthYear = monthYear,
                amountPaid = student?.monthlyFee ?: dueAmount,
                paymentDate = todayStr,
                nextDueDate = student?.nextPaymentDueDate ?: "",
                currency = currency,
                centerName = centerName,
                baseTemplate = settings?.paymentConfirmationTemplate ?: ""
            )
            WhatsAppMessageTemplateType.ABSENCE_NOTICE -> WhatsAppHelper.buildAbsenceMessage(
                studentName = stName,
                groupName = grpName,
                sessionDate = todayStr,
                centerName = centerName,
                baseTemplate = settings?.absenceNoticeTemplate ?: ""
            )
            WhatsAppMessageTemplateType.STUDENT_REPORT -> WhatsAppHelper.buildStudentReportSummaryMessage(
                studentName = stName,
                groupName = grpName,
                attendancePercent = attendancePercent,
                presentCount = presentCount,
                absentCount = absentCount,
                totalPaid = totalPaid,
                totalRemaining = totalRemaining,
                currency = currency,
                centerName = centerName
            )
            WhatsAppMessageTemplateType.GROUP_ANNOUNCEMENT -> WhatsAppHelper.buildGroupAnnouncementMessage(
                groupName = grpName.ifBlank { "المجموعة" },
                daysOfWeek = group?.daysOfWeek ?: "",
                sessionTime = group?.sessionTime ?: "",
                location = group?.location ?: "",
                centerName = centerName
            )
            WhatsAppMessageTemplateType.CUSTOM -> buildString {
                append("السلام عليكم ورحمة الله وبركاته،\n")
                if (centerName.isNotBlank()) append("من إدارة $centerName:\n\n")
                if (student != null) append("بخصوص الطالب: ${student.fullName}\n")
                if (group != null) append("المجموعة: ${group.name}\n")
                append("\n")
            }
        }
    }

    var messageText by remember { mutableStateOf(generateFormattedMessage(selectedTemplate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنسيق رسالة واتساب جاهزة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Target summary card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (student != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = EduTealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = student.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (group != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp), tint = EduTealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "المجموعة: ${group.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Recipient Selection
                if (availableRecipients.isNotEmpty()) {
                    Text(
                        text = "فتح المحادثة عبر:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableRecipients) { mode ->
                            val detail = when (mode) {
                                WhatsAppRecipientMode.STUDENT_PHONE -> student?.phone ?: ""
                                WhatsAppRecipientMode.PARENT_PHONE -> student?.parentPhone ?: ""
                                WhatsAppRecipientMode.GROUP_LINK -> if (group?.whatsappLink?.isNotBlank() == true) "رابط الجروب" else "مشاركة لواتساب"
                            }
                            FilterChip(
                                selected = recipientMode == mode,
                                onClick = { recipientMode = mode },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            WhatsAppRecipientMode.STUDENT_PHONE -> Icons.Default.Phone
                                            WhatsAppRecipientMode.PARENT_PHONE -> Icons.Default.Person
                                            WhatsAppRecipientMode.GROUP_LINK -> Icons.Default.Groups
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("${mode.label} ($detail)", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Template Picker
                Text(
                    text = "اختر نوع الرسالة الجاهزة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableTemplates) { type ->
                        FilterChip(
                            selected = selectedTemplate == type,
                            onClick = {
                                selectedTemplate = type
                                messageText = generateFormattedMessage(type)
                            },
                            label = { Text(type.title, fontSize = 11.sp) }
                        )
                    }
                }

                // Editable formatted preview
                Text(
                    text = "معاينة وتعديل نص الرسالة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 230.dp)
                        .testTag("whatsapp_message_preview_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF25D366).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (recipientMode == WhatsAppRecipientMode.GROUP_LINK) {
                            if (group?.whatsappLink?.isNotBlank() == true)
                                "ملاحظة: سيتم نسخ النص تلقائياً وفتح رابط مجموعة الواتساب مباشرة للصقه."
                            else
                                "ملاحظة: لم يتم تسجيل رابط جروب، سيتم فتح مشاركة واتساب لاختيار المجموعة."
                        } else {
                            "سيتم فتح محادثة واتساب مباشرة مع تعبئة الرسالة الجاهزة تلقائياً."
                        },
                        fontSize = 11.sp,
                        color = eduWhatsAppTextColor(),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (recipientMode) {
                        WhatsAppRecipientMode.STUDENT_PHONE -> {
                            WhatsAppHelper.openWhatsAppChat(context, student?.phone ?: "", messageText)
                        }
                        WhatsAppRecipientMode.PARENT_PHONE -> {
                            val phone = student?.parentPhone?.ifBlank { student.phone } ?: ""
                            WhatsAppHelper.openWhatsAppChat(context, phone, messageText)
                        }
                        WhatsAppRecipientMode.GROUP_LINK -> {
                            WhatsAppHelper.openWhatsAppGroup(context, group?.whatsappLink ?: "", messageText)
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("whatsapp_send_direct_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("فتح واتساب وإرسال", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { WhatsAppHelper.copyToClipboard(context, "رسالة واتساب", messageText) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إغلاق")
                }
            }
        }
    )
}
