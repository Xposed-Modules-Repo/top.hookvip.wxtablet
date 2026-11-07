package top.hookvip.wxtablet.utils

import android.view.View
import dalvik.system.BaseDexClassLoader
import com.highcapable.yukihookapi.hook.log.YLog
import org.luckypray.dexkit.DexKitBridge
import top.hookvip.wxtablet.data.HostInfo
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object WXConfig {
    // 从当前加载的代码查找。
    fun find(loader: ClassLoader): Pair<Set<Method>, Set<Method>> {
        YLog.warn("start dexkit find config(${HostInfo.toVerStr()})")
        System.loadLibrary("dexkit")
        val tabletChecks = linkedSetOf<Method>()
        val loginButtons = linkedSetOf<Method>()
        var current: ClassLoader? = loader
        while (current != null) {
            val scannedLoader = current
            if (scannedLoader is BaseDexClassLoader) {
                DexKitBridge.create(scannedLoader, false).use { bridge ->
                    bridge.findMethod {
                        matcher {
                            usingStrings("inTabletEnv, no tablet condition matched, return false")
                            returnType = "boolean"
                        }
                    }.forEach { target ->
                        val method = runCatching { target.getMethodInstance(loader) }.getOrNull()
                        if (method != null && method.declaringClass.classLoader === scannedLoader &&
                            Modifier.isStatic(method.modifiers)) {
                            tabletChecks.add(method)
                            // 一并处理同类的无参入口。
                            target.callers.forEach { caller ->
                                val entry = runCatching { caller.getMethodInstance(loader) }.getOrNull()
                                if (entry != null && isTabletEntry(entry, method.declaringClass)) {
                                    tabletChecks.add(entry)
                                }
                            }
                        }
                    }
                    bridge.findMethod {
                        matcher { usingStrings("loginAsOtherDeviceBtn") }
                    }.forEach { target ->
                        val method = runCatching { target.getMethodInstance(loader) }.getOrNull()
                        if (method != null && method.declaringClass.classLoader === scannedLoader &&
                            method.parameterTypes.firstOrNull()?.let {
                                View::class.java.isAssignableFrom(it)
                            } == true) {
                            loginButtons.add(method)
                        }
                    }
                }
            }
            current = scannedLoader.parent
        }
        check(tabletChecks.isNotEmpty()) { "No compatible WeChat tablet check found in loaded code" }
        return tabletChecks to loginButtons
    }

    internal fun isTabletEntry(method: Method, owner: Class<*>): Boolean =
        method.declaringClass == owner && Modifier.isStatic(method.modifiers) &&
            method.returnType == Boolean::class.javaPrimitiveType && method.parameterTypes.isEmpty()

    internal fun tabletResult(trace: Array<StackTraceElement>): Boolean =
        trace.none { it.className.contains("com.tencent.mm.pluginsdk.ui.chat") }
}
