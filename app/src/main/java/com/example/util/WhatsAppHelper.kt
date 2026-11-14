package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

enum class WhatsAppMessageTemplateType(val title: String, val isGroupRelevant: Boolean = false) {
    WELCOME("رسالة ترحيب بالانضمام", isGroupRelevant = false),
    CLASS_REMINDER("تذكير بموعد الحصة القادمة", isGroupRelevant = true),
    FEE_REMINDER("تذكير بسداد الاشتراك", isGroupRelevant = false),
    UPCOMING_DUE("تذكير بموعد الدفع القادم", isGroupRelevant = false),
    PAYMENT_CONFIRMATION("إشعار تأكيد استلام دفعة", isGroupRelevant = false),
    ABSENCE_NOTICE("إشعار غياب عن الحصة", isGroupRelevant = false),
    STUDENT_REPORT("ملخص مستوى الحضور والسداد", isGroupRelevant = false),
    GROUP_ANNOUNCEMENT("إعلان / تنبيه هام للمجموعة", isGroupRelevant = true),
    CUSTOM("رسالة مخصصة", isGroupRelevant = true)
}

object WhatsAppHelper {

    fun formatPhoneNumber(raw: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        return when {
            hasPlus -> digits
            digits.startsWith("00") && digits.length > 4 -> digits.removePrefix("00")
            digits.startsWith("20") && digits.length >= 12 -> digits
            digits.startsWith("01") && digits.length == 11 -> "20" + digits.substring(1)
            digits.startsWith("1") && digits.length == 10 -> "20$digits"
            else -> digits
        }
    }

