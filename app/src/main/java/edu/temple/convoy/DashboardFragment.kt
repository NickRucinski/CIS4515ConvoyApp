package edu.temple.convoy

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class DashboardFragment : Fragment(){

    private val AnimOpen: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.open) }
    private val AnimClose: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.close) }


    lateinit var fab: FloatingActionButton
    lateinit var menuFAB: FloatingActionButton
    lateinit var joinFAB: FloatingActionButton
    lateinit var recordAudioFAB: FloatingActionButton
    lateinit var audioText: TextView

    private var clicked = true
    private var canRecord = false

    lateinit var audioPlayer: AudioPlayer
    lateinit var audioRecorder: AudioRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let the system know that this fragment
        // wants to contribute to the app menu
        setHasOptionsMenu(true)

        FirebaseMessaging.getInstance()
            .token.addOnSuccessListener {
                Log.d("Firebase", it)
            }


        Helper.user.getFCMToken(requireContext())?.run {
            Helper.api.updateFCMToken(
                requireContext(),
                Helper.user.get(requireContext()),
                Helper.user.getSessionKey(requireContext())!!,
                this
            ){
                Log.d("Sent Token", it.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val layout =  inflater.inflate(R.layout.fragment_dashboard, container, false)
        audioText = layout.findViewById(R.id.audioDisplay)


        fab = layout.findViewById(R.id.startFloatingActionButton)
        joinFAB = layout.findViewById(R.id.joinFloatingActionButton)
        menuFAB = layout.findViewById(R.id.menuFloatingActionButton)
        recordAudioFAB = layout.findViewById(R.id.recordAudioButton)
        //When not in a convoy it is invisible
        recordAudioFAB.visibility = View.INVISIBLE

        // Query the server for the current Convoy ID (if available)
        // and use it to close the convoy
        fab.setOnLongClickListener {
            Helper.api.queryStatus(requireContext(),
            Helper.user.get(requireContext()),
            Helper.user.getSessionKey(requireContext())!!
            ) { response ->
                Helper.api.closeConvoy(
                    requireContext(),
                    Helper.user.get(requireContext()),
                    Helper.user.getSessionKey(requireContext())!!,
                    response.getString("convoy_id"),
                    null
                )
            }
            true
        }

        menuFAB.setOnClickListener{
            setVisibility(clicked)
            setAnim(clicked)
            clicked = !clicked
        }

        joinFAB.setOnClickListener {
            (activity as DashboardInterface).joinConvoy()
            recordAudioFAB.visibility = View.VISIBLE
        }
        joinFAB.setOnLongClickListener {
            Helper.api.queryStatus(requireContext(),
                Helper.user.get(requireContext()),
                Helper.user.getSessionKey(requireContext())!!
            ) { response ->
                Helper.api.leaveConvoy(
                    requireContext(),
                    Helper.user.get(requireContext()),
                    Helper.user.getSessionKey(requireContext())!!,
                    response.getString("convoy_id"),
                    null
                )
            }
            true
        }

        fab.setOnClickListener{
            (activity as DashboardInterface).createConvoy()
        }

        recordAudioFAB.setOnClickListener {
            if(!canRecord){
                setButtonRed(recordAudioFAB, R.drawable.mic_off_24px)
                audioRecorder.start()
            } else{
                setButtonGreen(recordAudioFAB, R.drawable.mic_24px)
                audioRecorder.stop()
            }

            canRecord = !canRecord

        }

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Use ViewModel to determine if we're in an active Convoy
        // Change FloatingActionButton behavior depending on if we're
        // currently in a convoy
        val viewModel = ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java)
        audioPlayer = AudioPlayer(requireContext(), viewModel)
        audioRecorder = AudioRecorder(requireContext(), requireActivity(), viewModel)
        viewModel.getUserJoinedConvoy().observe(viewLifecycleOwner) { joinedConvoy ->
            if(joinedConvoy == false){
                setButtonRed(fab, android.R.drawable.ic_menu_close_clear_cancel){(activity as DashboardInterface).endConvoy()}
                recordAudioFAB.visibility = View.VISIBLE
            } else if(joinedConvoy == true){
                setButtonRed(joinFAB, android.R.drawable.ic_menu_close_clear_cancel){(activity as DashboardInterface).leaveConvoy()}
                recordAudioFAB.visibility = View.VISIBLE
            } else{
                setButtonGreen(fab, android.R.drawable.ic_input_add){(activity as DashboardInterface).createConvoy()}
                setButtonGreen(joinFAB, R.drawable.join_24px){(activity as DashboardInterface).joinConvoy()}
                recordAudioFAB.visibility = View.INVISIBLE
            }
        }

        viewModel.getAudioPlaying().observe(viewLifecycleOwner){
            if(it){
                audioText.visibility = View.VISIBLE
                audioText.text = "Audio Message From ${viewModel.audioQueue.peek()?.userName}:"
            } else{
                audioText.visibility = View.INVISIBLE
                audioText.text = ""
            }
        }
    }

    // This fragment places a menu item in the app bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.dashboard, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_logout) {
            (activity as DashboardInterface).logout()
            return true
        }

        return false
    }

    private fun setVisibility(clicked: Boolean){
        if(clicked){
            fab.visibility = View.VISIBLE
            joinFAB.visibility = View.VISIBLE
        } else{
            fab.visibility = View.INVISIBLE
            joinFAB.visibility = View.INVISIBLE

        }
    }

    private fun setAnim(clicked: Boolean){
        if(clicked){
            fab.startAnimation(AnimOpen)
            joinFAB.startAnimation(AnimOpen)
        } else{
            fab.startAnimation(AnimClose)
            joinFAB.startAnimation(AnimClose)

        }
    }

    private fun setButtonGreen(fab: FloatingActionButton, resID: Int, action: OnClickListener){
        fab.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
        fab.setImageResource(resID)
        fab.setOnClickListener(action)
    }

    private fun setButtonRed(fab: FloatingActionButton, resID: Int){
        fab.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#e91e63"))
        fab.setImageResource(resID)
    }

    private fun setButtonGreen(fab: FloatingActionButton, resID: Int){
        fab.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
        fab.setImageResource(resID)
    }

    private fun setButtonRed(fab: FloatingActionButton, resID: Int, action: OnClickListener){
        fab.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#e91e63"))
        fab.setImageResource(resID)
        fab.setOnClickListener(action)
    }

    interface DashboardInterface {
        fun createConvoy()
        fun joinConvoy()
        fun leaveConvoy()
        fun endConvoy()
        fun logout()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

}