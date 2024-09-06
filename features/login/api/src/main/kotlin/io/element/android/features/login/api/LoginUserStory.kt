/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.login.api

import kotlinx.coroutines.flow.StateFlow

interface LoginUserStory {
    val loginFlowIsDone: StateFlow<Boolean>
}
