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
    fun detectsRomanGujaratiDeviceCommands() {
        assertTrue(detectLang("Rahul ne call kar") == Lang.GU)
        assertTrue(detectLang("YouTube kholo") == Lang.GU)
    }

    @Test
    fun detectsRomanHindiDeviceCommands() {
        assertTrue(detectLang("Rahul ko call karo") == Lang.HI)
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

    @Test
    fun quickRouterDoesNotExecuteQuestionShapedRequests() {
        assertTrue(Router.quick("What is torch mode?") == null)
        assertTrue(Router.quick("શું ટોર્ચ છે?") == null)
        assertTrue(Router.quick("How do I set volume to 50?") == null)
    }

    @Test
    fun quickRouterRequiresExplicitTorchState() {
        assertTrue(Router.quick("torch") == null)
        assertTrue(Router.quick("ટોર્ચ") == null)
        assertTrue(Router.quick("turn torch on")?.name == "flashlight")
        assertTrue(Router.quick("ટોર્ચ બંધ કર")?.args?.optBoolean("on") == false)
    }

    @Test
    fun agentParserRejectsIncompleteAndOutOfRangeActions() {
        assertTrue(AgentPrompt.parse("""{"a":"call"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"timer","seconds":0}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"alarm","hour":25,"minute":0}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"whatsapp","to":"Rahul"}""") == null)
    }

    @Test
    fun agentParserAcceptsValidSafeDraftActions() {
        val call = AgentPrompt.parse("""{"a":"call","to":"Rahul"}""")
        assertTrue(call?.name == "call")
        val message = AgentPrompt.parse("""{"a":"whatsapp","to":"Rahul","text":"I am coming"}""")
        assertTrue(message?.name == "whatsapp")
    }
    @Test
    fun agentParserRejectsUnknownSettingsPages() {
        assertTrue(AgentPrompt.parse("""{"a":"settings","page":"secret"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"settings","page":"wifi"}""")?.name == "settings")
    }

    @Test
    fun agentParserHandlesBracesInsideJsonStrings() {
        val parsed = AgentPrompt.parse("""prefix {"a":"done","msg":"literal { brace } and \\n quote"} suffix""")
        assertTrue(parsed?.name == "done")
    }

    @Test
    fun agentParserRejectsUnsupportedActionsAndMalformedJson() {
        assertTrue(AgentPrompt.parse("""{"a":"launch_nuke"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"mystery_action"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"timer","seconds":"five"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"alarm","hour":7}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"media_control","action":"delete"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"scroll","dir":"sideways"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"brightness","mode":"set:101"}""") == null)
        assertTrue(AgentPrompt.parse("""{"a":"volume","mode":"set:-1"}""") == null)
    }

}
