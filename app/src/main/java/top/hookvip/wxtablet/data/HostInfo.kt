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

    fun toVerStr(): String {
        return buildString {
            if (isPlay) append("Play")
            append(verName)
            append("($verCode)")
            append("_")
            append(clientVer)
        }
    }

    /** Compares [verName] to a dotted version such as 8.0.78. Unparseable names are older. */
    fun isAtLeast(versionName: String): Boolean {
        val current = versionNumbers(verName)
        val target = versionNumbers(versionName)
        if (current.size < 3 || target.size < 3) return false
        for (index in 0 until 3) {
            if (current[index] != target[index]) return current[index] > target[index]
        }
        return true
    }

    private fun versionNumbers(versionName: String): List<Int> {
        return versionName.split('.').map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: return emptyList()
        }
    }
}