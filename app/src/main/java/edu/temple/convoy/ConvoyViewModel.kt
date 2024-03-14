package edu.temple.convoy

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.util.LinkedList
import java.util.Queue

// A single View Model is used to store all data we want to retain
// and observe
class ConvoyViewModel : ViewModel() {
    private val convoyState by lazy{
        MutableLiveData<Boolean>()
    }
    private val location by lazy {
        MutableLiveData<LatLng>()
    }

    private val convoyId by lazy {
        MutableLiveData<String>()
    }

    private val userJoinedConvoy by lazy {
        MutableLiveData<Boolean?>()
    }

    val audioQueue: Queue<AudioMessage> = LinkedList()

    private val isAudioPlaying by lazy{
        MutableLiveData<Boolean>()
    }

    fun setConvoyId(id: String) {
        convoyId.value = id
    }

    fun setLocation(latLng: LatLng) {
        location.value = latLng
    }

    fun setUserJoinedConvoy(joined: Boolean?){
        Log.d("Joined","joined is now $joined")
        userJoinedConvoy.value = joined
    }

    fun getLocation(): LiveData<LatLng> {
        return location
    }

    fun getConvoyId(): LiveData<String> {
        return convoyId
    }

    fun getUserJoinedConvoy(): LiveData<Boolean?>{
        return userJoinedConvoy
    }

    fun getConvoyState(): LiveData<Boolean>{
        return convoyState
    }
    fun setConvoyState(newState: Boolean){
        convoyState.value = newState
    }

    fun setAudioPlaying(isPlaying: Boolean){
        isAudioPlaying.value = isPlaying
    }

    fun getAudioPlaying(): LiveData<Boolean>{
        return isAudioPlaying
    }
}