package com.bcm.bmprinter.fingerprint.brandfingerprintutils

import android.content.Context
import android.os.Build
import android.util.Log
import com.bcm.bmprinter.fingerprint.AospFingerprintHelper
import com.bcm.bmprinter.fingerprint.IFingerprintUtil

/**
 * AOSP指纹识别模块，Android Marshmallow+
 *
 * Created by Kin on 2018/9/3
 */
class AospFingerprintUtil private constructor(context: Context) : IFingerprintUtil {
    companion object {
        private var instance: AospFingerprintUtil? = null

        fun getInstance(context: Context): AospFingerprintUtil {
            if (instance == null) {
                instance = AospFingerprintUtil(context)
            }
            Log.d(instance?.TAG, "Return AOSP fingerprint util.")
            return instance!!
        }
    }

    private val TAG = "AospFingerprintUtil"

    private lateinit var aospFingerprintHelper: AospFingerprintHelper

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper = AospFingerprintHelper(context)
        }
    }

    override fun canUseFingerprint(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.isHardwareDetected()
        } else false
    }

    override fun isDeviceSecure(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.isKeyguardSecure()
        } else false
    }

    override fun hasEnrolledFingerprints(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.hasEnrolledFingerprints()
        } else false
    }

    override fun destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.stopAuthenticate()
        }
    }

    override fun authenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.startAuthenticate(authenticateResult)
        }
    }

    override fun cancelAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            aospFingerprintHelper.stopAuthenticate()
        }
    }

    override fun getUnderDisplayFingerprintPosition() = intArrayOf()
}