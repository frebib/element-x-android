/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.login.impl.screens.loginpassword

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.libraries.di.AppScope

@ContributesNode(AppScope::class)
class LoginPasswordNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: LoginPasswordPresenter,
) : Node(buildContext, plugins = plugins) {
    interface Callback : Plugin {
        fun onWaitListError(loginFormState: LoginFormState)
    }

    private fun onWaitListError(loginFormState: LoginFormState) {
        plugins<Callback>().forEach { it.onWaitListError(loginFormState) }
    }

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        LoginPasswordView(
            state = state,
            modifier = modifier,
            onBackClick = ::navigateUp,
            onWaitListError = ::onWaitListError,
        )
    }
}
