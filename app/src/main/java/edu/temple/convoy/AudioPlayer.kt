package edu.temple.convoy

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

class AudioPlayer(val context: Context, val viewModel: ConvoyViewModel) {

    private var player: MediaPlayer? = null

    fun play(uri: Uri) {
        MediaPlayer.create(context, uri)?.apply {
            player = this
            start()
            Log.d("AudioPlayer", "Playing $uri")
        }
    }

    fun stop(){
        player?.apply {
            stop()
            release()
            player = null
            viewModel.setAudioPlaying(false)
        }
    }

}

data class AudioMessage(val uri: String?, val userName: String)