package top.hookvip.wxtablet.utils

import android.view.View
import com.highcapable.yukihookapi.hook.log.YLog
import org.luckypray.dexkit.DexKitBridge
import top.hookvip.wxtablet.data.DescriptorData
import top.hookvip.wxtablet.data.HostInfo
import top.hookvip.wxtablet.factory.toDexMethod
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.concurrent.thread

object WXConfig {
    private object MethodCheckPadTablet : DescriptorData("WXConfig.MethodCheckPadTablet")
    private object MethodVisibleLoginButton : DescriptorData("WXConfig.MethodVisibleLoginButton")

    private val config by lazy { FastKv("WXMethods") }

    data class Methods(val checkPadTablet: Method?, val visibleLoginButton: Method?)

    fun resolveMethods(): Methods {
        var bridge: DexKitBridge? = null

        fun findMethod(needle: String, returnType: String): String? {
            val dexKit = bridge ?: run {
                YLog.info("start dexkit lookup(${HostInfo.toVerStr()})")
                System.loadLibrary("dexkit")
                DexKitBridge.create(HostInfo.appFilePath).also { bridge = it }
            }
            val methods = dexKit.findMethod {
                matcher {
                    usingStrings(needle)
                    modifiers(Modifier.STATIC)
                    returnType(returnType)
                }
            }
            if (methods.isEmpty()) {
                YLog.warn("No method found for $needle")
                return null
            }
            check(methods.size == 1) { "Expected one method for $needle, found ${methods.size}" }
            return methods.single().descriptor
        }

        fun resolve(
            descriptor: DescriptorData,
            needle: String,
            returnType: String,
            matches: (Method) -> Boolean,
        ): Method? = runCatching {
            val key = descriptor.mKey
            val cached = if (HostInfo.canCacheMethods) runCatching {
                config.getString(key)
            }.onFailure { YLog.warn("Cannot read method cache for $key", it) }.getOrNull() else null

            resolveCachedMethod(
                cachedDescriptor = cached,
                resolve = { it.toDexMethod() },
                matches = matches,
                find = { findMethod(needle, returnType) },
                updateCache = { value ->
                    if (HostInfo.canCacheMethods) runCatching {
                        if (value == null) config.remove(key) else config.putString(key, value)
                    }.onFailure { YLog.warn("Cannot update method cache for $key", it) }
                },
            )
        }.onFailure { YLog.error("Cannot resolve ${descriptor.mKey}", it) }.getOrNull()

        return try {
            Methods(
                checkPadTablet = resolve(
                    MethodCheckPadTablet,
                    "inTabletEnv, no tablet condition matched, return false",
                    "boolean",
                ) { Modifier.isStatic(it.modifiers) && it.returnType == Boolean::class.javaPrimitiveType },
                visibleLoginButton = resolve(MethodVisibleLoginButton, "loginAsOtherDeviceBtn", "void") {
                    Modifier.isStatic(it.modifiers) && it.returnType == Void.TYPE &&
                        it.parameterTypes.firstOrNull()?.let(View::class.java::isAssignableFrom) == true
                },
            )
        } finally {
            bridge?.let { dexKit ->
                // 原生资源释放不能阻塞微信的主线程初始化。
                thread(name = "WeChatTablet-DexKitClose", isDaemon = true) {
                    runCatching { dexKit.close() }
                        .onSuccess { YLog.info("Closed DexKit bridge") }
                        .onFailure { YLog.warn("Cannot close DexKit bridge", it) }
                }
            }
        }
    }
}
