package dev.tomerklein.holocron.util

import android.util.Log
import dev.tomerklein.holocron.BuildConfig

/**
 * Debug-gated logging. **Never** log full SMS bodies in release builds — verbose/body
 * logging is compiled behind [BuildConfig.DEBUG]. Errors and high-level status may always log.
 */
object Logx {
    fun d(tag: String, msg: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg())
    }

    /** Logs the message only in debug; in release logs a redacted marker. */
    fun body(tag: String, label: String, body: String) {
        if (BuildConfig.DEBUG) Log.d(tag, "$label: $body")
        else Log.d(tag, "$label: <redacted ${body.length} chars>")
    }

    fun i(tag: String, msg: String) = Log.i(tag, msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = Log.w(tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = Log.e(tag, msg, t)
}
