/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.customreaction

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.hide
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReactionBottomSheet(
    state: CustomReactionState,
    onSelectEmoji: (EventOrTransactionId, String) -> Unit,
    onSelectReaction: (EventOrTransactionId, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val target = state.target as? CustomReactionState.Target.Success
    val localView = LocalView.current

    fun onDismiss() {
        localView.hideKeyboard()
        state.eventSink(CustomReactionEvents.DismissCustomReactionSheet)
    }

    fun onEmojiSelectedDismiss(emoji: String) {
        localView.hideKeyboard()
        if (target?.event == null) return
        sheetState.hide(coroutineScope) {
            state.eventSink(CustomReactionEvents.DismissCustomReactionSheet)
            onSelectEmoji(target.event.eventOrTransactionId, emoji)
        }
    }

    fun onReactionSelectedDismiss(reaction: String) {
        localView.hideKeyboard()
        if (target?.event?.eventId == null) return
        sheetState.hide(coroutineScope) {
            state.eventSink(CustomReactionEvents.DismissCustomReactionSheet)
            onSelectReaction(target.event.id, reaction)
        }
    }

    if (target?.emojibaseStore != null && target.event.eventId != null) {
        ModalBottomSheet(
            onDismissRequest = ::onDismiss,
            sheetState = sheetState,
            modifier = modifier
                .heightIn(min = if (state.searchState.isSearchActive) (LocalConfiguration.current.screenHeightDp).dp else Dp.Unspecified)
        ) {
            EmojiPicker(
                onSelectEmoji = ::onEmojiSelectedDismiss,
                onSelectReaction = ::onReactionSelectedDismiss,
                emojibaseStore = target.emojibaseStore,
                selectedEmojis = state.selectedEmoji,
                skinTone = state.skinTone,
                state = state.searchState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
