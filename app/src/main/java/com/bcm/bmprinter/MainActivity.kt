package com.bcm.bmprinter

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bcm.bmprinter.fingerprint.IFingerprintUtil
import com.bcm.bmprinter.fingerprint.createFingerprintUtil

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var fingerprintUtil: IFingerprintUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_fingerprint)

        initView()
    }

    private fun initView() {
        //创建指纹识别辅助器
        fingerprintUtil = createFingerprintUtil(applicationContext)
        //判断是否可用，设备是否存在锁密码，是否有录过指纹
        if (fingerprintUtil.canUseFingerprint() && fingerprintUtil.isDeviceSecure() && fingerprintUtil.hasEnrolledFingerprints()) {
            Log.d(TAG, "Device has fingerprint sensor and has enrolled fingerprints, start authenticate fingerprint.")
            switchToFingerprintFragment() //跳转到屏幕指纹识别
        } else {
            Log.d(TAG, "Device has no fingerprint sensor, start authenticate password.")
            switchToPasswordFragment(false)  //跳转到密码页面
        }
    }

    /**
     * 跳去指纹识别页
     */
    fun switchToFingerprintFragment() {
        val f = VerifyFingerprintFragment()
                .setFingerprintUtil(fingerprintUtil)
                .setCallback(this::handleCallback)
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, f)
                .commit()
    }

    /**
     * 跳转密码页
     */
    fun switchToPasswordFragment(hasFingerprint: Boolean, lockout: Boolean = false) {
        val f = VerifyPasswordFragment()
                .setHasFingerprint(hasFingerprint)
                .setHasLockout(lockout)
                .setCallback(this::handleCallback)
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, f)
                .commit()
    }

    private fun handleCallback(success: Boolean) {
        if (success) {
            Log.d(TAG, "Authenticate success.")
            setResult(Activity.RESULT_OK)
            Toast.makeText(this,"Success", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "Authenticate failed.")
            setResult(Activity.RESULT_CANCELED)
            Toast.makeText(this,"Fail", Toast.LENGTH_SHORT).show()
        }
//        finish()
    }

    override fun finish() {
        super.finish()
        fingerprintUtil.destroy() //清理数据
    }

    override fun onBackPressed() {
        finish()
    }
}
