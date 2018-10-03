package pt.rafaelalmeida.sshhh_o_matic

import android.arch.lifecycle.LiveData
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import java.io.IOException
import java.lang.RuntimeException
import java.util.*

class DecibelsLiveData(private val frequencyMs: Long = 500) : LiveData<String>() {

    companion object {
        private val TAG = DecibelsLiveData::class.java.simpleName!!
        private const val HANDLER_MSG_GET_DECIBELS = 1
        private const val SECONDS = 3
        private const val AVERAGE_DECIBELS_TRIGGER = 60
    }

    private val mediaRecorder = MediaRecorder()
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var avgDecibels = arrayListOf<Float>()

    private val avgDecibelsCleaner = object: Runnable {
        override fun run() {
            if (avgDecibels.average() > AVERAGE_DECIBELS_TRIGGER) {
                postValue("Noise!")
            }
            avgDecibels.clear()
            handler!!.postDelayed(this, SECONDS.toLong() * 1000)
        }
    }

    private val handlerCallback = { msg: Message ->
        if (msg.what == HANDLER_MSG_GET_DECIBELS) {
            var text = "Avg Decibels: ${String.format(Locale.getDefault(), "%.2f", avgDecibels.average())}"

            // Get the sound pressure value
            val volume = mediaRecorder.maxAmplitude
            if (volume != 0) {
                // Change the sound pressure value to the decibel value
                val decibels = 20 * Math.log10(volume.toDouble()).toFloat()

                avgDecibels.add(decibels)
            }

            Log.i(TAG, text)

            handler?.sendEmptyMessageDelayed(HANDLER_MSG_GET_DECIBELS, frequencyMs)
        }
        true
    }

    override fun onActive() {
        Log.i(TAG, "onActive")
        startRecording()
    }

    override fun onInactive() {
        Log.i(TAG, "onInactive")
        stopRecording()
    }

    fun startRecording() {
        with(mediaRecorder) {
            try {
                reset()
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile("/dev/null")

                prepare()
                start()

                // Start handler thread
                handlerThread = HandlerThread(TAG).also { handlerThread ->
                    handlerThread.start()
                    handler = Handler(handlerThread.looper, handlerCallback).also { handler ->
                        handler.sendEmptyMessage(HANDLER_MSG_GET_DECIBELS)
                    }
                }

                avgDecibelsCleaner.run()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error initializing mediaRecorder", e)
            } catch (e: IOException) {
                Log.e(TAG, "Error initializing mediaRecorder", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Error initializing mediaRecorder. Is permission allowed?", e)
            }
        }
    }

    fun stopRecording() {
        handler?.removeMessages(HANDLER_MSG_GET_DECIBELS).also { handler = null }
        handlerThread?.quitSafely().also { handlerThread = null }

        try {
            mediaRecorder.stop()
            mediaRecorder.release()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping mediaRecorder")
        }
    }
}