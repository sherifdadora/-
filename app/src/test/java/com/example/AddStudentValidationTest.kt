package com.example

import com.example.ui.screens.StudentInputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AddStudentValidationTest {

    @Test
    fun validEgyptianMobileNumbers_returnNullError() {
        assertNull(StudentInputValidator.validateStudentPhone("01012345678"))
        assertNull(StudentInputValidator.validateStudentPhone("011 2345 6789"))
        assertNull(StudentInputValidator.validateStudentPhone("012-3456-7890"))
        assertNull(StudentInputValidator.validateStudentPhone("01598765432"))
        assertNull(StudentInputValidator.validateStudentPhone("+201012345678"))
    }

    @Test
    fun arabicDigitsPhoneNumber_isNormalizedAndAccepted() {
        val arabicPhone = "٠١٠١٢٣٤٥٦٧٨"
        assertEquals("01012345678", StudentInputValidator.normalizePhoneInput(arabicPhone))
        assertNull(StudentInputValidator.validateStudentPhone(arabicPhone))
    }

    @Test
    fun invalidPhoneNumbers_returnValidationErrorMessage() {
        assertNotNull(StudentInputValidator.validateStudentPhone(""))
        assertNotNull(StudentInputValidator.validateStudentPhone("0101234"))
        assertNotNull(StudentInputValidator.validateStudentPhone("01912345678"))
        assertNotNull(StudentInputValidator.validateStudentPhone("0101234567899"))
        assertNotNull(StudentInputValidator.validateStudentPhone("01012abc678"))
        assertNotNull(StudentInputValidator.validateStudentPhone("00000000000"))
    }
}
