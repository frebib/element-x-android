/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.preferences.impl.advanced

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.compound.theme.Theme

open class AdvancedSettingsStateProvider : PreviewParameterProvider<AdvancedSettingsState> {
    override val values: Sequence<AdvancedSettingsState>
        get() = sequenceOf(
            aAdvancedSettingsState(),
            aAdvancedSettingsState(isDeveloperModeEnabled = true),
            aAdvancedSettingsState(showChangeThemeDialog = true),
            aAdvancedSettingsState(isSendPublicReadReceiptsEnabled = true),
            aAdvancedSettingsState(showChangeSkinToneDialog = true),
            aAdvancedSettingsState(skinTone = "🏽"),
        )
}

fun aAdvancedSettingsState(
    isDeveloperModeEnabled: Boolean = false,
    isSendPublicReadReceiptsEnabled: Boolean = false,
    isReactionPickerSearchEnabled: Boolean = false,
    showChangeThemeDialog: Boolean = false,
    skinTone: String? = null,
    showChangeSkinToneDialog: Boolean = false,
    eventSink: (AdvancedSettingsEvents) -> Unit = {},
) = AdvancedSettingsState(
    isDeveloperModeEnabled = isDeveloperModeEnabled,
    isSharePresenceEnabled = isSendPublicReadReceiptsEnabled,
    isReactionPickerSearchEnabled = isReactionPickerSearchEnabled,
    theme = Theme.System,
    showChangeThemeDialog = showChangeThemeDialog,
    skinTone = skinTone,
    showChangeSkinToneDialog = showChangeSkinToneDialog,
    eventSink = eventSink
)
