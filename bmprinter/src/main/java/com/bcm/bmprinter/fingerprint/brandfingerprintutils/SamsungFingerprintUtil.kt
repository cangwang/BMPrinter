package com.bcm.bmprinter.fingerprint.brandfingerprintutils

import android.content.Context
import android.os.Build
import android.util.Log
import com.bcm.bmprinter.fingerprint.*
import com.samsung.android.sdk.pass.Spass
import com.samsung.android.sdk.pass.SpassFingerprint

/**
 * Samsung指纹识别模块，Android M+依赖于AOSP，Android 4.2-5.1通过三星SDK进行支持
 * https://developer.samsung.com/galaxy/pass sdk文档下载
 * Created cangwang & by Kin on 2018/9/3
 */
class SamsungFingerprintUtil private constructor(context: Context) : IFingerprintUtil {
    companion object {
        private var instance: SamsungFingerprintUtil? = null

        fun getInstance(context: Context): SamsungFingerprintUtil {
            if (instance == null) {
                instance = SamsungFingerprintUtil(context)
            }
            Log.d(instance?.TAG, "Return Samsung fingerprint util.")
            return instance!!
        }
    }

    private val TAG = "SamsungFingerprintUtil"

    private lateinit var mPass: Spass
    private lateinit var sPassFingerprint: SpassFingerprint
    private lateinit var aospFingerprintHelper: AospFingerprintHelper
    private var passInitialized = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper = AospFingerprintHelper(context)
        } else {
            mPass = Spass()
            try {
                mPass.initialize(context)
                if (mPass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT)) {
                    sPassFingerprint = SpassFingerprint(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun canUseFingerprint(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->aospFingerprintHelper.isHardwareDetected()
            passInitialized -> mPass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT)
            else -> false
        }
    }

    override fun isDeviceSecure(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.isKeyguardSecure()
        } else {
            true
        }
    }

    override fun hasEnrolledFingerprints(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.hasEnrolledFingerprints()
        } else {
            sPassFingerprint.hasRegisteredFinger()
        }
    }

    override fun destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.stopAuthenticate()
        } else {
            sPassFingerprint.cancelIdentify()
        }
    }

    override fun authenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.startAuthenticate(authenticateResult)
        } else {
            sPassFingerprint.startIdentify(object : SpassFingerprint.IdentifyListener {
                override fun onFinished(p0: Int) {
                    when (p0) {
                        SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS, SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS -> { //识别成功或密码成功
                            authenticateResult(true, AUTHENTICATE_SUCCESS, null)
                        }
                        SpassFingerprint.STATUS_USER_CANCELLED, SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE -> { //取消识别
                            authenticateResult(false, AUTHENTICATE_FAILED_CANCELED, null)
                        }
                        SpassFingerprint.STATUS_TIMEOUT_FAILED -> {  //识别超时
                            authenticateResult(false, AUTHENTICATE_FAILED_TIMEOUT, null)
                        }
                        else -> {  //其他未知返回
                            authenticateResult(false, AUTHENTICATE_FAILED_UNKNOWN, null)
                        }
                    }
                }

                override fun onReady() {}

                override fun onStarted() {}

                override fun onCompleted() {}
            })
        }
    }

    override fun cancelAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.stopAuthenticate()
        } else {
            sPassFingerprint.cancelIdentify()
        }
    }

    override fun getUnderDisplayFingerprintPosition() = intArrayOf()
}