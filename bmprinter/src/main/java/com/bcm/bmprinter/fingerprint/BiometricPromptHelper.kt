package com.bcm.bmprinter.fingerprint

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

/**
 * sdk>28 后使用BiometricPrompt识别(等Android P 发出再适配)
 * https://developer.android.google.cn/reference/android/hardware/biometrics/BiometricPrompt#public-methods
 * Created by cangwang & kin on 2018/9/6.
 */
@SuppressLint("NewApi")
class BiometricPromptHelper(context: Context){
    private val TAG = this::class.java.simpleName
    val biometricPrompt = BiometricPrompt.Builder(context).setTitle("BiometricPrompt").build()
    private val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val KEY_STORE_PROVIDER = "AndroidKeyStore"
    private val KEY_STORE_ALIAS = "com.bcm.messenger.fingerprint"
    private lateinit var cipher: Cipher
    private var cancelSignal: CancellationSignal? = null

    init {
        initKeyAndCipher()
    }

    fun isHardwareDetected() = fingerprintManager.isHardwareDetected

    /**
     * 是否录入指纹，有些设备上即使录入了指纹，但是没有开启锁屏密码的话此方法还是返回false
     */
    fun hasEnrolledFingerprints():Boolean {
        try {
            // 有些厂商api23之前的版本可能没有做好兼容，这个方法内部会崩溃（redmi note2, redmi note3等）
            return fingerprintManager.hasEnrolledFingerprints()
        }catch (e:SecurityException){

        }catch (e:Throwable){

        }
        return false
    }

    fun isKeyguardSecure() = keyguardManager.isKeyguardSecure

    fun startAuthenticate(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit) {
        cancelSignal = CancellationSignal()
        // flags为1是因为设置小米屏下指纹的图标颜色，原生API属于保留字段，未被使用
        try {
            biometricPrompt.authenticate(BiometricPrompt.CryptoObject(cipher), cancelSignal, null,callback(authenticateResult))
        }catch (e:SecurityException){ //失败重试机制
            try {
                biometricPrompt.authenticate(null, cancelSignal, null, callback(authenticateResult))
            }catch (e2:SecurityException){
                authenticateResult(false, AUTHENTICATE_FAILED_NOT_MATCH, null)
            }
        }
    }

    private fun callback(authenticateResult: (success: Boolean, errCode: Int, errMsg: String?) -> Unit):BiometricPrompt.AuthenticationCallback{
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                Log.d(TAG, "AOSP authentication error, error code: $errorCode, error msg: $errString")
                val resultCode = when (errorCode) {
                    BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT -> AUTHENTICATE_FAILED_LOCKOUT
                    BiometricPrompt.BIOMETRIC_ERROR_CANCELED -> AUTHENTICATE_FAILED_CANCELED
                    BiometricPrompt.BIOMETRIC_ERROR_TIMEOUT -> AUTHENTICATE_FAILED_TIMEOUT
                    BiometricPrompt.BIOMETRIC_ACQUIRED_INSUFFICIENT -> AUTHENTICATE_FAILED_DIRTY
                    BiometricPrompt.BIOMETRIC_ACQUIRED_PARTIAL -> AUTHENTICATE_FAILED_ACQUIRED_PARTIAL
                    else -> AUTHENTICATE_FAILED_UNKNOWN
                }
                authenticateResult(false, resultCode, errString?.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                Log.d(TAG, "AOSP authentication success")
                authenticateResult(true, AUTHENTICATE_SUCCESS, null)
            }

            override fun onAuthenticationFailed() {
                Log.d(TAG, "AOSP authentication failed, not match")
                authenticateResult(false, AUTHENTICATE_FAILED_NOT_MATCH, null)
            }
        }
    }

    fun stopAuthenticate() {
        if (cancelSignal?.isCanceled == false) {
            cancelSignal?.cancel()
            cancelSignal = null
        }
    }

    private fun initKeyAndCipher() {
        try {
            val keyStore = KeyStore.getInstance(KEY_STORE_PROVIDER)
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_PROVIDER)
            keyStore.load(null)
            val builder = KeyGenParameterSpec.Builder(KEY_STORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true)
            }
            keyGenerator.init(builder.build())
            val key = keyGenerator.generateKey()
            cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            Log.d(TAG, "Generate key success")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Generate key failed")
        }
    }
}