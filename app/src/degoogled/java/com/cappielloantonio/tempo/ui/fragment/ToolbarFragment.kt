package com.cappielloantonio.tempo.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentToolbarBinding
import com.cappielloantonio.tempo.ui.activity.MainActivity

@UnstableApi
class ToolbarFragment : Fragment() {

    private var _bind: FragmentToolbarBinding? = null
    private val bind get() = _bind!!
    private lateinit var activity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_page_menu, menu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = getActivity() as MainActivity
        _bind = FragmentToolbarBinding.inflate(inflater, container, false)
        return bind.root
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                activity.navController.navigate(R.id.searchFragment)
                true
            }
            R.id.action_settings -> {
                activity.navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bind = null
    }

    companion object {
        private const val TAG = "ToolbarFragment"
    }
}
