/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.customreaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.emojibasebindings.Emoji
import io.element.android.emojibasebindings.EmojibaseCategory
import io.element.android.emojibasebindings.EmojibaseDatasource
import io.element.android.emojibasebindings.EmojibaseStore
import io.element.android.emojibasebindings.allEmojis
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toSp
import io.element.android.libraries.designsystem.theme.components.ElementSearchBarDefaults
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    onSelectEmoji: (Emoji) -> Unit,
    emojibaseStore: EmojibaseStore,
    selectedEmojis: ImmutableSet<String>,
    state: EmojiPickerState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val categories = remember { emojibaseStore.categories }
    val pagerState = rememberPagerState(pageCount = { EmojibaseCategory.entries.size })
    val searchFocusRequester = remember { FocusRequester() }

    Column(modifier) {
        EmojiPickerSearchBar(
            query = state.searchQuery,
            active = state.isSearchActive,
            onActiveChange = { state.eventSink(EmojiPickerEvents.OnSearchActiveChanged(it)) },
            onQueryChange = { state.eventSink(EmojiPickerEvents.UpdateSearchQuery(it)) },
            focusRequester = searchFocusRequester,
        )

        Column(
            modifier = Modifier
                .pointerInput(state.isSearchActive) {
                    awaitEachGesture {
                        // For any consumed pointer event in this column, deactivate the search field
                        awaitFirstDown(requireUnconsumed = false)
                        if (state.isSearchActive) {
                            state.eventSink(EmojiPickerEvents.OnSearchActiveChanged(false))
                        }
                    }
                }
        ) {
            if (state.searchQuery.isEmpty()) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    EmojibaseCategory.entries.forEachIndexed { index, category ->
                        Tab(icon = {
                            Icon(
                                imageVector = category.icon, contentDescription = stringResource(id = category.title)
                            )
                        }, selected = pagerState.currentPage == index, onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        })
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { index ->
                    val category = EmojibaseCategory.entries[index]
                    val emojis = categories[category] ?: listOf()
                    EmojiGrid(emojis = emojis, selectedEmojis = selectedEmojis, onSelectEmoji = onSelectEmoji)
                }
            } else {
                when (state.searchResults) {
                    is SearchBarResultState.Results<ImmutableList<Emoji>> -> {
                        EmojiGrid(
                            emojis = state.searchResults.results,
                            selectedEmojis = selectedEmojis,
                            onSelectEmoji = onSelectEmoji,
                        )
                    }

                    is SearchBarResultState.NoResultsFound<ImmutableList<Emoji>> -> {
                        // No results found, show a message
                        Spacer(Modifier.size(80.dp))

                        Text(
                            text = stringResource(CommonStrings.common_no_results),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        // Not searching - nothing to show.
                    }
                }
            }
        }
    }

    // Automatically open the keyboard if search is active
    LaunchedEffect(Unit) {
        if (state.isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun EmojiGrid(
    emojis: List<Emoji>,
    selectedEmojis: ImmutableSet<String>,
    onSelectEmoji: (Emoji) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 48.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(emojis, key = { it.unicode }) { item ->
            EmojiItem(
                modifier = Modifier.aspectRatio(1f),
                item = item,
                isSelected = selectedEmojis.contains(item.unicode),
                onSelectEmoji = onSelectEmoji,
                emojiSize = 32.dp.toSp(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSearchBar(
    query: String,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester = FocusRequester.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusManager = LocalFocusManager.current

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onActiveChange(true) },
        placeholder = {
            Text(text = stringResource(CommonStrings.common_search_for_emoji))
        },
        trailingIcon = when {
            query.isNotEmpty() -> {
                {
                    IconButton(onClick = {
                        onQueryChange("")
                    }) {
                        Icon(
                            imageVector = CompoundIcons.Close(),
                            contentDescription = stringResource(CommonStrings.action_clear),
                        )
                    }
                }
            }

            else -> {
                {
                    Icon(
                        imageVector = CompoundIcons.Search(),
                        contentDescription = stringResource(CommonStrings.action_search),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        shape = SearchBarDefaults.inputFieldShape,
        singleLine = true,
        colors = (
            if (active) ElementSearchBarDefaults.activeColors().inputFieldColors
            else ElementSearchBarDefaults.inactiveColors().inputFieldColors
            )
            .copy(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        interactionSource = interactionSource,
    )

    val isFocused = interactionSource.collectIsFocusedAsState().value
    val shouldClearFocus = !active && isFocused
    LaunchedEffect(active) {
        if (shouldClearFocus) {
            focusManager.clearFocus()
        }
    }
}

@PreviewsDayNight
@Composable
internal fun EmojiPickerPreview() = ElementPreview {
    EmojiPicker(
        onSelectEmoji = {},
        emojibaseStore = EmojibaseDatasource().load(LocalContext.current),
        selectedEmojis = persistentSetOf("😀", "😄", "😃"),
        state = EmojiPickerState(
            startActive = false,
            isSearchActive = false,
            searchQuery = "",
            searchResults = SearchBarResultState.Initial(),
        ) {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewsDayNight
@Composable
internal fun EmojiPickerSearchPreview() = ElementPreview {
    val emojibaseStore = EmojibaseDatasource().load(LocalContext.current)
    val query = "grin"
    EmojiPicker(
        onSelectEmoji = {},
        emojibaseStore = emojibaseStore,
        selectedEmojis = persistentSetOf("😀", "😄", "😃"),
        state = EmojiPickerState(
            startActive = false,
            isSearchActive = true,
            searchQuery = query,
            searchResults = searchEmojis(query, emojibaseStore.allEmojis)
        ) {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewsDayNight
@Composable
internal fun EmojiPickerSearchNoMatchPreview() = ElementPreview {
    val emojibaseStore = EmojibaseDatasource().load(LocalContext.current)
    val query = "this is a very long string that won't match anything"
    EmojiPicker(
        onSelectEmoji = {},
        emojibaseStore = emojibaseStore,
        selectedEmojis = persistentSetOf("😀", "😄", "😃"),
        state = EmojiPickerState(
            startActive = false,
            isSearchActive = true,
            searchQuery = query,
            searchResults = searchEmojis(query, emojibaseStore.allEmojis)
        ) {},
        modifier = Modifier.fillMaxWidth(),
    )
}
