package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettingsEntity
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.EnrollmentEntity
import com.example.data.model.GroupEntity
import com.example.data.model.MonthlySubscriptionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentById(id: String): Flow<StudentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("UPDATE students SET isArchived = 1 WHERE id = :id")
    suspend fun archiveStudent(id: String)

    @Query("UPDATE students SET nextPaymentDueDate = :dueDate WHERE id = :id")
    suspend fun updateNextPaymentDueDate(id: String, dueDate: String)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE isActive = 1 ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    fun getGroupById(id: String): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Query("UPDATE groups SET isActive = 0 WHERE id = :id")
    suspend fun archiveGroup(id: String)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}

@Dao
interface EnrollmentDao {
    @Query("SELECT * FROM enrollments WHERE isActive = 1")
    fun getAllEnrollments(): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE groupId = :groupId AND isActive = 1")
    fun getEnrollmentsForGroup(groupId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE studentId = :studentId AND isActive = 1")
    fun getEnrollmentsForStudent(studentId: String): Flow<List<EnrollmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollments(enrollments: List<EnrollmentEntity>)

    @Query("DELETE FROM enrollments WHERE studentId = :studentId AND groupId = :groupId")
    suspend fun deleteByStudentAndGroup(studentId: String, groupId: String)

    @Query("DELETE FROM enrollments WHERE groupId = :groupId")
    suspend fun deleteEnrollmentsForGroup(groupId: String)

    @Query("DELETE FROM enrollments WHERE studentId = :studentId")
    suspend fun deleteEnrollmentsForStudent(studentId: String)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM monthly_subscriptions ORDER BY monthSortKey DESC")
    fun getAllSubscriptions(): Flow<List<MonthlySubscriptionEntity>>

    @Query("SELECT * FROM monthly_subscriptions WHERE studentId = :studentId ORDER BY monthSortKey DESC")
    fun getSubscriptionsForStudent(studentId: String): Flow<List<MonthlySubscriptionEntity>>

    @Query("SELECT * FROM monthly_subscriptions WHERE groupId = :groupId ORDER BY monthSortKey DESC")
    fun getSubscriptionsForGroup(groupId: String): Flow<List<MonthlySubscriptionEntity>>

    @Query("SELECT * FROM monthly_subscriptions WHERE id = :id LIMIT 1")
    fun getSubscriptionById(id: String): Flow<MonthlySubscriptionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: MonthlySubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subs: List<MonthlySubscriptionEntity>)

    @Update
    suspend fun updateSubscription(sub: MonthlySubscriptionEntity)

    @Query("UPDATE monthly_subscriptions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE monthly_subscriptions SET nextDueDate = :nextDueDate WHERE id = :id")
    suspend fun updateNextDueDate(id: String, nextDueDate: String)

    @Query("SELECT * FROM monthly_subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionDirect(id: String): MonthlySubscriptionEntity?

    @Query("SELECT * FROM monthly_subscriptions WHERE studentId = :studentId")
    suspend fun getSubscriptionsListForStudent(studentId: String): List<MonthlySubscriptionEntity>
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentTimestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE subscriptionId = :subscriptionId ORDER BY paymentTimestamp ASC")
    fun getPaymentsForSubscription(subscriptionId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY paymentTimestamp DESC")
    fun getPaymentsForStudent(studentId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY paymentTimestamp DESC")
    fun getPaymentsForGroup(groupId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE subscriptionId = :subscriptionId")
    suspend fun getPaymentsListForSubscription(subscriptionId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE studentId = :studentId")
    suspend fun getPaymentsListForStudent(studentId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_sessions ORDER BY sessionDate DESC, createdAt DESC")
    fun getAllSessions(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId ORDER BY sessionDate DESC")
    fun getSessionsForGroup(groupId: String): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: String): Flow<AttendanceSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<AttendanceSessionEntity>)

    @Query("SELECT * FROM attendance_sessions WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedSessionsList(): List<AttendanceSessionEntity>

    @Query(
        "UPDATE attendance_sessions SET isSynced = :isSynced, syncStatus = :syncStatus, " +
            "lastSyncedAt = :lastSyncedAt, syncError = :syncError WHERE id = :sessionId"
    )
    suspend fun updateSessionSyncState(
        sessionId: String,
        isSynced: Boolean,
        syncStatus: String,
        lastSyncedAt: Long?,
        syncError: String?
    )

    @Delete
    suspend fun deleteSession(session: AttendanceSessionEntity)

    @Query("SELECT * FROM attendance_records")
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    fun getRecordsForSession(sessionId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun getRecordsForSessionDirect(sessionId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getRecordsForStudent(studentId: String): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettingsEntity?

    @Query("UPDATE app_settings SET teacherName = '', userRole = 'المعلم' WHERE id = 1")
    suspend fun clearTeacherAndRole()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettingsEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}
