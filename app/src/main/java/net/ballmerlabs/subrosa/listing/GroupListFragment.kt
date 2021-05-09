package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.thread.ThreadFragmentArgs

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
        val a = IntArray(args.grouplist.size)
        for (x in args.grouplist.indices) {
            val v = GroupItemBinding.inflate(inflater, binding.listconstraintlayout, false)
            v.root.id = View.generateViewId()
            v.root.setOnClickListener {  v
                val action =
                    GroupListFragmentDirections.actionGroupListFragmentToThreadFragment(v.name.text.toString())
                findNavController().navigate(action)
            }
            v.name.text = args.grouplist[x]
            binding.listconstraintlayout.addView(v.root)
            a[x] = v.root.id
        }
        binding.listflow.referencedIds = a
        return binding.root
    }
}