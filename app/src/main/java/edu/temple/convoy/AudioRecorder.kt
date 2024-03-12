package edu.temple.convoy

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(val context: Context) {
    private var recorder: MediaRecorder? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private fun create(): MediaRecorder{
        return MediaRecorder(context)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun start(output: File){
        create().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            //If professor needs a different format change this
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(output).fd)

            prepare()
            start()

            recorder = this
        }

    }

    fun stop(){
        recorder?.stop()
        recorder?.reset()
        recorder = null
    }
}