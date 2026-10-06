package com.example.unit

import com.example.data.model.UtilityType
import com.example.telecom.IvrSequenceBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IvrSequenceBuilderTest {

    @Test
    fun buildDialString_replacesPlaceholderWithIndex() {
        val result = IvrSequenceBuilder.buildDialString("0800800200,12345#,,,XXXX#,,1", "2899")
        assertEquals("0800800200,12345#,,,2899#,,1", result)
    }

    @Test
    fun buildDialString_stripsSpacesFromIndex() {
        val result = IvrSequenceBuilder.buildDialString("0800,XXXX#", " 5 397 ")
        assertEquals("0800,5397#", result)
    }

    @Test
    fun buildDialString_withoutPlaceholderAppendsIndex() {
        val result = IvrSequenceBuilder.buildDialString("0800", "123")
        assertEquals("0800,123#,,1", result)
    }

    @Test
    fun testSequence_stopsRightBeforeTheIndex() {
        val result = IvrSequenceBuilder.buildTestSequenceUntilIndex("0800800200,12345#,,,XXXX#,,1")
        assertEquals("0800800200,12345#,,,", result)
    }

    @Test
    fun templateValidation_acceptsExactlyOnePlaceholder() {
        assertTrue(IvrSequenceBuilder.isValidTemplate("0800800200,12345#,,,XXXX#,,1"))
        assertFalse(IvrSequenceBuilder.isValidTemplate("0800800200,12345#,,,#,,1"))
        assertFalse(IvrSequenceBuilder.isValidTemplate("0800,XXXX#,XXXX#"))
        assertFalse(IvrSequenceBuilder.isValidTemplate("0800 abc XXXX"))
        assertFalse(IvrSequenceBuilder.isValidTemplate(""))
    }

    @Test
    fun defaultTemplates_haveTheExpectedShape() {
        assertEquals(
            "0800800200,12345#,,,XXXX#,,1",
            UtilityType.GAS.ivrTemplateFor("0800800200", "12345")
        )
        assertEquals(
            "0800070701,1,,12345#,,,1,,,,,XXXX#,,1",
            UtilityType.ELECTRICITY.ivrTemplateFor("0800070701", "12345")
        )
        assertTrue(IvrSequenceBuilder.isValidTemplate(UtilityType.GAS.defaultIvrTemplate))
        assertTrue(IvrSequenceBuilder.isValidTemplate(UtilityType.ELECTRICITY.defaultIvrTemplate))
    }

    @Test
    fun estimatedDuration_growsWithPauses() {
        assertEquals(10, IvrSequenceBuilder.estimateDurationSeconds("0800"))
        assertEquals(20, IvrSequenceBuilder.estimateDurationSeconds("0800,,,,"))
    }
}
