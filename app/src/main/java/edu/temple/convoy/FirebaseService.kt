package edu.temple.convoy

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

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
        val myMessage = message.data.get("payload").toString()

        Log.d("Firebase", myMessage)
    }
}