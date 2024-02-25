package edu.temple.convoy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject


class MapsFragment : Fragment(), FCMCallbackHelper.FCMCallback {

    lateinit var map: GoogleMap
    var myMarker: Marker? = null
    var currentMarkers = emptyArray<Marker?>()

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as FCMCallbackHelper).registerCallback(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)


        // Update location on map whenever ViewModel is updated
        ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java).getLocation()
            .observe(requireActivity()) {
                if (myMarker == null) myMarker = map.addMarker(
                    MarkerOptions().position(it)
                ) else myMarker?.setPosition(it)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
            }
    }

    override fun messageReceived(message: JSONObject) {
        if(message.getString("action") == "UPDATE"){
            val data = message.getJSONArray("data")
            val markers = mutableListOf<Marker>()

            Log.d("Firebase", data[0].toString())

            if(data.length() > 0){
                for(i in 0 until data.length()){
                    val groupMember = data[i] as JSONObject
                    if(groupMember.getString("username") != Helper.user.get(requireContext()).username){
                        val memberLatLng =
                            LatLng(
                                groupMember.getString("latitude").toDouble(),
                                groupMember.getString("longitude").toDouble()
                            )
                        //need to make a way to remove markers when someone leaves
                        val marker = map.addMarker(
                            MarkerOptions().position(memberLatLng)
                        )!!
                        markers.add(marker)
                    }
                }
            }

            currentMarkers.filterNot {
                markers.contains(it)
            }.forEach{
                it?.remove()
            }
            currentMarkers = markers.toTypedArray()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity().application as FCMCallbackHelper).registerCallback(null)
    }
}