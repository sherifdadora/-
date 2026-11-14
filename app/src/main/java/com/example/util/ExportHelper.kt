package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object ExportHelper {

    const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    const val DEFAULT_APK_FILENAME = "EduGroups_Offline.apk"

    fun shareText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    fun getInstalledApkFile(context: Context): File? {
        val sourceDir = context.applicationInfo?.sourceDir ?: return null
        val file = File(sourceDir)
        return if (file.exists() && file.canRead()) file else null
    }

    fun getApkSizeMbText(context: Context): String {
        val file = getInstalledApkFile(context) ?: return "غير متاح"
        val sizeMb = file.length() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f ميجابايت", sizeMb)
    }

    fun saveApkToDownloads(
        context: Context,
        fileName: String = DEFAULT_APK_FILENAME
    ): Result<String> {
        val sourceFile = getInstalledApkFile(context)
            ?: return Result.failure(IllegalStateException("لم يتم العثور على حزمة التثبيت الحالية"))

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, APK_MIME_TYPE)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: return Result.failure(IllegalStateException("تعذر إنشاء ملف في مجلد التنزيلات"))

                resolver.openOutputStream(itemUri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: return Result.failure(IllegalStateException("تعذر الكتابة في ملف التنزيلات"))

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)

                Result.success("تم حفظ $fileName في مجلد التنزيلات (Downloads) بنجاح")
            } else {
                Result.failure(IllegalStateException("USE_SAF"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun copyApkToUri(context: Context, targetUri: Uri): Result<String> {
        val sourceFile = getInstalledApkFile(context)
            ?: return Result.failure(IllegalStateException("لم يتم العثور على حزمة التثبيت الحالية"))

        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("تعذر فتح مسار الحفظ المحدد"))
            Result.success("تم حفظ ملف APK في المسار المختار بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareApkOffline(
        context: Context,
        fileName: String = DEFAULT_APK_FILENAME
    ): Result<Unit> {
        val sourceFile = getInstalledApkFile(context)
            ?: return Result.failure(IllegalStateException("لم يتم العثور على حزمة التثبيت الحالية"))

        return try {
            val cacheDir = File(context.cacheDir, "apk").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(cacheDir, fileName)
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = APK_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "تطبيق إدارة المجموعات (APK أوفلاين)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة ملف التطبيق APK").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateStudentStatementText(
        studentName: String,
        phone: String,
        groupName: String,
        academicYear: String,
        totalExpected: Double,
        totalPaid: Double,
        totalRemaining: Double,
        currency: String,
        records: List<String>
    ): String {
        val sb = StringBuilder()
        sb.append("📋 كشف حساب الطالب: ").append(studentName).append("\n")
        sb.append("📞 الهاتف: ").append(phone).append("\n")
        sb.append("👥 المجموعة: ").append(groupName).append("\n")
        sb.append("📅 العام الدراسي: ").append(academicYear).append("\n")
        sb.append("───────────────────────\n")
        sb.append("💰 إجمالي الاشتراكات: ").append(totalExpected.toInt()).append(" ").append(currency).append("\n")
        sb.append("✅ إجمالي المدفوع: ").append(totalPaid.toInt()).append(" ").append(currency).append("\n")
        sb.append("⏳ المتبقي: ").append(totalRemaining.toInt()).append(" ").append(currency).append("\n")
        sb.append("───────────────────────\n")
        sb.append("تفاصيل الشهور والمدفوعات:\n")
        for (r in records) {
            sb.append("• ").append(r).append("\n")
        }
        sb.append("───────────────────────\n")
        sb.append("تم استخراج التقرير بواسطة نظام إدارة المجموعات التعليمية.\n")
        return sb.toString()
    }
}
