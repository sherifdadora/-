package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MonthlySubscriptionEntity
import com.example.ui.components.AppDrawerContent
import com.example.ui.components.AppTopBar
import com.example.ui.components.PaymentDialog
import com.example.ui.screens.AttendanceReportsScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GroupDetailScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.OverdueScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.StatementScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.screens.WhatsAppNoticesScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedStudentId by viewModel.selectedStudentId.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val lastSyncFeedback by viewModel.lastSyncFeedback.collectAsStateWithLifecycle()

    val students by viewModel.students.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val enrollments by viewModel.enrollments.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val sessions by viewModel.attendanceSessions.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val overdueItems by viewModel.overdueItems.collectAsStateWithLifecycle()
    val upcomingPayments by viewModel.upcomingPayments.collectAsStateWithLifecycle()
    val frequentAbsentees by viewModel.frequentAbsentees.collectAsStateWithLifecycle()

    // Global Payment Dialog
    var activeSubForPayment by remember { mutableStateOf<MonthlySubscriptionEntity?>(null) }

    val teacherName = settings?.teacherName ?: ""
    val centerName = settings?.centerName ?: "سنتر التفوق والريادة"
    val userRole = settings?.userRole ?: "المعلم"
    val currency = settings?.currency ?: "جنيه"

    // BackHandler for sub-screens
    BackHandler(enabled = currentScreen != ScreenDestination.DASHBOARD) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            when (currentScreen) {
                ScreenDestination.STUDENT_PROFILE, ScreenDestination.STATEMENT -> {
                    viewModel.navigateTo(ScreenDestination.STUDENTS)
                }
                ScreenDestination.GROUP_DETAIL -> {
                    viewModel.navigateTo(ScreenDestination.GROUPS)
                }
                else -> {
                    viewModel.navigateTo(ScreenDestination.DASHBOARD)
                }
            }
        }
    }

    // Force Arabic RTL for entire Application
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    AppDrawerContent(
                        currentScreen = currentScreen,
                        centerName = centerName,
                        overdueCount = overdueItems.size,
                        onNavigate = { dest ->
                            viewModel.navigateTo(dest)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        currentScreen = currentScreen,
                        centerName = centerName,
                        syncStatus = syncStatus,
                        isDarkMode = isDarkMode,
                        isOnline = isOnline,
                        pendingSyncCount = pendingSyncCount,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onBackClick = {
                            when (currentScreen) {
                                ScreenDestination.STUDENT_PROFILE, ScreenDestination.STATEMENT -> {
                                    viewModel.navigateTo(ScreenDestination.STUDENTS)
                                }
                                ScreenDestination.GROUP_DETAIL -> {
                                    viewModel.navigateTo(ScreenDestination.GROUPS)
                                }
                                else -> {
                                    viewModel.navigateTo(ScreenDestination.DASHBOARD)
                                }
                            }
                        },
                        onToggleDarkMode = onToggleDarkMode,
                        onSyncClick = { viewModel.triggerManualSync() }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (currentScreen) {
                        ScreenDestination.DASHBOARD -> {
                            DashboardScreen(
                                students = students,
                                groups = groups,
                                enrollments = enrollments,
                                subscriptions = subscriptions,
                                payments = payments,
                                sessions = sessions,
                                attendanceRecords = attendanceRecords,
                                overdueItems = overdueItems,
                                frequentAbsentees = frequentAbsentees,
                                settings = settings,
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenStudentProfile = { viewModel.openStudentProfile(it) },
                                onOpenPaymentDialog = { activeSubForPayment = it },
                                onOpenGroupDetail = { viewModel.openGroupDetail(it) },
                                onOpenAttendanceForGroup = { viewModel.openAttendanceForGroup(it) },
                                onAddGroup = { viewModel.addGroup(it) }
                            )
                        }

                        ScreenDestination.STUDENTS -> {
                            StudentsScreen(
                                students = students,
                                groups = groups,
                                enrollments = enrollments,
                                subscriptions = subscriptions,
                                payments = payments,
                                settings = settings,
                                onOpenProfile = { viewModel.openStudentProfile(it) },
                                onOpenStatement = { viewModel.openStudentStatement(it) },
                                onAddStudent = { student, groupId -> viewModel.addStudent(student, groupId) },
                                onUpdateStudent = { viewModel.updateStudent(it) },
                                onArchiveStudent = { id, name -> viewModel.archiveStudent(id, name) },
                                onQuickPayMonth = { sId, mName -> viewModel.quickPayMonth(sId, mName) },
                                onCancelMonthPayment = { sId, mName -> viewModel.cancelMonthPayment(sId, mName) },
                                onRecordStudentSubscriptionAndPayment = { sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, mStatus ->
                                    viewModel.recordStudentSubscriptionAndPayment(
                                        sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, mStatus
                                    )
                                }
                            )
                        }

                        ScreenDestination.STUDENT_PROFILE -> {
                            StudentProfileScreen(
                                studentId = selectedStudentId,
                                students = students,
                                groups = groups,
                                enrollments = enrollments,
                                subscriptions = subscriptions,
                                payments = payments,
                                sessions = sessions,
                                records = attendanceRecords,
                                upcomingPayments = upcomingPayments,
                                settings = settings,
                                onOpenStatement = { viewModel.openStudentStatement(it) },
                                onOpenPaymentDialog = { activeSubForPayment = it },
                                onQuickPayMonth = { sId, mName -> viewModel.quickPayMonth(sId, mName) },
                                onCancelMonthPayment = { sId, mName -> viewModel.cancelMonthPayment(sId, mName) },
                                onDeleteStudent = { sId, sName -> viewModel.deleteStudent(sId, sName) },
                                onRecordStudentSubscriptionAndPayment = { sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, mStatus ->
                                    viewModel.recordStudentSubscriptionAndPayment(
                                        sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, mStatus
                                    )
                                }
                            )
                        }

                        ScreenDestination.GROUPS -> {
                            GroupsScreen(
                                groups = groups,
                                students = students,
                                enrollments = enrollments,
                                settings = settings,
                                onOpenGroupDetail = { viewModel.openGroupDetail(it) },
                                onOpenAttendance = { viewModel.openAttendanceForGroup(it) },
                                onAddGroup = { viewModel.addGroup(it) },
                                onDeleteGroup = { gId, gName -> viewModel.deleteGroup(gId, gName) }
                            )
                        }

                        ScreenDestination.GROUP_DETAIL -> {
                            GroupDetailScreen(
                                groupId = selectedGroupId,
                                groups = groups,
                                students = students,
                                enrollments = enrollments,
                                subscriptions = subscriptions,
                                payments = payments,
                                sessions = sessions,
                                attendanceRecords = attendanceRecords,
                                settings = settings,
                                onOpenStudentProfile = { viewModel.openStudentProfile(it) },
                                onOpenAttendance = { viewModel.openAttendanceForGroup(it) },
                                onEnrollStudent = { sId, gId -> viewModel.enrollStudentInGroup(sId, gId) },
                                onUnenrollStudent = { sId, gId -> viewModel.unenrollStudent(sId, gId) },
                                onDeleteGroup = { gId, gName -> viewModel.deleteGroup(gId, gName) }
                            )
                        }

                        ScreenDestination.SCHEDULE -> {
                            ScheduleScreen(
                                groups = groups,
                                settings = settings,
                                onOpenAttendanceForGroup = { viewModel.openAttendanceForGroup(it) }
                            )
                        }

                        ScreenDestination.ATTENDANCE -> {
                            AttendanceScreen(
                                initialGroupId = selectedGroupId,
                                groups = groups,
                                students = students,
                                enrollments = enrollments,
                                sessions = sessions,
                                records = attendanceRecords,
                                settings = settings,
                                isOnline = isOnline,
                                isSyncing = isSyncing,
                                pendingSyncCount = pendingSyncCount,
                                lastSyncFeedback = lastSyncFeedback,
                                onSaveSession = { session, recs -> viewModel.saveAttendanceSession(session, recs) },
                                onTriggerSync = { viewModel.triggerManualSync() },
                                onUpdateCloudSyncUrl = { viewModel.updateCloudSyncUrl(it) },
                                onClearSyncFeedback = { viewModel.clearSyncFeedback() }
                            )
                        }

                        ScreenDestination.ATTENDANCE_REPORTS -> {
                            AttendanceReportsScreen(
                                students = students,
                                groups = groups,
                                sessions = sessions,
                                records = attendanceRecords,
                                frequentAbsentees = frequentAbsentees,
                                settings = settings,
                                onOpenStudentProfile = { viewModel.openStudentProfile(it) }
                            )
                        }

                        ScreenDestination.PAYMENTS -> {
                            PaymentsScreen(
                                subscriptions = subscriptions,
                                payments = payments,
                                students = students,
                                groups = groups,
                                enrollments = enrollments,
                                upcomingPayments = upcomingPayments,
                                settings = settings,
                                onOpenStatement = { viewModel.openStudentStatement(it) },
                                onRecordPayment = { subId, sId, gId, amt, method, notes ->
                                    viewModel.recordPayment(subId, sId, gId, amt, method, notes)
                                },
                                onRecordDirectPayment = { sId, amt, date, method, notes, monthName ->
                                    viewModel.recordDirectStudentPayment(sId, amt, date, method, notes, monthName)
                                },
                                onRecordStudentSubscriptionAndPayment = { sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, mStatus ->
                                    viewModel.recordStudentSubscriptionAndPayment(
                                        sId, gId, mName, aYear, expFee, amtPaid, pDate, nDueDate, method, notes, mStatus
                                    )
                                },
                                onRecordExemption = { subId, reason ->
                                    viewModel.recordExemption(subId, reason)
                                },
                                onCreateMonthlySubscriptions = { month, key, year ->
                                    viewModel.createMonthlySubscriptions(month, key, year)
                                }
                            )
                        }

                        ScreenDestination.OVERDUE -> {
                            OverdueScreen(
                                overdueItems = overdueItems,
                                settings = settings,
                                onOpenStatement = { viewModel.openStudentStatement(it) },
                                onRecordPayment = { subId, sId, gId, amt, method, notes ->
                                    viewModel.recordPayment(subId, sId, gId, amt, method, notes)
                                },
                                onRecordExemption = { subId, reason ->
                                    viewModel.recordExemption(subId, reason)
                                }
                            )
                        }

                        ScreenDestination.STATEMENT -> {
                            StatementScreen(
                                initialStudentId = selectedStudentId,
                                students = students,
                                groups = groups,
                                subscriptions = subscriptions,
                                payments = payments,
                                settings = settings
                            )
                        }

                        ScreenDestination.FINANCIAL_REPORTS -> {
                            ReportsScreen(
                                subscriptions = subscriptions,
                                payments = payments,
                                groups = groups,
                                students = students,
                                settings = settings
                            )
                        }

                        ScreenDestination.WHATSAPP_NOTICES -> {
                            WhatsAppNoticesScreen(
                                students = students,
                                groups = groups,
                                enrollments = enrollments,
                                sessions = sessions,
                                attendanceRecords = attendanceRecords,
                                overdueItems = overdueItems,
                                upcomingPayments = upcomingPayments,
                                settings = settings,
                                onUpdateSettings = { viewModel.updateSettings(it) }
                            )
                        }

                        ScreenDestination.BACKUP, ScreenDestination.SETTINGS -> {
                            BackupSettingsScreen(
                                settings = settings,
                                students = students,
                                groups = groups,
                                subscriptions = subscriptions,
                                payments = payments,
                                auditLogs = auditLogs,
                                syncStatus = syncStatus,
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onTriggerSync = { viewModel.triggerManualSync() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Global Payment Dialog
    if (activeSubForPayment != null) {
        val sub = activeSubForPayment!!
        val student = students.find { it.id == sub.studentId }
        val group = groups.find { it.id == sub.groupId }
        val subPayments = payments.filter { it.subscriptionId == sub.id }
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
                viewModel.recordPayment(sub.id, sub.studentId, sub.groupId, amount, method, notes)
                activeSubForPayment = null
            },
            onExempt = { reason ->
                viewModel.recordExemption(sub.id, reason)
                activeSubForPayment = null
            }
        )
    }
}
