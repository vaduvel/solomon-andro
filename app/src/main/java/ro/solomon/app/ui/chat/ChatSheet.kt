package ro.solomon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.components.SolHairlineDivider
import ro.solomon.app.ui.theme.SolSpacing
import ro.solomon.app.ui.theme.SolomonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(onDismiss: () -> Unit, vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.size - 1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SolomonColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Solomon", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.Primary)
                    Text("Vocea ta financiar\u0103", style = MaterialTheme.typography.bodySmall, color = SolomonColors.TextTertiary)
                }
                IconButton(onClick = { vm.toggleVoice() }) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = if (state.voiceEnabled) "Opre\u0219te vocea" else "Porne\u0219te vocea",
                        tint = if (state.voiceEnabled) SolomonColors.Primary else SolomonColors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "\u00CEnchide", tint = SolomonColors.TextSecondary) }
            }
            SolHairlineDivider()
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = list,
                contentPadding = PaddingValues(SolSpacing.base),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
            ) {
                items(state.messages) { m ->
                    ChatBubble(m, onSpeak = { vm.speakMessage(m.text) })
                }
            }
            ChatInput(
                value = state.draft,
                onChange = vm::setDraft,
                onSend = { vm.send(); scope.launch { list.animateScrollToItem(Int.MAX_VALUE) } },
                onVoiceResult = { text -> vm.setDraft(state.draft + " " + text) }
            )
        }
    }
}

@Composable
private fun ChatBubble(m: ChatViewModel.Message, onSpeak: () -> Unit = {}) {
    val isUser = m.role == ChatViewModel.Role.User
    val isTool = m.role == ChatViewModel.Role.Tool
    val isAssistant = m.role == ChatViewModel.Role.Assistant
    val shape = RoundedCornerShape(
        topStart = SolSpacing.md,
        topEnd = SolSpacing.md,
        bottomStart = if (isUser) SolSpacing.md else 2.dp,
        bottomEnd = if (isUser) 2.dp else SolSpacing.md
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(shape)
                .background(if (isUser) SolomonColors.Primary else Color.White.copy(alpha = 0.05f))
                .then(
                    if (isUser) Modifier
                    else Modifier.border(1.dp, Color.White.copy(alpha = if (isTool) 0.12f else 0.08f), shape)
                )
                .padding(horizontal = SolSpacing.md, vertical = SolSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTool) {
                    Text("\u2713 ", color = SolomonColors.Primary, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    m.text,
                    color = if (isUser) SolomonColors.OnPrimary else SolomonColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (isAssistant) {
            IconButton(onClick = onSpeak) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Cite\u0219te", tint = SolomonColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ChatInput(value: String, onChange: (String) -> Unit, onSend: () -> Unit, onVoiceResult: (String) -> Unit) {
    val voiceState by ServiceLocator.voiceInput.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(voiceState.state, voiceState.lastResult) {
        val r = voiceState.lastResult ?: return@LaunchedEffect
        if (voiceState.state == ro.solomon.app.services.VoiceInputService.State.Result) {
            onVoiceResult(r.text)
            ServiceLocator.voiceInput.reset()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(SolSpacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value.ifEmpty { voiceState.partialText },
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    when (voiceState.state) {
                        ro.solomon.app.services.VoiceInputService.State.Listening -> "Ascult\u2026"
                        ro.solomon.app.services.VoiceInputService.State.Processing -> "Procesez\u2026"
                        else -> "\u00CEntreab\u0103 Solomon\u2026"
                    },
                    color = SolomonColors.TextTertiary
                )
            },
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SolomonColors.Primary,
                unfocusedBorderColor = SolomonColors.Outline,
                focusedTextColor = SolomonColors.TextPrimary,
                unfocusedTextColor = SolomonColors.TextPrimary,
                cursorColor = SolomonColors.Primary
            )
        )
        Spacer(Modifier.width(SolSpacing.sm))
        FilledIconButton(
            onClick = {
                when (voiceState.state) {
                    ro.solomon.app.services.VoiceInputService.State.Listening -> ServiceLocator.voiceInput.cancel()
                    else -> ServiceLocator.voiceInput.start()
                }
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (voiceState.state == ro.solomon.app.services.VoiceInputService.State.Listening) SolomonColors.Rose else SolomonColors.SurfaceVariant,
                contentColor = if (voiceState.state == ro.solomon.app.services.VoiceInputService.State.Listening) SolomonColors.OnPrimary else SolomonColors.TextPrimary
            )
        ) {
            Icon(Icons.Filled.Mic, "Voice")
        }
        Spacer(Modifier.width(SolSpacing.xs))
        FilledIconButton(
            onClick = onSend,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = SolomonColors.Primary, contentColor = SolomonColors.OnPrimary)
        ) {
            Icon(Icons.Filled.Send, "Trimite")
        }
    }
}
