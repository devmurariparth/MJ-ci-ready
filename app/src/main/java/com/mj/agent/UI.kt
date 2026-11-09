package com.mj.agent

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgTop = Color(0xFF0B0F1A)
private val BgBottom = Color(0xFF111832)
private val Card = Color(0xFF151B2B)
private val Accent1 = Color(0xFF7C5CFF)
private val Accent2 = Color(0xFF00D4FF)
private val Muted = Color(0xFF9AA4BF)
private val Danger = Color(0xFFFF5C7A)
private val Good = Color(0xFF3DDC97)
private val Warn = Color(0xFFFFB86B)

@Composable
fun MJTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent1, background = BgTop, surface = Card), content = content)
}

@Composable
fun MainScreen(onPickModel: () -> Unit, onPerms: () -> Unit, onA11y: () -> Unit, onAppInfo: () -> Unit) {
    val lang = MJBrain.voiceLang
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastText = MJBrain.messages.lastOrNull()?.text
    LaunchedEffect(MJBrain.messages.size, lastText) {
        if (MJBrain.messages.isNotEmpty()) listState.scrollToItem(MJBrain.messages.size - 1)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Header(lang)
        Controls(lang)
        SetupCard(lang, onPickModel, onPerms, onA11y, onAppInfo)
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (MJBrain.messages.isEmpty()) {
                item {
                    Text(S.greeting(lang), color = Muted, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
                }
            }
            items(MJBrain.messages) { Bubble(it) }
        }
        InputBar(lang, input, { input = it }, {
            MJBrain.submit(input)
            input = ""
        })
    }
}

@Composable
private fun Header(lang: Lang) {
    val active = MJBrain.busy || MJBrain.listening
    val t = rememberInfiniteTransition(label = "orb")
    val s by t.animateFloat(
        1f, if (active) 1.3f else 1.08f,
        infiniteRepeatable(tween(if (active) 600 else 1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(46.dp).scale(s).alpha(0.35f)
                    .background(Brush.radialGradient(listOf(Accent2, Color.Transparent)), CircleShape)
            )
            Box(Modifier.size(30.dp).background(Brush.linearGradient(listOf(Accent1, Accent2)), CircleShape))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("MJ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                MJBrain.status.ifBlank { S.ready(lang) },
                color = Muted, fontSize = 12.sp, maxLines = 1
            )
        }
        if (active) {
            Text(
                "■ " + S.stop(lang), color = Color.White, fontSize = 13.sp,
                modifier = Modifier
                    .background(Danger, RoundedCornerShape(16.dp))
                    .clickable { MJBrain.stop() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Brush.linearGradient(listOf(Accent1, Color(0xFF5B8CFF)))
    else Brush.linearGradient(listOf(Card, Card))
    Text(
        text, color = if (selected) Color.White else Muted, fontSize = 13.sp,
        modifier = Modifier
            .background(bg, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

@Composable
private fun Controls(lang: Lang) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Lang.values().forEach { l -> Chip(l.label, lang == l) { MJBrain.updateVoiceLang(l) } }
        Spacer(Modifier.width(4.dp))
        Mode.values().forEach { m ->
            Chip(m.name.lowercase().replaceFirstChar { it.uppercase() }, MJBrain.mode == m) { MJBrain.updateMode(m) }
        }
        Spacer(Modifier.width(4.dp))
        Text(S.speakLabel(lang), color = Muted, fontSize = 12.sp)
        Switch(checked = MJBrain.speakReplies, onCheckedChange = { MJBrain.updateSpeak(it) })
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SetupRow(ok: Boolean, title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✔" else "●", color = if (ok) Good else Warn, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (!ok) {
            Text(
                action, color = Color.White, fontSize = 13.sp,
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Accent1, Accent2)), RoundedCornerShape(14.dp))
                    .clickable { onClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SetupCard(lang: Lang, onPickModel: () -> Unit, onPerms: () -> Unit, onA11y: () -> Unit, onAppInfo: () -> Unit) {
    val modelOk = MJBrain.modelPresent && MJBrain.modelError.isEmpty()
    if (modelOk && MJBrain.permsOk && MJBrain.a11yOn && MJBrain.importProgress < 0) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Card, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Text(S.setupTitle(lang), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        SetupRow(modelOk, S.uModel(lang), S.aImport(lang), onPickModel)
        if (MJBrain.importProgress >= 0) {
            LinearProgressIndicator(
                progress = { MJBrain.importProgress / 100f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            Text("${MJBrain.importProgress}%", color = Muted, fontSize = 12.sp)
        }
        if (MJBrain.modelError.isNotEmpty()) {
            Text(MJBrain.modelError.take(200), color = Danger, fontSize = 12.sp)
        }
        if (MJBrain.modelLoading) Text(S.loadingModel(lang), color = Muted, fontSize = 12.sp)
        SetupRow(MJBrain.permsOk, S.uPerms(lang), S.aAllow(lang), onPerms)
        SetupRow(MJBrain.a11yOn, S.uA11y(lang), S.aEnable(lang), onA11y)
        if (!MJBrain.a11yOn) {
            Text(
                S.hintRestricted(lang) + "  [" + S.aAppInfo(lang) + "]",
                color = Warn, fontSize = 12.sp,
                modifier = Modifier.clickable { onAppInfo() }.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun Bubble(m: Msg) {
    if (m.kind == 1) {
        Text(m.text, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp))
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
        val shape = RoundedCornerShape(
            18.dp, 18.dp,
            if (m.fromUser) 4.dp else 18.dp,
            if (m.fromUser) 18.dp else 4.dp
        )
        val mod = if (m.fromUser) Modifier.background(Brush.linearGradient(listOf(Accent1, Color(0xFF5B8CFF))), shape)
        else Modifier.background(Card, shape)
        Text(
            if (m.text.isEmpty()) "..." else m.text, color = Color.White, fontSize = 15.sp,
            modifier = mod.widthIn(max = 300.dp).padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun InputBar(lang: Lang, input: String, onChange: (String) -> Unit, onSend: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input, onValueChange = onChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(S.inputHint(lang), color = Color(0xFF6B7390)) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedContainerColor = Card, unfocusedContainerColor = Card,
                focusedBorderColor = Accent1, unfocusedBorderColor = Color.Transparent
            ),
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        if (input.isNotBlank()) {
            Box(
                Modifier.size(52.dp).background(Brush.linearGradient(listOf(Accent1, Accent2)), CircleShape)
                    .clickable { onSend() },
                contentAlignment = Alignment.Center
            ) { Text("➤", color = Color.White, fontSize = 20.sp) }
            Spacer(Modifier.width(8.dp))
        }
        val micBrush = if (MJBrain.listening) Brush.linearGradient(listOf(Danger, Color(0xFFFF8A5C)))
        else Brush.linearGradient(listOf(Accent2, Accent1))
        Box(
            Modifier.size(56.dp).background(micBrush, CircleShape).clickable { MJBrain.toggleListening() },
            contentAlignment = Alignment.Center
        ) { Text("🎤", fontSize = 22.sp) }
    }
}
