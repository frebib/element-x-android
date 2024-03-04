/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.messages.impl.timeline.components.customreaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.emojibasebindings.Emoji
import io.element.android.emojibasebindings.EmojibaseCategory
import io.element.android.emojibasebindings.EmojibaseDatasource
import io.element.android.emojibasebindings.EmojibaseStore
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toSp
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.SearchBar
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    onEmojiSelected: (Emoji) -> Unit,
    emojibaseStore: EmojibaseStore,
    selectedEmojis: ImmutableSet<String>,
    state: EmojiPickerState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val categories = remember { emojibaseStore.categories }
    val pagerState = rememberPagerState(pageCount = { EmojibaseCategory.entries.size })

    Column(modifier) {
        EmojiPickerSearchBar(
            query = state.searchQuery,
            state = state.searchResults,
            active = state.isSearchActive,
            selectedEmojis = selectedEmojis,
            onActiveChanged = { state.eventSink(EmojiPickerEvents.OnSearchActiveChanged(it)) },
            onQueryChange = { state.eventSink(EmojiPickerEvents.UpdateSearchQuery(it)) },
            onEmojiSelected = {
                state.eventSink(EmojiPickerEvents.OnSearchActiveChanged(false))
                onEmojiSelected(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (!state.isSearchActive) {
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
                EmojiGrid(emojis = emojis, selectedEmojis = selectedEmojis, onEmojiSelected = onEmojiSelected)
            }
        }
    }
}

@Composable
private fun EmojiGrid(
    emojis: List<Emoji>,
    selectedEmojis: ImmutableSet<String>,
    onEmojiSelected: (Emoji) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
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
                onEmojiSelected = onEmojiSelected,
                emojiSize = 32.dp.toSp(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSearchBar(
    query: String,
    state: SearchBarResultState<ImmutableList<Emoji>>,
    active: Boolean, selectedEmojis: ImmutableSet<String>,
    onActiveChanged: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onEmojiSelected: (Emoji) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        active = active,
        onActiveChange = onActiveChanged,
        placeHolderTitle = stringResource(CommonStrings.common_search_for_emoji),
        resultState = state,
        resultHandler = { results ->
            EmojiGrid(emojis = results, selectedEmojis = selectedEmojis, onEmojiSelected = onEmojiSelected)
        },
        modifier = modifier,
    )
}

@PreviewsDayNight
@Composable
internal fun EmojiPickerPreview() = ElementPreview {
    EmojiPicker(
        onEmojiSelected = {},
        emojibaseStore = EmojibaseDatasource().load(LocalContext.current),
        selectedEmojis = persistentSetOf("😀", "😄", "😃"),
        state = EmojiPickerState(false, "", SearchBarResultState.Results(listOf()), {}),
        modifier = Modifier.fillMaxWidth(),
    )
}
