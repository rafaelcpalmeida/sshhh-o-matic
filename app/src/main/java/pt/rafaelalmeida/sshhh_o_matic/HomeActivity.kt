package pt.rafaelalmeida.sshhh_o_matic

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import java.util.*


class HomeActivity : AppCompatActivity() {

    companion object {
        private val TAG = HomeActivity::class.java.simpleName!!
    }

    private val viewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")

        val mp = MediaPlayer.create (this, R.raw.sshhh)

        mp.setOnCompletionListener {
            var audios = arrayOf("rafa", "mafalda", "julia", "arne").asList()

            val res = R.raw::class.java
            val field = res.getField(audios.shuffled()[0])
            val resourceId = field.getInt(null)

            val mpCustom = MediaPlayer.create (this, resourceId)

            mpCustom.start()

            mpCustom.setOnCompletionListener {
                it.release()
            }
        }

        viewModel.decibelsLiveData.observe(this, Observer { message ->
            if (message === "Noise!") {
                mp.start()
            }
        })
    }
}
