package com.antimobile.callhs

import com.antimobile.callhs.data.messaging.SmsRecipientParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRecipientParserTest {
    @Test
    fun parsesSingleRecipientAndRemovesQuery() {
        assertEquals(listOf("0901234567"), SmsRecipientParser.parse("0901234567?body=Xin%20chao"))
    }

    @Test
    fun detectsCommaAndSemicolonSeparatedRecipients() {
        assertEquals(
            listOf("0901", "0902", "0903"),
            SmsRecipientParser.parse("0901, 0902;0903"),
        )
    }

    @Test
    fun ignoresBlankRecipientTokens() {
        assertEquals(listOf("0901"), SmsRecipientParser.parse(" ; 0901, "))
    }

    @Test
    fun validatesFormattedNumbersWithoutAcceptingNamesContainingDigits() {
        assertTrue(SmsRecipientParser.isValidAddress("+84 (90) 123-4567"))
        assertTrue(SmsRecipientParser.isValidAddress("1234"))
        assertFalse(SmsRecipientParser.isValidAddress("John 1"))
        assertFalse(SmsRecipientParser.isValidAddress("   "))
    }
}
