package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.databinding.GroupItemBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.thread.ThreadFragmentArgs
import javax.inject.Inject

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class GroupListFragment @Inject constructor() : Fragment() {

    private val args: GroupListFragmentArgs by navArgs()
    private var _binding: FragmentGroupListBinding? = null
    private val binding get() = _binding!!
    private val groupList = ArrayList<GroupItem>()

    @Inject lateinit var repository: NewsRepository

    private lateinit var viewModel: GroupListViewModel
    
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater)
        lifecycleScope.launch {
            for (item in args.grouplist) {
                val n = GroupItem(requireContext())
                n.id = View.generateViewId()
                n.set(item)
                n.setOnClickListener { v ->
                    val i = v as GroupItem
                    if (i.isCreated) {
                        lifecycleScope.launch {
                            val children = repository.getChildren(i.uuid!!)
                            val action = GroupListFragmentDirections.actionGroupListFragmentToSelf(
                                children.toTypedArray(),
                                false
                            )
                            v.findNavController().navigate(action)
                        }
                    }
                }
                binding.listconstraintlayout.addView(n)
                groupList.add(n)
            }
            if (!args.immutable) {
                val create = GroupItem(requireContext())
                create.id = View.generateViewId()
                binding.listconstraintlayout.addView(create)
                groupList.add(create)
            }
            binding.listflow.referencedIds = groupList.map { v -> v.id }.toIntArray()
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GroupListViewModel::class.java)
    }
}