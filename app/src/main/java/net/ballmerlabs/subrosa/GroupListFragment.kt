package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.listing.GroupListRecyclerViewAdapter
import net.ballmerlabs.subrosa.listing.PostListFragment
import net.ballmerlabs.subrosa.listing.PostListFragmentDirections
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import javax.inject.Inject

@AndroidEntryPoint
class GroupListFragment : Fragment() {

    private lateinit var binding: FragmentGroupListBinding

    @Inject lateinit var repository: NewsRepository

    private val groupAdapter = GroupListRecyclerViewAdapter { group ->
        onGroupListItemClick(group)
    }


    private fun onGroupListItemClick(group: NewsGroup) {
        lifecycleScope.launch {
            val children = withContext(Dispatchers.IO) { repository.getChildren(group.uuid) }
            val action = GroupListFragmentDirections.actionGroupListFragmentToPostListFragment(
                children.toTypedArray(),
                false,
                group,
                arrayOf(group)
            )
            binding.root.findNavController().navigate(action)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupListBinding.inflate(inflater)
        binding.groupListRecyclerview.adapter = groupAdapter
        repository.observeGroups()
            .observe(viewLifecycleOwner) { groups ->
                Log.v("debug", "observed groups ${groups.size}")
                groupAdapter.addItems(groups)
            }
        return binding.root
    }
}