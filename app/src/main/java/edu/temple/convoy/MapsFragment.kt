package edu.temple.convoy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONObject


class MapsFragment : Fragment(), FCMCallbackHelper.FCMCallback {

    private var map: GoogleMap? = null
    private var myMarker: Marker? = null
    private var currentMarkers = mutableListOf<Marker?>()

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as FCMCallbackHelper).registerMapCallback(this)
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
        val viewModel = ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java)


        // Update location on map whenever ViewModel is updated

            viewModel
            .getLocation()
            .observe(requireActivity()) {
                //if(map != null){
                    if (myMarker == null) {
                        myMarker = map?.addMarker(
                            MarkerOptions().position(it)
                        )
                    } else {
                        myMarker?.setPosition(it)
                    }
                    if(currentMarkers.isEmpty()){
                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
                    }
                //}
            }

        viewModel
            .getConvoyState()
            .observe(requireActivity()){
                if(it == false){
                    for(m in currentMarkers){
                        m?.remove()
                    }
                    myMarker?.remove()
                    myMarker = null
                }
            }
    }

    override fun messageReceived(message: JSONObject) {
        if(message.getString("action") == "UPDATE"){
            val data = message.getJSONArray("data")
            Log.d("Firebase", data.toString())
            activity?.runOnUiThread {
                addMemberMarkers(data)
            }
        }
    }

    fun addMemberMarkers(data: JSONArray){

        val markers = mutableListOf<Marker?>()
        val hostUserName = Helper.user.get(requireContext()).username
        if(data.length() > 0){
            for(i in 0 until data.length()){
                val groupMember = data[i] as JSONObject
                if(groupMember.getString("username") != hostUserName){
                    val memberLatLng =
                        LatLng(
                            groupMember.getString("latitude").toDouble(),
                            groupMember.getString("longitude").toDouble()
                        )
                    val marker = map?.addMarker(
                        MarkerOptions()
                            .position(memberLatLng)
                            .title(groupMember.getString("username"))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.airport_shuttle_fill0_wght400_grad0_opsz24))
                    )!!
                    markers.add(marker)
                }
            }
            // I think this is wrong not sure though
            currentMarkers.filterNot {
                markers.contains(it)
            }.forEach{
                it?.remove()
            }
//            markers.add(myMarker)
            currentMarkers = markers
            if(currentMarkers.isNotEmpty()){
                val boundsBuilder = LatLngBounds.Builder()
                for(marker in currentMarkers){
                    Log.d("Markers", marker?.title.toString())
                    boundsBuilder.include(marker!!.position)
                }
                boundsBuilder.include(myMarker!!.position)
                map?.animateCamera(
                    CameraUpdateFactory
                        .newLatLngBounds(boundsBuilder.build(), 20, 20, 0)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity().application as FCMCallbackHelper).registerMapCallback(null)
    }
}