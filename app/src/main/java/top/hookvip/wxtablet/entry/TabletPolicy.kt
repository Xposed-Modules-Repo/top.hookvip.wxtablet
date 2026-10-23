package top.hookvip.wxtablet.entry

internal fun isChatCall(stackTrace: Array<StackTraceElement>): Boolean =
    stackTrace.any { it.className.contains("com.tencent.mm.pluginsdk.ui.chat") }
