/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.preferences.DropdownOption
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import kotlinx.collections.immutable.ImmutableList

data class AdvancedSettingsState(
    val isDeveloperModeEnabled: Boolean,
    val isSharePresenceEnabled: Boolean,
    val mediaOptimizationState: MediaOptimizationState?,
    val theme: ThemeOption,
    val availableThemeOptions: ImmutableList<ThemeOption>,
    val skinTone: SkinToneOption,
    val mediaPreviewConfigState: MediaPreviewConfigState,
    val liveLocationMinimumDistanceUpdate: Int?,
    val eventSink: (AdvancedSettingsEvent) -> Unit
)

sealed interface MediaOptimizationState {
    data class AllMedia(val isEnabled: Boolean) : MediaOptimizationState
    data class Split(
        val compressImages: Boolean,
        val videoPreset: VideoCompressionPreset,
    ) : MediaOptimizationState

    val shouldCompressImages: Boolean get() = when (this) {
        is AllMedia -> isEnabled
        is Split -> compressImages
    }
}

enum class ThemeOption : DropdownOption {
    System {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(R.string.theme_system)
    },

    Light {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(R.string.theme_light)
    },

    Dark {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(R.string.theme_dark)
    },

    Black {
        @Composable
        @ReadOnlyComposable
        override fun getText(): String = stringResource(R.string.theme_black)
    }
}

enum class SkinToneOption(val unicode: String) : DropdownOption {
    None("") {
        @Composable
        override fun getText(): String = "${preview()} No modifier"
    },
    Light("🏻") {
        @Composable
        override fun getText(): String = "${preview()} Light skin tone"
    },
    MediumLight("🏼") {
        @Composable
        override fun getText(): String = "${preview()} Medium-Light skin tone"
    },
    Medium("🏽") {
        @Composable
        override fun getText(): String = "${preview()} Medium skin tone"
    },
    MediumDark("🏾") {
        @Composable
        override fun getText(): String = "${preview()} Medium-Dark skin tone"
    },
    Dark("🏿") {
        @Composable
        override fun getText(): String = "${preview()} Dark skin tone"
    };

    fun preview(): String = "👋$unicode 🧑$unicode 🏃$unicode"

    @Composable
    override fun getSummary(): String? = "👋$unicode"

    companion object {
        fun fromUnicode(value: String?): SkinToneOption =
            when (value) {
                "🏻" -> Light
                "🏼" -> MediumLight
                "🏽" -> Medium
                "🏾" -> MediumDark
                "🏿" -> Dark
                else -> None
            }
    }
}
