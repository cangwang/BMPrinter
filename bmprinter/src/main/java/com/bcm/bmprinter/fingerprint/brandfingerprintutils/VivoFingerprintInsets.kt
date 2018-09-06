package com.bcm.bmprinter.fingerprint.brandfingerprintutils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import android.util.JsonReader
import android.util.Log

import java.io.IOException
import java.io.StringReader
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Created by vivo fingerprint team on 2018/3/20.
 * Utils to get fingerprint information such as icon position when
 * under display fingerprint valid.
 */
class VivoFingerprintInsets private constructor(context: Context) {

    /** Flag indicating whether we have called bind on the service.  */
    private var mIsBound: Boolean = false

    private val mProperties: MutableMap<String, Property<*>> = ArrayMap(8)

    private var mListener: FingerprintInsetsListener? = null

    /** Messenger for communicating with service.  */
    private var mService: Messenger? = null

    private val mHandler: Handler = IncomingHandler(this)

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private val mMessenger: Messenger = Messenger(mHandler)

    private val mContextRef: WeakReference<Context> = WeakReference(context)

    val isReady: Boolean = !mIsBound || mProperties.isEmpty()

    val fingerprintIconState: Int = getPropertyInteger(KEY_ICON_STATE)

    val fingerprintIconPosition: Rect = getPropertyRect(KEY_ICON_POSITION) ?: Rect(-1, -1, -1, -1)

    val fingerprintIconLeft: Int = getPropertyRect(KEY_ICON_POSITION)?.left ?: -1

    val fingerprintIconTop: Int = getPropertyRect(KEY_ICON_POSITION)?.top ?: -1

    val fingerprintIconRight: Int = getPropertyRect(KEY_ICON_POSITION)?.right ?: -1

    val fingerprintIconBottom: Int = getPropertyRect(KEY_ICON_POSITION)?.bottom ?: -1

    /**
     * Class for interacting with the main interface of the service.
     */
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = Messenger(service)
            Log.d(TAG, "Attached.")

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                val msg = Message.obtain(null, MSG_QUERY_INFO)
                msg.arg1 = VERSION_MAJOR
                msg.arg2 = VERSION_MINOR

                val context = mContextRef.get()
                if (context != null) {
                    val extras = Bundle()
                    extras.putString(KEY_TOKEN, context.packageName)
                    msg.data = extras
                }

