package top.hookvip.wxtablet.entry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletPolicyTest {
    @Test
    fun chatCallerKeepsPhoneLayout() {
        assertTrue(isChatCall(arrayOf(
            frame("com.tencent.mm.ui.UIUtils"),
            frame("com.tencent.mm.pluginsdk.ui.chat.ChatFooter"),
        )))
    }

    @Test
    fun loginCallerUsesTabletMode() {
        assertFalse(isChatCall(arrayOf(frame("com.tencent.mm.plugin.account.ui.LoginHistoryUI"))))
    }

    @Test
    fun fileNameDoesNotMistakenlyDisableTabletMode() {
        val frame = StackTraceElement(
            "com.tencent.mm.plugin.account.ui.LoginHistoryUI",
            "initView",
            "com.tencent.mm.pluginsdk.ui.chat.kt",
            1,
        )
        assertFalse(isChatCall(arrayOf(frame)))
    }

    private fun frame(className: String) = StackTraceElement(className, "update", "WeChat.kt", 1)
}
