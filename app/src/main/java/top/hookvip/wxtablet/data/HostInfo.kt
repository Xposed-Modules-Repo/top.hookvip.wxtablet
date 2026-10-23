package top.hookvip.wxtablet.data

import kotlin.properties.Delegates

object HostInfo {
    var modulePath by Delegates.notNull<String>()
    var appPackageName by Delegates.notNull<String>()
    var appClassLoader by Delegates.notNull<ClassLoader>()
    var appFilePath by Delegates.notNull<String>()
    var isPlay by Delegates.notNull<Boolean>()
    var verName by Delegates.notNull<String>()
    var verCode by Delegates.notNull<Int>()
    var clientVer by Delegates.notNull<String>()
    var canCacheMethods = false

    fun toVerStr(): String {
        return buildString {
            if (isPlay) append("Play")
            append(verName)
            append("($verCode)")
            append("_")
            append(clientVer)
        }
    }
}