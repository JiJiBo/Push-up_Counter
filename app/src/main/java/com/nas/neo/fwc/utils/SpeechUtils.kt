package com.nas.neo.fwc.utils

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class SpeechUtils private constructor(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        // 用 applicationContext 避免 Activity 泄漏
        tts = TextToSpeech(context.applicationContext, this)
    }
    fun init(){

    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 优先尝试设置中文
            val langResult = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (langResult == TextToSpeech.LANG_MISSING_DATA
                || langResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // 如果不支持中文，则退回系统默认
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setPitch(1.0f)       // 音调：1.0 正常
            tts?.setSpeechRate(0.85f) // 语速：0.85 略慢
            isReady = true

            // 可选：监听播报进度
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) { }
                override fun onDone(utteranceId: String) { }
                override fun onError(utteranceId: String) { }
            })
        } else {
            // 初始化失败，可考虑重试或上报错误
            isReady = false
        }
    }

    /**
     * 立即朗读 [text]，会清空队列中的其他请求
     */
    fun speak(text: String) {
        if (!isReady) {
            Log.e("SpeechUtils", "TTS is not ready")
            return
        }
        val id = UUID.randomUUID().toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = id
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    /**
     * 朗读 [text]，[queueMode] 可指定 QUEUE_FLUSH 或 QUEUE_ADD
     */
    fun speak(text: String, queueMode: Int) {
        if (!isReady) return
        val id = UUID.randomUUID().toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, queueMode, null, id)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = id
            tts?.speak(text, queueMode, params)
        }
    }

    /** 停止当前朗读，但不释放资源 */
    fun stop() {
        tts?.stop()
    }

    /** 彻底释放 TTS 资源，之后再调用需重新初始化 */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        synchronized(SpeechUtils::class.java) {
            instance = null
        }
    }

    companion object {
        private var instance: SpeechUtils? = null

        /**
         * 获取单例，内部使用 applicationContext 避免内存泄漏
         */
        fun getInstance(context: Context): SpeechUtils {
            return instance ?: synchronized(this) {
                instance ?: SpeechUtils(context).also { instance = it }
            }
        }
    }
}
