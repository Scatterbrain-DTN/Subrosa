package net.ballmerlabs.subrosa.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentUserlistListBinding
import java.util.*
import javax.inject.Inject

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class UserListFragment @Inject constructor() : Fragment() {

    private var columnCount = 1
    private lateinit var binding: FragmentUserlistListBinding

    private lateinit var userAdapter: UserListRecyclerViewAdapter

    @Inject lateinit var repository: NewsRepository

    private suspend fun onUserDelete(uuid: UUID) {
        Log.v("debug", "onUserDelete $uuid")
        val res = withContext(Dispatchers.IO) { repository.deleteUser(uuid) }
        if (!res) {
            withContext(Dispatchers.Main) {
                val toast = Toast(requireContext())
                toast.setText(R.string.fail_delete_user)
                toast.show()
            }
        } else {
           withContext(Dispatchers.Main) {  userAdapter.delItem(uuid) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserlistListBinding.inflate(inflater)
        with(binding.list) {
            layoutManager = when {
                columnCount <= 1 -> LinearLayoutManager(context)
                else -> GridLayoutManager(context, columnCount)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    userAdapter = UserListRecyclerViewAdapter(requireContext())
                    adapter = userAdapter
                    userAdapter.setOnDeleteClickListener { uuid ->
                        lifecycleScope.launch {
                            onUserDelete(uuid)
                        }
                    }
                }
            }
        }
        repository.observeUsers()
            .observe(viewLifecycleOwner) { users ->
                Log.v("debug", "observed users ${users.size}" )
                users.forEach { u ->
                    userAdapter.addItem(u)
                }
            }

        return binding.root
    }
}