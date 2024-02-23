package edu.temple.convoy

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject

class DashboardFragment : Fragment() {

    private val AnimOpen: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.open) }
    private val AnimClose: Animation by lazy { AnimationUtils.loadAnimation(context, R.anim.close) }


    lateinit var fab: FloatingActionButton
    lateinit var menuFAB: FloatingActionButton
    lateinit var joinFAB: FloatingActionButton

    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let the system know that this fragment
        // wants to contribute to the app menu
        setHasOptionsMenu(true)

        Helper.user.getSessionKey(requireContext())?.run {
            Helper.api.updateFCMToken(
                requireContext(),
                Helper.user.get(requireContext()),
                this,
                Helper.user.getFCMToken(requireContext())!!
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

        fab = layout.findViewById(R.id.startFloatingActionButton)
        joinFAB = layout.findViewById(R.id.joinFloatingActionButton)
        menuFAB = layout.findViewById(R.id.menuFloatingActionButton)

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
        }

        fab.setOnClickListener{
            (activity as DashboardInterface).createConvoy()
        }

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Use ViewModel to determine if we're in an active Convoy
        // Change FloatingActionButton behavior depending on if we're
        // currently in a convoy
        val viewmodel = ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java)
        viewmodel.getConvoyId().observe(requireActivity()) { convoyID ->
            viewmodel.getUserJoinedConvoy().observe(requireActivity()){joined ->
                if(!joined){
                    if (convoyID.isNullOrEmpty()) {
                        fab.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
                        fab.setImageResource(android.R.drawable.ic_input_add)
                        fab.setOnClickListener {(activity as DashboardInterface).createConvoy()}
                    } else {
                        fab.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#e91e63"))
                        fab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                        fab.setOnClickListener {(activity as DashboardInterface).endConvoy()}
                    }
                } else{
                    if (convoyID.isNullOrEmpty()) {
                        joinFAB.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
                        joinFAB.setImageResource(R.drawable.join_24px)
                        joinFAB.setOnClickListener {(activity as DashboardInterface).joinConvoy()}
                    } else {
                        joinFAB.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#e91e63"))
                        joinFAB.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                        joinFAB.setOnClickListener {(activity as DashboardInterface).leaveConvoy()}
                    }
                }
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
    interface DashboardInterface {
        fun createConvoy()
        fun joinConvoy()
        fun leaveConvoy()
        fun endConvoy()
        fun logout()
    }

}