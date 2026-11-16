package com.example

import com.example.data.model.StudentEntity
import com.example.ui.screens.filterStudentsByQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class StudentSearchFilterTest {

    private val sampleStudents = listOf(
        StudentEntity(
            id = "1",
            fullName = "محمد أحمد علي",
            phone = "01012345678",
            parentPhone = "01122334455",
            educationalStage = "المرحلة الثانوية",
            grade = "الصف الثالث الثانوي",
            subject = "فيزياء",
            registrationDate = "2025-01-01"
        ),
        StudentEntity(
            id = "2",
            fullName = "سارة محمود حسن",
            phone = "01298765432",
            parentPhone = "01555667788",
            educationalStage = "المرحلة الإعدادية",
            grade = "الصف الثالث الإعدادي",
            subject = "رياضيات",
            registrationDate = "2025-01-02"
        )
    )

    @Test
    fun filterStudentsByName_matchesCaseInsensitiveAndTrimmed() {
        val result = filterStudentsByQuery(sampleStudents, "  سارة  ")
        assertEquals(1, result.size)
        assertEquals("2", result.first().id)
    }

    @Test
    fun filterStudentsByPhone_supportsFormattedAndArabicDigits() {
        val bySpacedPhone = filterStudentsByQuery(sampleStudents, "010 1234")
        assertEquals(1, bySpacedPhone.size)
        assertEquals("1", bySpacedPhone.first().id)

        val byArabicDigits = filterStudentsByQuery(sampleStudents, "٠١٢٩٨")
        assertEquals(1, byArabicDigits.size)
        assertEquals("2", byArabicDigits.first().id)

        val byParentPhone = filterStudentsByQuery(sampleStudents, "0155566")
        assertEquals(1, byParentPhone.size)
        assertEquals("2", byParentPhone.first().id)
    }

    @Test
    fun blankQuery_returnsAllStudentsSortedByName() {
        val result = filterStudentsByQuery(sampleStudents, "   ")
        assertEquals(2, result.size)
        assertEquals("سارة محمود حسن", result[0].fullName)
        assertEquals("محمد أحمد علي", result[1].fullName)
    }
}
