/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.userprofile.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.features.createroom.api.StartDMAction
import io.element.android.features.userprofile.shared.UserProfileEvents
import io.element.android.features.userprofile.shared.UserProfilePresenterHelper
import io.element.android.features.userprofile.shared.UserProfileState
import io.element.android.features.userprofile.shared.UserProfileState.ConfirmationDialog
import io.element.android.libraries.androidutils.clipboard.ClipboardHelper
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.bool.orFalse
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class UserProfilePresenter @AssistedInject constructor(
    @Assisted private val userId: UserId,
    private val client: MatrixClient,
    private val startDMAction: StartDMAction,
    private val dispatchers: CoroutineDispatchers,
    private val clipboardHelper: ClipboardHelper,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<UserProfileState> {
    interface Factory {
        fun create(userId: UserId): UserProfilePresenter
    }

    private val userProfilePresenterHelper = UserProfilePresenterHelper(
        userId = userId,
        client = client,
    )

    @Composable
    override fun present(): UserProfileState {
        val coroutineScope = rememberCoroutineScope()
        var confirmationDialog by remember { mutableStateOf<ConfirmationDialog?>(null) }
        var userProfile by remember { mutableStateOf<MatrixUser?>(null) }
        val startDmActionState: MutableState<AsyncAction<RoomId>> = remember { mutableStateOf(AsyncAction.Uninitialized) }
        val isBlocked: MutableState<AsyncData<Boolean>> = remember { mutableStateOf(AsyncData.Uninitialized) }
        val dmRoomId by userProfilePresenterHelper.getDmRoomId()
        val canCall by userProfilePresenterHelper.getCanCall(dmRoomId)
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        LaunchedEffect(Unit) {
            client.ignoredUsersFlow
                .map { ignoredUsers -> userId in ignoredUsers }
                .distinctUntilChanged()
                .onEach { isBlocked.value = AsyncData.Success(it) }
                .launchIn(this)
        }
        LaunchedEffect(Unit) {
            userProfile = client.getProfile(userId).getOrNull()
        }

        fun handleEvents(event: UserProfileEvents) {
            when (event) {
                is UserProfileEvents.BlockUser -> {
                    if (event.needsConfirmation) {
                        confirmationDialog = ConfirmationDialog.Block
                    } else {
                        confirmationDialog = null
                        userProfilePresenterHelper.blockUser(coroutineScope, isBlocked)
                    }
                }
                is UserProfileEvents.UnblockUser -> {
                    if (event.needsConfirmation) {
                        confirmationDialog = ConfirmationDialog.Unblock
                    } else {
                        confirmationDialog = null
                        userProfilePresenterHelper.unblockUser(coroutineScope, isBlocked)
                    }
                }
                UserProfileEvents.ClearConfirmationDialog -> confirmationDialog = null
                UserProfileEvents.ClearBlockUserError -> {
                    isBlocked.value = AsyncData.Success(isBlocked.value.dataOrNull().orFalse())
                }
                UserProfileEvents.StartDM -> {
                    coroutineScope.launch {
                        startDMAction.execute(userId, startDmActionState)
                    }
                }
                UserProfileEvents.ClearStartDMState -> {
                    startDmActionState.value = AsyncAction.Uninitialized
                }
                is UserProfileEvents.CopyID -> {
                    coroutineScope.launch(dispatchers.io) {
                        clipboardHelper.copyPlainText(event.text)
                        snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_copied_to_clipboard))
                    }
                }
            }
        }

        return UserProfileState(
            userId = userId,
            userName = userProfile?.displayName,
            avatarUrl = userProfile?.avatarUrl,
            isBlocked = isBlocked.value,
            startDmActionState = startDmActionState.value,
            displayConfirmationDialog = confirmationDialog,
            isCurrentUser = client.isMe(userId),
            dmRoomId = dmRoomId,
            canCall = canCall,
            snackbarMessage = snackbarMessage,
            eventSink = ::handleEvents,
        )
    }
}
