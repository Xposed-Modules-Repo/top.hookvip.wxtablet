package top.hookvip.wxtablet.entry

import android.app.Application
import android.content.Context
import android.view.View
import androidx.core.view.isGone
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import top.hookvip.wxtablet.data.HostInfo
import top.hookvip.wxtablet.factory.toAppClassOrNull
import top.hookvip.wxtablet.utils.FastKv
import top.hookvip.wxtablet.utils.WXConfig
import java.lang.reflect.Method

object TabletHooker : YukiBaseHooker() {
    private val hookedMethods = mutableSetOf<Method>()

    override fun onHook() {
        if (!isWeChat()) return

        runCatching {
            doHostInit(this)
            YLog.info("start hook wechat tablet(${HostInfo.toVerStr()})")

            val bootstrap = "com.tencent.tinker.loader.app.TinkerApplication".toClass().method {
                name("onBaseContextAttached")
                param(Context::class.java, Long::class.java, Long::class.java)
            }.give() ?: error("TinkerApplication.onBaseContextAttached not found")

            hookMethod(bootstrap, "bootstrap") { param ->
                HostInfo.appClassLoader = (param.thisObject as Application).classLoader
                installTabletHooks()
            }
        }.onFailure { YLog.error("Cannot initialize WeChatTablet", it) }
    }

    private fun installTabletHooks() {
        val methods = WXConfig.resolveMethods()
        YLog.info("Resolved tablet methods: check=${methods.checkPadTablet != null}, login=${methods.visibleLoginButton != null}")
        methods.checkPadTablet?.let { method ->
            hookMethod(method, "tablet check") { param ->
                param.result = !isChatCall(Throwable().stackTrace)
            }
        } ?: YLog.error("Tablet check hook unavailable")

        methods.visibleLoginButton?.let { method ->
            hookMethod(method, "login button", before = true) { param ->
                (param.args.firstOrNull() as? View)?.let { loginButton ->
                    if (loginButton.isGone) loginButton.visibility = View.VISIBLE
                }
            }
        } ?: YLog.warn("Login button hook unavailable")
    }

    private fun hookMethod(
        target: Method,
        label: String,
        before: Boolean = false,
        callback: (XC_MethodHook.MethodHookParam) -> Unit,
    ) = synchronized(hookedMethods) {
        if (target in hookedMethods) return@synchronized
        runCatching {
            // 初始化 Hook 必须同步安装，避免错过 onBaseContextAttached。
            XposedBridge.hookMethod(target, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (before) invokeSafely(param)
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!before) invokeSafely(param)
                }

                private fun invokeSafely(param: MethodHookParam) {
                    runCatching { callback(param) }
                        .onFailure { YLog.error("$label hook callback failed", it) }
                }
            })
            hookedMethods.add(target)
            YLog.info("Installed $label hook: $target")
        }.onFailure { YLog.error("$label hook installation failed", it) }
        Unit
    }

    private fun isWeChat(): Boolean {
        return "com.tencent.mm.app.Application".toClassOrNull(appClassLoader) != null
    }

    private fun doHostInit(packageParam: PackageParam) {
        FastKv.initialize("${appInfo.dataDir}/files/WeChatTablet/")
        HostInfo.apply {
            modulePath = packageParam.moduleAppFilePath
            appPackageName = packageParam.packageName
            appClassLoader = requireNotNull(packageParam.appClassLoader)
            appFilePath = packageParam.appInfo.sourceDir
            val buildConfigClass = "com.tencent.mm.boot.BuildConfig".toAppClassOrNull()
            fun readField(name: String): Any? = runCatching {
                buildConfigClass?.getDeclaredField(name)?.apply { isAccessible = true }?.get(null)
            }.getOrNull()

            val buildTag = readField("BUILD_TAG") as? String
            isPlay = buildTag?.contains("_GP_") == true
            verName = readField("VERSION_NAME") as? String ?: ""
            verCode = (readField("VERSION_CODE") as? Number)?.toInt() ?: 0
            clientVer = readField("CLIENT_VERSION_ARM64") as? String ?: ""
            canCacheMethods = buildTag != null && verName.isNotEmpty() && verCode > 0 && clientVer.isNotEmpty()
            if (!canCacheMethods) YLog.warn("Host build metadata incomplete; method cache disabled")
        }
    }
}
