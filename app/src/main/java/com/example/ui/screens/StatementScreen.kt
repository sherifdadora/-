package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
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
import com.example.util.ExportHelper

@Composable
fun StatementScreen(
    initialStudentId: String?,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    settings: AppSettingsEntity?
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"

    var selectedStudentId by remember {
        mutableStateOf(initialStudentId ?: students.firstOrNull()?.id ?: "")
    }

    val selectedStudent = students.find { it.id == selectedStudentId }
    val groupMap = groups.associateBy { it.id }

    val studentSubs = subscriptions.filter { it.studentId == selectedStudentId }.sortedByDescending { it.monthSortKey }
    val studentPayments = payments.filter { it.studentId == selectedStudentId }.sortedByDescending { it.paymentTimestamp }
    val paymentsBySub = studentPayments.groupBy { it.subscriptionId }

    val totalExpected = studentSubs.sumOf { it.expectedAmount }
    val totalPaid = studentPayments.sumOf { it.amountPaid }
    val totalRemaining = (totalExpected - totalPaid).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Student Picker
        item {
            Text(
                text = "اختر الطالب لعرض كشف الحساب:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(students) { s ->
                    FilterChip(
                        selected = selectedStudentId == s.id,
                        onClick = { selectedStudentId = s.id },
                        label = { Text(s.fullName, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (selectedStudent == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("لا يوجد طلاب مسجلون", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            // Statement Summary Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    text = "كشف حساب: ${selectedStudent.fullName}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "الهاتف: ${selectedStudent.phone} • ولي الأمر: ${selectedStudent.parentPhone}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${selectedStudent.educationalStage} • ${selectedStudent.grade}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Share / Export Statement
                            Button(
                                onClick = {
                                    val records = studentSubs.map { sub ->
                                        val p = paymentsBySub[sub.id]?.sumOf { it.amountPaid } ?: 0.0
                                        val rem = (sub.expectedAmount - p).coerceAtLeast(0.0)
                                        "${sub.monthYear}: المطلوب ${sub.expectedAmount.toInt()} | مدفوع: ${p.toInt()} | متبقي: ${rem.toInt()} | الحالة: ${sub.status}"
                                    }
                                    val statementText = ExportHelper.generateStudentStatementText(
                                        studentName = selectedStudent.fullName,
                                        phone = selectedStudent.phone,
                                        groupName = selectedStudent.subject,
                                        academicYear = settings?.academicYear ?: "2024 - 2025",
                                        totalExpected = totalExpected,
                                        totalPaid = totalPaid,
                                        totalRemaining = totalRemaining,
                                        currency = currency,
                                        records = records
                                    )
                                    ExportHelper.shareText(context, "كشف حساب الطالب ${selectedStudent.fullName}", statementText)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة الكشف", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Totals Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "إجمالي الاشتراكات", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(text = "${totalExpected.toInt()} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EduSuccessContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "إجمالي المدفوع", fontSize = 10.sp, color = EduSuccessGreen)
                                    Text(text = "${totalPaid.toInt()} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (totalRemaining > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "المبلغ المتبقي", fontSize = 10.sp, color = if (totalRemaining > 0) EduErrorRed else MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${totalRemaining.toInt()} $currency",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalRemaining > 0) EduErrorRed else EduSuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Breakdown Table
            item {
                Text(
                    text = "تفصيل الشهور والاشتراكات المستحقة",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (studentSubs.isEmpty()) {
                item {
                    Text("لا توجد اشتراكات مسجلة لهذا الطالب", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                }
            } else {
                items(studentSubs) { sub ->
                    val group = groupMap[sub.groupId]
                    val subPayments = paymentsBySub[sub.id] ?: emptyList()
                    val paid = subPayments.sumOf { it.amountPaid }
                    val remaining = (sub.expectedAmount - paid).coerceAtLeast(0.0)

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
                                Text(
                                    text = "${sub.monthYear} (${group?.name ?: "مجموعة"})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                Text(text = "قيمة الاشتراك: ${sub.expectedAmount.toInt()} $currency", fontSize = 11.sp)
                                Text(text = "المدفوع: ${paid.toInt()} $currency", fontSize = 11.sp, color = EduSuccessGreen)
                                Text(text = "المتبقي: ${remaining.toInt()} $currency", fontSize = 11.sp, color = if (remaining > 0) EduErrorRed else EduSuccessGreen, fontWeight = FontWeight.Bold)
                            }

                            // If there are payments made for this month, list them
                            if (subPayments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "الدفعات المستلمة:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                subPayments.forEach { p ->
                                    Text(
                                        text = "  • ${p.paymentDateFormatted}: تم دفع ${p.amountPaid.toInt()} $currency (${p.paymentMethod}) ${if (p.notes.isNotBlank()) "- ${p.notes}" else ""}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
}
