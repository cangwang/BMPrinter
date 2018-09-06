package com.bcm.bmprinter.fingerprint.brandfingerprintutils

import android.content.Context
import android.os.Build
import android.util.Log
import com.bcm.bmprinter.fingerprint.AospFingerprintHelper
import com.bcm.bmprinter.fingerprint.IFingerprintUtil

/**
 * Vivo指纹识别模块，依赖于AOSP，支持返回屏下指纹位置，单位px
 * https://dev.vivo.com.cn/documentCenter/doc/104 sdk文档地址
 * Created by Kin on 2018/8/31
 */
class VivoFingerprintUtil private constructor(context: Context) : IFingerprintUtil {
    companion object {
        private var instance: VivoFingerprintUtil? = null

        fun getInstance(context: Context): VivoFingerprintUtil {
            if (instance == null) {
                instance = VivoFingerprintUtil(context)
            }
            Log.d(instance?.TAG, "Return Vivo fingerprint util.")
            return instance!!
        }
    }

    private val TAG = "VivoFingerprintUtil"

    private lateinit var aospFingerprint: AospFingerprintHelper
    //屏下指纹技术
    private var mInset = VivoFingerprintInsets.create(context, object : VivoFingerprintInsets.FingerprintInsetsListener {
        override fun onReady() {}

        override fun onIconStateChanged(state: Int) {}
    })

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprint = AospFingerprintHelper(context)
        }
    }

    override fun canUseFingerprint(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return aospFingerprint.isHardwareDetected()
        }
        return false
    }

    override fun isDeviceSecure(): Boolean {
        return aospFingerprint.isKeyguardSecure()
    }

    override fun hasEnrolledFingerprints(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return aospFingerprint.hasEnrolledFingerprints()
        }
        return false
    }

    override fun destroy() {
        mInset?.destroy()
        aospFingerprint.stopAuthenticate()
    }

    override fun authenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit) {
        aospFingerprint.startAuthenticate(authenticateResult)
    }

    override fun cancelAuthenticate() {
        aospFingerprint.stopAuthenticate()
    }

    override fun getUnderDisplayFingerprintPosition(): IntArray {
        if (mInset?.hasUnderDisplayFingerprint() == true) { //判断是否评下指纹
            return intArrayOf(mInset?.fingerprintIconLeft ?: -1, mInset?.fingerprintIconTop ?: -1, mInset?.fingerprintIconRight ?: -1, mInset?.fingerprintIconBottom ?: -1)
        }
        return intArrayOf()
    }
}