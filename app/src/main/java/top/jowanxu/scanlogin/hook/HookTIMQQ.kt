package top.jowanxu.scanlogin.hook

import android.app.Activity
import android.os.Bundle
import android.os.Message
import android.widget.Button
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.jowanxu.scanlogin.Constant
import top.jowanxu.scanlogin.Constant.ANDROID_WIDGET_BUTTON
import top.jowanxu.scanlogin.Constant.COM_TENCENT_QQ
import top.jowanxu.scanlogin.Constant.COM_TENCENT_TIM
import top.jowanxu.scanlogin.Constant.CONTAIN_TEXT
import top.jowanxu.scanlogin.Constant.HANDLE_MESSAGE
import top.jowanxu.scanlogin.getPreferenceBoolean
import top.jowanxu.scanlogin.tryHook

class HookTIMQQ {
    companion object {
        private val TAG = HookTIMQQ::class.java.simpleName
        private const val QR_CODE_HOOK_CLASS_NAME = "com.tencent.biz.qrcode.activity.QRLoginActivity"
        private const val DO_ON_CREATE = "doOnCreate"

        /**
         * 判断doOnCreate
         */
        private var count = 0
    }

    /**
     * 扫一扫电脑端二维码后，自动点击允许登录TIM/QQ按钮
     * @param lpParam LoadPackageParam
     */
    fun autoConfirmQQLogin(lpParam: XC_LoadPackage.LoadPackageParam) {
        // 获取Class
        val aClass = XposedHelpers.findClassIfExists(QR_CODE_HOOK_CLASS_NAME, lpParam.classLoader) ?: return
        // 获取Class里面的Field
        val declaredFields = aClass.declaredFields ?: return
        tryHook(TAG, Constant.HOOK_ERROR) {
            // Hook指定方法
            XposedHelpers.findAndHookMethod(aClass, DO_ON_CREATE, Bundle::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 每次打开都会调用两次doOnCreate
                    if (count == 1) {
                        return
                    }
                    // 调用doOnCreate超过两次后，也就是第二次打开扫一扫后就设置为0
                    if (count > 1) {
                        count = 0
                    }
                    val activity = param.thisObject as Activity
                    val enable = getPreferenceBoolean(activity,
                            Constant.PREFERENCE_BOOLEAN,
                            Constant.TIM_QQ_ENABLE, true)
                    if (!enable) {
                        return
                    }
                    val resultStr = lpParam.packageName.getHookName(
                            activity.packageManager.getPackageInfo(lpParam.packageName, 0).versionName)
                    declaredFields.filter {
                        it.type.canonicalName.toString() == (ANDROID_WIDGET_BUTTON)
                    }.forEach {
                        val handlerClass = XposedHelpers.findClassIfExists(resultStr, lpParam.classLoader) ?: return
                        // 设置true
                        it.isAccessible = true
                        // 获取值
                        val loginButton = it.get(param.thisObject) as Button
                        tryHook(TAG, Constant.HOOK_ERROR) {
                            // Hook方法，对handleMessage方法调用后，进行判断Button的Text进行判断，并且自动调用点击方法
                            XposedHelpers.findAndHookMethod(handlerClass, HANDLE_MESSAGE, Message::class.java, object : XC_MethodHook() {
                                @Throws(Throwable::class)
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    // 当Button的Text为允许登录TIM/允许登录QQ的时候才实现点击
                                    if (loginButton.text.toString().contains(CONTAIN_TEXT)) {
                                        if (count == 0) {
                                            loginButton.performClick()
                                        }
                                        // 每次都增加
                                        count++
                                    }
                                }
                            })
                        }
                    }
                }
            })
        }
    }

    /**
     * 根据包名和版本号获取需要Hook的类名
     *
     * @param packageName 包名
     * *
     * @param versionName 版本号
     * *
     * @return 类名
     */
    private fun String.getHookName(versionName: String): String = when (this) {
        COM_TENCENT_TIM -> getTIMHookName(versionName)
        COM_TENCENT_QQ -> getQQHookName(versionName)
        else -> getQQHookName(versionName)
    }

    /**
     * 根据版本号获取TIM需要Hook的类名
     *
     * @param versionName 版本号
     * *
     * @return 类名
     */
    private fun getTIMHookName(versionName: String): String = when (versionName) {
        "2.0.1" -> "hxr"
        "2.0.0" -> "hxq"
        "1.2.0" -> "hzq"
        "1.1.5" -> "ghk"
        "1.1.0" -> "giy"
        "1.0.5" -> "gjd"
        "1.0.4" -> "gir"
        "1.0.0" -> "gik"
        else -> "hxr"
    }

    /**
     * 根据版本号获取QQ需要Hook的类名
     *
     * @param versionName 版本号
     * *
     * @return 类名
     */
    private fun getQQHookName(versionName: String): String = when (versionName) {
        "7.2.5" -> "nea"
        "7.2.0" -> "myi"
        "7.1.8" -> "mco"
        "7.1.5" -> "mcf"
        "7.1.0" -> "lri"
        "7.0.0" -> "lhi"
        else -> "nea"
    }

}