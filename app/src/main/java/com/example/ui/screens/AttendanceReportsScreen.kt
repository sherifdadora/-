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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.StudentAttendanceStat
import com.example.ui.theme.EduErrorContainer
import com.example.ui.theme.EduErrorRed
import com.example.ui.theme.EduSkyTertiary
import com.example.ui.theme.EduSuccessContainer
import com.example.ui.theme.EduSuccessGreen
import com.example.ui.theme.EduTealPrimary
import com.example.ui.theme.EduWarningAmber
import com.example.util.ExportHelper
import com.example.util.WhatsAppHelper

@Composable
fun AttendanceReportsScreen(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    sessions: List<AttendanceSessionEntity>,
    records: List<AttendanceRecordEntity>,
    frequentAbsentees: List<StudentAttendanceStat>,
    settings: AppSettingsEntity?,
    onOpenStudentProfile: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedGroupFilter by remember { mutableStateOf("الكل") }

    val filteredSessions = if (selectedGroupFilter == "الكل") sessions else sessions.filter { it.groupId == selectedGroupFilter }
    val filteredRecords = if (selectedGroupFilter == "الكل") records else records.filter { it.groupId == selectedGroupFilter }

    val totalPresent = filteredRecords.count { it.status == "حاضر" }
    val totalAbsent = filteredRecords.count { it.status == "غائب" }
    val totalLate = filteredRecords.count { it.status == "متأخر" }
    val totalExcused = filteredRecords.count { it.status == "غياب بعذر" }
    val total = filteredRecords.size
    val overallPercentage = if (total > 0) ((totalPresent + totalLate) * 100 / total) else 100

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Group Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تقارير الحضور والغياب الشاملة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "متابعة نسب الالتزام، الطلاب كثيري الغياب، والحصص المسجلة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(onClick = {
                    val sb = StringBuilder()
                    sb.append("📊 تقرير الحضور والغياب العام\n")
                    sb.append("───────────────────────\n")
                    sb.append("إجمالي الحصص: ${filteredSessions.size}\n")
                    sb.append("نسبة الحضور الإجمالية: $overallPercentage%\n")
                    sb.append("عدد الحاضرين: $totalPresent | الغياب: $totalAbsent | التأخير: $totalLate | بعذر: $totalExcused\n")
                    sb.append("───────────────────────\n")
                    sb.append("الطلاب الأكثر غياباً:\n")
                    frequentAbsentees.forEach { s ->
                        sb.append("• ${s.studentName}: غاب ${s.absentCount} حصص (نسبة: ${s.attendancePercentage}%)\n")
                    }
                    ExportHelper.shareText(context, "تقرير الحضور والغياب", sb.toString())
                }) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة التقرير", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Group Filter chips
        item {
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

        // Summary KPI Banner
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
                        Text(text = "معدل الحضور العام", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$overallPercentage%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EduTealPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatPill("حاضر: $totalPresent", EduSuccessGreen, EduSuccessContainer, Modifier.weight(1f))
                        StatPill("غائب: $totalAbsent", EduErrorRed, EduErrorContainer, Modifier.weight(1f))
                        StatPill("متأخر: $totalLate", EduWarningAmber, EduWarningAmber.copy(alpha = 0.15f), Modifier.weight(1f))
                        StatPill("بعذر: $totalExcused", EduSkyTertiary, EduSkyTertiary.copy(alpha = 0.15f), Modifier.weight(1f))
                    }
                }
            }
        }

        // High Absence Watchlist (Frequent Absentees)
        item {
            Text(
                text = "قائمة الطلاب الأكثر غياباً (تحذير المتابعة)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EduErrorRed
            )
        }

        if (frequentAbsentees.isEmpty()) {
            item {
                Surface(
                    color = EduSuccessContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "رائع! لا يوجد طلاب تجاوزوا حد الغياب المتكرر.",
                        color = EduSuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            items(frequentAbsentees) { stat ->
                val student = students.find { it.id == stat.studentId }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenStudentProfile(stat.studentId) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stat.studentName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "غاب ${stat.absentCount} حصص من أصل ${stat.totalSessions} • نسبة الحضور: ${stat.attendancePercentage}%",
                                fontSize = 11.sp,
                                color = EduErrorRed
                            )
                        }

                        if (student != null && student.parentPhone.isNotBlank()) {
                            Button(
                                onClick = {
                                    val msg = "${settings?.absenceNoticeTemplate ?: ""}\nنلفت انتباهكم لتكرار غياب الطالب ${student.fullName} (${stat.absentCount} حصص)."
                                    WhatsAppHelper.openWhatsAppChat(context, student.parentPhone, msg)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EduErrorRed)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب ولي الأمر", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sessions Log History
        item {
            Text(
                text = "سجل كشوف الحصص المنفذة (${filteredSessions.size} حصة)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (filteredSessions.isEmpty()) {
            item {
                Text("لا توجد حصص مسجلة في هذا القسم بعد.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
            }
        } else {
            items(filteredSessions) { session ->
                val group = groups.find { it.id == session.groupId }
                val sessionRecords = records.filter { it.sessionId == session.id }
                val pCount = sessionRecords.count { it.status == "حاضر" }
                val aCount = sessionRecords.count { it.status == "غائب" }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${group?.name ?: "مجموعة"} • ${session.sessionDate}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "حاضر: $pCount | غائب: $aCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (session.lessonTopic.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "موضوع الدرس: ${session.lessonTopic}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

@Composable
fun StatPill(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}
