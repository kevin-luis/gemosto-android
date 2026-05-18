package com.gemosto.feature.gemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import com.gemosto.domain.gemo.GemoAiDisclaimers
import com.gemosto.domain.gemo.GemoChatAuthor
import com.gemosto.domain.gemo.GemoChatMessage
import com.gemosto.domain.gemo.RecommendedAction
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.RiskLevel
import com.gemosto.domain.gemo.SuggestedQuestion
import org.koin.androidx.compose.koinViewModel

@Composable
fun GemoChatScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    viewModel: GemoChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    GemoChatContent(
        state = state,
        paddingValues = paddingValues,
        onBack = onBack,
        onSuggestedQuestionClick = viewModel::onSuggestedQuestionClick,
        onSend = viewModel::onSend,
        onRetry = viewModel::onRetry,
        onNewSession = viewModel::onNewSession,
    )
}

@Composable
private fun GemoChatContent(
    state: GemoChatUiState,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onSuggestedQuestionClick: (SuggestedQuestion) -> Unit,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onNewSession: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        GemoHeader(
            onBack = onBack,
            onNewSession = {
                input = ""
                onNewSession()
            },
        )
        Spacer(Modifier.height(12.dp))
        GemoDisclaimerBanner()
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.suggestedQuestions.isNotEmpty()) {
                    item {
                        SuggestedQuestionSection(
                            questions = state.suggestedQuestions,
                            onQuestionClick = onSuggestedQuestionClick,
                        )
                    }
                }

                items(
                    items = state.messages,
                    key = { it.id },
                ) { message ->
                    GemoMessageBubble(message = message)
                }

                if (state.isLoading) {
                    item {
                        GemoLoadingRow()
                    }
                }

                if (state.error != null) {
                    item {
                        GemoErrorRow(onRetry = onRetry)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GemoInputBar(
            input = input,
            isLoading = state.isLoading,
            onInputChange = { input = it },
            onSend = {
                val text = input
                input = ""
                onSend(text)
            },
        )
    }
}

@Composable
private fun GemoHeader(
    onBack: () -> Unit,
    onNewSession: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.gemo_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.gemo_motto),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = GemColors.TextSecondary,
                ),
            )
        }
        TextButton(onClick = onNewSession) {
            Text(stringResource(R.string.gemo_new_session))
        }
    }
}

@Composable
private fun GemoLoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = GemColors.EmeraldPrimary,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.gemo_loading),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = GemColors.TextSecondary,
            ),
        )
    }
}

@Composable
private fun GemoErrorRow(
    onRetry: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GemColors.BackgroundSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.gemo_send_failed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun GemoInputBar(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = {
                Text(stringResource(R.string.gemo_input_placeholder))
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4,
        )
        Spacer(Modifier.size(8.dp))
        IconButton(
            onClick = onSend,
            enabled = input.isNotBlank() && !isLoading,
        ) {
            Icon(
                imageVector = Icons.Outlined.Send,
                contentDescription = stringResource(R.string.gemo_send),
                tint = if (input.isNotBlank() && !isLoading) {
                    GemColors.EmeraldPrimary
                } else {
                    GemColors.TextSecondary
                },
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Gemo Empty")
@Composable
private fun GemoEmptyPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            GemoChatContent(
                state = GemoChatUiState(
                    suggestedQuestions = listOf(
                        SuggestedQuestion("Apa itu osteoartritis lutut?"),
                        SuggestedQuestion("Latihan ringan apa yang umumnya baik untuk OA lutut?"),
                        SuggestedQuestion("Bagaimana cara membantu mengurangi kaku pada lutut?"),
                    ),
                ),
                paddingValues = PaddingValues(0.dp),
                onBack = {},
                onSuggestedQuestionClick = {},
                onSend = {},
                onRetry = {},
                onNewSession = {},
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Gemo Conversation")
@Composable
private fun GemoConversationPreview() {
    GemostoTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            GemoChatContent(
                state = GemoChatUiState(
                    messages = listOf(
                        GemoChatMessage(
                            id = "user_1",
                            author = GemoChatAuthor.USER,
                            text = "Apa itu OA lutut?",
                        ),
                        GemoChatMessage(
                            id = "gemo_2",
                            author = GemoChatAuthor.GEMO,
                            text = "Osteoartritis lutut adalah perubahan bertahap pada sendi lutut.",
                            responseType = ResponseType.EDUCATION,
                            riskLevel = RiskLevel.LOW,
                            disclaimer = GemoAiDisclaimers.DEFAULT,
                            recommendedAction = RecommendedAction.NONE,
                        ),
                        GemoChatMessage(
                            id = "gemo_3",
                            author = GemoChatAuthor.GEMO,
                            text = "Gejala seperti lutut panas dan disertai demam perlu dinilai tenaga medis segera.",
                            responseType = ResponseType.ESCALATION,
                            riskLevel = RiskLevel.URGENT,
                            disclaimer = GemoAiDisclaimers.URGENT,
                            recommendedAction = RecommendedAction.SEEK_URGENT_CARE,
                        ),
                    ),
                ),
                paddingValues = PaddingValues(0.dp),
                onBack = {},
                onSuggestedQuestionClick = {},
                onSend = {},
                onRetry = {},
                onNewSession = {},
            )
        }
    }
}
