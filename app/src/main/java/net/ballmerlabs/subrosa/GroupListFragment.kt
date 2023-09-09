package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.switchMap
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.listing.GroupListRecyclerViewAdapter
import net.ballmerlabs.subrosa.listing.GroupListViewModel
import net.ballmerlabs.subrosa.listing.PostListFragment
import net.ballmerlabs.subrosa.listing.PostListFragmentDirections
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import javax.inject.Inject

@AndroidEntryPoint
class GroupListFragment : Fragment() {

    private lateinit var binding: FragmentGroupListBinding

    @Inject lateinit var repository: NewsRepository

    private val mainViewModel by activityViewModels<MainViewModel>()

    private lateinit var groupAdapter: GroupListRecyclerViewAdapter


    private fun onGroupListItemClick(group: NewsGroup) {
        lifecycleScope.launch {
            try {
                val children = withContext(Dispatchers.IO) { repository.getChildren(group.uuid) }
                val action = GroupListFragmentDirections.actionGroupListFragmentToPostListFragment(
                    children.toTypedArray(),
                    false,
                    group,
                    arrayOf(group)
                )
                binding.root.findNavController().navigate(action)
            } catch (exc: Exception) {
                Log.w("debug", "failed to navigate to group $group: $exc")
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupListBinding.inflate(inflater)
        groupAdapter = GroupListRecyclerViewAdapter(repository) { group ->
            onGroupListItemClick(group)
        }
        binding.groupListRecyclerview.adapter = groupAdapter

        mainViewModel.search.switchMap { search ->
            if (search != null) {
                repository.observeGroups(search)
            } else {
                repository.observeGroups()
            }
        }.observe(viewLifecycleOwner) { groups ->
                Log.v("debug", "observed groups ${groups.size}")
                groupAdapter.addItems(groups)
            }
        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search -> Log.v("debug", "search2")
        }
        return true
    }
}