    fun openWhatsAppChat(context: Context, phone: String, message: String = "") {
        val cleanPhone = formatPhoneNumber(phone)
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "رقم الهاتف غير مسجل أو غير صالح", Toast.LENGTH_SHORT).show()
            return
        }
        val uriBuilder = StringBuilder("https://wa.me/").append(cleanPhone)
        if (message.isNotBlank()) {
            uriBuilder.append("?text=").append(Uri.encode(message))
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح محادثة واتساب: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openWhatsAppGroup(context: Context, groupUrl: String, prefilledMessage: String = "") {
        val cleanUrl = groupUrl.trim()
        if (cleanUrl.isBlank()) {
            if (prefilledMessage.isNotBlank()) {
                shareTextToWhatsApp(context, prefilledMessage)
            } else {
                Toast.makeText(context, "لم يتم تسجيل رابط لمجموعة واتساب بعد", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (prefilledMessage.isNotBlank()) {
            copyToClipboardInternal(context, "رسالة مجموعة واتساب", prefilledMessage)
            Toast.makeText(
                context,
                "تم نسخ الرسالة الجاهزة للحافظة؛ الصقها داخل جروب واتساب فور فتحه",
                Toast.LENGTH_LONG
            ).show()
        }
        val finalUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "https://$cleanUrl"
        } else {
            cleanUrl
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح رابط المجموعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareTextToWhatsApp(context: Context, message: String) {
        if (message.isBlank()) {
            Toast.makeText(context, "لا يوجد نص لإرساله", Toast.LENGTH_SHORT).show()
            return
        }
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in whatsappPackages) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(pkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(sendIntent)
                return
            } catch (_: Exception) {
                // Try next package or fallback chooser
            }
        }
        val fallbackIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "إرسال الرسالة عبر واتساب"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            copyToClipboard(context, "رسالة واتساب", message)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        copyToClipboardInternal(context, label, text)
        Toast.makeText(context, "تم النسخ للحافظة بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboardInternal(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun applyPlaceholders(
        template: String,
        studentName: String = "",
        groupName: String = "",
        monthYear: String = "",
        amount: String = "",
        date: String = "",
        centerName: String = "",
        sessionTime: String = "",
        location: String = ""
    ): String {
        return template
            .replace("{اسم_الطالب}", studentName)
            .replace("{الطالب}", studentName)
            .replace("{المجموعة}", groupName)
            .replace("{الشهر}", monthYear)
            .replace("{المبلغ}", amount)
            .replace("{التاريخ}", date)
            .replace("{السنتر}", centerName)
            .replace("{الموعد}", sessionTime)
            .replace("{المكان}", location)
    }

    fun buildWelcomeMessage(
        studentName: String,
        groupName: String = "",
        grade: String = "",
        centerName: String = "",
        baseTemplate: String = ""
    ): String {
        val resolvedBase = if (baseTemplate.isNotBlank()) {
            applyPlaceholders(
                template = baseTemplate,
                studentName = studentName,
                groupName = groupName,
                centerName = centerName
            )
        } else {
            "أهلاً وسهلاً بك معنا في مجموعتنا التعليمية، نتمنى لك عاماً دراسياً مليئاً بالتفوق والنجاح."
        }

        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("من إدارة ").append(centerName).append(":\n\n")
        }
        sb.append(resolvedBase).append("\n\n")
        sb.append("📌 اسم الطالب: ").append(studentName).append("\n")
        if (grade.isNotBlank()) {
            sb.append("🎓 الصف الدراسي: ").append(grade).append("\n")
        }
        if (groupName.isNotBlank()) {
            sb.append("📚 المجموعة: ").append(groupName).append("\n")
        }
        sb.append("\nنرحب بتواصلكم الدائم معنا.")
        return sb.toString()
    }

    fun buildPaymentConfirmationMessage(
        studentName: String,
        groupName: String,
        monthYear: String,
        amountPaid: Double,
        paymentDate: String,
        nextDueDate: String = "",
        currency: String = "جنيه",
        centerName: String = "",
        baseTemplate: String = ""
    ): String {
        val resolvedBase = if (baseTemplate.isNotBlank()) {
            applyPlaceholders(
                template = baseTemplate,
                studentName = studentName,
                groupName = groupName,
                monthYear = monthYear,
                amount = "${amountPaid.toInt()} $currency",
                date = paymentDate,
                centerName = centerName
            )
        } else {
            "تم استلام سداد اشتراك الطالب بنجاح، شكراً لتعاونكم وثقتكم بنا."
        }

        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("من إدارة ").append(centerName).append(":\n\n")
        }
        sb.append("✅ ").append(resolvedBase).append("\n\n")
        sb.append("📌 اسم الطالب: ").append(studentName).append("\n")
        if (groupName.isNotBlank()) {
            sb.append("📚 المجموعة: ").append(groupName).append("\n")
        }
        if (monthYear.isNotBlank()) {
            sb.append("🗓️ شهر الاشتراك: ").append(monthYear).append("\n")
        }
        sb.append("💰 المبلغ المدفوع: ").append(amountPaid.toInt()).append(" ").append(currency).append("\n")
        if (paymentDate.isNotBlank()) {
            sb.append("📅 تاريخ السداد: ").append(paymentDate).append("\n")
        }
        if (nextDueDate.isNotBlank()) {
            sb.append("🔔 موعد الاستحقاق القادم: ").append(nextDueDate).append("\n")
        }
        sb.append("\nمع تمنياتنا بالتوفيق والتفوق الدائم.")
        return sb.toString()
    }

    fun buildAbsenceMessage(
        studentName: String,
        groupName: String,
        sessionDate: String,
        lessonTopic: String = "",
        centerName: String = "",
        baseTemplate: String = ""
    ): String {
        val template = if (baseTemplate.isNotBlank()) {
            applyPlaceholders(
                template = baseTemplate,
                studentName = studentName,
                groupName = groupName,
                date = sessionDate,
                centerName = centerName
            )
        } else {
            "نحيطكم علمًا بغياب الطالب عن الحصة، ونرجو متابعة انتظامه في الحضور والتواصل معنا في حال وجود عذر."
        }

        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("من إدارة ").append(centerName).append(":\n\n")
        }
        sb.append(template).append("\n\n")
        sb.append("📌 اسم الطالب: ").append(studentName).append("\n")
        if (groupName.isNotBlank()) {
            sb.append("📚 المجموعة الدراسية: ").append(groupName).append("\n")
        }
        sb.append("📅 تاريخ الحصة: ").append(sessionDate).append("\n")
        if (lessonTopic.isNotBlank()) {
            sb.append("📖 موضوع الدرس: ").append(lessonTopic).append("\n")
        }
        sb.append("\nشاكرين حرصكم واهتمامكم بمستقبل أبنائكم.")
        return sb.toString()
    }

    fun buildFeeReminderMessage(
        studentName: String,
        groupName: String,
        monthYear: String,
        remainingAmount: Double,
        currency: String = "جنيه",
        centerName: String = "",
        baseTemplate: String = ""
    ): String {
        val template = if (baseTemplate.isNotBlank()) {
            applyPlaceholders(
                template = baseTemplate,
                studentName = studentName,
                groupName = groupName,
                monthYear = monthYear,
                amount = "${remainingAmount.toInt()} $currency",
                centerName = centerName
            )
        } else {
            "نذكركم بسداد اشتراك الشهر الحالي، شكرًا لتعاونكم مع إدارة السنتر."
        }

        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("من إدارة ").append(centerName).append(":\n\n")
        }
        sb.append(template).append("\n\n")
        sb.append("📌 اسم الطالب: ").append(studentName).append("\n")
        if (groupName.isNotBlank()) {
            sb.append("📚 المجموعة: ").append(groupName).append("\n")
        }
        sb.append("🗓️ اشتراك شهر: ").append(monthYear).append("\n")
        sb.append("💰 المبلغ المستحق: ").append(remainingAmount.toInt()).append(" ").append(currency).append("\n\n")
        sb.append("شاكرين ومقدرين حسن تعاونكم الدائم معنا.")
        return sb.toString()
    }

    fun buildClassReminderMessage(
        groupName: String,
        daysOfWeek: String,
        sessionTime: String,
        location: String,
        centerName: String = ""
    ): String {
        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("من إدارة ").append(centerName).append(":\n\n")
        }
        sb.append("تذكير بموعد الحصة القادمة:\n\n")
        sb.append("📚 المجموعة: ").append(groupName).append("\n")
        if (daysOfWeek.isNotBlank()) {
            sb.append("🗓️ أيام الحصص: ").append(daysOfWeek).append("\n")
        }
        if (sessionTime.isNotBlank()) {
            sb.append("⏰ موعد الحصة: ").append(sessionTime).append("\n")
        }
        if (location.isNotBlank()) {
            sb.append("📍 القاعة / المكان: ").append(location).append("\n")
        }
        sb.append("\nنرجو الالتزام بالحضور في الموعد المحدد مع إحضار كشكول الحصة والواجب.")
        return sb.toString()
    }

    fun buildGroupAnnouncementMessage(
        groupName: String,
        daysOfWeek: String = "",
        sessionTime: String = "",
        location: String = "",
        announcementBody: String = "",
        centerName: String = ""
    ): String {
        val body = announcementBody.ifBlank {
            "نود تنبيه جميع طلاب المجموعة بضرورة مراجعة الدرس السابق والاستعداد للاختبار القادم."
        }
        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("إعلان هام من إدارة ").append(centerName).append(":\n\n")
        } else {
            sb.append("📢 إعلان هام للمجموعة:\n\n")
        }
        sb.append("📚 المجموعة: ").append(groupName).append("\n")
        if (daysOfWeek.isNotBlank() && sessionTime.isNotBlank()) {
            sb.append("⏰ الموعد: ").append(daysOfWeek).append(" - ").append(sessionTime).append("\n")
        }
        if (location.isNotBlank()) {
            sb.append("📍 المكان: ").append(location).append("\n")
        }
        sb.append("\n").append(body).append("\n\n")
        sb.append("مع أطيب الأمنيات بالتفوق للجميع.")
        return sb.toString()
    }

    fun buildStudentReportSummaryMessage(
        studentName: String,
        groupName: String,
        attendancePercent: Int,
        presentCount: Int,
        absentCount: Int,
        totalPaid: Double,
        totalRemaining: Double,
        currency: String = "جنيه",
        centerName: String = ""
    ): String {
        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("تقرير متابعة دوري من ").append(centerName).append(":\n\n")
        } else {
            sb.append("📋 تقرير متابعة الطالب:\n\n")
        }
        sb.append("📌 اسم الطالب: ").append(studentName).append("\n")
        if (groupName.isNotBlank()) {
            sb.append("📚 المجموعة: ").append(groupName).append("\n")
        }
        sb.append("📊 نسبة الحضور: ").append(attendancePercent).append("% (حاضر: ").append(presentCount).append(" | غياب: ").append(absentCount).append(")\n")
        sb.append("💵 إجمالي المدفوع: ").append(totalPaid.toInt()).append(" ").append(currency).append("\n")
        if (totalRemaining > 0) {
            sb.append("⚠️ المتبقي من الاشتراكات: ").append(totalRemaining.toInt()).append(" ").append(currency).append("\n")
        } else {
            sb.append("✅ الحساب المالي: مسدد بالكامل\n")
        }
        sb.append("\nنشكر لكم حرصكم وتعاونكم المستمر معنا.")
        return sb.toString()
    }

    fun buildUpcomingPaymentReminderMessage(
        studentName: String,
        groupName: String,
        dueDate: String,
        daysRemaining: Int,
        expectedAmount: Double,
        currency: String = "جنيه",
        centerName: String = ""
    ): String {
        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته،\n")
        if (centerName.isNotBlank()) {
            sb.append("من إدارة ").append(centerName).append(":\n\n")
        }
        sb.append("نود تذكيركم بموعد استحقاق الدفعة القادمة لاشتراك الطالب:\n\n")
        sb.append("📌 اسم الطالب: ").append(studentName).append("\n")
        if (groupName.isNotBlank()) {
            sb.append("📚 المجموعة: ").append(groupName).append("\n")
        }
        sb.append("🗓️ موعد الاستحقاق القادم: ").append(dueDate).append("\n")
        if (daysRemaining > 0) {
            sb.append("⏳ متبقي على موعد السداد: ").append(daysRemaining).append(" يوم\n")
        } else if (daysRemaining == 0) {
            sb.append("⏰ الموعد: مستحق الدفع اليوم\n")
        } else {
            sb.append("⚠️ متأخر عن موعد الاستحقاق منذ: ").append(-daysRemaining).append(" يوم\n")
        }
        sb.append("💰 قيمة الاشتراك المقررة: ").append(expectedAmount.toInt()).append(" ").append(currency).append("\n\n")
        sb.append("شاكرين ومقدرين حسن تعاونكم وحرصكم الدائم معنا.")
        return sb.toString()
    }
}
