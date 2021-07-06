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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.subrosa.MainViewModel
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.databinding.FragmentGroupListBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.thread.Post
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
    private val postList = ArrayList<Int>()
    private lateinit var nameListener: (name: String) -> Unit

    @Inject lateinit var repository: NewsRepository

    private val viewModel by viewModels<GroupListViewModel>()
    private val activityViewModel by activityViewModels<MainViewModel>()


    private fun addPost(body: String, sender: Identity) {
        addPost(body, sender.givenname, sender.fingerprint.toString())
    }

    private fun addPost(body: String, name: String, fingerprint: String) {
        val p = Post(requireContext())
        p.body = body
        p.fingerprint = fingerprint
        p.name = name
        p.id = View.generateViewId()
        p.maxCardElevation = 128.toFloat()
        postList.add(p.id)
        binding.threadLayout.addView(p)
        binding.postFlow.referencedIds = postList.toIntArray()
    }

    private fun getGroupItem(group: NewsGroup?): GroupItem {
        val n = GroupItem(requireContext())
        n.id = View.generateViewId()
        if (group != null) {
            n.set(group)
        }
        n.setOnClickListener { v ->
            val i = v as GroupItem
            if (i.isCreated) {
                Log.e("debug", "entering group with parent ${i.newsGroup!!.name} ${args.path.size}")
                lifecycleScope.launch {
                    val children = withContext(Dispatchers.IO) { repository.getChildren(i.newsGroup!!.uuid) }
                    Log.e("debug", "found ${children.size} children")
                    val action = GroupListFragmentDirections.actionGroupListFragmentToSelf(
                        children.toTypedArray(),
                        false,
                        i.newsGroup!!,
                        args.path + arrayOf(i.newsGroup!!)
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
        _binding = FragmentGroupListBinding.inflate(inflater)
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
            if (!args.immutable) {
                val create = getGroupItem(null)
                nameListener = { name ->
                    lifecycleScope.launch {
                        Log.e("debug", "creating group with parent ${args.parent.name}")
                        val g = repository.createGroup(name, args.parent)
                        create.set(g)
                        val newCreate = getGroupItem(null)
                        newCreate.setOnNameListener(nameListener)
                        addGroupItem(newCreate)
                        groupList.add(newCreate)
                        refreshFlow()
                    }
                }
                create.setOnNameListener(nameListener)
                withContext(Dispatchers.Main) {
                    addGroupItem(create)
                    groupList.add(create)
                }
            }
            refreshFlow()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addPost("testbody", "testname", "testfingerprint")
    }
}