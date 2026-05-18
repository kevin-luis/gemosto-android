package com.gemosto.feature.gemo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.domain.gemo.ResponseType
import com.gemosto.domain.gemo.SuggestedQuestion

@Composable
internal fun GemoDisclaimerBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GemColors.EmeraldLight,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GemColors.Border, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = GemColors.EmeraldDark,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.gemo_disclaimer_banner),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = GemColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SuggestedQuestionSection(
    questions: List<SuggestedQuestion>,
    onQuestionClick: (SuggestedQuestion) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.gemo_suggested_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            questions.forEach { question ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GemColors.BackgroundSoft,
                    modifier = Modifier
                        .border(1.dp, GemColors.Border, RoundedCornerShape(999.dp))
                        .clickable { onQuestionClick(question) },
                ) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GemoMessageBubble(message: GemoChatMessage) {
    val bubbleColor = when (message.author) {
        GemoChatAuthor.USER -> GemColors.EmeraldPrimary
        GemoChatAuthor.GEMO -> when (message.responseType) {
            ResponseType.ESCALATION -> GemColors.DangerBg
            ResponseType.REFUSAL -> GemColors.BackgroundSoft
            ResponseType.EDUCATION,
            null,
            -> GemColors.EmeraldLight
        }
    }
    val textColor = when (message.author) {
        GemoChatAuthor.USER -> GemColors.OnPrimary
        GemoChatAuthor.GEMO -> GemColors.TextPrimary
    }
    val borderColor = when (message.responseType) {
        ResponseType.ESCALATION -> GemColors.Danger
        else -> GemColors.Border
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.author == GemoChatAuthor.USER) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (message.author == GemoChatAuthor.GEMO) {
                    ResponseTypeLabel(type = message.responseType)
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                )
                if (
                    message.author == GemoChatAuthor.GEMO &&
                    !message.disclaimer.isNullOrBlank()
                ) {
                    Text(
                        text = message.disclaimer,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GemColors.TextSecondary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseTypeLabel(type: ResponseType?) {
    when (type) {
        ResponseType.REFUSAL -> Text(
            text = stringResource(R.string.gemo_response_refusal_label),
            style = MaterialTheme.typography.labelMedium.copy(
                color = GemColors.TextSecondary,
                fontWeight = FontWeight.W500,
            ),
        )

        ResponseType.ESCALATION -> Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = GemColors.Danger,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.gemo_response_escalation_label),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = GemColors.Danger,
                    fontWeight = FontWeight.W500,
                ),
            )
        }

        ResponseType.EDUCATION,
        null,
        -> Unit
    }
}
