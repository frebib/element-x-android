/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */
plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.libraries.sessionstorage.impl.memory"
}

dependencies {
    implementation(projects.libraries.sessionStorage.api)
    implementation(libs.coroutines.core)
}
