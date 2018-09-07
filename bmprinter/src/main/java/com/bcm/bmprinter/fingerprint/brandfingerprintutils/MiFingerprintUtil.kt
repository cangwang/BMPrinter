package com.bcm.bmprinter.fingerprint.brandfingerprintutils

import android.content.Context
import android.util.Log
import com.bcm.bmprinter.fingerprint.AospFingerprintHelper
import com.bcm.bmprinter.fingerprint.IFingerprintUtil

/**
 * Xiaomi指纹识别模块，依赖于AOSP，支持返回屏下指纹位置，单位px
 *
 * Created by cangwang & Kin on 2018/8/31
 */
class MiFingerprintUtil private constructor(context: Context) : IFingerprintUtil {
    companion object {
        private var instance: MiFingerprintUtil? = null

        fun getInstance(context: Context): MiFingerprintUtil {
            if (instance == null) {
                instance = MiFingerprintUtil(context)
            }
            Log.d(instance?.TAG, "Return Mi fingerprint util.")
            return instance!!
        }
    }

    private val TAG = "MiFingerprintUtil"
    private val fingerprintPosition = intArrayOf(-1, -1, -1, -1)
    private var hasUnderDisplayFingerprint = false
    private val aospFingerprintUtil = AospFingerprintHelper(context)

    init {
        try {
            //反射判断是否拥有使用屏下指纹识别
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod("get", String::class.java)
            val value = method.invoke(clazz, "ro.hardware.fp.fod") as String
            if (value == "true") {
                hasUnderDisplayFingerprint = true
                val xy = method.invoke(clazz, "ro.hardware.persist.sys.fp.fod.location.X_Y") as String
                val wh = method.invoke(clazz, "persist.sys.fp.fod.size.width_height") as String
                val xyArray = xy.split(",")
                val whArray = wh.split(",")
                //设置指纹位置
                fingerprintPosition[0] = xyArray[0].toInt()
                fingerprintPosition[1] = xyArray[1].toInt()
                fingerprintPosition[2] = xyArray[0].toInt() + whArray[0].toInt()
                fingerprintPosition[3] = xyArray[1].toInt() + whArray[1].toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun canUseFingerprint() = aospFingerprintUtil.isHardwareDetected()

    override fun isDeviceSecure() = aospFingerprintUtil.isKeyguardSecure()

    override fun hasEnrolledFingerprints() = aospFingerprintUtil.hasEnrolledFingerprints()

    override fun destroy() {
        aospFingerprintUtil.stopAuthenticate()
    }

    override fun authenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit) {
        aospFingerprintUtil.startAuthenticate(authenticateResult)
    }

    override fun cancelAuthenticate() {
        aospFingerprintUtil.stopAuthenticate()
    }

    override fun getUnderDisplayFingerprintPosition(): IntArray {
        if (hasUnderDisplayFingerprint) {
            return fingerprintPosition
        }
        return intArrayOf()
    }
}