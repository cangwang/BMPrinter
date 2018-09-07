package com.bcm.bmprinter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bcm.bmprinter.fingerprint.AUTHENTICATE_FAILED_LOCKOUT
import com.bcm.bmprinter.fingerprint.IFingerprintUtil
import kotlinx.android.synthetic.main.fragment_verify_fingerprint.*

/**
 * Created by Kin on 2018/9/3
 */
class VerifyFingerprintFragment : Fragment() {
    private val TAG = "VerifyFingerprint"
    private val DIALOG_TAG = "Fingerprint_Dialog"

    private var fingerprintUtil: IFingerprintUtil? = null
    private var verifyCallback: ((success: Boolean) -> Unit)? = null
    private var fragmentOnTop = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_verify_fingerprint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        verify_fingerprint_cancel.setOnClickListener {
            verifyCallback?.invoke(false)
        }
        verify_fingerprint_password.setOnClickListener {
            (activity as? MainActivity)?.switchToPasswordFragment(true)
        }
    }

    /**
     * 开启指纹识别
     */
    private fun startAuthenticate() {
        Log.d(TAG, "Start authenticate")
        fingerprintUtil?.authenticate { success, errCode, errMsg ->
            Log.d(TAG, "Get authenticate result, success: $success, error code: $errCode, error msg: $errMsg")
            if (success) { //返回成功
                verify_fingerprint_icon.setImageDrawable(context?.getDrawable(R.drawable.fingerprint_match_icon))
                Handler(Looper.getMainLooper()).postDelayed({
                    verifyCallback?.invoke(true)
                    verify_fingerprint_icon.postDelayed(resetImage,2000)
                }, 1000)
            } else {
                if (errCode == AUTHENTICATE_FAILED_LOCKOUT) {  //如果返回验证超过次数被锁
                    // 失败被锁，关闭错误提示框，跳转到输入密码界面
                    val oldDialog = fragmentManager?.findFragmentByTag(DIALOG_TAG)
                    if (oldDialog is FingerprintDialog) {
                        oldDialog.dismiss()
                    }
                    (activity as? MainActivity)?.switchToPasswordFragment(true, true)
                    return@authenticate
                }
                if (fragmentOnTop) { //页面不在顶层
                    showFailDialog(getString(R.string.fingerprint_not_match), errCode == AUTHENTICATE_FAILED_LOCKOUT)
                }
            }
            fingerprintUtil?.cancelAuthenticate()
        }
    }

    fun setFingerprintUtil(util: IFingerprintUtil): VerifyFingerprintFragment {
        fingerprintUtil = util
        return this
    }

    /**
     * 设置回调
     */
    fun setCallback(callback: (success: Boolean) -> Unit): VerifyFingerprintFragment {
        verifyCallback = callback
        return this
    }

    val resetImage = Runnable {
        verify_fingerprint_icon.setImageDrawable(context?.getDrawable(R.drawable.fingerprint_icon))
        startAuthenticate()
    }

    private fun showFailDialog(errMsg: String, lockout: Boolean) {
        val oldDialog = fragmentManager?.findFragmentByTag(DIALOG_TAG)
        if (oldDialog != null) {
            return
        }
        FingerprintDialog()
                .setHasFingerprint(true)
                .setMessage(errMsg)
                .setCallback({
                    startAuthenticate()
                }, {
                    (activity as? MainActivity)?.switchToPasswordFragment(true, lockout)
                })
                .show(fragmentManager, DIALOG_TAG)
    }

    override fun onResume() {
        super.onResume()
        fragmentOnTop = true
        //开启指纹识别
        startAuthenticate()
    }

    override fun onStop() {
        super.onStop()
        fragmentOnTop = false
        fingerprintUtil?.cancelAuthenticate()
        verify_fingerprint_icon.removeCallbacks(resetImage)
    }

    override fun onDetach() {
        super.onDetach()
        fingerprintUtil = null
        verifyCallback = null
    }
}