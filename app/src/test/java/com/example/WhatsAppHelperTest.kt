package com.example

import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppHelperTest {

    @Test
    fun formatPhoneNumber_normalizesEgyptianAndInternationalNumbers() {
        assertEquals("201012345678", WhatsAppHelper.formatPhoneNumber("01012345678"))
        assertEquals("201012345678", WhatsAppHelper.formatPhoneNumber("010-1234-5678"))
        assertEquals("201012345678", WhatsAppHelper.formatPhoneNumber("00201012345678"))
        assertEquals("966501234567", WhatsAppHelper.formatPhoneNumber("+966 50 123 4567"))
        assertEquals("", WhatsAppHelper.formatPhoneNumber("   "))
    }

    @Test
    fun applyPlaceholders_substitutesAllSupportedTokens() {
        val template = "مرحباً {اسم_الطالب} في {المجموعة} - المطلوب {المبلغ} لشهر {الشهر}"
        val formatted = WhatsAppHelper.applyPlaceholders(
            template = template,
            studentName = "أحمد علي",
            groupName = "الصف الثالث",
            monthYear = "مارس",
            amount = "300 جنيه"
        )
        assertEquals("مرحباً أحمد علي في الصف الثالث - المطلوب 300 جنيه لشهر مارس", formatted)
    }

    @Test
    fun builders_formatMessagesWithStudentAndGroupDetails() {
        val feeMsg = WhatsAppHelper.buildFeeReminderMessage(
            studentName = "محمود حسن",
            groupName = "مجموعة السبت",
            monthYear = "فبراير 2025",
            remainingAmount = 250.0,
            currency = "جنيه",
            centerName = "سنتر التفوق"
        )
        assertTrue(feeMsg.contains("محمود حسن"))
        assertTrue(feeMsg.contains("مجموعة السبت"))
        assertTrue(feeMsg.contains("250 جنيه"))
        assertTrue(feeMsg.contains("سنتر التفوق"))

        val upcomingMsg = WhatsAppHelper.buildUpcomingPaymentReminderMessage(
            studentName = "سارة محمد",
            groupName = "مجموعة الأحد",
            dueDate = "2025-04-01",
            daysRemaining = 0,
            expectedAmount = 300.0,
            currency = "جنيه",
            centerName = "سنتر التفوق"
        )
        assertTrue(upcomingMsg.contains("سارة محمد"))
        assertTrue(upcomingMsg.contains("مستحق الدفع اليوم"))
        assertTrue(upcomingMsg.contains("300 جنيه"))

        val groupMsg = WhatsAppHelper.buildClassReminderMessage(
            groupName = "فيزياء 3 ثانوي",
            daysOfWeek = "السبت والثلاثاء",
            sessionTime = "04:30 م",
            location = "قاعة 1",
            centerName = "سنتر التفوق"
        )
        assertTrue(groupMsg.contains("فيزياء 3 ثانوي"))
        assertTrue(groupMsg.contains("04:30 م"))
        assertTrue(groupMsg.contains("قاعة 1"))
    }
}
