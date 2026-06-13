package ro.solomon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
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
                Text("Solomon", style = MaterialTheme.typography.headlineSmall, color = SolomonColors.Primary, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Închide", tint = SolomonColors.TextSecondary) }
            }
            HorizontalDivider(color = SolomonColors.Hairline)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = list,
                contentPadding = PaddingValues(SolSpacing.base),
                verticalArrangement = Arrangement.spacedBy(SolSpacing.sm)
            ) {
                items(state.messages) { m ->
                    ChatBubble(m)
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
private fun ChatBubble(m: ChatViewModel.Message) {
    val isUser = m.role == ChatViewModel.Role.User
    val isTool = m.role == ChatViewModel.Role.Tool
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(
                    topStart = SolSpacing.md,
                    topEnd = SolSpacing.md,
                    bottomStart = if (isUser) SolSpacing.md else 2.dp,
                    bottomEnd = if (isUser) 2.dp else SolSpacing.md
                ))
                .background(
                    when {
                        isUser -> SolomonColors.Primary
                        isTool -> SolomonColors.SurfaceVariant
                        else -> SolomonColors.Surface
                    }
                )
                .padding(horizontal = SolSpacing.md, vertical = SolSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTool) {
                    Text("✓ ", color = SolomonColors.Primary, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    m.text,
                    color = when {
                        isUser -> SolomonColors.OnPrimary
                        isTool -> SolomonColors.TextPrimary
                        else -> SolomonColors.TextPrimary
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
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
                        ro.solomon.app.services.VoiceInputService.State.Listening -> "Ascult…"
                        ro.solomon.app.services.VoiceInputService.State.Processing -> "Procesez…"
                        else -> "Întreabă Solomon…"
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
