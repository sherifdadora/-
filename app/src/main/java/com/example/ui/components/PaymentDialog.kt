package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduWarningAmber

@Composable
fun PaymentDialog(
    studentName: String,
    groupName: String,
    monthYear: String,
    expectedAmount: Double,
    alreadyPaid: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, method: String, notes: String) -> Unit,
    onExempt: (reason: String) -> Unit
) {
    val remaining = (expectedAmount - alreadyPaid).coerceAtLeast(0.0)
    var amountText by remember { mutableStateOf(if (remaining > 0) remaining.toInt().toString() else "0") }
    var selectedMethod by remember { mutableStateOf("نقدًا") }
    var notesText by remember { mutableStateOf("") }
    var isExemptionMode by remember { mutableStateOf(false) }
    var exemptionReason by remember { mutableStateOf("") }

    val methods = listOf("نقدًا", "محفظة إلكترونية (فودافون كاش)", "إنستاباي / تحويل بنكي", "أخرى")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExemptionMode) "تسجيل إعفاء من الاشتراك" else "تسجيل دفعة جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Info Summary Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "الطالب:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = studentName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "المجموعة:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = groupName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "الشهر المستحق:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = monthYear, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "قيمة الاشتراك:", fontSize = 13.sp)
                            Text(text = "${expectedAmount.toInt()} $currency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "المدفوع سابقاً:", fontSize = 13.sp)
                            Text(text = "${alreadyPaid.toInt()} $currency", fontSize = 13.sp, color = EduSuccessGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "المبلغ المتبقي:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${remaining.toInt()} $currency",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remaining > 0) EduErrorRed else EduSuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle between Payment and Exemption
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isExemptionMode,
                        onClick = { isExemptionMode = false },
                        label = { Text("تسجيل سداد نقدي/إلكتروني") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isExemptionMode,
                        onClick = { isExemptionMode = true },
                        label = { Text("تسجيل إعفاء") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isExemptionMode) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ المدفوع ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "طريقة الدفع:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        methods.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("ملاحظات الدفعة (اختياري)") },
                        placeholder = { Text("مثال: تم التحويل برقم عملية...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                } else {
                    OutlinedTextField(
                        value = exemptionReason,
                        onValueChange = { exemptionReason = it },
                        label = { Text("سبب الإعفاء") },
                        placeholder = { Text("مثال: ظروف أسرية، تفوق علمي، منحة سنتر...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isExemptionMode) {
                        onExempt(if (exemptionReason.isNotBlank()) exemptionReason else "إعفاء من الإدارة")
                    } else {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            onConfirmPayment(amount, selectedMethod, notesText)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExemptionMode) EduWarningAmber else EduSuccessGreen
                )
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(if (isExemptionMode) "حفظ الإعفاء" else "تأكيد واستلام الدفعة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