                msg.replyTo = mMessenger
                mService!!.send(msg)
            } catch (e: RemoteException) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null
            Log.d(TAG, "Disconnected.")
        }
    }

    init {
        Log.i(TAG, String.format(Locale.ENGLISH,
                "model:%s, product:%s, device:%s, manufacturer:%s",
                Build.MODEL, Build.PRODUCT, Build.DEVICE, Build.MANUFACTURER))
    }

    fun destroy() {
        if (sInstance != null) {
            doUnbindService()

            mProperties.clear()
            mContextRef.clear()

            sInstance = null
        }
    }

    fun hasUnderDisplayFingerprint(): Boolean {
        return getPropertyBoolean(KEY_HAS_UNDER_DISPLAY_FINGERPRINT)
    }

    fun setFingerprintInsetsListener(listener: FingerprintInsetsListener) {
        mListener = listener
    }

    private fun doBindService(): Boolean {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        val context = mContextRef.get()
        if (context == null) {
            Log.d(TAG, "Context missed!")
            return false
        }

        val intent = Intent()
        intent.setClassName("com.vivo.udfingerprint",
                "com.vivo.udfingerprint.service.MessengerService")
        intent.putExtra(KEY_MAJOR_VERSION, VERSION_MAJOR)
        intent.putExtra(KEY_MINOR_VERSION, VERSION_MINOR)
        val binded = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        if (!binded) {
            Log.i(TAG, "Service not exist")
            mIsBound = false
            loadPropertiesOffline()

            Message.obtain(mHandler, MSG_INTERNAL_NOTIFY_READY).sendToTarget()
        } else {
            Log.d(TAG, "Binding.")
            mIsBound = true
        }

        return mIsBound
    }

    private fun doUnbindService() {
        if (!mIsBound) {
            Log.d(TAG, "Service not bound")
            return
        }

        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (mService != null) {
            try {
                val msg = Message.obtain(null,
                        MSG_CLEAR)
                msg.replyTo = mMessenger
                mService!!.send(msg)
            } catch (e: RemoteException) {
                // There is nothing special we need to do if the service
                // has crashed.
            }

            mService = null
        }

        mIsBound = false

        val context = mContextRef.get()
        if (context == null) {
            Log.d(TAG, "Context missed!")
            return
        }

        // Detach our existing connection.
        context.unbindService(mConnection)
        Log.d(TAG, "Unbinding.")
    }

    private fun parseQueryResult(jsonString: String?) {
        setPropertyBoolean(KEY_HAS_UNDER_DISPLAY_FINGERPRINT, false)

        if (!TextUtils.isEmpty(jsonString)) {
            val reader = JsonReader(StringReader(jsonString!!))
            try {
                parseProperties(reader)
            } catch (e: IOException) {
                mProperties.clear()
            } finally {
                try {
                    reader.close()
                } catch (e: IOException) {
                    //
                }

            }
        }

        Message.obtain(mHandler, MSG_INTERNAL_NOTIFY_READY).sendToTarget()
    }

    @Throws(IOException::class)
    private fun parseProperties(reader: JsonReader) {
        reader.beginObject()

        while (reader.hasNext()) {
            val name = reader.nextName()
            if (TextUtils.equals(name, KEY_ICON_STATE)) {
                setPropertyInteger(name, reader.nextInt())
            } else if (TextUtils.equals(name, KEY_HAS_UNDER_DISPLAY_FINGERPRINT)) {
                setPropertyBoolean(name, reader.nextBoolean())
            } else if (TextUtils.equals(name, KEY_ICON_POSITION)) {
                val positionRect = parsePosition(reader)
                setPropertyRect(name, positionRect)
            } else {
                reader.skipValue()
            }
        }

        reader.endObject()
    }

    @Throws(IOException::class)
    private fun parsePosition(reader: JsonReader): Rect {
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        reader.beginArray()
        if (reader.hasNext()) {
            left = reader.nextInt()
        }

        if (reader.hasNext()) {
            top = reader.nextInt()
        }

        if (reader.hasNext()) {
            right = reader.nextInt()
        }

        if (reader.hasNext()) {
            bottom = reader.nextInt()
        }
        reader.endArray()

        return Rect(left, top, right, bottom)
    }

    private fun loadPropertiesOffline(): Boolean {
        if (Build.VERSION.SDK_INT < 24) {
            // There is no device use under display fingerprint lower than android N.
            setPropertyBoolean(KEY_HAS_UNDER_DISPLAY_FINGERPRINT, false)
            return false
        }

        if (Build.VERSION.SDK_INT < 26) {
            Log.d(TAG, "fingerprint: $fingerprintModule")
        }

        var centerX = 0
        var centerY = 0
        var iconWidth = 0
        var iconHeight = 0
        val rect = Rect()
        when {
            isX20PlusUD -> {
                Log.d(TAG, "isX20PlusUD")
                centerX = 540
                centerY = 2006
                iconWidth = 160
                iconHeight = 160
            }
            isX21UD -> {
                Log.d(TAG, "isX21UD")
                centerX = 540
                centerY = 1924
                iconWidth = 170
                iconHeight = 170
            }
            else -> {
                Log.d(TAG, "No under display fingerprint detected")
            }
        }

        rect.set(centerX, centerY, centerX + iconWidth, centerY + iconHeight)
        rect.offset(-iconWidth / 2, -iconHeight / 2)

        if (rect.isEmpty) {
            setPropertyBoolean(KEY_HAS_UNDER_DISPLAY_FINGERPRINT, false)
        } else {
            setPropertyRect(KEY_ICON_POSITION, rect)
            setPropertyInteger(KEY_ICON_STATE, ICON_VISIBLE)
            setPropertyBoolean(KEY_HAS_UNDER_DISPLAY_FINGERPRINT, true)
        }

        return true
    }

    private fun setPropertyBoolean(key: String, value: Boolean) {
        if (mProperties.containsKey(key)) {
            Log.d(TAG, "update property $key")
        }

        val property = Property(key, value)
        mProperties[key] = property
    }

    private fun setPropertyInteger(key: String, value: Int) {
        if (mProperties.containsKey(key)) {
            Log.d(TAG, "update property $key")
        }

        val property = Property(key, value)
        mProperties[key] = property
    }

    private fun setPropertyRect(key: String, rect: Rect) {
        val property = Property(KEY_ICON_POSITION, rect)
        mProperties[key] = property
    }

    private fun getPropertyBoolean(key: String): Boolean {
        val property = mProperties[key] ?: return false
        return property.value as Boolean
    }

    private fun getPropertyInteger(key: String): Int {
        val property = mProperties[key] ?: return -1
        return property.value as Int
    }

    private fun getPropertyRect(key: String): Rect? {
        val property = mProperties[key] ?: return null
        return property.value as Rect
    }

    private fun notifyReady() {
        if (mListener != null) {
            mListener!!.onReady()
        }
    }

    private fun notifyIconStateChanged(state: Int) {
        setPropertyInteger(KEY_ICON_STATE, state)

        if (mListener != null) {
            mListener!!.onIconStateChanged(state)
        }
    }

    interface FingerprintInsetsListener {
        fun onReady()
        fun onIconStateChanged(state: Int)
    }

    private class Property<T> internal constructor(internal var key: String, internal var value: T)

    /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler internal constructor(insets: VivoFingerprintInsets) : Handler() {
        private val mInsets: WeakReference<VivoFingerprintInsets> = WeakReference(insets)

        override fun handleMessage(msg: Message) {
            val insets = mInsets.get()
            if (insets == null) {
                Log.d(TAG, "missing insets reference")
                super.handleMessage(msg)
                return
            }

            when (msg.what) {
                MSG_QUERY_INFO -> {
                    Log.d(TAG, String.format(Locale.ENGLISH,
                            "Received from service, version:%d.%d", msg.arg1, msg.arg2))
                    val extras = msg.data
                    val jsonString = extras?.getString(KEY_QUERY_JSON_STRING)
                    insets.parseQueryResult(jsonString)
                }

                MSG_ICON_STATE_CHANGE -> {
                    val state = msg.arg1
                    Log.d(TAG, "Received from service, icon state:$state")
                    insets.notifyIconStateChanged(state)
                }

                MSG_INTERNAL_NOTIFY_READY -> insets.notifyReady()

                else -> super.handleMessage(msg)
            }
        }
    }

    companion object {
        private const val TAG = "VivoFingerprintInsets"

        const val ICON_INVISIBLE = 0
        const val ICON_VISIBLE = 1

        private const val VERSION_MAJOR = 1
        private const val VERSION_MINOR = 0

        /** Keep the same as service.  */
        private const val MSG_QUERY_INFO = 10000
        private const val MSG_ICON_STATE_CHANGE = 10001
        private const val MSG_CLEAR = 10002

        private const val MSG_INTERNAL_NOTIFY_READY = 10

        private const val KEY_MAJOR_VERSION = "version_major"
        private const val KEY_MINOR_VERSION = "version_minor"
        private const val KEY_QUERY_JSON_STRING = "query_json"
        private const val KEY_HAS_UNDER_DISPLAY_FINGERPRINT = "has_under_display_fingerprint"
        private const val KEY_ICON_POSITION = "icon_position"
        private const val KEY_ICON_STATE = "icon_state"

        private const val KEY_TOKEN = "token"

        private var sDebuggable = true

        private var sInstance: VivoFingerprintInsets? = null

        fun create(context: Context, listener: FingerprintInsetsListener): VivoFingerprintInsets? {
            if (!isVivoDevice) {
                return null
            }

            if (sInstance == null) {
                val insets = VivoFingerprintInsets(context)
                insets.setFingerprintInsetsListener(listener)
                insets.doBindService()

                sInstance = insets
            }

            return sInstance
        }

        fun setDebugEnable(enable: Boolean) {
            sDebuggable = enable
        }

        private val isVivoDevice: Boolean = Build.MANUFACTURER.equals("vivo", ignoreCase = true)

        private// Just support level below O, Api after O_MR1 don't allow use reflection.
        val isX20PlusUD: Boolean = Build.DEVICE.equals("PD1721", ignoreCase = true)
                || (Build.DEVICE.equals("PD1710", ignoreCase = true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.O && isUdModule)

        private// isUdModule use reflection to get fingerprint property.
        // Api after O_MR1 don't allow use reflection.
        val isX21UD: Boolean
            get() {
                if (Build.DEVICE.equals("PD1728UD", ignoreCase = true)) {
                    return true
                }

                if (Build.DEVICE.contains("1728") || Build.DEVICE.contains("1725")) {
                    if (Build.VERSION.SDK_INT <= 27) {
                        if (isUdModule) {
                            return true
                        }
                    }
                }

                return false
            }

        private val isUdModule: Boolean
            get() {
                val fingerprint = fingerprintModule
                return !TextUtils.isEmpty(fingerprint) && fingerprint.startsWith("udfp_")
            }

        private val fingerprintModule: String
            get() {
                var fingerprint = getProperty("sys.fingerprint.boot", "")
                if (TextUtils.isEmpty(fingerprint)) {
                    fingerprint = getProperty("persist.sys.fptype", "unknown")
                }
                return fingerprint
            }

        /**
         * TODO: Api after O_MR1 don't allow use reflection.
         * @param key property name
         * @param defaultValue return value if property is empty.
         * @return property
         */
        private fun getProperty(key: String, defaultValue: String): String {
            var value = defaultValue
            try {
                val c = Class.forName("android.os.SystemProperties")
                val get = c.getMethod("get", String::class.java, String::class.java)
                value = get.invoke(c, key, defaultValue) as String
            } catch (e: Exception) {
                Log.d(TAG, e.message)
            }

            return value
        }
    }
}