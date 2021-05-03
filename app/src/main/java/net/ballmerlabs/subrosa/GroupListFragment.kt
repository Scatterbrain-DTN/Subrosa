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
import net.ballmerlabs.subrosa.databinding.GroupItemBinding

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
        val a = arrayListOf<Int>()
        for (x in args.grouplist.indices) {
            val v = GroupItemBinding.inflate(inflater, binding.listconstraintlayout, false)
            v.root.id = View.generateViewId()
            v.name.text = args.grouplist[x]
            binding.listconstraintlayout.addView(v.root)
            a.add(v.root.id)
        }
        binding.listflow.referencedIds = a.toIntArray()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}