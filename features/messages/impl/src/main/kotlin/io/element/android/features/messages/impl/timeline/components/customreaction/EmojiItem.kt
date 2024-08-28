/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.customreaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.emojibasebindings.Emoji
import io.element.android.emojibasebindings.EmojiSkin
import io.element.android.features.messages.impl.utils.SKIN_MODIFIERS
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toDp
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiItem(
    emoji: String,
    isSelected: Boolean,
    onSelectEmoji: (String) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 20.sp,
) {
    val highlightSize = (emojiSize * 1.4).toDp()
    val backgroundColor = if (isSelected) {
        ElementTheme.colors.bgActionPrimaryRest
    } else {
        Color.Transparent
    }
    val description = if (isSelected) {
        stringResource(id = CommonStrings.a11y_remove_reaction_with, emoji)
    } else {
        stringResource(id = CommonStrings.a11y_react_with, emoji)
    }
    Box(
        modifier = modifier
            .sizeIn(minWidth = highlightSize, minHeight = highlightSize)
            .background(backgroundColor, CircleShape)
            .combinedClickable(
                enabled = true,
                onClick = { onSelectEmoji(emoji) },
                onLongClick = onLongPress,
                indication = ripple(bounded = false, radius = emojiSize.toDp() / 2 + 10.dp),
                interactionSource = remember { MutableInteractionSource() }
            )
            .clearAndSetSemantics {
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = LocalTextStyle.current.copy(fontSize = emojiSize),
        )
    }
}

fun String.getEmojiSkinTone(): String? {
    return codePoints()
        .boxed()
        .map { String(intArrayOf(it), 0, 1) }
        .filter { it in SKIN_MODIFIERS }
        .findFirst()
        .getOrNull()
}

/**
 *
 */
fun Emoji.withSkinTone(tone: String): EmojiSkin? {
//    assert(tone in SKIN_MODIFIERS)
    return skins?.firstOrNull { skin -> tone in skin.unicode }
}

@PreviewsDayNight
@Composable
internal fun EmojiItemPreview(@PreviewParameter(EmojiItemVariant::class) variant: String?) = ElementPreview {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (isSelected in listOf(true, false)) {
            EmojiItem(
                emoji = "👍" + variant.orEmpty(),
                isSelected = isSelected,
                onSelectEmoji = {},
                onLongPress = {},
            )
        }
    }
}

internal class EmojiItemVariant: PreviewParameterProvider<String?> {
    override val values = sequenceOf(null) + SKIN_MODIFIERS.asSequence()
}
