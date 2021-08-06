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
    private val postAdapter = PostListRecylerViewAdapter()

    @Inject lateinit var repository: NewsRepository

    private val viewModel by viewModels<GroupListViewModel>()
    private val activityViewModel by activityViewModels<MainViewModel>()

    private val groupListAdapter = GroupListRecyclerViewAdapter { group -> onGroupListItemClick(group)}


    private fun onGroupListItemClick(group: NewsGroup) {
        Log.e("debug", "entering group with parent ${group.name} ${args.path.size}")
        lifecycleScope.launch {
            val children = withContext(Dispatchers.IO) { repository.getChildren(group.uuid) }
            Log.e("debug", "found ${children.size} children")
            val action = GroupListFragmentDirections.actionGroupListFragmentToSelf(
                children.toTypedArray(),
                false,
                group,
                args.path + arrayOf(group)
            )
            binding.root.findNavController().navigate(action)
        }
    }

    private fun setEmpty(empty: Boolean) {
        if(empty) {
            binding.noPostsTextview.visibility = View.VISIBLE
            binding.threadRecyclerview.visibility = View.GONE
        } else {
            binding.noPostsTextview.visibility = View.GONE
            binding.threadRecyclerview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupListBinding.inflate(inflater)
        groupListAdapter.values.clear()
        binding.groupRecyclerview.adapter = groupListAdapter
        binding.postScrollView.isNestedScrollingEnabled = true

        with(binding.threadRecyclerview) {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }

        if (args.immutable) {
            lifecycleScope.launch(Dispatchers.Main) {
                groupListAdapter.values.addAll(args.grouplist)
                groupListAdapter.notifyDataSetChanged()
            }
        } else {
            Log.v("debug", "starting post observation")
            repository.observePosts(args.parent).observe(viewLifecycleOwner) { posts ->
                Log.e("debug", "livedata received posts ${posts.size}")
                setEmpty(posts.isEmpty())
                postAdapter.values.clear()
                postAdapter.values.addAll(posts)
                postAdapter.notifyDataSetChanged()
            }

            repository.observeChildren(args.parent.uuid).observe(viewLifecycleOwner) { children ->
                Log.v("debug", "observing groups ${children.size}")
                groupListAdapter.values.clear()
                groupListAdapter.values.addAll(children)
                groupListAdapter.notifyDataSetChanged()
            }
        }

        return binding.root
    }

}