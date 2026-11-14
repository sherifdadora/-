package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
fun ReportsScreen(
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    groups: List<GroupEntity>,
    students: List<StudentEntity>,
    settings: AppSettingsEntity?
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"

    var selectedGroupFilter by remember { mutableStateOf("الكل") }
    var selectedMethodFilter by remember { mutableStateOf("الكل") }

    val filteredSubscriptions = if (selectedGroupFilter == "الكل") subscriptions else subscriptions.filter { it.groupId == selectedGroupFilter }
    val filteredPayments = payments.filter { p ->
        val groupMatches = selectedGroupFilter == "الكل" || p.groupId == selectedGroupFilter
        val methodMatches = selectedMethodFilter == "الكل" || p.paymentMethod == selectedMethodFilter
        groupMatches && methodMatches
    }

    val totalExpected = filteredSubscriptions.sumOf { it.expectedAmount }
    val totalCollected = filteredPayments.sumOf { it.amountPaid }
    val totalRemaining = (totalExpected - totalCollected).coerceAtLeast(0.0)
    val collectionRate = if (totalExpected > 0) ((totalCollected * 100) / totalExpected).toInt() else 100

    val methods = listOf("الكل", "نقدًا", "فودافون كاش", "إنستاباي", "أخرى")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التقارير المالية وحصيلة الإيرادات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "تحليل التدفقات المالية والمبالغ المحصلة والمتبقية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = {
                    val sb = StringBuilder()
                    sb.append("💰 التقرير المالي الشامل\n")
                    sb.append("───────────────────────\n")
                    sb.append("إجمالي المبالغ المستحقة: ${totalExpected.toInt()} $currency\n")
                    sb.append("إجمالي المبالغ المحصلة: ${totalCollected.toInt()} $currency\n")
                    sb.append("إجمالي المتبقي: ${totalRemaining.toInt()} $currency\n")
                    sb.append("نسبة التحصيل: $collectionRate%\n")
                    sb.append("عدد العمليات المنفذة: ${filteredPayments.size}\n")
                    sb.append("───────────────────────\n")
                    ExportHelper.shareText(context, "التقرير المالي", sb.toString())
                }) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة التقرير", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Group Filter Chips
        item {
            Text(text = "تصفية حسب المجموعة:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedGroupFilter == "الكل",
                        onClick = { selectedGroupFilter = "الكل" },
                        label = { Text("جميع المجموعات", fontSize = 11.sp) }
                    )
                }
                items(groups) { g ->
                    FilterChip(
                        selected = selectedGroupFilter == g.id,
                        onClick = { selectedGroupFilter = g.id },
                        label = { Text(g.name, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Summary Card
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
                        Text(text = "معدل التحصيل العام", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$collectionRate%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EduSuccessContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "المحصل الفعلي", fontSize = 10.sp, color = EduSuccessGreen)
                                Text(text = "${totalCollected.toInt()} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "إجمالي المطلوب", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text(text = "${totalExpected.toInt()} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (totalRemaining > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "المتبقي", fontSize = 10.sp, color = if (totalRemaining > 0) EduErrorRed else MaterialTheme.colorScheme.outline)
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

        // Revenue Breakdown By Group
        item {
            Text(
                text = "توزيع الإيرادات حسب المجموعات الدراسية",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(groups) { g ->
            val gSubs = subscriptions.filter { it.groupId == g.id }
            val gPayments = payments.filter { it.groupId == g.id }
            val gExpected = gSubs.sumOf { it.expectedAmount }
            val gPaid = gPayments.sumOf { it.amountPaid }
            val gRemaining = (gExpected - gPaid).coerceAtLeast(0.0)
            val gRate = if (gExpected > 0) ((gPaid * 100) / gExpected).toInt() else 100

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
                        Text(text = g.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "تحصيل: $gRate%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EduTealPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المحصل: ${gPaid.toInt()} $currency", fontSize = 11.sp, color = EduSuccessGreen)
                        Text(text = "المستحق: ${gExpected.toInt()} $currency", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = "المتبقي: ${gRemaining.toInt()} $currency", fontSize = 11.sp, color = if (gRemaining > 0) EduErrorRed else EduSuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
