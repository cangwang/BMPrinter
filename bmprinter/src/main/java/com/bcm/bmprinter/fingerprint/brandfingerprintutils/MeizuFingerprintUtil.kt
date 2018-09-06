package com.bcm.bmprinter.fingerprint.brandfingerprintutils

import android.content.Context

import android.os.Build
import android.util.Log
import com.bcm.bmprinter.fingerprint.AUTHENTICATE_FAILED_NOT_MATCH
import com.bcm.bmprinter.fingerprint.AUTHENTICATE_SUCCESS
import com.bcm.bmprinter.fingerprint.AospFingerprintHelper
import com.bcm.bmprinter.fingerprint.IFingerprintUtil
import com.fingerprints.service.FingerprintManager

/**
 * Meizu指纹识别模块，Android M+依赖于AOSP，Android L通过魅族SDK进行支持
 * http://open-wiki.flyme.cn/index.php?title=指纹识别API sdk文档地址
 * Created by Kin on 2018/8/31
 */
class MeizuFingerprintUtil private constructor(context: Context) : IFingerprintUtil {
    companion object {
        private var instance: MeizuFingerprintUtil? = null

        fun getInstance(context: Context): MeizuFingerprintUtil {
            if (instance == null) {
                instance = MeizuFingerprintUtil(context)
            }
            instance?.initManager()
            Log.d(instance?.TAG, "Return Meizu fingerprint util.")
            return instance!!
        }
    }

    private val TAG = "MeizuFingerprintUtil"

    private var mManager: FingerprintManager? = null
    private lateinit var aospFingerprintHelper: AospFingerprintHelper
    private var released = true

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //6.0或以上使用
            aospFingerprintHelper = AospFingerprintHelper(context)
        } else { //5.0一下使用
            initManager()
        }
    }

    fun initManager() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mManager = FingerprintManager.open()
            released = false
        }
    }

    private fun releaseManager() {
        mManager?.release()
        released = true
    }

    override fun canUseFingerprint(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.isHardwareDetected()
        } else {
            mManager != null && mManager?.ids != null
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
            mManager?.ids != null
        }
    }

    override fun destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.stopAuthenticate()
        } else {
            mManager?.abort()
            releaseManager()
        }
    }

    override fun authenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.startAuthenticate(authenticateResult)
        } else {
            if (released) {
                initManager()
            }
            mManager?.startIdentify(object : FingerprintManager.IdentifyCallback {
                override fun onNoMatch() {
                    authenticateResult(false, AUTHENTICATE_FAILED_NOT_MATCH, null)
                    releaseManager()
                }

                override fun onIdentified(p0: Int, p1: Boolean) {
                    authenticateResult(true, AUTHENTICATE_SUCCESS, null)
                    releaseManager()
                }
            }, mManager?.ids)
        }
    }

    override fun cancelAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.stopAuthenticate()
        } else {
            mManager?.abort()
            releaseManager()
        }
    }

    override fun getUnderDisplayFingerprintPosition() = intArrayOf()
}