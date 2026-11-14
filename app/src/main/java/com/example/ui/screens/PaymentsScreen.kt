package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.UpcomingPaymentItem
import com.example.ui.components.PaymentDialog
import com.example.ui.components.StudentPaymentSubscriptionDialog
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PaymentsScreen(
    subscriptions: List<MonthlySubscriptionEntity>,
    payments: List<PaymentEntity>,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity> = emptyList(),
    upcomingPayments: List<UpcomingPaymentItem> = emptyList(),
    settings: AppSettingsEntity?,
    onOpenStatement: (String) -> Unit,
    onRecordPayment: (subscriptionId: String, studentId: String, groupId: String, amount: Double, method: String, notes: String) -> Unit,
    onRecordDirectPayment: (studentId: String, amount: Double, paymentDate: String, method: String, notes: String, monthName: String) -> Unit,
    onRecordStudentSubscriptionAndPayment: (
        studentId: String,
        groupId: String,
        monthName: String,
        academicYear: String,
        expectedFee: Double,
        amountPaid: Double,
        paymentDate: String,
        nextDueDate: String,
        paymentMethod: String,
        notes: String,
        monthStatus: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onRecordExemption: (subscriptionId: String, reason: String) -> Unit,
    onCreateMonthlySubscriptions: (monthYear: String, monthSortKey: String, academicYear: String) -> Unit
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"
    val centerName = settings?.centerName ?: ""

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("الكل") }
    var selectedMonthFilter by remember { mutableStateOf("الكل") }
    var selectedGroupFilter by remember { mutableStateOf<String?> (null) }
    var showCreateMonthDialog by remember { mutableStateOf(false) }
    var showSimplePaymentForm by remember { mutableStateOf(false) }
    var showUpcomingSection by remember { mutableStateOf(false) }

    // Active subscription selected for payment dialog
    var activeSubForPayment by remember { mutableStateOf<MonthlySubscriptionEntity?>(null) }

    val studentMap = remember(students) { students.associateBy { it.id } }
    val groupMap = remember(groups) { groups.associateBy { it.id } }
    val paymentsBySub = remember(payments) { payments.groupBy { it.subscriptionId } }

    // Distinct months
    val allMonths = remember(subscriptions) {
        listOf("الكل") + subscriptions.map { it.monthYear }.distinct()
    }

    // Filtered subscriptions
    val filteredSubscriptions = remember(
        subscriptions,
        searchQuery,
        selectedStatusFilter,
        selectedMonthFilter,
        selectedGroupFilter,
        studentMap
    ) {
        subscriptions.filter { sub ->
            val student = studentMap[sub.studentId]
            val matchesSearch = student?.fullName?.contains(searchQuery, ignoreCase = true) == true ||
                    student?.phone?.contains(searchQuery) == true
            val matchesStatus = when (selectedStatusFilter) {
                "الكل" -> true
                "مدفوع بالكامل", "مدفوع" -> sub.status == "مدفوع بالكامل" || sub.status == "مدفوع"
                "غير مدفوع", "لم يدفع" -> sub.status == "غير مدفوع" || sub.status == "لم يدفع"
                else -> sub.status == selectedStatusFilter
            }
            val matchesMonth = selectedMonthFilter == "الكل" || sub.monthYear == selectedMonthFilter
            val matchesGroup = selectedGroupFilter == null || sub.groupId == selectedGroupFilter
            matchesSearch && matchesStatus && matchesMonth && matchesGroup
        }.sortedByDescending { it.monthSortKey }
    }

    // Calculate group collections
    val totalAllCollected = remember(payments) { payments.sumOf { it.amountPaid } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Group Collection Section (إجمالي المبالغ المحصلة لكل مجموعة)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_collections_section")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = EduTealPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إجمالي المبالغ المحصلة لكل مجموعة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EduSuccessContainer
                        ) {
                            Text(
                                text = "الكل: ${totalAllCollected.toInt()} $currency",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduSuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (groups.isEmpty()) {
                        Text(
                            text = "لا توجد مجموعات دراسية مسجلة حالياً",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // "All Groups" Chip/Card
                            item {
                                val isSelected = selectedGroupFilter == null
                                OutlinedCard(
                                    onClick = { selectedGroupFilter = null },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) EduTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) EduTealPrimary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(96.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "جميع المجموعات",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) EduTealPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${totalAllCollected.toInt()} $currency",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EduSuccessGreen
                                        )
                                        Text(
                                            text = "${groups.size} مجموعة نشطة",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            items(groups) { group ->
                                val groupPayments = payments.filter { it.groupId == group.id }
                                val groupSubs = subscriptions.filter { it.groupId == group.id }
                                val groupCollected = groupPayments.sumOf { it.amountPaid }
                                val groupExpected = groupSubs.sumOf { it.expectedAmount }
                                val groupStudentsCount = enrollments.count { it.groupId == group.id && it.isActive }
                                val progress = if (groupExpected > 0) (groupCollected / groupExpected).toFloat().coerceIn(0f, 1f) else 0f
                                val isSelected = selectedGroupFilter == group.id

                                OutlinedCard(
                                    onClick = {
                                        selectedGroupFilter = if (selectedGroupFilter == group.id) null else group.id
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) EduTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) EduTealPrimary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(96.dp)
                                        .testTag("group_collection_card_${group.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = group.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isSelected) EduTealPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = EduTealPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Text(
                                                text = "${groupCollected.toInt()} $currency",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EduSuccessGreen
                                            )
                                            Text(
                                                text = "$groupStudentsCount طالب",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        Column {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                color = EduTealPrimary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "من ${groupExpected.toInt()}",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                Text(
                                                    text = "${(progress * 100).toInt()}%",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EduTealPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Subscription & Payment Form + Upcoming Due Dates + Generate Month
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSimplePaymentForm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("open_simple_payment_form_button")
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل اشتراك / دفعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showCreateMonthDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إنشاء شهر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (upcomingPayments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val urgentCount = upcomingPayments.count { it.daysRemaining <= 7 }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (urgentCount > 0) EduWarningAmber.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (urgentCount > 0) EduWarningAmber.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUpcomingSection = !showUpcomingSection }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (urgentCount > 0) EduWarningAmber else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "المواعيد القادمة للدفع والتذكيرات (${upcomingPayments.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (urgentCount > 0) "$urgentCount مواعيد مستحقة خلال 7 أيام" else "اضغط لعرض المواعيد وإرسال تذكير واتساب",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showUpcomingSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showUpcomingSection) "طي" else "عرض",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showUpcomingSection) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                upcomingPayments.take(5).forEach { upItem ->
                                    val badgeColor = when {
                                        upItem.daysRemaining < 0 -> EduErrorRed
                                        upItem.daysRemaining <= 5 -> EduWarningAmber
                                        else -> EduSuccessGreen
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
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
                                                Text(
                                                    text = upItem.student.fullName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${upItem.group?.name ?: "عام"} • الاستحقاق: ${upItem.dueDate}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${upItem.statusText} • ${upItem.expectedAmount.toInt()} $currency",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = badgeColor
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    val phone = upItem.student.parentPhone.ifBlank { upItem.student.phone }
                                                    val msg = WhatsAppHelper.buildUpcomingPaymentReminderMessage(
                                                        studentName = upItem.student.fullName,
                                                        groupName = upItem.group?.name ?: "",
                                                        dueDate = upItem.dueDate,
                                                        daysRemaining = upItem.daysRemaining,
                                                        expectedAmount = upItem.expectedAmount,
                                                        currency = currency,
                                                        centerName = centerName
                                                    )
                                                    WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تذكير", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم الطالب أو رقم الهاتف...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Month Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allMonths) { m ->
                    FilterChip(
                        selected = selectedMonthFilter == m,
                        onClick = { selectedMonthFilter = m },
                        label = { Text(m, fontSize = 11.sp) }
                    )
                }
            }

            // Status Filter Chips
            val statusList = listOf("الكل", "غير مدفوع", "دفع جزئي", "مدفوع", "معفى")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(statusList) { st ->
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = { Text(st, fontSize = 11.sp) }
                    )
                }
            }

            // Active Group Filter Indicator
            if (selectedGroupFilter != null) {
                val grp = groupMap[selectedGroupFilter]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تصفية حسب: ${grp?.name ?: ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EduTealPrimary
                    )
                    Text(
                        text = "إلغاء التصفية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { selectedGroupFilter = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subscriptions List
            if (filteredSubscriptions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد اشتراكات أو دفعات مطابقة", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredSubscriptions, key = { it.id }) { sub ->
                        val student = studentMap[sub.studentId]
                        val group = groupMap[sub.groupId]
                        val subPayments = paymentsBySub[sub.id] ?: emptyList()
                        val totalPaid = subPayments.sumOf { it.amountPaid }
                        val remaining = (sub.expectedAmount - totalPaid).coerceAtLeast(0.0)

                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                            text = student?.fullName ?: "طالب غير معروف",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${group?.name ?: "مجموعة"} • شهر: ${sub.monthYear}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

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

                                Spacer(modifier = Modifier.height(8.dp))

                                // Financial Breakdown Row
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = "المطلوب", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(text = "${sub.expectedAmount.toInt()} $currency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text(text = "المدفوع", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(text = "${totalPaid.toInt()} $currency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EduSuccessGreen)
                                        }
                                        Column {
                                            Text(text = "المتبقي", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(
                                                text = "${remaining.toInt()} $currency",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (remaining > 0) EduErrorRed else EduSuccessGreen
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (remaining > 0 && sub.status != "معفى" && sub.status != "مدفوع" && sub.status != "مدفوع بالكامل") {
                                        Button(
                                            onClick = { activeSubForPayment = sub },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EduSuccessGreen),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("دفع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (student != null) {
                                            Button(
                                                onClick = {
                                                    val phone = student.parentPhone.ifBlank { student.phone }
                                                    val msg = WhatsAppHelper.buildFeeReminderMessage(
                                                        studentName = student.fullName,
                                                        groupName = group?.name ?: "",
                                                        monthYear = sub.monthYear,
                                                        remainingAmount = remaining,
                                                        currency = currency,
                                                        centerName = centerName,
                                                        baseTemplate = settings?.feeReminderTemplate ?: ""
                                                    )
                                                    WhatsAppHelper.openWhatsAppChat(context, phone, msg)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تذكير", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { student?.let { onOpenStatement(it.id) } },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كشف حساب", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }

        // Floating Action Button for recording student payments
        ExtendedFloatingActionButton(
            onClick = { showSimplePaymentForm = true },
            containerColor = EduTealPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_record_payment"),
            icon = {
                Icon(Icons.Default.AddCard, contentDescription = "تسجيل دفعة طالب", modifier = Modifier.size(24.dp))
            },
            text = {
                Text("تسجيل دفعة طالب", fontWeight = FontWeight.Bold)
            }
        )
    }

    // Student Payment & Subscription Form Dialog with Next Due Date Reminder
    if (showSimplePaymentForm) {
        StudentPaymentSubscriptionDialog(
            students = students.filter { it.status == "نشط" },
            groups = groups,
            enrollments = enrollments,
            subscriptions = subscriptions,
            settings = settings,
            onDismiss = { showSimplePaymentForm = false },
            onConfirm = { sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, monthStatus ->
                onRecordStudentSubscriptionAndPayment(
                    sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, monthStatus
                )
                showSimplePaymentForm = false
            }
        )
    }

    // Specific Subscription Payment Dialog
    if (activeSubForPayment != null) {
        val sub = activeSubForPayment!!
        val student = studentMap[sub.studentId]
        val group = groupMap[sub.groupId]
        val subPayments = paymentsBySub[sub.id] ?: emptyList()
        val alreadyPaid = subPayments.sumOf { it.amountPaid }

        PaymentDialog(
            studentName = student?.fullName ?: "",
            groupName = group?.name ?: "",
            monthYear = sub.monthYear,
            expectedAmount = sub.expectedAmount,
            alreadyPaid = alreadyPaid,
            currency = currency,
            onDismiss = { activeSubForPayment = null },
            onConfirmPayment = { amount, method, notes ->
                onRecordPayment(sub.id, sub.studentId, sub.groupId, amount, method, notes)
                activeSubForPayment = null
                Toast.makeText(context, "تم تسجيل الدفعة بنجاح", Toast.LENGTH_SHORT).show()
            },
            onExempt = { reason ->
                onRecordExemption(sub.id, reason)
                activeSubForPayment = null
                Toast.makeText(context, "تم تسجيل إعفاء الطالب من الاشتراك", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Generate Month Dialog
    if (showCreateMonthDialog) {
        var newMonthName by remember { mutableStateOf("مارس 2025") }
        var newMonthKey by remember { mutableStateOf("2025-03") }
        var academicYear by remember { mutableStateOf(settings?.academicYear ?: "2024 - 2025") }

        AlertDialog(
            onDismissRequest = { showCreateMonthDialog = false },
            title = { Text("إنشاء اشتراكات شهر جديد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "سيتم إنشاء مستحقات اشتراك الشهر لجميع الطلاب النشطين الملتحقين بالمجموعات تلقائياً بحسب سعر كل مجموعة:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newMonthName,
                        onValueChange = { newMonthName = it },
                        label = { Text("اسم الشهر (مثال: مارس 2025)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newMonthKey,
                        onValueChange = { newMonthKey = it },
                        label = { Text("رمز الترتيب (مثال: 2025-03)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("العام الدراسي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMonthName.isNotBlank()) {
                            onCreateMonthlySubscriptions(newMonthName, newMonthKey, academicYear)
                            showCreateMonthDialog = false
                            Toast.makeText(context, "تم إنشاء اشتراكات $newMonthName لجميع الطلاب بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary)
                ) {
                    Text("إنشاء الاشتراكات")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateMonthDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * نموذج (Form) بسيط لتسجيل دفعات الطلاب
 * يتضمن:
 * 1. اختيار الطالب
 * 2. المبلغ المدفوع
 * 3. تاريخ الدفعة
 * بالإضافة لطريقة الدفع والملاحظات واسم الشهر
 */
@Composable
fun SimplePaymentFormDialog(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, amount: Double, paymentDate: String, method: String, notes: String, monthName: String) -> Unit
) {
    val groupMap = remember(groups) { groups.associateBy { it.id } }
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var studentSearchText by remember { mutableStateOf("") }
    var isSelectingStudent by remember { mutableStateOf(false) }

    val selectedStudent = students.find { it.id == selectedStudentId }

    var amountText by remember {
        mutableStateOf(selectedStudent?.monthlyFee?.toInt()?.toString() ?: "150")
    }
    var paymentDate by remember { mutableStateOf(todayDateStr) }
    var selectedMethod by remember { mutableStateOf("نقدًا") }
    var selectedMonth by remember { mutableStateOf("فبراير") }
    var notesText by remember { mutableStateOf("") }

    val methods = listOf("نقدًا", "فودافون كاش", "إنستاباي", "تحويل بنكي")
    val months = listOf("فبراير", "مارس", "يناير")

    val filteredStudents = remember(students, studentSearchText) {
        if (studentSearchText.isBlank()) students
        else students.filter {
            it.fullName.contains(studentSearchText, ignoreCase = true) || it.phone.contains(studentSearchText)
        }
    }

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
                        Icons.Default.AddCard,
                        contentDescription = null,
                        tint = EduTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل دفعة طالب",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 17.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. اختيار الطالب
                Text(
                    text = "1. اختيار الطالب *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedCard(
                    onClick = { isSelectingStudent = !isSelectingStudent },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedStudent != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_student_trigger")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = EduTealPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = EduTealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedStudent?.fullName ?: "اضغط لاختيار الطالب",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedStudent != null)
                                        "${selectedStudent.grade} • الرسوم: ${selectedStudent.monthlyFee.toInt()} $currency"
                                    else "اختر من قائمة الطلاب النشطين",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = EduTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Student Selection Expanded Dropdown/Search
                if (isSelectingStudent) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            OutlinedTextField(
                                value = studentSearchText,
                                onValueChange = { studentSearchText = it },
                                placeholder = { Text("بحث عن طالب بالاسم أو الرقم...", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (filteredStudents.isEmpty()) {
                                    Text(
                                        text = "لا يوجد طلاب مطابقين للبحث",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    filteredStudents.forEach { st ->
                                        val isStSelected = st.id == selectedStudentId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedStudentId = st.id
                                                    amountText = st.monthlyFee.toInt().toString()
                                                    isSelectingStudent = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = st.fullName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isStSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isStSelected) EduTealPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${st.grade} • ${st.monthlyFee.toInt()} $currency",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            if (isStSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = EduTealPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. المبلغ المدفوع
                Text(
                    text = "2. المبلغ المدفوع ($currency) *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("المبلغ المدفوع") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EduSuccessGreen, modifier = Modifier.size(20.dp))
                    }
                )

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickAmounts = listOfNotNull(
                        selectedStudent?.monthlyFee?.toInt()?.toString(),
                        "100",
                        "150",
                        "200",
                        "300"
                    ).distinct()

                    quickAmounts.forEach { amt ->
                        FilterChip(
                            selected = amountText == amt,
                            onClick = { amountText = amt },
                            label = { Text("$amt $currency", fontSize = 10.sp) }
                        )
                    }
                }

                // 3. تاريخ الدفعة
                Text(
                    text = "3. تاريخ الدفعة (سنة-شهر-يوم) *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("تاريخ السداد (yyyy-MM-dd)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_date_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EduTealPrimary, modifier = Modifier.size(20.dp))
                    }
                )

                // Quick Date Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentDate == todayDateStr,
                        onClick = { paymentDate = todayDateStr },
                        label = { Text("اليوم", fontSize = 11.sp) }
                    )

                    val yesterdayDateStr = remember {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                    }
                    FilterChip(
                        selected = paymentDate == yesterdayDateStr,
                        onClick = { paymentDate = yesterdayDateStr },
                        label = { Text("أمس", fontSize = 11.sp) }
                    )
                }

                // 4. الشهر المستحق
                Text(
                    text = "عن شهر:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    months.forEach { m ->
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }

                // 5. طريقة الدفع
                Text(
                    text = "طريقة الدفع:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m, fontSize = 10.sp) }
                        )
                    }
                }

                // 6. ملاحظات إضافية
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            val isFormValid = selectedStudent != null && parsedAmount > 0.0 && paymentDate.isNotBlank()

            Button(
                onClick = {
                    if (isFormValid && selectedStudent != null) {
                        onConfirm(
                            selectedStudent.id,
                            parsedAmount,
                            paymentDate.trim(),
                            selectedMethod,
                            notesText.trim(),
                            selectedMonth
                        )
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = EduTealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_payment_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد وحفظ الدفعة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
