package edu.temple.convoy

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import org.json.JSONObject

class MainActivity : AppCompatActivity(), DashboardFragment.DashboardInterface, FCMCallbackHelper.FCMCallback{

    var serviceIntent: Intent? = null
    val convoyViewModel : ConvoyViewModel by lazy {
        ViewModelProvider(this)[ConvoyViewModel::class.java]
    }

    // Update ViewModel with location data whenever received from LocationService
    var locationHandler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            convoyViewModel.setLocation(msg.obj as LatLng)
        }
    }

    var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {

            // Provide service with handler
            (iBinder as LocationService.LocationBinder).setHandler(locationHandler)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val filter = IntentFilter("DOWNLOAD_COMPLETED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadCompleteReceiver, filter, RECEIVER_NOT_EXPORTED)
        }

        FirebaseApp.initializeApp(this)
        (application as FCMCallbackHelper).registerMessageCallback(this)
        createNotificationChannel()
        serviceIntent = Intent(this, LocationService::class.java)

        convoyViewModel.getConvoyId().observe(this) {
            if (!it.isNullOrEmpty())
                supportActionBar?.title = "Convoy - $it"
            else
                supportActionBar?.title = "Convoy"
        }

        Helper.user.getConvoyId(this)?.run {
            convoyViewModel.setConvoyId(this)
            startLocationService()
        }
        Helper.user.getJoinedState(this).run {
            convoyViewModel.setUserJoinedConvoy(this)
        }

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )
        }

    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel("default", "Active Convoy", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun createConvoy() {
        Helper.api.createConvoy(this, Helper.user.get(this), Helper.user.getSessionKey(this)!!
        ) { response ->
            if (Helper.api.isSuccess(response)) {
                convoyViewModel.setConvoyId(response.getString("convoy_id"))
                convoyViewModel.setUserJoinedConvoy(false)
                convoyViewModel.setConvoyState(true)
                Helper.user.saveConvoyId(this@MainActivity, convoyViewModel.getConvoyId().value!!)
                Helper.user.saveJoinedState(this@MainActivity, false)
                startLocationService()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    Helper.api.getErrorMessage(response),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun joinConvoy() {
        val editText = EditText(this)
        AlertDialog.Builder(this).setTitle("Join Convoy")
            .setView(editText)
            .setMessage("Enter Convoy ID")
            .setPositiveButton("Yes"
            ) { _, _ -> Helper.api.joinConvoy(
                this,
                Helper.user.get(this),
                Helper.user.getSessionKey(this)!!,
                editText.text.toString()
            ) { response ->
                if (Helper.api.isSuccess(response)) {
                    convoyViewModel.setConvoyId(editText.text.toString())
                    convoyViewModel.setUserJoinedConvoy(true)
                    convoyViewModel.setConvoyState(true)
                    Helper.user.saveConvoyId(this@MainActivity, convoyViewModel.getConvoyId().value!!)
                    Helper.user.saveJoinedState(this@MainActivity, false)
                    startLocationService()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        Helper.api.getErrorMessage(response),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            }
            .setNegativeButton("Cancel") { p0, _ -> p0.cancel() }
            .show()
    }

    override fun leaveConvoy() {
        if(ViewModelProvider(this).get(ConvoyViewModel::class.java).getUserJoinedConvoy().value == false){
            Toast.makeText(
                this@MainActivity,
                "You are the host. Please use the end button.",
                Toast.LENGTH_SHORT
            ).show()
        }
        AlertDialog.Builder(this).setTitle("Leave Convoy")
            .setMessage("Are you sure you want to leave the convoy?")
            .setPositiveButton("Yes"
            ) { _, _ -> Helper.api.leaveConvoy(
                this,
                Helper.user.get(this),
                Helper.user.getSessionKey(this)!!,
                Helper.user.getConvoyId(this)!!
            ) { response ->
                if (Helper.api.isSuccess(response)) {
                    convoyViewModel.setConvoyId("")
                    convoyViewModel.setUserJoinedConvoy(null)
                    convoyViewModel.setConvoyState(false)
                    Helper.user.clearConvoyId(this@MainActivity)
                    Helper.user.clearJoinedState(this@MainActivity)
                    stopLocationService()
                } else
                    Toast.makeText(
                        this@MainActivity,
                        Helper.api.getErrorMessage(response),
                        Toast.LENGTH_SHORT
                    ).show()
            }
            }
            .setNegativeButton("Cancel") { p0, _ -> p0.cancel() }
            .show()
    }

    override fun endConvoy() {
        if(ViewModelProvider(this).get(ConvoyViewModel::class.java).getUserJoinedConvoy().value == true){
            Toast.makeText(
                this@MainActivity,
                "You are not the host. Please use the leave button.",
                Toast.LENGTH_SHORT
            ).show()
        }
        AlertDialog.Builder(this).setTitle("Close Convoy")
            .setMessage("Are you sure you want to close the convoy?")
            .setPositiveButton("Yes"
            ) { _, _ -> Helper.api.closeConvoy(
                this,
                Helper.user.get(this),
                Helper.user.getSessionKey(this)!!,
                convoyViewModel.getConvoyId().value!!
            ) { response ->
                if (Helper.api.isSuccess(response)) {
                    convoyViewModel.setConvoyId("")
                    convoyViewModel.setUserJoinedConvoy(null)
                    convoyViewModel.setConvoyState(false)
                    Helper.user.clearConvoyId(this@MainActivity)
                    Helper.user.clearJoinedState(this@MainActivity)

                    stopLocationService()
                } else
                    Toast.makeText(
                        this@MainActivity,
                        Helper.api.getErrorMessage(response),
                        Toast.LENGTH_SHORT
                    ).show()
            }
            }
            .setNegativeButton("Cancel") { p0, _ -> p0.cancel() }
            .show()
    }

    override fun logout() {
        Helper.user.clearSessionData(this)
        convoyViewModel.setConvoyId("")
        Navigation.findNavController(findViewById(R.id.fragmentContainerView))
            .navigate(R.id.action_dashboardFragment_to_loginFragment)
    }

    private fun startLocationService() {
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        startService(serviceIntent)
    }
    private fun stopLocationService() {
        unbindService(serviceConnection)
        stopService(serviceIntent)
    }
    var userName: String = ""
    override fun messageReceived(message: JSONObject) {
        if(message.getString("action") == "END" && convoyViewModel.getUserJoinedConvoy().value == true){
            runOnUiThread {
                convoyViewModel.setConvoyId("")
                convoyViewModel.setUserJoinedConvoy(null)
                convoyViewModel.setConvoyState(false)
                Helper.user.clearConvoyId(this@MainActivity)
                Helper.user.clearJoinedState(this@MainActivity)
                stopLocationService()
            }
        } else if(message.getString("action") == "MESSAGE"){
            if(message.getString("username") != Helper.user.get(this).username){
                val link = message.getString("message_file")
                userName = message.getString("username")
                //add a check for username to make sure you are not playing your own audio
                val audioDownloader = AudioDownloader(this)
                audioDownloader.downloadFile(link)
            }

        }
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "DOWNLOAD_COMPLETED") {
                val downloadedUri = intent.getStringExtra("downloadedUri")
                // Update ViewModel with downloadedUri
                convoyViewModel.addToAudioQueue(AudioMessage(downloadedUri, userName))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as FCMCallbackHelper).registerMessageCallback(null)
        unregisterReceiver(downloadCompleteReceiver)
    }

}