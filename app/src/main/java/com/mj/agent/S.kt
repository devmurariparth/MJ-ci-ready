package com.mj.agent

/** Badhi user-facing strings: Gujarati, Hindi, English. */
object S {
    fun tr(l: Lang, en: String, gu: String, hi: String): String = when (l) {
        Lang.EN -> en
        Lang.GU -> gu
        Lang.HI -> hi
    }

    fun ready(l: Lang) = tr(l, "Ready", "તૈયાર", "तैयार")
    fun listening(l: Lang) = tr(l, "Listening...", "સાંભળું છું...", "सुन रहा हूँ...")
    fun thinking(l: Lang) = tr(l, "Thinking...", "વિચારું છું...", "सोच रहा हूँ...")
    fun loadingModel(l: Lang) = tr(l, "Loading AI model...", "AI મોડેલ લોડ થાય છે...", "AI मॉडल लोड हो रहा है...")
    fun step(l: Lang, n: Int) = tr(l, "Working (step $n)", "કામ ચાલુ છે (પગલું $n)", "काम जारी है (चरण $n)")
    fun opened(l: Lang, app: String) = tr(l, "Opened $app", "$app ખોલ્યું", "$app खोल दिया")
    fun appNotFound(l: Lang, app: String) = tr(l, "App '$app' not found", "'$app' નામની એપ મળી નહીં", "'$app' नाम का ऐप नहीं मिला")
    fun calling(l: Lang, n: String) = tr(l, "Calling $n", "$n ને ફોન કરું છું", "$n को कॉल कर रहा हूँ")
    fun contactsPermission(l: Lang) = tr(l, "Allow Contacts permission so MJ can find this person.", "આ વ્યક્તિને શોધવા MJ ને Contacts પરવાનગી આપો.", "इस व्यक्ति को ढूँढने के लिए MJ को Contacts अनुमति दें।")
    fun dialerOpened(l: Lang, n: String, number: String) = tr(l, "Dialer opened for $n ($number). Review it and tap Call to place the call.", "$n ($number) માટે Dialer ખોલ્યું છે. નંબર ચકાસીને Call દબાવો.", "$n ($number) के लिए Dialer खुल गया है। नंबर जाँचें और कॉल करने के लिए Call दबाएँ।")
    fun musicSearchOpened(l: Lang, q: String) = tr(l, "Music search opened for \"$q\". Choose a track in the music app to start playback.", "\"$q\" માટે music search ખોલ્યું છે. ગીત પસંદ કરીને વગાડો.", "\"$q\" के लिए music search खुल गई है। गाना चुनकर चलाएँ।")
    fun musicAppUnavailable(l: Lang) = tr(l, "Could not open a music app or music search. Install Spotify or YouTube and try again.", "Music app અથવા music search ખોલી શકાઈ નહીં. Spotify અથવા YouTube install કરીને ફરી પ્રયાસ કરો.", "Music app या music search नहीं खुली। Spotify या YouTube install करके फिर कोशिश करें।")
    fun simpMusicOpened(l: Lang, q: String) = tr(
        l,
        "Opened SimpMusic for \"$q\". Choose the track there if search did not open automatically.",
        "\"$q\" માટે SimpMusic ખોલ્યું છે. Search આપમેળે ન ખુલે તો ત્યાં ગીત શોધો.",
        "\"$q\" के लिए SimpMusic खोला है। Search अपने-आप न खुले तो वहाँ गाना खोजें।"
    )
    fun volumeChanged(l: Lang, mode: String) = when (mode) {
        "down" -> tr(l, "Media volume decreased.", "મીડિયાનું વોલ્યુમ ઓછું કર્યું.", "मीडिया वॉल्यूम कम किया।")
        "max" -> tr(l, "Media volume set to maximum.", "મીડિયાનું વોલ્યુમ મહત્તમ કર્યું.", "मीडिया वॉल्यूम अधिकतम किया।")
        "mute" -> tr(l, "Media volume muted.", "મીડિયાનું વોલ્યુમ મ્યૂટ કર્યું.", "मीडिया वॉल्यूम म्यूट किया।")
        "unmute" -> tr(l, "Media volume unmuted.", "મીડિયાનું વોલ્યુમ ચાલુ કર્યું.", "मीडिया वॉल्यूम अनम्यूट किया।")
        else -> tr(l, "Media volume increased.", "મીડિયાનું વોલ્યુમ વધાર્યું.", "मीडिया वॉल्यूम बढ़ाया।")
    }
    fun volumeFailed(l: Lang) = tr(l, "Could not change media volume.", "મીડિયાનું વોલ્યુમ બદલી શકાયું નહીં.", "मीडिया वॉल्यूम नहीं बदल सका।")
    fun volumeFixed(l: Lang) = tr(l, "This device does not allow app-based volume changes.", "આ ફોનમાં એપ દ્વારા વોલ્યુમ બદલી શકાતું નથી.", "इस डिवाइस में ऐप से वॉल्यूम नहीं बदला जा सकता।")
    fun mediaControlSent(l: Lang, action: String) = when (action) {
        "next" -> tr(l, "Sent next-track command.", "આગળના ગીતનો કમાન્ડ મોકલ્યો.", "अगले गाने का कमांड भेजा।")
        "previous" -> tr(l, "Sent previous-track command.", "પહેલાના ગીતનો કમાન્ડ મોકલ્યો.", "पिछले गाने का कमांड भेजा।")
        "pause" -> tr(l, "Sent pause command.", "Pause કમાન્ડ મોકલ્યો.", "Pause कमांड भेजा।")
        else -> tr(l, "Sent play command.", "Play કમાન્ડ મોકલ્યો.", "Play कमांड भेजा।")
    }
    fun mediaControlFailed(l: Lang) = tr(l, "Could not send the media control command.", "મીડિયા કંટ્રોલ કમાન્ડ મોકલી શકાયો નહીં.", "मीडिया कंट्रोल कमांड नहीं भेज सका।")

