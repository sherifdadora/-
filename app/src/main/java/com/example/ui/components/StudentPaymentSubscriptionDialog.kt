package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StudentPaymentSubscriptionDialog(
    initialStudentId: String? = null,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity>,
    subscriptions: List<MonthlySubscriptionEntity> = emptyList(),
    settings: AppSettingsEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    val context = LocalContext.current
    val currency = settings?.currency ?: "جنيه"
    val centerName = settings?.centerName ?: "السنتر التعليمي"
    val defaultAcademicYear = settings?.academicYear ?: "2024 - 2025"

    val groupMap = remember(groups) { groups.associateBy { it.id } }
    val enrollMap = remember(enrollments) { enrollments.groupBy { it.studentId } }

    var selectedStudent by remember {
        mutableStateOf(students.find { it.id == initialStudentId } ?: students.firstOrNull())
    }
    var studentSearchQuery by remember { mutableStateOf("") }
    var isSelectingStudent by remember { mutableStateOf(initialStudentId == null) }

    val studentEnrollments = remember(selectedStudent, enrollments) {
        selectedStudent?.let { enrollMap[it.id] } ?: emptyList()
    }
    val enrolledGroup = remember(studentEnrollments, groupMap) {
        val gId = studentEnrollments.firstOrNull()?.groupId
        if (gId != null) groupMap[gId] else null
    }

    var selectedGroupId by remember(selectedStudent, studentEnrollments) {
        mutableStateOf(enrolledGroup?.id ?: groups.firstOrNull()?.id ?: "group_general")
    }

    val months = listOf("سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو")
    val currentMonthArabic = remember {
        when (Calendar.getInstance().get(Calendar.MONTH)) {
            Calendar.SEPTEMBER -> "سبتمبر"
            Calendar.JANUARY -> "يناير"
            Calendar.FEBRUARY -> "فبراير"
            Calendar.MARCH -> "مارس"
            Calendar.APRIL -> "أبريل"
            Calendar.MAY -> "مايو"
            Calendar.JUNE -> "يونيو"
            Calendar.OCTOBER -> "أكتوبر"
            Calendar.NOVEMBER -> "نوفمبر"
            Calendar.DECEMBER -> "ديسمبر"
            else -> "فبراير"
        }
    }
    var selectedMonth by remember { mutableStateOf(currentMonthArabic) }

    val studentSubscriptions = remember(selectedStudent, subscriptions) {
        selectedStudent?.let { st -> subscriptions.filter { it.studentId == st.id } } ?: emptyList()
    }
    val existingSubForMonth = remember(studentSubscriptions, selectedMonth) {
        studentSubscriptions.find { it.monthYear.contains(selectedMonth) }
    }

    val initialFee = remember(selectedStudent, studentEnrollments) {
        val custom = studentEnrollments.firstOrNull()?.customMonthlyFee
        val fee = custom ?: selectedStudent?.monthlyFee ?: 250.0
        fee.toInt().toString()
    }
    var expectedFeeText by remember(selectedStudent) { mutableStateOf(initialFee) }

    // Default to "مدفوع" unless the existing saved record is explicitly paid/unpaid
    val initialStatus = remember(selectedStudent, selectedMonth, existingSubForMonth) {
        if (existingSubForMonth != null &&
            (existingSubForMonth.status == "غير مدفوع" || existingSubForMonth.status == "لم يدفع" || existingSubForMonth.status == "دفع جزئي")
        ) {
            // When recording a payment for an unpaid month, default selection to "مدفوع" so one click saves it
            "مدفوع"
        } else if (existingSubForMonth?.status == "مدفوع" || existingSubForMonth?.status == "مدفوع بالكامل") {
            "مدفوع"
        } else {
            "مدفوع"
        }
    }
    var selectedMonthStatus by remember(selectedStudent, selectedMonth) { mutableStateOf(initialStatus) }

    var amountPaidText by remember(selectedStudent, selectedMonth) {
        mutableStateOf(
            if (selectedMonthStatus == "غير مدفوع") "0" else initialFee
        )
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { sdf.format(Date()) }
    var paymentDate by remember { mutableStateOf(todayDateStr) }

    // Next Due Date calculations
    val defaultNextMonthFirstDay = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        sdf.format(cal.time)
    }
    val dateIn30Days = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        sdf.format(cal.time)
    }
    val dateIn14Days = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 14)
        sdf.format(cal.time)
    }

    var nextPaymentDueDate by remember(selectedStudent) {
        mutableStateOf(
            if (!selectedStudent?.nextPaymentDueDate.isNullOrBlank()) {
                selectedStudent!!.nextPaymentDueDate
            } else {
                defaultNextMonthFirstDay
            }
        )
    }

    val methods = listOf("نقدًا", "فودافون كاش", "إنستاباي / تحويل بنكي", "أخرى")
    var selectedMethod by remember { mutableStateOf("نقدًا") }
    var notesText by remember { mutableStateOf("") }
    var sendWhatsAppNotice by remember { mutableStateOf(true) }

    val filteredStudents = remember(students, studentSearchQuery) {
        if (studentSearchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(studentSearchQuery, ignoreCase = true) ||
            it.grade.contains(studentSearchQuery, ignoreCase = true) ||
            it.phone.contains(studentSearchQuery)
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
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = EduTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل اشتراك ودفعة طالب",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
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
                    text = "1. بيانات الطالب المستفيد *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (selectedStudent != null && !isSelectingStudent) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = EduTealPrimary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, EduTealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedStudent!!.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${selectedStudent!!.educationalStage} • ${selectedStudent!!.grade} • هاتف: ${selectedStudent!!.phone}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (enrolledGroup != null) {
                                    Text(
                                        text = "المجموعة: ${enrolledGroup.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = EduTealPrimary
                                    )
                                }
                            }
                            if (initialStudentId == null) {
                                OutlinedButton(
                                    onClick = { isSelectingStudent = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("تغيير الطالب", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedCard(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            OutlinedTextField(
                                value = studentSearchQuery,
                                onValueChange = { studentSearchQuery = it },
                                placeholder = { Text("ابحث باسم الطالب أو الكود...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                            ) {
                                items(filteredStudents) { st ->
                                    val isStSelected = st.id == selectedStudent?.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedStudent = st
                                                isSelectingStudent = false
                                                expectedFeeText = st.monthlyFee.toInt().toString()
                                                amountPaidText = st.monthlyFee.toInt().toString()
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = st.fullName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isStSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isStSelected) EduTealPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${st.grade} • ${st.monthlyFee.toInt()} $currency",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (isStSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = EduTealPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }

                // 2. المجموعة والشهر
                Text(
                    text = "2. المجموعة وشهر الاشتراك *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (groups.isNotEmpty()) {
                    Text(text = "المجموعة الدراسية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        groups.take(3).forEach { g ->
                            FilterChip(
                                selected = selectedGroupId == g.id,
                                onClick = { selectedGroupId = g.id },
                                label = { Text(g.name, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "عن شهر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    val savedLabel = when (existingSubForMonth?.status) {
                        "مدفوع", "مدفوع بالكامل" -> "الحالة الحالية: مدفوع ✓"
                        "دفع جزئي" -> "الحالة الحالية: دفع جزئي"
                        "لم يدفع", "غير مدفوع" -> "الحالة الحالية: غير مدفوع"
                        "معفى" -> "الحالة الحالية: معفى"
                        else -> "الحالة الحالية: غير مسجل"
                    }
                    val savedColor = when (existingSubForMonth?.status) {
                        "مدفوع", "مدفوع بالكامل" -> EduSuccessGreen
                        "دفع جزئي" -> EduWarningAmber
                        "لم يدفع", "غير مدفوع" -> EduErrorRed
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = savedColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = savedLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = savedColor,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("current_month_saved_status")
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(months) { m ->
                        val mSub = studentSubscriptions.find { it.monthYear.contains(m) }
                        val isAlreadyPaid = mSub?.status == "مدفوع" || mSub?.status == "مدفوع بالكامل"
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            leadingIcon = if (isAlreadyPaid) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "مدفوع",
                                        tint = EduSuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = { Text(m, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("dialog_month_chip_$m")
                        )
                    }
                }

                // حالة الشهر (مدفوع / غير مدفوع)
                Text(
                    text = "تحديث حالة الشهر في السجل *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPaidSelected = selectedMonthStatus == "مدفوع"
                    Surface(
                        onClick = {
                            selectedMonthStatus = "مدفوع"
                            if (amountPaidText.isBlank() || amountPaidText == "0") {
                                amountPaidText = expectedFeeText.ifBlank { initialFee }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPaidSelected) EduSuccessGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            width = if (isPaidSelected) 2.dp else 1.dp,
                            color = if (isPaidSelected) EduSuccessGreen else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("month_status_paid_chip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "مدفوع",
                                tint = if (isPaidSelected) EduSuccessGreen else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مدفوع",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaidSelected) EduSuccessGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val isUnpaidSelected = selectedMonthStatus == "غير مدفوع"
                    Surface(
                        onClick = {
                            selectedMonthStatus = "غير مدفوع"
                            amountPaidText = "0"
                            sendWhatsAppNotice = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUnpaidSelected) EduErrorRed.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            width = if (isUnpaidSelected) 2.dp else 1.dp,
                            color = if (isUnpaidSelected) EduErrorRed else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("month_status_unpaid_chip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "غير مدفوع",
                                tint = if (isUnpaidSelected) EduErrorRed else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "غير مدفوع",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnpaidSelected) EduErrorRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 3. المبالغ المالية (المقرر والمدفوع)
                Text(
                    text = "3. قيمة الاشتراك والمبلغ المحصّل *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = expectedFeeText,
                        onValueChange = { expectedFeeText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("قيمة الاشتراك ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expected_fee_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = {
                            val clean = it.filter { ch -> ch.isDigit() || ch == '.' }
                            amountPaidText = clean
                            val num = clean.toDoubleOrNull() ?: 0.0
                            if (num > 0) {
                                selectedMonthStatus = "مدفوع"
                            }
                        },
                        enabled = selectedMonthStatus != "غير مدفوع",
                        label = { Text("المدفوع الآن ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("amount_paid_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EduSuccessGreen, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                // Quick Amount Fill Chips
                val expectedParsed = expectedFeeText.toDoubleOrNull() ?: 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMonthStatus == "مدفوع" && amountPaidText == expectedParsed.toInt().toString(),
                        onClick = {
                            selectedMonthStatus = "مدفوع"
                            amountPaidText = expectedParsed.toInt().toString()
                        },
                        label = { Text("سداد كامل", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EduSuccessGreen.copy(alpha = 0.2f))
                    )
                    if (expectedParsed > 0) {
                        val half = (expectedParsed / 2).toInt().toString()
                        FilterChip(
                            selected = amountPaidText == half && selectedMonthStatus == "مدفوع",
                            onClick = {
                                selectedMonthStatus = "مدفوع"
                                amountPaidText = half
                            },
                            label = { Text("نصف المبلغ ($half)", fontSize = 10.sp) }
                        )
                    }
                    FilterChip(
                        selected = selectedMonthStatus == "غير مدفوع" || amountPaidText == "0",
                        onClick = {
                            selectedMonthStatus = "غير مدفوع"
                            amountPaidText = "0"
                            sendWhatsAppNotice = false
                        },
                        label = { Text("غير مدفوع (0)", fontSize = 10.sp) }
                    )
                }

                // 4. تاريخ السداد وطريقة الدفع
                Text(
                    text = "4. تفاصيل وتاريخ السداد",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("تاريخ السداد (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EduTealPrimary, modifier = Modifier.size(18.dp))
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // 5. موعد الدفع القادم والتذكير (FEATURE FOCUS)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📅 موعد الاستحقاق القادم للتذكير بالسداد *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "سيقوم التطبيق بتنبيهك تلقائيًا بموعد التحصيل وإتاحة إرسال تذكير بضغطة زر لولي الأمر عبر واتساب.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = nextPaymentDueDate,
                            onValueChange = { nextPaymentDueDate = it },
                            label = { Text("تاريخ الاستحقاق القادم (yyyy-MM-dd)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("next_payment_due_date_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "خيارات سريعة للموعد القادم:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = nextPaymentDueDate == defaultNextMonthFirstDay,
                                onClick = { nextPaymentDueDate = defaultNextMonthFirstDay },
                                label = { Text("أول الشهر القادم", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilterChip(
                                selected = nextPaymentDueDate == dateIn30Days,
                                onClick = { nextPaymentDueDate = dateIn30Days },
                                label = { Text("بعد 30 يوم", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilterChip(
                                selected = nextPaymentDueDate == dateIn14Days,
                                onClick = { nextPaymentDueDate = dateIn14Days },
                                label = { Text("بعد أسبوعين", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // 6. ملاحظات إضافية
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات إضافية (اختياري)") },
                    placeholder = { Text("مثال: تم السداد مع خصم الأخوة أو رقم التحويل...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                // 7. خيار إرسال إشعار فوري عبر واتساب
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF25D366).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sendWhatsAppNotice = !sendWhatsAppNotice }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sendWhatsAppNotice,
                            onCheckedChange = { sendWhatsAppNotice = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF25D366))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "إرسال إشعار فوري وتذكير بالموعد القادم عبر واتساب",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF075E54)
                            )
                            Text(
                                text = "فتح رسالة واتساب منسقة فور الحفظ لإرسالها لولي أمر الطالب",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val parsedExpected = expectedFeeText.toDoubleOrNull() ?: 0.0
            val parsedAmount = if (selectedMonthStatus == "غير مدفوع") 0.0 else (amountPaidText.toDoubleOrNull() ?: 0.0)
            val effectiveStatus = when {
                selectedMonthStatus == "غير مدفوع" -> "غير مدفوع"
                parsedAmount > 0 && parsedAmount < parsedExpected -> "دفع جزئي"
                else -> "مدفوع"
            }
            val isFormValid = selectedStudent != null && parsedExpected > 0 && paymentDate.isNotBlank()

            Button(
                onClick = {
                    val st = selectedStudent ?: return@Button
                    onConfirm(
                        st.id,
                        selectedGroupId,
                        selectedMonth,
                        defaultAcademicYear,
                        parsedExpected,
                        parsedAmount,
                        paymentDate.trim(),
                        nextPaymentDueDate.trim(),
                        selectedMethod,
                        notesText.trim(),
                        effectiveStatus
                    )

                    if (sendWhatsAppNotice && effectiveStatus != "غير مدفوع") {
                        val recipientPhone = if (st.parentPhone.isNotBlank()) st.parentPhone else st.phone
                        val targetGroup = groupMap[selectedGroupId]?.name ?: enrolledGroup?.name ?: "العامة"

                        val reminderMessage = WhatsAppHelper.buildUpcomingPaymentReminderMessage(
                            studentName = st.fullName,
                            groupName = targetGroup,
                            dueDate = nextPaymentDueDate.trim(),
                            daysRemaining = 30,
                            expectedAmount = parsedExpected,
                            currency = currency,
                            centerName = centerName
                        )

                        val fullMsg = if (parsedAmount > 0) {
                            "تم استلام دفعة بقيمة ${parsedAmount.toInt()} $currency لاشتراك شهر $selectedMonth للطالب ${st.fullName} بنجاح ✅\n\n$reminderMessage"
                        } else {
                            reminderMessage
                        }

                        WhatsAppHelper.openWhatsAppChat(context, recipientPhone, fullMsg)
                    } else {
                        val msgToast = if (effectiveStatus == "غير مدفوع") {
                            "تم تحديث حالة شهر $selectedMonth إلى (غير مدفوع) وحفظه فوراً"
                        } else {
                            "تم تسجيل الدفعة وتحديث حالة شهر $selectedMonth إلى (مدفوع) ✅"
                        }
                        Toast.makeText(context, msgToast, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMonthStatus == "غير مدفوع") EduErrorRed else EduTealPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_subscription_payment_button")
            ) {
                Icon(
                    imageVector = if (selectedMonthStatus == "غير مدفوع") Icons.Default.Cancel else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedMonthStatus == "غير مدفوع") "حفظ كـ غير مدفوع" else "حفظ وتحديث الشهر (مدفوع)",
                    fontWeight = FontWeight.Bold
                )
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
