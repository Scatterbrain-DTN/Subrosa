package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentPostListBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.thread.PostListRecylerViewAdapter
import java.lang.Exception
import java.util.*
import javax.inject.Inject

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class PostListFragment @Inject constructor() : Fragment() {

    private val args: PostListFragmentArgs by navArgs()
    private lateinit var binding: FragmentPostListBinding
    private val postAdapter = PostListRecylerViewAdapter()

    @Inject lateinit var repository: NewsRepository

    private val groupListAdapter = GroupListRecyclerViewAdapter { group -> onGroupListItemClick(group)}


    private fun onGroupListItemClick(group: NewsGroup) {
        Log.e("debug", "entering group with parent ${group.groupName} ${args.path.size}")
        lifecycleScope.launch {
            val children = withContext(Dispatchers.IO) { repository.getChildren(group.uuid) }
            Log.e("debug", "found ${children.size} children")
            val action = PostListFragmentDirections.actionPostListFragmentToSelf(
                children.toTypedArray(),
                false,
                group,
                args.path + arrayOf(group)
            )
            binding.root.findNavController().navigate(action)
        }
    }

    private fun initialSetupGroupList() {
        binding.groupRecyclerview.adapter = groupListAdapter
        if (args.immutable) {
            lifecycleScope.launch(Dispatchers.Main) {
                groupListAdapter.addItems(args.grouplist.asList())
            }
        } else {
            repository.observeChildren(args.parent.uuid).observe(viewLifecycleOwner) { children ->
                Log.v("debug", "observing groups ${children.size}")
                groupListAdapter.addItems(children)
            }
        }
    }

    private suspend fun refreshLatest() {
        if (repository.isConnected()) {
            try {
                val time = repository.getLastSyncTime()
                if (!repository.fullSync(time)) {
                    withContext(Dispatchers.Main) {
                        val toast = Toast(requireContext())
                        toast.setText(R.string.refresh_in_progress)
                        toast.show()
                    }
                } else {
                    repository.setLastSyncTime(Date())
                }
            } catch (exc: Exception) {
                Log.e("debug", "exception: $exc while refreshing posts")
            }
        } else {
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_LONG).show()
        }
    }

    private fun initialSetupPosts() {
        with(binding.threadRecyclerview) {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }

        if (!args.immutable) {
            Log.v("debug", "starting post observation")
            repository.observePosts(args.parent).observe(viewLifecycleOwner) { posts ->
                Log.e("debug", "livedata received posts ${posts.size}")
                postAdapter.addItems(posts)
            }

        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostListBinding.inflate(inflater)
        initialSetupGroupList()
        initialSetupPosts()
        binding.nestedAppbar.setExpanded(false, true)
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                refreshLatest()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        return binding.root
    }

}