    fun contactNotFound(l: Lang, n: String) = tr(
        l, "Contact '$n' was not found or has multiple matches (check Contacts permission and use the full name)",
        "'$n' નામનો કોન્ટેક્ટ મળ્યો નહીં અથવા એકથી વધુ મેળ મળ્યા (કોન્ટેક્ટ્સ પરવાનગી તપાસો અને પૂરું નામ બોલો)",
        "'$n' नाम का कॉन्टैक्ट नहीं मिला या एक से अधिक मिलान हैं (कॉन्टैक्ट अनुमति जाँचें और पूरा नाम बोलें)"
    )
    fun waSent(l: Lang, n: String) = tr(l, "WhatsApp message sent to $n", "$n ને WhatsApp મેસેજ મોકલ્યો", "$n को WhatsApp मैसेज भेज दिया")
    fun waOpened(l: Lang, n: String) = tr(
        l, "Chat with $n is open, but I could not press Send. Please press Send yourself.",
        "$n ની ચેટ ખોલી છે, પણ Send બટન દબાવી શક્યો નહીં. કૃપા કરી જાતે Send દબાવો.",
        "$n की चैट खोल दी है, लेकिन Send बटन नहीं दबा सका। कृपया खुद Send दबाएँ।"
    )
    fun alarmSet(l: Lang, h: Int, m: Int): String {
        val t = "%02d:%02d".format(h, m)
        return tr(l, "Alarm set for $t", "$t નો અલાર્મ સેટ કર્યો", "$t का अलार्म सेट कर दिया")
    }
    fun timerSet(l: Lang, sec: Int): String {
        val whole = sec % 60 == 0
        val n = if (whole) sec / 60 else sec
        return tr(
            l,
            if (whole) "Timer set for $n min" else "Timer set for $n sec",
            if (whole) "$n મિનિટનો ટાઈમર ચાલુ કર્યો" else "$n સેકન્ડનો ટાઈમર ચાલુ કર્યો",
            if (whole) "$n मिनट का टाइमर चालू किया" else "$n सेकंड का टाइमर चालू किया"
        )
    }
    fun torch(l: Lang, on: Boolean) =
        if (on) tr(l, "Flashlight on", "ટોર્ચ ચાલુ કરી", "टॉर्च चालू की")
        else tr(l, "Flashlight off", "ટોર્ચ બંધ કરી", "टॉर्च बंद की")
    fun web(l: Lang) = tr(l, "Searching in the browser", "બ્રાઉઝરમાં શોધું છું", "ब्राउज़र में खोज रहा हूँ")
    fun settingsOpened(l: Lang) = tr(l, "Settings opened", "સેટિંગ્સ ખોલી", "सेटिंग्स खोल दी")
    fun needAccessibility(l: Lang) = tr(
        l, "Please enable MJ's Accessibility service for this task",
        "આ કામ માટે MJ ની Accessibility સેવા ચાલુ કરો",
        "इस काम के लिए MJ की Accessibility सेवा चालू करें"
    )
    fun failed(l: Lang) = tr(l, "Sorry, I could not finish this task", "માફ કરજો, આ કામ પૂરું થઈ શક્યું નહીં", "माफ़ कीजिए, यह काम पूरा नहीं हो सका")
    fun stopped(l: Lang) = tr(l, "Stopped", "રોકી દીધું", "रोक दिया")
    fun done(l: Lang) = tr(l, "Done", "કામ પૂરું થયું", "काम पूरा हुआ")
    fun modelImporting(l: Lang) = tr(
        l, "Please wait until the model import finishes before sending a message.",
        "મોડેલ import પૂરું થાય પછી મેસેજ મોકલો.", "मॉडल import पूरा होने के बाद संदेश भेजें।"
    )
    fun noModel(l: Lang) = tr(
        l, "AI model is not added yet. Import the model in Setup above.",
        "AI મોડેલ ઉમેરેલું નથી. ઉપર Setup માં મોડેલ import કરો.",
        "AI मॉडल जोड़ा नहीं गया है। ऊपर Setup में मॉडल import करें।"
    )
    fun micDenied(l: Lang) = tr(l, "Please allow the microphone permission", "માઇક્રોફોનની પરવાનગી આપો", "माइक्रोफ़ोन की अनुमति दें")
    fun cameraPermission(l: Lang) = tr(
        l, "Please allow Camera permission to control the flashlight",
        "ટોર્ચ ચલાવવા Camera પરવાનગી આપો", "टॉर्च चलाने के लिए Camera अनुमति दें"
    )
    fun noSpeech(l: Lang) = tr(l, "Speech recognition is not available", "સ્પીચ રેકગ્નિશન ઉપલબ્ધ નથી", "स्पीच रेकग्निशन उपलब्ध नहीं है")
    fun notUnderstood(l: Lang) = tr(l, "Didn't catch that, please try again", "સમજાયું નહીં, ફરી બોલો", "समझ नहीं आया, फिर से बोलें")
    fun greeting(l: Lang) = tr(
        l, "Hi! I'm MJ. Ask me anything, or give me a task like \"Open YouTube\" or \"Call Rahul\".",
        "નમસ્તે! હું MJ છું. મને કંઈ પણ પૂછો, અથવા કામ સોંપો, જેમ કે \"YouTube ખોલ\" કે \"રાહુલ ને ફોન કર\".",
        "नमस्ते! मैं MJ हूँ। मुझसे कुछ भी पूछें, या काम दें, जैसे \"YouTube खोलो\" या \"राहुल को कॉल करो\"।"
    )

