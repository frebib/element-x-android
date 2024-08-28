/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.customreaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.emojibasebindings.Emoji
import io.element.android.emojibasebindings.EmojiSkin
import io.element.android.emojibasebindings.EmojibaseCategory
import io.element.android.emojibasebindings.EmojibaseDatasource
import io.element.android.emojibasebindings.EmojibaseStore
import io.element.android.emojibasebindings.allEmojis
import io.element.android.features.messages.impl.timeline.components.MessagesReactionButton
import io.element.android.features.messages.impl.timeline.components.MessagesReactionsButtonContent
import io.element.android.features.messages.impl.timeline.model.MAX_REACTION_LENGTH_CHARS
import io.element.android.libraries.core.extensions.ellipsize
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toSp
import io.element.android.libraries.designsystem.theme.components.ElementSearchBarDefaults
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    onSelectEmoji: (String) -> Unit,
    onSelectReaction: (String) -> Unit,
    emojibaseStore: EmojibaseStore,
    selectedEmojis: ImmutableSet<String>,
    state: EmojiPickerState,
    modifier: Modifier = Modifier,
    skinTone: String? = null,
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
            when (state.searchResults) {
                is SearchBarResultState.Initial -> {
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
                        EmojiGrid(emojis = emojis, selectedEmojis = selectedEmojis, onSelectEmoji = onSelectEmoji, skinTone = skinTone)
                    }
                }
                is SearchBarResultState.Results -> {
                    FreeformReaction(
                        searchQuery = state.searchQuery,
                        onSelectReaction = onSelectReaction
                    )
                    EmojiGrid(
                        emojis = state.searchResults.results,
                        skinTone = skinTone,
                        selectedEmojis = selectedEmojis,
                        onSelectEmoji = onSelectEmoji,
                    )
                }
                is SearchBarResultState.NoResultsFound -> {
                    FreeformReaction(
                        searchQuery = state.searchQuery,
                        onSelectReaction = onSelectReaction
                    )
                    // No results found, show a message
                    Spacer(Modifier.size(80.dp))

                    Text(
                        text = stringResource(CommonStrings.common_no_results),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiGrid(
    emojis: List<Emoji>,
    selectedEmojis: ImmutableSet<String>,
    onSelectEmoji: (String) -> Unit,
    modifier: Modifier = Modifier,
    skinTone: String? = null,
) {
    val scope = rememberCoroutineScope()
    val emojiSize = 32.dp.toSp()
    val horizontalArrangement = Arrangement.spacedBy(8.dp)
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val cellSize = 48.dp
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = cellSize),
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(emojis, key = { it.unicode }) { item ->
            val content = @Composable {
                val emoji = if (skinTone != null) item.withSkinTone(skinTone)?.unicode ?: item.unicode else item.unicode
                EmojiItem(
                    modifier = Modifier.aspectRatio(1f),
                    emoji = emoji,
                    emojiSize = emojiSize,
                    isSelected = selectedEmojis.contains(emoji),
                    onSelectEmoji = onSelectEmoji,
                    onLongPress = {},
                )
            }

            if (item.skins != null) {
                val scrollState = rememberScrollState()
                val tooltipState = rememberTooltipState(initialIsVisible = false, isPersistent = true)
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ElementTheme.colors.bgSubtleSecondary)
                                .horizontalScroll(scrollState),
                        ) {
                            Row(
                                modifier = Modifier.padding(contentPadding),
                                horizontalArrangement = horizontalArrangement,
                            ) {
                                val variants = listOf(item.unicode) + item.skins!!.map(EmojiSkin::unicode)
                                variants.forEach { variant ->
                                    EmojiItem(
                                        emoji = variant,
                                        emojiSize = emojiSize,
                                        isSelected = selectedEmojis.contains(variant),
                                        onSelectEmoji = {
                                            scope.launch { tooltipState.dismiss() }
                                            onSelectEmoji(it)
                                        },
                                        onLongPress = {
                                            scope.launch { tooltipState.show(MutatePriority.UserInput) }
                                        },
                                    )
                                }
                            }
                        }
                    },
                    state = tooltipState,
                    content = content,
                )
            } else content()
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
            Text(text = "Search for emoji")
        },
        trailingIcon = when {
            query.isNotEmpty() -> {
                {
                    IconButton(
                        onClick = { onQueryChange("") },
                    ) {
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

@Composable
private fun FreeformReaction(
    searchQuery: String,
    onSelectReaction: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val reaction = searchQuery.trim()

        Text(text = "Tap to react with  ")
        MessagesReactionButton(
            content = MessagesReactionsButtonContent.Text(
                text = reaction.ellipsize(MAX_REACTION_LENGTH_CHARS),
                highlight = true,
            ),
            onClick = { onSelectReaction(reaction) },
            onLongClick = {},
        )
    }
    HorizontalDivider(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .fillMaxWidth()
    )
}

@PreviewsDayNight
@Composable
internal fun EmojiPickerPreview() = ElementPreview {
    EmojiPicker(
        onSelectEmoji = {},
        onSelectReaction = {},
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
    val query = "thu"
    EmojiPicker(
        onSelectEmoji = {},
        onSelectReaction = {},
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
    // padded with whitespace to test that it's trimmed
    val query = "   this is a very long string that won't match anything   "
    EmojiPicker(
        onSelectEmoji = {},
        onSelectReaction = {},
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
