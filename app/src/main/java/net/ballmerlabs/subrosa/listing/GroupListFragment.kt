package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.util.Log
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

    private fun getGroupItem(group: NewsGroup): GroupItem {
        val n = GroupItem(requireContext())
        n.id = View.generateViewId()
        n.set(group)
        n.setOnClickListener { v ->
            val i = v as GroupItem
            if (i.isCreated) {
                Log.e("debug", "entering group with parent ${i.newsGroup!!.name}")
                lifecycleScope.launch {
                    val children = repository.getChildren(i.newsGroup!!.uuid)
                    Log.e("debug", "found ${children.size} children")
                    val action = GroupListFragmentDirections.actionGroupListFragmentToSelf(
                        children.toTypedArray(),
                        false,
                        i.newsGroup!!
                    )
                    v.findNavController().navigate(action)
                }
            }
        }
        return n
    }

    private fun refreshFlow() {
        binding.listconstraintlayout.invalidate()
        binding.listflow.invalidate()
        binding.listflow.referencedIds = groupList.map { v -> v.id }.toIntArray()
    }

    private fun appendNewsGroup(group: NewsGroup) {
        val item = getGroupItem(group)
        binding.listconstraintlayout.addView(item)
        groupList.add(item)
        refreshFlow()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupListBinding.inflate(inflater)
        lifecycleScope.launch {
            for (newsGroup in args.grouplist) {
                val item = getGroupItem(newsGroup)
                binding.listconstraintlayout.addView(item)
                groupList.add(item)
            }
            if (!args.immutable) {
                val create = GroupItem(requireContext())
                create.id = View.generateViewId()
                create.setOnNameListener { name ->
                    lifecycleScope.launch {
                        Log.e("debug", "creating group with parent ${args.parent.name}")
                        val g = repository.createGroup(name, args.parent)
                        create.set(g)
                        val newCreate = GroupItem(requireContext())
                        binding.listconstraintlayout.addView(newCreate)
                        groupList.add(newCreate)
                        refreshFlow()
                    }
                }
                binding.listconstraintlayout.addView(create)
                groupList.add(create)
            }
            refreshFlow()
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(GroupListViewModel::class.java)
    }
}