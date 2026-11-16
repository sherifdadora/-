package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.eduSuccessContentColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object StudentInputValidator {
    fun normalizePhoneInput(rawPhone: String): String {
        val sb = StringBuilder()
        for (ch in rawPhone.trim()) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                ' ', '-', '(', ')' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun validateStudentName(rawName: String): String? {
        val trimmed = rawName.trim()
        return when {
            trimmed.isEmpty() -> "يرجى إدخال اسم الطالب"
            trimmed.length < 2 -> "اسم الطالب قصير جداً"
            else -> null
        }
    }

    fun validateStudentPhone(rawPhone: String): String? {
        if (rawPhone.isBlank()) {
            return "يرجى إدخال رقم هاتف الطالب"
        }
        val normalized = normalizePhoneInput(rawPhone)
        val hasLeadingPlus = normalized.startsWith("+")
        val body = if (hasLeadingPlus) normalized.substring(1) else normalized

        if (body.isEmpty() || body.any { !it.isDigit() }) {
            return "رقم الهاتف يجب أن يحتوي على أرقام فقط"
        }

        if (body.all { it == body[0] }) {
            return "رقم الهاتف غير صالح"
        }

        if (!hasLeadingPlus && body.startsWith("01")) {
            val egyptMobileRegex = Regex("^01[0125][0-9]{8}$")
            if (!egyptMobileRegex.matches(body)) {
                return "رقم الموبايل المصري يجب أن يتكون من 11 رقماً ويبدأ بـ 010 أو 011 أو 012 أو 015"
            }
            return null
        }

        if ((hasLeadingPlus && body.startsWith("20")) || (!hasLeadingPlus && body.startsWith("201"))) {
            val localPart = body.removePrefix("20")
            val egyptIntlRegex = Regex("^1[0125][0-9]{8}$")
            if (!egyptIntlRegex.matches(localPart)) {
                return "صيغة الرقم الدولي لمصر غير صحيحة (مثال: +2010xxxxxxxx)"
            }
            return null
        }

        if (body.length < 10 || body.length > 15) {
            return "رقم الهاتف يجب أن يتكون من 10 إلى 15 رقماً"
        }

        return null
    }
}

@Composable
fun AddStudentScreen(
    groups: List<GroupEntity>,
    settings: AppSettingsEntity?,
    onSaveStudent: (student: StudentEntity, selectedGroupId: String?) -> Unit,
    onCancel: () -> Unit,
    onNavigateToGroups: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedGroupId by remember(groups) {
        mutableStateOf(groups.firstOrNull()?.id)
    }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    var nameTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    val nameErrorMsg = StudentInputValidator.validateStudentName(fullName)
    val phoneErrorMsg = StudentInputValidator.validateStudentPhone(phone)

    val showNameError = (showValidationErrors || nameTouched) && nameErrorMsg != null
    val showPhoneError = (showValidationErrors || phoneTouched) && phoneErrorMsg != null
    val isPhoneValid = phone.isNotBlank() && phoneErrorMsg == null

    val selectedGroup = groups.find { it.id == selectedGroupId }
    val selectedGroupLabel = selectedGroup?.name
        ?: if (groups.isEmpty()) "لا توجد مجموعات متاحة حالياً" else "بدون مجموعة حالياً"

    val submitStudent: () -> Unit = {
        if (nameErrorMsg != null || phoneErrorMsg != null) {
            showValidationErrors = true
        } else {
            focusManager.clearFocus()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val defaultFee = selectedGroup?.defaultMonthlyFee ?: settings?.defaultFee ?: 250.0
            val cleanPhone = StudentInputValidator.normalizePhoneInput(phone)
            val student = StudentEntity(
                id = UUID.randomUUID().toString(),
                fullName = fullName.trim(),
                phone = cleanPhone,
                parentPhone = cleanPhone,
                educationalStage = selectedGroup?.stage ?: "عام",
                grade = selectedGroup?.grade ?: "غير محدد",
                subject = selectedGroup?.subject ?: "",
                registrationDate = today,
                status = "نشط",
                monthlyFee = defaultFee,
                notes = ""
            )
            onSaveStudent(student, selectedGroup?.id)
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
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إضافة طالب جديد",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "أدخل الاسم ورقم الهاتف الصحيح واختر المجموعة الدراسية ثم اضغط حفظ",
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
                    // 1. Student Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (!nameTouched && it.isNotEmpty()) nameTouched = true
                        },
                        label = { Text("اسم الطالب *") },
                        placeholder = { Text("مثال: محمد أحمد علي") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        isError = showNameError,
                        supportingText = if (showNameError) {
                            { Text(nameErrorMsg ?: "", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_student_name_input")
                    )

                    // 2. Phone Number Field with Live Validation
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneTouched = true
                        },
                        label = { Text("رقم الهاتف *") },
                        placeholder = { Text("مثال: 01012345678") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (showPhoneError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        trailingIcon = if (isPhoneValid) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "رقم هاتف صحيح",
                                    tint = eduSuccessContentColor(),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else null,
                        isError = showPhoneError,
                        supportingText = when {
                            showPhoneError -> {
                                { Text(phoneErrorMsg ?: "", color = MaterialTheme.colorScheme.error) }
                            }
                            isPhoneValid -> {
                                { Text("رقم هاتف صحيح ✓", color = eduSuccessContentColor()) }
                            }
                            else -> {
                                { Text("أدخل رقم موبايل صحيح (مثال: 010/011/012/015)") }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                phoneTouched = true
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_student_phone_input")
                    )

                    // 3. Study Group Field (DropdownMenu selecting from available groups)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedGroupLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("المجموعة الدراسية") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (groupMenuExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = "اختيار المجموعة الدراسية",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Full-area clickable overlay to guarantee DropdownMenu opens on tap anywhere
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = groups.isNotEmpty()) {
                                        focusManager.clearFocus()
                                        groupMenuExpanded = true
                                    }
                                    .testTag("add_student_group_dropdown")
                            )

                            DropdownMenu(
                                expanded = groupMenuExpanded,
                                onDismissRequest = { groupMenuExpanded = false },
                                modifier = Modifier.widthIn(min = 260.dp, max = 360.dp)
                            ) {
                                groups.forEach { group ->
                                    val isSelected = selectedGroupId == group.id
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = group.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${group.subject} • ${group.daysOfWeek} • ${group.sessionTime}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Class,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "مختارة",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectedGroupId = group.id
                                            groupMenuExpanded = false
                                        }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "بدون مجموعة حالياً",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = if (selectedGroupId == null) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "مختارة",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        selectedGroupId = null
                                        groupMenuExpanded = false
                                    }
                                )
                            }
                        }

                        AnimatedVisibility(visible = selectedGroup != null) {
                            selectedGroup?.let { grp ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Class,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${grp.subject} • ${grp.grade} • الاشتراك: ${grp.defaultMonthlyFee.toInt()} ${settings?.currency ?: "جنيه"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        if (groups.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "لا توجد مجموعات مسجلة بعد",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = onNavigateToGroups) {
                                    Text("إضافة مجموعة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Save & Cancel Actions
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = submitStudent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("add_student_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ الطالب",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_student_cancel_button")
                ) {
                    Text(
                        text = "إلغاء",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
