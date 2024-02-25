package edu.temple.convoy

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class FirebaseService : FirebaseMessagingService(){

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM Token", token)

        Helper.user.saveFCMToken(this, token)
        Helper.user.getSessionKey(this)?.run {
            Helper.api.updateFCMToken(
                this@FirebaseService,
                Helper.user.get(this@FirebaseService),
                this,
                token
            ){
                Log.d("Sent Token", it.toString())
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val myMessage= JSONObject(message.data.get("payload")!!)

        (application as FCMCallbackHelper).getCallback()?.run {
            messageReceived(myMessage)
        }
    }
}

class FCMCallbackHelper : Application() {
    var messageCallback: FCMCallback? = null

    interface FCMCallback {
        fun messageReceived(message: JSONObject)
    }

    fun registerCallback(callback: FCMCallback?){
        messageCallback = callback
    }

    fun getCallback(): FCMCallback?{
        return messageCallback
    }
}