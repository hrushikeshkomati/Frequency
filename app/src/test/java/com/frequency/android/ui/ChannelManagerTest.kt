package com.frequency.android.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChannelManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences("frequency_prefs", Context.MODE_PRIVATE)

    @Before
    fun clearPreferences() {
        preferences.edit().clear().commit()
    }

    @Test
    fun `password verification rejects wrong password and accepts correct password after reload`() {
        val channel = "#frequency-test"
        val setupState = ChatState(scope = TestScope())
        val setupManager = ChannelManager(
            setupState,
            MessageManager(setupState),
            DataManager(context),
            TestScope()
        )
        setupManager.setChannelPassword(channel, "correct horse")

        val restoredState = ChatState(scope = TestScope())
        restoredState.setPasswordProtectedChannels(setOf(channel))
        val restoredManager = ChannelManager(
            restoredState,
            MessageManager(restoredState),
            DataManager(context),
            TestScope()
        )

        assertFalse(restoredManager.joinChannel(channel, "wrong password", "peer"))
        assertTrue(restoredManager.joinChannel(channel, "correct horse", "peer"))
        assertTrue(restoredManager.hasChannelKey(channel))
    }

    @Test
    fun `successful password verification switches to an already joined channel`() {
        val channel = "#frequency-test"
        val dataManager = DataManager(context)
        val setupState = ChatState(scope = TestScope())
        val setupManager = ChannelManager(
            setupState,
            MessageManager(setupState),
            dataManager,
            TestScope()
        )
        setupManager.setChannelPassword(channel, "correct horse")

        val restoredState = ChatState(scope = TestScope())
        restoredState.setJoinedChannels(setOf(channel))
        restoredState.setPasswordProtectedChannels(setOf(channel))
        val restoredManager = ChannelManager(
            restoredState,
            MessageManager(restoredState),
            dataManager,
            TestScope()
        )

        assertTrue(restoredManager.joinChannel(channel, "correct horse", "peer"))
        assertTrue(restoredManager.hasChannelKey(channel))
        assertTrue(restoredState.getCurrentChannelValue() == channel)
    }
}