    // UI labels
    fun setupTitle(l: Lang) = tr(l, "Setup", "સેટઅપ", "सेटअप")
    fun uModel(l: Lang) = tr(l, "AI model", "AI મોડેલ", "AI मॉडल")
    fun uPerms(l: Lang) = tr(l, "Microphone, Contacts and Camera permissions", "માઇક્રોફોન, કોન્ટેક્ટ્સ અને કેમેરાની પરવાનગી", "माइक्रोफ़ोन, कॉन्टैक्ट्स और कैमरा अनुमति")
    fun uA11y(l: Lang) = tr(l, "Accessibility (phone control)", "Accessibility (ફોન કંટ્રોલ)", "Accessibility (फ़ोन कंट्रोल)")
    fun aImport(l: Lang) = tr(l, "Import", "Import કરો", "Import करें")
    fun aAllow(l: Lang) = tr(l, "Allow", "આપો", "दें")
    fun aEnable(l: Lang) = tr(l, "Enable", "ચાલુ કરો", "चालू करें")
    fun hintRestricted(l: Lang) = tr(
        l, "Switch greyed out? Open App info, tap the 3 dots, then Allow restricted settings.",
        "સ્વિચ ગ્રે હોય તો: App info ખોલો, 3 ટપકાં દબાવો, પછી Allow restricted settings.",
        "स्विच ग्रे हो तो: App info खोलें, 3 बिंदु दबाएँ, फिर Allow restricted settings।"
    )
    fun aAppInfo(l: Lang) = tr(l, "App info", "App info", "App info")
    fun inputHint(l: Lang) = tr(l, "Ask or command MJ...", "MJ ને પૂછો કે કામ સોંપો...", "MJ से पूछें या काम दें...")
    fun stop(l: Lang) = tr(l, "Stop", "રોકો", "रोकें")
    fun speakLabel(l: Lang) = tr(l, "Speak replies", "જવાબ બોલો", "जवाब बोलें")
}
