package com.mj.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF070B14)
private val BgGlow = Color(0xFF111A31)
private val Panel = Color(0xCC151E31)
private val PanelSoft = Color(0x99131B2C)
private val Accent = Color(0xFF8B7CFF)
private val AccentBlue = Color(0xFF55C7FF)
private val TextPrimary = Color(0xFFF5F7FF)
private val TextMuted = Color(0xFF98A5BE)
private val Danger = Color(0xFFFF647C)
private val Good = Color(0xFF55D6A3)
private val Warn = Color(0xFFFFC078)
private val Stroke = Color(0x30FFFFFF)

@Composable
fun MJTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = AccentBlue,
            background = Bg,
            surface = Panel,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun MainScreen(
    onPickModel: () -> Unit,
    onPerms: () -> Unit,
    onA11y: () -> Unit,
    onAppInfo: () -> Unit
) {
    val lang = MJBrain.voiceLang
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastText = MJBrain.messages.lastOrNull()?.text

    LaunchedEffect(MJBrain.messages.size, lastText) {
        if (MJBrain.messages.isNotEmpty()) {
            listState.animateScrollToItem(MJBrain.messages.size - 1)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(BgGlow, Bg, Color(0xFF10172C), Bg),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(900f, 1500f)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Header(lang)
        Controls(lang)
        SetupCard(lang, onPickModel, onPerms, onA11y, onAppInfo)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (MJBrain.messages.isEmpty()) {
                item {
                    WelcomeCard(
                        lang = lang,
                        onQuick = { input = it }
                    )
                }
            }
            items(MJBrain.messages) { message ->
                Bubble(message)
            }
        }

        InputBar(
            lang = lang,
            input = input,
            onChange = { input = it },
            onSend = {
                MJBrain.submit(input)
                input = ""
            }
        )
    }
}

@Composable
private fun Header(lang: Lang) {
    val active = MJBrain.busy || MJBrain.listening
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("MJ", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Box(Modifier.size(7.dp).background(if (active) AccentBlue else Good, CircleShape))
            }
            Text(
                if (active) MJBrain.status.ifBlank { S.listening(lang) } else "YOUR PERSONAL AI",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (MJBrain.messages.isNotEmpty()) {
                TextButton(onClick = { MJBrain.messages.clear() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Clear", color = TextMuted, fontSize = 12.sp)
                }
            }
            OrbButton(active = active, onClick = { MJBrain.toggleListening() })
        }
    }
}

