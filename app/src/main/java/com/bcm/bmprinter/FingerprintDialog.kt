package com.bcm.bmprinter

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import kotlinx.android.synthetic.main.dialog_fingerprint.*

/**
 * Created by Kin on 2018/9/4
 */
class FingerprintDialog : DialogFragment() {
    private val TAG = "FingerprintDialog"

    private var positiveCallback: (() -> Unit)? = null
    private var negativeCallback: (() -> Unit)? = null
    private var message = ""
    private var hasFingerprint = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_fingerprint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog_fingerprint_positive.setOnClickListener {
            dismiss()
            positiveCallback?.invoke()
        }
        dialog_fingerprint_negative.setOnClickListener {
            dismiss()
            negativeCallback?.invoke()
        }
        if (!hasFingerprint) {
            dialog_fingerprint_positive.visibility = View.GONE
        }
        dialog_fingerprint_text.text = message
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.window.setLayout(dp2Px(resources, 275), WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
    }

    fun setCallback(positiveCallback: () -> Unit, negativeCallback: () -> Unit): FingerprintDialog {
        this.positiveCallback = positiveCallback
        this.negativeCallback = negativeCallback
        return this
    }

    fun setMessage(msg: String): FingerprintDialog {
        message = msg
        return this
    }

    fun setHasFingerprint(hasFingerprint: Boolean): FingerprintDialog {
        this.hasFingerprint = hasFingerprint
        return this
    }
}

/**
 * 单位转换：dp转px
 * @param res
 * @param dp
 * @return
 */
fun dp2Px(res: Resources, dp: Int): Int {
    return (dp * res.displayMetrics.density + 0.5f).toInt()
}