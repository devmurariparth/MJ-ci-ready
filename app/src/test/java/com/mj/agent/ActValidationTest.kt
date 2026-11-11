package com.mj.agent

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActValidationTest {
    @Test
    fun integerArgumentsAcceptWholeNumbersOnly() {
        assertEquals(7, Act("timer", JSONObject().put("seconds", 7)).intOrNull("seconds"))
        assertEquals(7, Act("timer", JSONObject().put("seconds", "7")).intOrNull("seconds"))
        assertEquals(7, Act("timer", JSONObject().put("seconds", 7.0)).intOrNull("seconds"))
        assertNull(Act("timer", JSONObject().put("seconds", 7.5)).intOrNull("seconds"))
        assertNull(Act("timer", JSONObject().put("seconds", "7.5")).intOrNull("seconds"))
        assertNull(Act("timer", JSONObject().put("seconds", true)).intOrNull("seconds"))
        assertNull(Act("timer", JSONObject().put("seconds", JSONObject.NULL)).intOrNull("seconds"))
    }

    @Test
    fun integerArgumentsRejectOverflowAndNonFiniteValues() {
        assertNull(Act("timer", JSONObject().put("seconds", Long.MAX_VALUE)).intOrNull("seconds"))
        assertNull(Act("timer", JSONObject().put("seconds", Double.NaN)).intOrNull("seconds"))
        assertNull(Act("timer", JSONObject().put("seconds", Double.POSITIVE_INFINITY)).intOrNull("seconds"))
    }
}
