package edu.temple.convoy

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(val context: Context, val activity: Activity, val viewModel: ConvoyViewModel) {
    private var recorder: MediaRecorder? = null
    private val file: File = File(context.filesDir, "RecordingFile.mp3")
    private fun create(): MediaRecorder{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    fun start(){
        if(checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            create().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                //If professor needs a different format change this
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(FileOutputStream(file).fd)

                prepare()
                start()

                recorder = this
            }
        } else{
            requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ), 1
            )
        }


    }

    fun stop(): File{
        recorder?.stop()
        recorder?.reset()
        recorder = null

        Helper.api.sendAudioMessage(context, Helper.user.get(context), Helper.user.getSessionKey(context)!!, viewModel.getConvoyId().value!!, file){
            Log.d("Audio Sent", "")
        }
        return file
    }
}