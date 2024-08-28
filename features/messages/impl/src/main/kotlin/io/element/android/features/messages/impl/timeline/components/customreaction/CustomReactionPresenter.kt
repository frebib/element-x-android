/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.customreaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.preferences.api.store.SessionPreferencesStore
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import javax.inject.Inject

class CustomReactionPresenter @Inject constructor(
    private val emojibaseProvider: EmojibaseProvider,
    private val emojiPickerStatePresenter: EmojiPickerStatePresenter,
    private val sessionPreferencesStore: SessionPreferencesStore,
) : Presenter<CustomReactionState> {
    @Composable
    override fun present(): CustomReactionState {
        val target: MutableState<CustomReactionState.Target> = remember {
            mutableStateOf(CustomReactionState.Target.None)
        }
        val searchState = emojiPickerStatePresenter.present()
        val skinTone by sessionPreferencesStore.getSkinTone().collectAsState(initial = null)

        val localCoroutineScope = rememberCoroutineScope()
        fun handleShowCustomReactionSheet(event: TimelineItem.Event) {
            target.value = CustomReactionState.Target.Loading(event)
            localCoroutineScope.launch {
                target.value = CustomReactionState.Target.Success(
                    event = event,
                    emojibaseStore = emojibaseProvider.emojibaseStore
                )
            }
        }

        fun handleDismissCustomReactionSheet() {
            target.value = CustomReactionState.Target.None
            searchState.eventSink(EmojiPickerEvents.Reset)
        }

        fun handleEvents(event: CustomReactionEvents) {
            when (event) {
                is CustomReactionEvents.ShowCustomReactionSheet -> handleShowCustomReactionSheet(event.event)
                is CustomReactionEvents.DismissCustomReactionSheet -> handleDismissCustomReactionSheet()
            }
        }

        val event = (target.value as? CustomReactionState.Target.Success)?.event
        val selectedEmoji = event
            ?.reactionsState
            ?.reactions
            ?.mapNotNull { if (it.isHighlighted) it.key else null }
            .orEmpty()
            .toImmutableSet()
        return CustomReactionState(
            target = target.value,
            selectedEmoji = selectedEmoji,
            skinTone = skinTone,
            eventSink = ::handleEvents,
            searchState = searchState,
        )
    }
}
