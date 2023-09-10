package net.ballmerlabs.subrosa.listing

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.MainViewModel
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.PostListType
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentPostListBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.thread.PostListRecylerViewAdapter
import net.ballmerlabs.subrosa.util.srLog
import java.util.*
import javax.inject.Inject
import kotlin.Exception

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class PostListFragment @Inject constructor() : Fragment() {
    private val log by srLog()
    private val args: PostListFragmentArgs by navArgs()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var binding: FragmentPostListBinding
    private val postAdapter = PostListRecylerViewAdapter()

    @Inject lateinit var repository: NewsRepository

    private val groupListAdapter = GroupListRecyclerViewAdapter { group -> onGroupListItemClick(group)}


    private fun onGroupListItemClick(group: NewsGroup) {
        log.e("entering group with parent ${group.groupName} ${args.path.size}")
        lifecycleScope.launch {
            try {
                val children = withContext(Dispatchers.IO) { repository.getChildren(group.uuid) }
                log.e("found ${children.size} children")
                val action = PostListFragmentDirections.actionPostListFragmentToSelf(
                    children.toTypedArray(),
                    false,
                    group,
                    args.path + arrayOf(group)
                )
                binding.root.findNavController().navigate(action)
            } catch (exc: Exception) {
                log.e("exception on group click $exc")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error opening group: $exc",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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
                log.v("observing groups ${children.size}")
                setGroupText(children)
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
                log.e("exception: $exc while refreshing posts")
            }
        } else {
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_LONG).show()
        }
    }

    private fun setGroupText(groups: List<NewsGroup>) {
        binding.groupsGoneText.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initialSetupPosts() {
        with(binding.threadRecyclerview) {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }

        if (!args.immutable) {
            log.v("starting post observation")
            repository.observePosts(args.parent).observe(viewLifecycleOwner) { posts ->
                log.e("livedata received posts ${posts.size}")
                postAdapter.values = posts.toMutableList()
                postAdapter.notifyDataSetChanged()
            }

        }
    }

    private fun openGroupList() {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            if (binding.slidingPaneLayout.isOpen) {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
        }

        binding.slidingPaneLayout.open()
    }

    private fun setupSlidingPaneLayout() {
        binding.slidingPaneLayout.addPanelSlideListener(object: SlidingPaneLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {}

            override fun onPanelOpened(panel: View) {
                mainViewModel.postListType.postValue(PostListType.TYPE_GROUP)
            }

            override fun onPanelClosed(panel: View) {
                mainViewModel.postListType.postValue(PostListType.TYPE_POST)
            }

        })
        if(binding.slidingPaneLayout.isOpen) {
            mainViewModel.postListType.postValue(PostListType.TYPE_POST)
        } else {
            mainViewModel.postListType.postValue(PostListType.TYPE_GROUP)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostListBinding.inflate(inflater)
        initialSetupGroupList()
        initialSetupPosts()
        setupSlidingPaneLayout()
        binding.nestedAppbar.setExpanded(false, true)
        binding.descriptionText.text = args.parent.description.ifEmpty {
            getString(R.string.default_description)
        }
        val menu = requireActivity().findViewById<View>(R.id.action_subgroups)
        binding.slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_UNLOCKED
        menu?.setOnClickListener { openGroupList() }
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                refreshLatest()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        return binding.root
    }

}