package com.nas.neo.fwc.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log


@SuppressLint("StaticFieldLeak")
object FWC_manager {
    var status: ACTION_STATUS = ACTION_STATUS.SLEEP
    var count = 0
    var mContext: Context? = null
    fun setContext(context: Context) {
        mContext = context
    }

    fun toogleStart() {
        Log.e("FWC_manager", "toogleStart")

        if (status == ACTION_STATUS.SLEEP) {
            status = ACTION_STATUS.PREPARE
            speak("准备开始")
        } else if (status == ACTION_STATUS.STOP || status == ACTION_STATUS.PREPARE) {
            status = ACTION_STATUS.SLEEP
            resetCount()
        }
    }

    fun updateCount() {
        if (status == ACTION_STATUS.START) {
            count++
            speak("$count 次")
        }
    }

    fun resetCount() {
        speak("计数清零，此次共完成$count 次俯卧撑")
        count = 0
    }

    fun start() {
        if (status == ACTION_STATUS.PREPARE || status == ACTION_STATUS.STOP) {
            status = ACTION_STATUS.START
            speak("开始")
        }

    }

    fun stop() {
        if (status == ACTION_STATUS.START) {
            status = ACTION_STATUS.STOP
            speak("结束")
        }
    }

    fun speak(text: String) {
        Log.e("FWC_manager", "speak: $text")
        mContext?.let {

            SpeechUtils.getInstance(it).speak(text)
        }
    }
}