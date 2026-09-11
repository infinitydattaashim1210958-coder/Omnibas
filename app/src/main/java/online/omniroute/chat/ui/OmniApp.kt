package online.omniroute.chat.ui

import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import online.omniroute.chat.R
import online.omniroute.chat.data.ChatMessage
import online.omniroute.chat.data.Conversation
import online.omniroute.chat.data.ProviderSettings
import online.omniroute.chat.data.RouteId
import online.omniroute.chat.data.SUGGESTIONS
import online.omniroute.chat.data.previewFrom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OmniApp(vm: ChatViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(vm.toast) {
        val msg = vm.toast ?: return@LaunchedEffect
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        vm.consumeToast()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(OmniBg),
    ) {
        when (val s = vm.screen) {
            Screen.List -> ChatListScreen(vm)
            is Screen.Thread -> ChatThreadScreen(vm, s.chatId)
            Screen.Settings -> SettingsScreen(vm)
        }
    }
}

@Composable
private fun ChatListScreen(vm: ChatViewModel) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_omni),
                    contentDescription = null,
                    tint = OmniAccent,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "OmniRoute",
                    color = OmniFg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = vm::openSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = OmniMuted)
                }
            }
            SearchField(value = vm.search, onChange = { vm.search = it })
            val items = vm.filtered
            if (items.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (vm.conversations.isEmpty()) "No chats yet" else "No matching chats",
                        color = OmniFg,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (vm.conversations.isEmpty()) "Start one — it stays on this device."
                        else "Try a different search.",
                        color = OmniMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, start = 8.dp, end = 8.dp),
                ) {
                    items(items, key = { it.id }) { chat ->
                        ChatRow(chat) { vm.openChat(chat.id) }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { vm.newChat() },
            containerColor = OmniAccent,
            contentColor = OmniAccentFg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New chat")
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OmniSurface2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = OmniSubtle, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(OmniAccent),
            textStyle = TextStyle(color = OmniFg, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Search chats", color = OmniSubtle, fontSize = 14.sp)
                inner()
            },
        )
    }
}

