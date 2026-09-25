package top.hookvip.wxtablet.utils

import com.highcapable.yukihookapi.hook.log.YLog
import org.luckypray.dexkit.DexKitBridge
import top.hookvip.wxtablet.data.DescriptorData
import top.hookvip.wxtablet.data.HostInfo
import top.hookvip.wxtablet.factory.toDexMethod
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object WXConfig {
    /** System property WeChat reads to detect a foldable screen. */
    private const val FOLDABLE_SCREEN_PROPERTY = "ro.os_foldable_screen_support"

    /** Device names older WeChat builds also accepted as foldable. */
    private const val ROYOLE_DEVICE = "royole"
    private const val TECNO_DEVICE = "tecno"

    /**
     * 8.0.78 moved the device-name checks out of the pad-mode gate.
     * 8.0.76 keeps them together in com.tencent.mm.ui.bk.Q();
     * 8.0.78 leaves only the system property in com.tencent.mm.ui.gk.h0().
     */
    private const val PAD_UI_SPLIT_VERSION = "8.0.78"

    private object MethodUnLockPadModeUI : DescriptorData("WXConfig.MethodUnLockPadModeUI")
    private object MethodCheckIsPadMode : DescriptorData("WXConfig.MethodCheckIsPadMode")
    private object MethodVisibleLoginButton : DescriptorData("WXConfig.MethodVisibleLoginButton")

    private val config = FastKv("WXConfig")
    private val bridge by lazy {
        YLog.warn("start dexkit find config apply to cache(${HostInfo.toVerStr()})")
        System.loadLibrary("dexkit");
        DexKitBridge.create(HostInfo.appFilePath)
    }

    val unlockPadModeUi: Method?
        get() = config.getStringPut(MethodUnLockPadModeUI.mKey) {
            bridge.findMethod {
                searchPackages("com.tencent.mm.ui")
                matcher {
                    modifiers(Modifier.PUBLIC or Modifier.STATIC)
                    paramCount(0)
                    returnType("boolean")
                    if (HostInfo.isAtLeast(PAD_UI_SPLIT_VERSION)) {
                        usingStrings(FOLDABLE_SCREEN_PROPERTY)
                    } else {
                        usingStrings(ROYOLE_DEVICE, TECNO_DEVICE, FOLDABLE_SCREEN_PROPERTY)
                    }
                }
            }.single().descriptor
        }?.toDexMethod()

    val checkIsPadMode: Method?
        get() = config.getStringPut(MethodCheckIsPadMode.mKey) {
            bridge.findMethod {
                matcher {
                    paramCount(3)
                    usingStrings("MicroMsg.CgiCheckLoginAsPad", "/cgi-bin/micromsg-bin/checkloginaspad")
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