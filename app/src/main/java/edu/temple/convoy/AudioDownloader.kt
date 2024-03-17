package edu.temple.convoy

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class AudioDownloader(private val context: Context) {

    suspend fun downloadAudio(audioUrl: String): File? = withContext(Dispatchers.IO) {
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null
        var file: File? = null

        try {
            val urlConnection = java.net.URL(audioUrl).openConnection()
            urlConnection.connect()

            Log.d("AudioDownloader", "Started")
            inputStream = BufferedInputStream(urlConnection.getInputStream())

            val mimeType = urlConnection.contentType
            val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val fileName = "downloaded_audio.${fileExtension ?: "mp3"}"

            file = File(context.cacheDir, fileName)
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // You can handle errors here or propagate them
        } finally {
            Log.d("AudioDownloader", "Finished or failed")
            try {
                outputStream?.close()
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        file
    }

    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    fun downloadFile(url: String): Long {
        val request = DownloadManager.Request(url.toUri())
            .setMimeType("audio/mp4")
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle("download.mp4")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "download.mp4")
        return downloadManager.enqueue(request)
    }
}