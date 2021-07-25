package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.MainViewModel
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.thread.PostListRecylerViewAdapter
import javax.inject.Inject

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class GroupListFragment @Inject constructor() : Fragment() {

    private val args: GroupListFragmentArgs by navArgs()
    private lateinit var binding: FragmentGroupListBinding
    private val groupList = ArrayList<GroupItem>()
    private val postAdapter = PostListRecylerViewAdapter()

    @Inject lateinit var repository: NewsRepository

    private val viewModel by viewModels<GroupListViewModel>()
    private val activityViewModel by activityViewModels<MainViewModel>()


    private fun getGroupItem(group: NewsGroup): GroupItem {
        val n = GroupItem(requireContext())
        n.id = View.generateViewId()
        n.newsGroup = group
        n.setOnClickListener { v ->
            val i = v as GroupItem

            Log.e("debug", "entering group with parent ${i.newsGroup.name} ${args.path.size}")
            lifecycleScope.launch {
                val children = withContext(Dispatchers.IO) { repository.getChildren(i.newsGroup.uuid) }
                Log.e("debug", "found ${children.size} children")
                val action = GroupListFragmentDirections.actionGroupListFragmentToSelf(
                    children.toTypedArray(),
                    false,
                    i.newsGroup,
                    args.path + arrayOf(i.newsGroup)
                )
                binding.root.findNavController().navigate(action)
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

    private fun addGroupItem(item: GroupItem) {
        if (item.parent != null) {
            (item.parent as ViewGroup).removeView(item)
        }
        binding.listconstraintlayout.addView(item)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupListBinding.inflate(inflater)
        groupList.clear()
        activityViewModel.collapsed.observe(viewLifecycleOwner) { v ->
            if (v)
                binding.listconstraintlayout.visibility = View.GONE
            else
                binding.listconstraintlayout.visibility = View.VISIBLE
        }
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                for (newsGroup in args.grouplist) {
                    val item = getGroupItem(newsGroup)
                    groupList.add(item)
                }
            }
            groupList.forEach { item -> addGroupItem(item) }
            refreshFlow()
        }

        with(binding.threadRecyclerview) {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }

        if (!args.immutable) {
            Log.v("debug", "starting post observation")
            repository.observePosts(args.parent).observe(viewLifecycleOwner) { posts ->
                Log.e("debug", "livedata received posts ${posts.size}")
                postAdapter.values.clear()
                postAdapter.values.addAll(posts)
                postAdapter.notifyDataSetChanged()
            }
        }

        return binding.root
    }

}