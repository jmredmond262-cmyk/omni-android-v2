package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AiSender
import com.example.ui.MainViewModel
import com.example.ui.OmniAiMessage
import com.example.ui.Screen
import com.example.ui.theme.CyberGlowAmber
import com.example.ui.theme.CyberGlowCyan
import com.example.ui.theme.CyberGlowGreen
import com.example.ui.theme.CyberGlowPurple
import com.example.ui.theme.CyberOrbGradient

@Composable
fun OmniAiScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.aiMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val currentCity by viewModel.currentCity.collectAsState()
    val displayLocality = currentCity.ifBlank { "tu ciudad" }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "❔ ¿Cómo uso OMNI?",
        "🏪 ¿Cómo publico mi comercio?",
        "📍 Comercios y ofertas en $displayLocality"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A14))
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
            .imePadding()
    ) {
        // Top Banner / Header Omni AI
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1626)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Glowing AI Orb Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberOrbGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ai",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Omni AI Assistant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "EN LÍNEA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Inteligencia para $displayLocality y la región",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearAiChat() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Limpiar conversación",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Quick Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF131D33),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF233558)),
                    modifier = Modifier.clickable {
                        viewModel.sendAiPrompt(prompt)
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                AiMessageItem(
                    message = msg,
                    onActionClick = { action ->
                        action.targetScreen?.let { screen ->
                            viewModel.navigateTo(screen)
                        }
                    }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CyberGlowCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Omni AI está consultando a Gemini...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input Dock
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1829)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Preguntale a Omni AI sobre tu barrio...",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0),
                        cursorColor = CyberGlowCyan
                    ),
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_field")
                )

                // Mic Action
                IconButton(
                    onClick = {
                        inputText = "¿Qué farmacia de guardia está abierta en este momento?"
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictar consulta",
                        tint = Color(0xFF94A3B8)
                    )
                }

                // Send Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyberGlowAmber, Color(0xFFFF6D00))
                            )
                        )
                        .clickable {
                            if (inputText.isNotBlank()) {
                                viewModel.sendAiPrompt(inputText)
                                inputText = ""
                            }
                        }
                        .testTag("send_ai_prompt_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AiMessageItem(
    message: OmniAiMessage,
    onActionClick: (com.example.ui.AiAction) -> Unit
) {
    val isUser = message.sender == AiSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF1E1B4B) else Color(0xFF0F172A)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isUser) Color(0xFF8B5CF6).copy(alpha = 0.4f) else Color(0xFF00E5FF).copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.96f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header of message if Omni AI
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CyberOrbGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ai",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Omni AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGlowCyan
                        )

                        message.tag?.let { tagText ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF22D3EE).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = tagText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22D3EE),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Message Text
                Text(
                    text = message.text,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFF1F5F9),
                    fontWeight = FontWeight.Normal
                )

                // Actions if present
                if (message.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.actions.forEach { action ->
                            Button(
                                onClick = { onActionClick(action) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16233B),
                                    contentColor = CyberGlowCyan
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = action.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "→",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = message.timestamp,
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}