@Composable
private fun OrbButton(active: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "mj-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (active) 1.16f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (active) 520 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb-pulse"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "orb-active"
    )

    Box(
        modifier = Modifier.size(74.dp).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(70.dp).scale(pulse).alpha(if (active) 0.34f else 0.2f)
                .background(Brush.radialGradient(listOf(AccentBlue, Accent, Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.size(58.dp).scale(scale)
                .background(Brush.linearGradient(listOf(Color(0xFF6A5BFF), Color(0xFF35BCEB), Color(0xFF8B7CFF))), CircleShape)
                .border(1.dp, Color(0x88FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (active) "•••" else "MJ", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(PanelSoft)
            .border(1.dp, Stroke, RoundedCornerShape(24.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun Controls(lang: Lang) {
    GlassPanel(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 9.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Lang.values().forEach { language ->
                ChoiceChip(language.label, lang == language) { MJBrain.updateVoiceLang(language) }
            }
            Box(Modifier.width(1.dp).height(22.dp).background(Stroke))
            Mode.values().forEach { mode ->
                ChoiceChip(mode.name.lowercase().replaceFirstChar { it.uppercase() }, MJBrain.mode == mode) {
                    MJBrain.updateMode(mode)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(S.speakLabel(lang), color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = MJBrain.speakReplies, onCheckedChange = { MJBrain.updateSpeak(it) })
        }
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        Brush.linearGradient(listOf(Accent, Color(0xFF526DDE)))
    } else {
        Brush.linearGradient(listOf(Color(0x33263249), Color(0x22263249)))
    }
    Box(
        Modifier.clip(CircleShape)
            .background(background)
            .border(1.dp, if (selected) Color(0x665A9EFF) else Stroke, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color.White else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SetupCard(
    lang: Lang,
    onPickModel: () -> Unit,
    onPerms: () -> Unit,
    onA11y: () -> Unit,
    onAppInfo: () -> Unit
) {
    val modelOk = MJBrain.modelPresent && MJBrain.modelError.isEmpty()
    val visible = !(modelOk && MJBrain.permsOk && MJBrain.a11yOn && MJBrain.importProgress < 0)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { -it / 4 }, animationSpec = tween(260))
    ) {
        GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(S.setupTitle(lang), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Finish these steps to unlock phone controls", color = TextMuted, fontSize = 11.sp)
                }
                Text("SETUP", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            SetupRow(modelOk, S.uModel(lang), S.aImport(lang), onPickModel)
            if (MJBrain.importProgress >= 0) {
                LinearProgressIndicator(
                    progress = { MJBrain.importProgress / 100f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    color = AccentBlue,
                    trackColor = Color(0x332F3A55)
                )
                Text("${MJBrain.importProgress}%", color = TextMuted, fontSize = 11.sp)
            }
            if (MJBrain.modelError.isNotEmpty()) {
                Text(MJBrain.modelError.take(200), color = Danger, fontSize = 11.sp)
            }
            if (MJBrain.modelLoading) Text(S.loadingModel(lang), color = TextMuted, fontSize = 11.sp)
            SetupRow(MJBrain.permsOk, S.uPerms(lang), S.aAllow(lang), onPerms)
            SetupRow(MJBrain.a11yOn, S.uA11y(lang), S.aEnable(lang), onA11y)
            if (!MJBrain.a11yOn) {
                Text(
                    S.hintRestricted(lang),
                    color = Warn,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { onAppInfo() }.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SetupRow(ok: Boolean, title: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (ok) Color(0x2255D6A3) else Color(0x22FFC078)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (ok) "✓" else "!", color = if (ok) Good else Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Text(title, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (!ok) {
            Text(
                action,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Accent, Color(0xFF526DDE))))
                    .clickable { onClick() }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun WelcomeCard(lang: Lang, onQuick: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "A little help,\na lot more done.",
            color = TextPrimary,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.8).sp
        )
        Text(S.greeting(lang), color = TextMuted, fontSize = 13.sp, lineHeight = 20.sp)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip("♫  Play music") { onQuick("Play music") }
            SuggestionChip("♫  SimpMusic") { onQuick("Play music on SimpMusic") }
            SuggestionChip("▶  Open YouTube") { onQuick("Open YouTube") }
            SuggestionChip("✦  Chat with MJ") { onQuick("Hello MJ") }
        }
        Spacer(Modifier.height(4.dp))
        Text("YOUR CONVERSATION", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(16.dp))
            .background(Color(0x331C2A42))
            .border(1.dp, Stroke, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Bubble(message: Msg) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 7 }, animationSpec = tween(220))
    ) {
        if (message.kind == 1) {
            Text(
                message.text,
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
            ) {
                val shape = RoundedCornerShape(
                    topStart = 21.dp,
                    topEnd = 21.dp,
                    bottomEnd = if (message.fromUser) 7.dp else 21.dp,
                    bottomStart = if (message.fromUser) 21.dp else 7.dp
                )
                val brush = if (message.fromUser) {
                    Brush.linearGradient(listOf(Color(0xFF6656D9), Color(0xFF367DC9)))
                } else {
                    Brush.linearGradient(listOf(Color(0xD91B263B), Color(0xD9141D30)))
                }
                Text(
                    text = if (message.text.isEmpty()) "…" else message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .clip(shape)
                        .background(brush)
                        .border(1.dp, Stroke, shape)
                        .padding(horizontal = 15.dp, vertical = 11.dp)
                        .animateContentSize()
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    lang: Lang,
    input: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(27.dp))
                .background(Panel)
                .border(1.dp, Stroke, RoundedCornerShape(27.dp))
                .padding(start = 17.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, lineHeight = 19.sp),
                cursorBrush = SolidColor(AccentBlue),
                maxLines = 4,
                decorationBox = { inner ->
                    Box {
                        if (input.isBlank()) {
                            Text(S.inputHint(lang), color = TextMuted, fontSize = 13.sp)
                        }
                        inner()
                    }
                }
            )
            if (input.isNotBlank()) {
                RoundAction("↑", accent = true, onClick = onSend)
            } else {
                RoundAction("＋", accent = false, onClick = { MJBrain.toggleListening() })
            }
        }
        RoundAction(
            text = if (MJBrain.listening) "■" else "⌁",
            accent = MJBrain.listening,
            onClick = { MJBrain.toggleListening() },
            size = 52
        )
    }
}

@Composable
private fun RoundAction(
    text: String,
    accent: Boolean,
    onClick: () -> Unit,
    size: Int = 40
) {
    val shape = CircleShape
    Box(
        Modifier.size(size.dp).clip(shape)
            .background(
                if (accent) Brush.linearGradient(listOf(Accent, AccentBlue))
                else Brush.linearGradient(listOf(Color(0xFF25334E), Color(0xFF1B263B)))
            )
            .border(1.dp, Stroke, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = if (size > 45) 25.sp else 20.sp, fontWeight = FontWeight.Medium)
    }
}
