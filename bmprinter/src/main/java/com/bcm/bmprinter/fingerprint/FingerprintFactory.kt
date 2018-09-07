package com.bcm.bmprinter.fingerprint

import android.content.Context
import android.os.Build
import com.bcm.bmprinter.fingerprint.brandfingerprintutils.*

/**
 * 获取指纹识别接口静态方法
 * Java调用函数的方法：FingerprintFactoryKt.MethodName(context)
 * Java调用常量的方法：FingerprintFactoryKt.ConstantsName
 * Kotlin调用函数的方法：直接访问函数名
 * Kotlin调用厂常量的方法：直接访问常量名
 *
 * Created by cangwang & Kin on 2018/8/31
 */
const val AUTHENTICATE_SUCCESS = 0                  // 成功
const val AUTHENTICATE_FAILED_NOT_MATCH = 1         // 不匹配
const val AUTHENTICATE_FAILED_CANCELED = 2          // 取消识别
const val AUTHENTICATE_FAILED_DIRTY = 3             // 传感器或手指太脏导致识别失败
const val AUTHENTICATE_FAILED_LOCKOUT = 4           // 尝试次数过多，暂时封锁
const val AUTHENTICATE_FAILED_TIMEOUT = 5           // 超时未识别
const val AUTHENTICATE_FAILED_ACQUIRED_PARTIAL = 6  // 只识别到一部分
const val AUTHENTICATE_FAILED_UNKNOWN = 7           // 其他错误

/**
 * 创建构造器
 */
fun createFingerprintUtil(context: Context):IFingerprintUtil {
    return when (Build.MANUFACTURER) { //判断厂商
        "Meizu" -> createMeizuFingerprintUtil(context)  //魅族
        "vivo" -> createVivoFingerprintUtil(context)    //vivo oppo
        "Xiaomi", "xiaomi" -> createMiFingerprintUtil(context)  //小米
        "Samsung", "samsung" -> createSamsuntFingerprintUtil(context)  //三星
        else -> AospFingerprintUtil.getInstance(context)
    }
}

private fun createMeizuFingerprintUtil(context: Context): IFingerprintUtil {
    return MeizuFingerprintUtil.getInstance(context)
}

private fun createVivoFingerprintUtil(context: Context): IFingerprintUtil {
    return VivoFingerprintUtil.getInstance(context)
}

private fun createMiFingerprintUtil(context: Context): IFingerprintUtil {
    return MiFingerprintUtil.getInstance(context)
}

private fun createSamsuntFingerprintUtil(context: Context): IFingerprintUtil {
    return SamsungFingerprintUtil.getInstance(context)
}