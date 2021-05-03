package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.navArgs
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GroupListFragment : Fragment() {

    private val args: GroupListFragmentArgs by navArgs()
    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val a = arrayListOf<Int>()
        for (x in args.grouplist.indices) {
            Log.e("debug", "adding $x")
            val text = TextView(context)
            text.text = args.grouplist[x]
            text.id = View.generateViewId()
            a.add(text.id)
            binding.listconstraintlayout.addView(text)
        }
        binding.listflow.referencedIds = a.toIntArray()

    }
}