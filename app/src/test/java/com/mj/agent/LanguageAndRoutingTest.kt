package com.mj.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageAndRoutingTest {

    @Test
    fun detectsGujaratiScript() {
        assertTrue(detectLang("આજે શું કરવું?") == Lang.GU)
    }

    @Test
    fun detectsHindiScript() {
        assertTrue(detectLang("आज क्या करना है?") == Lang.HI)
    }

    @Test
    fun detectsRomanGujarati() {
        assertTrue(detectLang("Kem cho?") == Lang.GU)
    }

    @Test
    fun detectsRomanHindi() {
        assertTrue(detectLang("Mujhe kya karna hai?") == Lang.HI)
    }

    @Test
    fun defaultsToEnglishForUnmarkedLatinText() {
        assertTrue(detectLang("Hello, how are you?") == Lang.EN)
    }

    @Test
    fun chatPromptKeepsLanguageInstructionAndConversationRoles() {
        val prompt = buildChatPrompt(
            listOf(true to "Hello", false to "Hi there"),
            "How are you?",
            Lang.EN
        )

        assertTrue(prompt.contains("Reply ONLY in English."))
        assertTrue(prompt.contains("<start_of_turn>model\nHi there<end_of_turn>"))
        assertTrue(prompt.endsWith("<start_of_turn>model\n"))
    }

    @Test
    fun chatPromptDropsAnOrphanAssistantMessageAtHistoryStart() {
        val prompt = buildChatPrompt(
            listOf(false to "orphan reply", true to "new question"),
            "follow up",
            Lang.EN
        )

        assertFalse(prompt.contains("orphan reply"))
        assertTrue(prompt.contains("new question"))
        assertTrue(prompt.contains("follow up"))
    }

    @Test
    fun taskClassifierRecognizesCommonMultilingualCommands() {
        assertTrue(Router.looksLikeTask("YouTube kholo"))
        assertTrue(Router.looksLikeTask("અલાર્મ મૂક"))
        assertTrue(Router.looksLikeTask("अगला गाना"))
    }

    @Test
    fun taskClassifierDoesNotTreatAnOrdinaryQuestionAsADeviceCommand() {
        assertFalse(Router.looksLikeTask("What is photosynthesis?"))
    }
}
