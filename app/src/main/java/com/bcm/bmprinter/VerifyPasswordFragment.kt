package com.bcm.bmprinter

import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import kotlinx.android.synthetic.main.fragment_verify_password.*

/**
 * Created by Kin on 2018/9/3
 */
class VerifyPasswordFragment : Fragment() {
    private val TAG = "VerifyPasswordFragment"

    private var verifyCallback: ((success: Boolean) -> Unit)? = null
    private var hasFingerprint = false
    private var lockout = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_verify_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        verify_password_fingerprint.setOnClickListener {
            (activity as? MainActivity)?.switchToFingerprintFragment()
        }
        verify_password_confirm.setOnClickListener {
            verifyPassword(verify_password_input.text.toString())
        }
        verify_password_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                verify_password_confirm.visibility = if (s != null && s.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        })
        verify_password_cancel.setOnClickListener {
            verifyCallback?.invoke(false)
        }
        if (!hasFingerprint) {
            // 设备没有指纹识别，隐藏跳转图标
            verify_password_fingerprint.visibility = View.GONE
            verify_password_try_again.visibility = View.GONE
        }
        if (lockout) {
            // 失败过多被锁，不允许跳转到指纹识别
            verify_password_fingerprint.setOnClickListener {
                Toast.makeText(activity,getString(R.string.fingerprint_lockout),Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(activity,getString(R.string.fingerprint_lockout),Toast.LENGTH_SHORT).show()
        }
    }

    fun setCallback(callback: (success: Boolean) -> Unit): VerifyPasswordFragment {
        verifyCallback = callback
        return this
    }

    fun setHasFingerprint(hasFingerprint: Boolean): VerifyPasswordFragment {
        this.hasFingerprint = hasFingerprint
        return this
    }

    fun setHasLockout(lockout: Boolean): VerifyPasswordFragment {
        this.lockout = lockout
        return this
    }

    override fun onDetach() {
        super.onDetach()
        verifyCallback = null
    }

    /**
     * 校验密码
     *
     * @param inputPassword 用户输入的密码
     */
    private fun verifyPassword(inputPassword: String) {
        try {
//            val loginProfile = AMESelfData.profile
//            loginProfile?.let {
//                val oldPrivateKeyString = it.privateKey
//                        ?: throw Exception("account private key is null")
//                OpenIdUtils.decodePrivateKey(oldPrivateKeyString, inputPassword)
//                verifyCallback?.invoke(true)
//                return
//            }
            Toast.makeText(activity,"Go to password input",Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {}
        verify_password_input.background = context?.getDrawable(R.drawable.register_input_error_bg)
    }
}