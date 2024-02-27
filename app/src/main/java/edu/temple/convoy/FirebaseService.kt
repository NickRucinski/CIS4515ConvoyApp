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
        Log.d("Firebase Message Full", myMessage.toString())
        (application as FCMCallbackHelper).getMessageCallback()?.run {
            messageReceived(myMessage)
        }
        (application as FCMCallbackHelper).getMapCallbackInternal()?.run {
            messageReceived(myMessage)
        }
    }
}

class FCMCallbackHelper : Application() {
    var messCallback: FCMCallback? = null
    var mapCallback: FCMCallback? = null

    interface FCMCallback {
        fun messageReceived(message: JSONObject)
    }

    fun registerMessageCallback(callback: FCMCallback?){
        messCallback = callback
    }

    fun getMessageCallback(): FCMCallback?{
        return messCallback
    }

    fun registerMapCallback(callback: FCMCallback?){
        mapCallback = callback
    }

    fun getMapCallbackInternal(): FCMCallback?{
        return mapCallback
    }
}