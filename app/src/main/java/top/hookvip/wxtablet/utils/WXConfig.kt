package top.hookvip.wxtablet.utils

import com.highcapable.yukihookapi.hook.log.YLog
import org.luckypray.dexkit.DexKitBridge
import top.hookvip.wxtablet.data.DescriptorData
import top.hookvip.wxtablet.data.HostInfo
import top.hookvip.wxtablet.factory.toDexMethod
import java.lang.reflect.Method

object WXConfig {
    private object MethodCheckPadTablet : DescriptorData("WXConfig.MethodCheckPadTablet")
    private object MethodVisibleLoginButton : DescriptorData("WXConfig.MethodVisibleLoginButton")

    private val config = FastKv("WXConfig")
    private val bridge by lazy {
        YLog.warn("start dexkit find config apply to cache(${HostInfo.toVerStr()})")
        System.loadLibrary("dexkit");
        DexKitBridge.create(HostInfo.appFilePath)
    }

    val checkPadTablet: Method?
        get() = config.getStringPut(MethodCheckPadTablet.mKey) {
            bridge.findMethod {
                matcher {
                    usingStrings("inTabletEnv, no tablet condition matched, return false")
                }
            }.single().descriptor
        }?.toDexMethod()

    val visibleLoginButton: Method?
        get() = config.getStringPut(MethodVisibleLoginButton.mKey) {
            bridge.findMethod {
                matcher {
                    usingStrings("loginAsOtherDeviceBtn")
                }
            }.singleOrNull()?.descriptor
        }?.toDexMethod()
}
