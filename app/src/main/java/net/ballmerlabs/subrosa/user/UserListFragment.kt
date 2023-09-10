package net.ballmerlabs.subrosa.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import net.ballmerlabs.subrosa.util.srLog
import java.util.*
import javax.inject.Inject

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class UserListFragment @Inject constructor() : Fragment() {
    private val log by srLog()

    private var columnCount = 1
    private lateinit var binding: FragmentUserlistListBinding

    private lateinit var userAdapter: UserListRecyclerViewAdapter

    @Inject lateinit var repository: NewsRepository

    private suspend fun onUserDelete(uuid: UUID) {
        log.v("onUserDelete $uuid")
        try {
            val res = withContext(Dispatchers.IO) { repository.deleteUser(uuid) }
            if (!res) {
                withContext(Dispatchers.Main) {
                    val toast = Toast(requireContext())
                    toast.setText(R.string.fail_delete_user)
                    toast.show()
                }
            }
        } catch (exc: Exception) {
            log.w("failed to handle user delete: $exc")
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
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    userAdapter = UserListRecyclerViewAdapter(requireContext())
                    repository.observeUsers()
                        .observe(viewLifecycleOwner) { users ->
                            Log.v("debug", "observed users ${users.size}")
                            userAdapter.addItems(users)
                        }
                    adapter = userAdapter
                    userAdapter.setOnDeleteClickListener { uuid ->
                        lifecycleScope.launch {
                            onUserDelete(uuid)
                        }
                    }
                } catch (exc: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch user list: $exc",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        return binding.root
    }
}