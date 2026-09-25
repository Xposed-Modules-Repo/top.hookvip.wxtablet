package top.hookvip.wxtablet.entry

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import androidx.core.view.isGone
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import top.hookvip.wxtablet.data.HostInfo
import top.hookvip.wxtablet.factory.toAppClass
import top.hookvip.wxtablet.utils.FastKv
import top.hookvip.wxtablet.utils.WXConfig

object TabletHooker : YukiBaseHooker() {

    override fun onHook() {
        if (isWeChat() && doHostInit(this)) {

            YLog.warn("start hook wechat tablet(${HostInfo.toVerStr()})")

            "com.tencent.tinker.loader.app.TinkerApplication".toClass().method {
                name("onBaseContextAttached")
                param(Context::class.java, Long::class.java, Long::class.java)
            }.hook {
                after {
                    val tinkerApplication = instance
                    val classloader = tinkerApplication.javaClass.method { name = "getClassLoader";superClass() }.get(tinkerApplication).invoke<ClassLoader>()!!
                    HostInfo.appClassLoader = classloader

                    YLog.warn("TinkerLoader.tryLoad -> classloader = $classloader")
                    YLog.warn("TinkerLoader.tryLoad -> instanceClassloader = ${instance.javaClass.classLoader}")

                    val intent = tinkerApplication.javaClass.field { name("tinkerResultIntent");superClass() }.get(tinkerApplication).cast<Intent>()
                    intent?.extras?.let {
                        for (key in it.keySet()) {
                            YLog.warn("TinkerLoader.tryLoad -> ResultIntent($key = ${it.get(key)})")
                        }
                    }

                    WXConfig.apply {
                        unlockPadModeUi?.hook {
                            before {
                                result = true
                            }
                        }
                        checkIsPadMode?.hook {
                            before {
                                result = true
                            }
                        }
                        visibleLoginButton?.hook {
                            before {
                                args(0).cast<Button>()?.let { loginButton ->
                                    if (loginButton.isGone) {
                                        loginButton.visibility = View.VISIBLE
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isWeChat(): Boolean {
        return "com.tencent.mm.app.Application".toClassOrNull(appClassLoader) != null
    }

    private fun doHostInit(packageParam: PackageParam): Boolean {
        FastKv.initialize("${appInfo.dataDir}/files/WeChatTablet/")
        HostInfo.apply {
            modulePath = packageParam.moduleAppFilePath
            appPackageName = packageParam.packageName
            appClassLoader = packageParam.appClassLoader!!
            appFilePath = packageParam.appInfo.sourceDir
            val buildConfigClass = "com.tencent.mm.boot.BuildConfig".toAppClass()
            isPlay = buildConfigClass.field { name = "BUILD_TAG" }.get().string().contains("_GP_")
            verName = buildConfigClass.field { name = "VERSION_NAME" }.get().string()
            verCode = buildConfigClass.field { name = "VERSION_CODE" }.get().int()
            clientVer = buildConfigClass.field { name = "CLIENT_VERSION_ARM64" }.get().string()
        }
        YLog.warn("unlockPadModeUi = ${WXConfig.unlockPadModeUi}")
        YLog.warn("checkIsPadMode = ${WXConfig.checkIsPadMode}")
        YLog.warn("visibleLoginButton = ${WXConfig.visibleLoginButton}")
        return true
    }
}
