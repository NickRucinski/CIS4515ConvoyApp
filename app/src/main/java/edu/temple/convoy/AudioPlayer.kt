package edu.temple.convoy

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.time.LocalDateTime

class AudioPlayer(val context: Context, val viewModel: ConvoyViewModel): FCMCallbackHelper.FCMCallback {

    private var player: MediaPlayer? = null
    fun play(file: File){
        MediaPlayer.create(context, file.toUri()).apply{
            player = this
            start()
        }
    }

    fun stop(){
        player?.stop()
        player?.release()
        player = null
    }

    override fun messageReceived(message: JSONObject) {
        if(message.getString("action") == "MESSAGE"){
            val link = message.getString("message_file")
            val userName = message.getString("username")
            val audioFile = downloadAudio(link, userName)
            viewModel.audioQueue.add(AudioMessage(audioFile, userName))
        }
    }

    fun downloadAudio(urlString: String, userName: String): File{
        val url = URL(urlString)

        val file = File(context.filesDir, "${userName}_Message_${LocalDateTime.now()}")
        try{
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("Download","Starting File Download")
                url.openStream().use { inp ->
                    BufferedInputStream(inp).use { bis ->
                        FileOutputStream(file).use { fos ->
                            val data = ByteArray(1024)
                            var count: Int
                            while (bis.read(data, 0, 1).also { count = it } != -1) {
                                fos.write(data, 0, count)
                            }
                        }
                    }
                }
                Log.d("Download","File Finished Downloading")

            }
        } catch (e: IOException){
            e.printStackTrace()
        }
        return file
    }

}

data class AudioMessage(val file: File, val userName: String)