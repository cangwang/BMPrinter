package com.bcm.bmprinter.fingerprint

/**
 * Created cangwang &  Kin on 2018/8/31
 */
interface IFingerprintUtil {
    /**
     * Check if device has fingerprint sensor
     * 是有拥有指纹识别功能
     */
    fun canUseFingerprint(): Boolean

    /**
     * Check if device has lock screen password
     * 是否存在屏幕密码
     */
    fun isDeviceSecure(): Boolean

    /**
     * Check if device has enrolled fingerprints
     * 是否已经录制过指纹
     */
    fun hasEnrolledFingerprints(): Boolean

    /**
     * Clean data
     * 清理验证数据
     */
    fun destroy()

    /**
     * Start to authenticate fingerprints
     * 开始识别指纹
     */
    fun authenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit)

    /**
     * Cancel authenticate fingerprints
     * 取消识别指纹
     */
    fun cancelAuthenticate()

    /**
     * Get fingerprint position, enable in under display fingerprint devices, return an IntArray(left, top, right, bottom).
     * In normal devices will return empty array.
     * 是否屏幕指纹识别位置
     */
    fun getUnderDisplayFingerprintPosition(): IntArray
}