@Composable
private fun ChatRow(chat: Conversation, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(OmniAccentDim),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(chat.title),
                color = OmniAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.title,
                    color = OmniFg,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatTime(chat.updatedAt), color = OmniSubtle, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.route.label.uppercase(Locale.US),
                    color = OmniAccent.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    previewFrom(chat.messages),
                    color = OmniMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatThreadScreen(vm: ChatViewModel, chatId: String) {
    val chat = vm.active(chatId)
    BackHandler { vm.openList() }
    var routesOpen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val streaming = vm.streamingChatId == chatId
    val listState = rememberLazyListState()
    val messages = chat?.messages.orEmpty()

    LaunchedEffect(messages.size, vm.streamText, chatId) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::openList) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OmniFg)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OmniAccentDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_omni),
                    contentDescription = null,
                    tint = OmniAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chat?.title ?: "New chat",
                    color = OmniFg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { routesOpen = true },
                ) {
                    Icon(
                        Icons.Outlined.AccountTree,
                        contentDescription = null,
                        tint = OmniAccent,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${(chat?.route ?: vm.defaultRoute).label} route",
                        color = OmniAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = chat != null) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Chat actions", tint = OmniMuted)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete chat", color = OmniDanger) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = OmniDanger) },
                        onClick = {
                            menu = false
                            vm.deleteChat(chatId)
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = OmniFg.copy(alpha = 0.08f))

        if (chat == null || messages.isEmpty()) {
            EmptyThread(
                modifier = Modifier.weight(1f),
                onPick = { prompt, route -> vm.send(prompt, chatId, route) },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    val isThis = streaming && vm.streamingMessageId == message.id
                    MessageBubble(
                        message = message,
                        streamingText = if (isThis) vm.streamText else null,
                        isStreaming = isThis,
                        onRetry = if (
                            message.role == "assistant" &&
                            message.id == messages.last().id &&
                            !streaming
                        ) {
                            { vm.retryLast(chatId) }
                        } else null,
                    )
                }
            }
        }

        Composer(
            value = vm.draft,
            onChange = { vm.draft = it },
            streaming = streaming,
            onSend = { vm.send(vm.draft, chatId) },
            onStop = vm::stop,
        )
    }

    if (routesOpen) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { routesOpen = false },
            sheetState = sheet,
            containerColor = OmniSurface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("ROUTE", color = OmniMuted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
                Text("How should OmniRoute think?", color = OmniFg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "One engine. Four routes. Switch any time — this chat keeps the choice.",
                    color = OmniMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                RouteId.entries.forEach { route ->
                    val selected = (chat?.route ?: vm.defaultRoute) == route
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) OmniAccentDim else OmniSurface2)
                            .clickable {
                                vm.setRoute(chatId, route)
                                routesOpen = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) OmniAccent else OmniSurface3),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.AccountTree,
                                contentDescription = null,
                                tint = if (selected) OmniAccentFg else OmniFg,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(route.label, color = OmniFg, fontWeight = FontWeight.Medium)
                            Text(route.hint, color = OmniMuted, fontSize = 13.sp)
                        }
                        if (selected) Icon(Icons.Default.Check, null, tint = OmniAccent)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun EmptyThread(modifier: Modifier, onPick: (String, RouteId) -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OmniAccentDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_omni),
                contentDescription = null,
                tint = OmniAccent,
                modifier = Modifier.size(36.dp),
            )
        }
        Text("Where to?", color = OmniFg, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text(
            "Unlimited chat on one route. Pick a prompt or just type.",
            color = OmniMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        SUGGESTIONS.forEach { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OmniSurface2)
                    .clickable { onPick(item.prompt, item.route) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    item.route.label.uppercase(Locale.US),
                    color = OmniAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
                )
                Text(item.prompt, color = OmniFg, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    streamingText: String?,
    isStreaming: Boolean,
    onRetry: (() -> Unit)?,
) {
    val isUser = message.role == "user"
    val body = if (isStreaming) streamingText.orEmpty() else message.content
    val showTyping = isStreaming && body.isBlank()
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OmniAccentDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_omni),
                    contentDescription = null,
                    tint = OmniAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(
            Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Box(
                Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 18.dp,
                        ),
                    )
                    .background(if (isUser) OmniUser else OmniSurface2)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                when {
                    showTyping -> TypingDots()
                    isUser -> Text(body, color = OmniFg, fontSize = 15.sp, lineHeight = 22.sp)
                    else -> MarkdownBody(body)
                }
            }
            if (!message.error.isNullOrBlank() && !isStreaming) {
                Text(message.error, color = OmniDanger, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            if (!isStreaming && (body.isNotBlank() || !message.error.isNullOrBlank())) {
                Row {
                    if (body.isNotBlank()) {
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(body))
                                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = OmniMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (onRetry != null && !isUser) {
                        IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = OmniMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingDots() {
    Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) {
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(OmniAccent),
            )
        }
    }
}

@Composable
private fun MarkdownBody(text: String) {
    val ctx = LocalContext.current
    val markwon = remember { Markwon.create(ctx) }
    AndroidView(
        factory = {
            TextView(it).apply {
                setTextColor(OmniFg.toArgb())
                textSize = 15f
                setLineSpacing(0f, 1.35f)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { markwon.setMarkdown(it, text) },
    )
}

@Composable
private fun Composer(
    value: String,
    onChange: (String) -> Unit,
    streaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val canSend = value.trim().isNotEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .background(OmniSurface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp, max = 140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OmniSurface2)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                cursorBrush = SolidColor(OmniAccent),
                textStyle = TextStyle(color = OmniFg, fontSize = 15.sp, lineHeight = 20.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Message OmniRoute", color = OmniSubtle, fontSize = 15.sp)
                    inner()
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        val enabled = streaming || canSend
        Box(
            Modifier
                .padding(bottom = 2.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    when {
                        streaming -> OmniSurface3
                        canSend -> OmniAccent
                        else -> OmniSurface2
                    },
                )
                .clickable(enabled = enabled) { if (streaming) onStop() else onSend() },
            contentAlignment = Alignment.Center,
        ) {
            if (streaming) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = OmniFg, modifier = Modifier.size(18.dp))
            } else {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) OmniAccentFg else OmniSubtle,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: ChatViewModel) {
    BackHandler { vm.openList() }
    var mode by remember { mutableStateOf(vm.settings.mode) }
    var baseUrl by remember { mutableStateOf(vm.settings.baseUrl) }
    var apiKey by remember { mutableStateOf(vm.settings.apiKey) }
    var model by remember { mutableStateOf(vm.settings.model) }
    var hideKey by remember { mutableStateOf(true) }
    var modelMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = vm::openList) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OmniFg)
            }
            Text("Settings", color = OmniFg, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OmniAccentDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_omni),
                            contentDescription = null,
                            tint = OmniAccent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("OmniRoute", color = OmniFg, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Unlimited AI chat", color = OmniMuted, fontSize = 14.sp)
                    }
                }
                Text(
                    "Mobile chat client inspired by the open-source OmniRoute gateway — one endpoint, many routes. Conversations stay on this device.",
                    color = OmniMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
                    lineHeight = 20.sp,
                )
            }
            item {
                Text("PROVIDER", color = OmniMuted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    title = "Free unlimited",
                    subtitle = "No key. Public OpenAI-compatible route.",
                    selected = mode == ProviderSettings.MODE_FREE,
                ) { mode = ProviderSettings.MODE_FREE }
                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    title = "Custom gateway",
                    subtitle = "OmniRoute, xAI, OpenRouter, or any /v1 endpoint.",
                    selected = mode == ProviderSettings.MODE_CUSTOM,
                ) { mode = ProviderSettings.MODE_CUSTOM }
            }
            item {
                AnimatedVisibility(visible = mode == ProviderSettings.MODE_CUSTOM) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        LabeledField("Base URL", baseUrl, { baseUrl = it }, "http://127.0.0.1:4141/v1")
                        Spacer(Modifier.height(12.dp))
                        LabeledField("Model", model, { model = it }, "auto")
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(OmniSurface2)
                                .clickable(enabled = !vm.modelsLoading) {
                                    vm.loadModels(baseUrl.trim(), apiKey.trim())
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = OmniAccent,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (vm.modelsLoading) "Loading…" else "Fetch models from server",
                                color = OmniAccent,
                                fontSize = 13.sp,
                            )
                        }
                        vm.modelsError?.let { err ->
                            Text(
                                err,
                                color = OmniDanger,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        if (vm.availableModels.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Box {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OmniSurface2)
                                        .clickable { modelMenu = true }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "${vm.availableModels.size} models found — tap to choose",
                                        color = OmniMuted,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                                    vm.availableModels.forEach { id ->
                                        DropdownMenuItem(
                                            text = { Text(id) },
                                            onClick = {
                                                model = id
                                                modelMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LabeledField(
                            "API key (required if your OmniRoute server has REQUIRE_API_KEY on)",
                            apiKey,
                            { apiKey = it },
                            "sk-…",
                            secret = hideKey,
                            trailing = {
                                IconButton(onClick = { hideKey = !hideKey }) {
                                    Icon(
                                        if (hideKey) Icons.Default.Close else Icons.Default.Check,
                                        contentDescription = "Toggle key",
                                        tint = OmniMuted,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OmniAccent)
                        .clickable {
                            vm.saveSettings(
                                ProviderSettings(
                                    mode = mode,
                                    baseUrl = baseUrl.trim(),
                                    apiKey = apiKey.trim(),
                                    model = model.trim().ifBlank { "auto" },
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save provider", color = OmniAccentFg, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OmniSurface2)
                        .padding(16.dp),
                ) {
                    Text("This device", color = OmniFg, fontWeight = FontWeight.Medium)
                    Text(
                        "Chats are stored locally. Clearing them cannot be undone.",
                        color = OmniMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OmniDanger.copy(alpha = 0.15f))
                            .clickable { vm.clearAll() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Clear all chats", color = OmniDanger, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) OmniAccentDim else OmniSurface2)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = OmniFg, fontWeight = FontWeight.Medium)
            Text(subtitle, color = OmniMuted, fontSize = 13.sp)
        }
        if (selected) Icon(Icons.Default.Check, null, tint = OmniAccent)
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    secret: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Text(label, color = OmniMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OmniSurface2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(OmniAccent),
            textStyle = TextStyle(color = OmniFg, fontSize = 14.sp),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = OmniSubtle, fontSize = 14.sp)
                inner()
            },
        )
        trailing?.invoke()
    }
}

private fun initials(title: String): String {
    val parts = title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "OR"
    val a = parts[0].first().uppercaseChar()
    val b = parts.getOrNull(1)?.first()?.uppercaseChar()
    return if (b != null) "$a$b" else a.toString()
}

private fun formatTime(ts: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return fmt.format(Date(ts))
}
