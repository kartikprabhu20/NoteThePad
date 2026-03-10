package com.mintanable.notethepad

import com.mintanable.notethepad.feature_note.domain.util.DispatcherProvider
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class TestDispatcherProvider(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : DispatcherProvider {
    override val main = testDispatcher
    override val io = testDispatcher
    override val default = testDispatcher
    override val unconfined = testDispatcher
}