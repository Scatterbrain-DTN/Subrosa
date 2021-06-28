package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.thread.ThreadFragmentArgs

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class GroupListFragment : Fragment() {

    private val args: GroupListFragmentArgs by navArgs()
    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GroupListViewModel
    
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater)
        val create = GroupItem(requireContext())
        create.id = View.generateViewId()
        val a = IntArray(args.grouplist.size+1)
        a[a.size-1] = create.id
        binding.listconstraintlayout.addView(create)
        binding.listflow.referencedIds = a
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GroupListViewModel::class.java)
    }
}