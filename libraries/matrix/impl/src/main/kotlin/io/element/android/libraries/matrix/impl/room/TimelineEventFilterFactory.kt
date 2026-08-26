/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.room

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.matrix.api.room.StateEventType
import io.element.android.libraries.matrix.api.room.join.JoinRule
import org.matrix.rustcomponents.sdk.FilterTimelineEventCondition
import org.matrix.rustcomponents.sdk.FilterTimelineEventType
import org.matrix.rustcomponents.sdk.TimelineEventFilter
import uniffi.matrix_sdk_ui.MembershipChangeFilter

interface TimelineEventFilterFactory {
    fun create(
        joinRule: JoinRule?,
        isEncrypted: Boolean?,
        excludedStateTypes: List<StateEventType>
    ): TimelineEventFilter?
}

@ContributesBinding(AppScope::class)
class RustTimelineEventFilterFactory : TimelineEventFilterFactory {
    override fun create(
        joinRule: JoinRule?,
        isEncrypted: Boolean?,
        excludedStateTypes: List<StateEventType>
    ): TimelineEventFilter? {
        val excludedEventTypes = excludedStateTypes.map {
            FilterTimelineEventCondition.EventType(FilterTimelineEventType.State(it.map()))
        }
        return if (excludedEventTypes.isNotEmpty()) {
            TimelineEventFilter.exclude(excludedEventTypes)
        } else {
            null
        }
    }
}
