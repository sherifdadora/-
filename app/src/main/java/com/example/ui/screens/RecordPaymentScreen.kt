package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduWarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PaymentInputHelper {
    private val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val academicOrderMonths = listOf(
        "أكتوبر", "نوفمبر", "ديسمبر", "يناير", "فبراير", "مارس",
        "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر"
    )

    fun normalizeDecimalInput(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw.trim()) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                '٫', ',' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun parsePositiveAmount(raw: String): Double? {
        val normalized = normalizeDecimalInput(raw)
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value > 0.0) value else null
    }

    fun validateAmount(raw: String): String? {
        if (raw.isBlank()) return "يرجى إدخال مبلغ الدفعة"
        val parsed = parsePositiveAmount(raw) ?: return "أدخل مبلغاً صحيحاً أكبر من صفر"
        if (parsed > 1_000_000.0) return "المبلغ المدخل غير منطقي"
        return null
    }

    fun arabicMonthNameForDate(dateIso: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateIso) ?: return arabicMonths[Calendar.getInstance().get(Calendar.MONTH)]
            val cal = Calendar.getInstance().apply { time = date }
            arabicMonths[cal.get(Calendar.MONTH)]
        } catch (_: Exception) {
            arabicMonths[Calendar.getInstance().get(Calendar.MONTH)]
        }
    }

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun yesterdayIso(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun formatDateDisplayArabic(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateIso) ?: return dateIso
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = arabicMonths[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            "$day $monthName $year ($dateIso)"
        } catch (_: Exception) {
            dateIso
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentScreen(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    enrollments: List<EnrollmentEntity>,
    subscriptions: List<MonthlySubscriptionEntity> = emptyList(),
    settings: AppSettingsEntity?,
    initialStudentId: String? = null,
    onQuickPayMonth: (
        studentId: String,
        monthName: String,
        groupId: String,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onCancelMonthPayment: (studentId: String, monthName: String) -> Unit = { _, _ -> },
    onSavePayment: (
        studentId: String,
        groupId: String,
        monthName: String,
        amount: Double,
        paymentDate: String,
        paymentMethod: String,
        notes: String
    ) -> Unit,
    onCancel: () -> Unit,
    onNavigateToAddStudent: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val currency = settings?.currency ?: "جنيه"

    val selectableStudents = remember(students) {
        val active = students.filter { it.status == "نشط" }
        (active.ifEmpty { students }).sortedBy { it.fullName }
    }
    val groupMap = remember(groups) { groups.associateBy { it.id } }

    var selectedStudentId by remember(selectableStudents, initialStudentId) {
        mutableStateOf(
            initialStudentId?.takeIf { id -> selectableStudents.any { it.id == id } }
                ?: selectableStudents.firstOrNull()?.id
        )
    }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    val selectedStudent = selectableStudents.find { it.id == selectedStudentId }
    val selectedStudentEnrollment = remember(selectedStudentId, enrollments) {
        enrollments.firstOrNull { it.studentId == selectedStudentId && it.isActive }
    }
    val selectedGroup = selectedStudentEnrollment?.groupId?.let { groupMap[it] }
    val suggestedAmount = selectedStudentEnrollment?.customMonthlyFee
        ?: selectedGroup?.defaultMonthlyFee
        ?: selectedStudent?.monthlyFee
        ?: settings?.defaultFee
        ?: 250.0

    val studentSubs = remember(selectedStudentId, subscriptions) {
        if (selectedStudentId == null) emptyList()
        else subscriptions.filter { it.studentId == selectedStudentId }
    }

    var amountText by remember(selectedStudentId) {
        mutableStateOf(if (selectedStudent != null) suggestedAmount.toInt().toString() else "")
    }
    var amountTouched by remember { mutableStateOf(false) }

    var paymentDate by remember { mutableStateOf(PaymentInputHelper.todayIso()) }
    var selectedMonth by remember { mutableStateOf(PaymentInputHelper.arabicMonthNameForDate(paymentDate)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var monthToCancel by remember { mutableStateOf<String?>(null) }

    val paymentMethods = listOf("نقدًا", "فودافون كاش", "إنستاباي", "تحويل بنكي")
    var selectedMethod by remember { mutableStateOf(paymentMethods.first()) }
    var notes by remember { mutableStateOf("") }

    var showValidationErrors by remember { mutableStateOf(false) }
    val studentError = showValidationErrors && selectedStudent == null
    val amountErrorMsg = PaymentInputHelper.validateAmount(amountText)
    val showAmountError = (showValidationErrors || amountTouched) && amountErrorMsg != null

    val submitPayment: () -> Unit = {
        val parsedAmount = PaymentInputHelper.parsePositiveAmount(amountText)
        if (selectedStudent == null || parsedAmount == null || amountErrorMsg != null) {
            showValidationErrors = true
        } else {
            focusManager.clearFocus()
            onSavePayment(
                selectedStudent.id,
                selectedGroup?.id ?: "",
                selectedMonth,
                parsedAmount,
                paymentDate,
                selectedMethod,
                notes.trim()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تسجيل دفعة طالب",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "اختر الطالب ثم اضغط على الشهر لتسجيل أنه دفع فوراً، أو حدد المبلغ والتاريخ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Form Card
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Select Student (DropdownMenu)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedStudent?.fullName
                                    ?: if (selectableStudents.isEmpty()) "لا يوجد طلاب مسجلون بعد" else "اختر الطالب *",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الطالب *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (studentError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (studentMenuExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = "اختيار الطالب",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                isError = studentError,
                                supportingText = if (studentError) {
                                    { Text("يرجى اختيار الطالب أولاً", color = MaterialTheme.colorScheme.error) }
                                } else null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = selectableStudents.isNotEmpty()) {
                                        focusManager.clearFocus()
                                        studentMenuExpanded = true
                                    }
                                    .testTag("record_payment_student_dropdown")
                            )

                            DropdownMenu(
                                expanded = studentMenuExpanded,
                                onDismissRequest = { studentMenuExpanded = false },
                                modifier = Modifier.widthIn(min = 260.dp, max = 360.dp)
                            ) {
                                selectableStudents.forEach { student ->
                                    val isSelected = selectedStudentId == student.id
                                    val stEnrollment = enrollments.firstOrNull { it.studentId == student.id && it.isActive }
                                    val stGroupName = stEnrollment?.groupId?.let { groupMap[it]?.name }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = student.fullName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(student.phone)
                                                        if (!stGroupName.isNullOrBlank()) {
                                                            append(" • ")
                                                            append(stGroupName)
                                                        }
                                                    },
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "مختار",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectedStudentId = student.id
                                            studentMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = selectedStudent != null) {
                            selectedStudent?.let { st ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = buildString {
                                            append("هاتف: ${st.phone}")
                                            if (selectedGroup != null) {
                                                append(" • ${selectedGroup.name}")
                                            }
                                            append(" • الاشتراك المقترح: ${suggestedAmount.toInt()} $currency")
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        if (selectableStudents.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "أضف طالباً أولاً لتسجيل مدفوعاته",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onNavigateToAddStudent) {
                                    Text("إضافة طالب", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. One-Tap Month Selection Grid ("مجرد مااضغط على الشهر يسجل انه دفع")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "اختر الشهر (الضغط على الشهر يسجل الدفع فوراً) *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "الشهر المحدد: $selectedMonth",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val monthRows = remember { PaymentInputHelper.academicOrderMonths.chunked(3) }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            monthRows.forEach { rowMonths ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowMonths.forEach { monthName ->
                                        val sub = studentSubs.find { it.monthYear.contains(monthName) }
                                        val isPaid = sub?.status == "مدفوع" || sub?.status == "مدفوع بالكامل"
                                        val isPartial = sub?.status == "دفع جزئي"
                                        val isExempt = sub?.status == "معفى"
                                        val isCurrent = selectedMonth == monthName

                                        val bgColor = when {
                                            isPaid -> EduSuccessGreen
                                            isPartial -> EduWarningAmber
                                            isExempt -> MaterialTheme.colorScheme.tertiary
                                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        val fgColor = when {
                                            isPaid || isPartial || isExempt -> Color.White
                                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = bgColor,
                                            border = if (!isPaid && !isPartial && !isExempt) {
                                                BorderStroke(
                                                    width = if (isCurrent) 2.dp else 1.dp,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            } else null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 54.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedMonth = monthName
                                                    val targetStudent = selectedStudent
                                                    if (targetStudent == null) {
                                                        showValidationErrors = true
                                                    } else if (isPaid) {
                                                        monthToCancel = monthName
                                                    } else {
                                                        focusManager.clearFocus()
                                                        val effectiveAmount =
                                                            PaymentInputHelper.parsePositiveAmount(amountText)
                                                                ?: suggestedAmount
                                                        onQuickPayMonth(
                                                            targetStudent.id,
                                                            monthName,
                                                            selectedGroup?.id ?: "",
                                                            effectiveAmount,
                                                            paymentDate,
                                                            selectedMethod,
                                                            notes.trim()
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "تم تسجيل دفع شهر $monthName للطالب ${targetStudent.fullName} ✓",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .testTag("record_payment_month_$monthName")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (isPaid) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "مدفوع",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                    }
                                                    Text(
                                                        text = monthName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = fgColor,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                                Text(
                                                    text = when {
                                                        isPaid -> "مدفوع ✓"
                                                        isPartial -> "دفع جزئي"
                                                        isExempt -> "معفى"
                                                        else -> "اضغط للدفع"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Medium,
                                                    color = fgColor.copy(alpha = if (isPaid || isPartial || isExempt) 0.95f else 0.75f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Payment Amount
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                amountTouched = true
                            },
                            label = { Text("المبلغ ($currency) *") },
                            placeholder = { Text("مثال: 250") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = if (showAmountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            },
                            suffix = { Text(currency, fontSize = 12.sp) },
                            isError = showAmountError,
                            supportingText = if (showAmountError) {
                                { Text(amountErrorMsg ?: "", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("record_payment_amount_input")
                        )

                        // Quick amount chips
                        val quickAmounts = remember(suggestedAmount) {
                            listOf(suggestedAmount.toInt(), 100, 150, 200, 300).distinct()
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickAmounts.forEach { amt ->
                                AssistChip(
                                    onClick = {
                                        amountText = amt.toString()
                                        amountTouched = true
                                    },
                                    label = { Text("$amt $currency", fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // 4. Payment Date Picker Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = PaymentInputHelper.formatDateDisplayArabic(paymentDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تاريخ الدفعة *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        focusManager.clearFocus()
                                        showDatePicker = true
                                    }
                                    .testTag("record_payment_date_field")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val today = remember { PaymentInputHelper.todayIso() }
                            val yesterday = remember { PaymentInputHelper.yesterdayIso() }
                            FilterChip(
                                selected = paymentDate == today,
                                onClick = { paymentDate = today },
                                label = { Text("اليوم", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = paymentDate == yesterday,
                                onClick = { paymentDate = yesterday },
                                label = { Text("أمس", fontSize = 12.sp) }
                            )
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text("اختيار من التقويم", fontSize = 12.sp)
                            }
                        }
                    }

                    // 5. Payment Method Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "طريقة الدفع:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                FilterChip(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method },
                                    label = { Text(method, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // 6. Optional Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات (اختياري)") },
                        placeholder = { Text("مثال: اشتراك شهر $selectedMonth") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_payment_notes_input")
                    )
                }
            }

            // Save & Cancel Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = submitPayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("record_payment_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ دفعة شهر $selectedMonth في قاعدة البيانات",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("record_payment_cancel_button")
                ) {
                    Text(
                        text = "رجوع",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (monthToCancel != null && selectedStudent != null) {
        val mName = monthToCancel.orEmpty()
        AlertDialog(
            onDismissRequest = { monthToCancel = null },
            title = { Text("تحديث حالة شهر $mName", fontWeight = FontWeight.Bold) },
            text = {
                Text("اشتراك شهر $mName مسجل حالياً كـ \"مدفوع\" للطالب ${selectedStudent.fullName}.\n\nهل ترغب في إلغاء الدفعة وتغييره إلى \"غير مدفوع\"؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelMonthPayment(selectedStudent.id, mName)
                        Toast.makeText(context, "تم تحديث شهر $mName إلى غير مدفوع", Toast.LENGTH_SHORT).show()
                        monthToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                ) {
                    Text("تغيير إلى غير مدفوع")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { monthToCancel = null }) {
                    Text("إبقاء كمدفوع")
                }
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(paymentDate) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(paymentDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            paymentDate = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
