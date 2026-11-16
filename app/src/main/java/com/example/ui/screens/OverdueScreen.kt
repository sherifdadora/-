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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import com.example.data.model.MonthlySubscriptionEntity
import com.example.ui.OverdueItem
import com.example.ui.components.PaymentDialog
import com.example.ui.theme.EduErrorContainer
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduWarningAmber
import com.example.util.ExportHelper
import com.example.util.WhatsAppHelper

@Composable
fun OverdueScreen(
    overdueItems: List<OverdueItem>,
    settings: AppSettingsEntity?,
    onOpenStatement: (String) -> Unit,
    onRecordPayment: (subscriptionId: String, studentId: String, groupId: String, amount: Double, method: String, notes: String) -> Unit,
    onRecordExemption: (subscriptionId: String, reason: String) -> Unit
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"

    val totalOverdueAmount = overdueItems.sumOf { it.remaining }
    var activeSubForPayment by remember { mutableStateOf<MonthlySubscriptionEntity?>(null) }
    var activeItemForPayment by remember { mutableStateOf<OverdueItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Summary Card
        ElevatedCard(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = EduErrorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = EduErrorRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إجمالي المبالغ المتأخرة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EduErrorRed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "عدد الاشتراكات المتأخرة: ${overdueItems.size} اشتراك",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${totalOverdueAmount.toInt()} $currency",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduErrorRed
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قائمة الطلاب المتأخرين في السداد",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = {
                val sb = StringBuilder()
                sb.append("📋 كشف الطلاب المتأخرين في سداد الاشتراكات\n")
                sb.append("───────────────────────\n")
                sb.append("إجمالي المتأخرات: ${totalOverdueAmount.toInt()} $currency\n\n")
                overdueItems.forEachIndexed { i, item ->
                    sb.append("${i + 1}. ${item.student.fullName}\n")
                    sb.append("   المجموعة: ${item.group?.name ?: ""} | الشهر: ${item.subscription.monthYear}\n")
                    sb.append("   المتبقي: ${item.remaining.toInt()} $currency | هاتف ولي الأمر: ${item.student.parentPhone}\n\n")
                }
                ExportHelper.shareText(context, "كشف الطلاب المتأخرين", sb.toString())
            }) {
                Icon(Icons.Default.Share, contentDescription = "تصدير الكشف", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (overdueItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    color = EduSuccessContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ممتاز! لا يوجد أي مبالغ متأخرة حالياً.",
                        color = EduSuccessGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(overdueItems) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.student.fullName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.group?.name ?: "مجموعة"} • مستحق: ${item.subscription.monthYear}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "هاتف الطالب: ${item.student.phone} | ولي الأمر: ${item.student.parentPhone}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "المتبقي: ${item.remaining.toInt()} $currency",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EduErrorRed
                                    )
                                    Text(
                                        text = "قيمة الاشتراك: ${item.subscription.expectedAmount.toInt()} $currency",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pay button
                                Button(
                                    onClick = {
                                        activeSubForPayment = item.subscription
                                        activeItemForPayment = item
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EduSuccessGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل دفع", fontSize = 11.sp)
                                }

                                // WhatsApp reminder to parent
                                Button(
                                    onClick = {
                                        val msg = WhatsAppHelper.buildFeeReminderMessage(
                                            studentName = item.student.fullName,
                                            groupName = item.group?.name ?: "",
                                            monthYear = item.subscription.monthYear,
                                            remainingAmount = item.remaining,
                                            currency = currency,
                                            centerName = settings?.centerName ?: "",
                                            baseTemplate = settings?.feeReminderTemplate ?: ""
                                        )
                                        val phone = if (item.student.parentPhone.isNotBlank()) item.student.parentPhone else item.student.phone
                                        WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.4f)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تذكير واتساب", fontSize = 11.sp, color = Color.White)
                                }

                                // Statement
                                OutlinedButton(
                                    onClick = { onOpenStatement(item.student.id) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("كشف الحساب", fontSize = 11.sp)
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
    }

    activeItemForPayment?.let { item ->
        PaymentDialog(
            studentName = item.student.fullName,
            groupName = item.group?.name ?: "",
            monthYear = item.subscription.monthYear,
            expectedAmount = item.subscription.expectedAmount,
            alreadyPaid = item.totalPaid,
            currency = currency,
            onDismiss = {
                activeSubForPayment = null
                activeItemForPayment = null
            },
            onConfirmPayment = { amount, method, notes ->
                onRecordPayment(item.subscription.id, item.student.id, item.subscription.groupId, amount, method, notes)
                activeSubForPayment = null
                activeItemForPayment = null
            },
            onExempt = { reason ->
                onRecordExemption(item.subscription.id, reason)
                activeSubForPayment = null
                activeItemForPayment = null
            }
        )
    }
}
