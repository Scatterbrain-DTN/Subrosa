package net.ballmerlabs.subrosa.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.databinding.FragmentUserlistListBinding
import javax.inject.Inject

/**
 * A fragment representing a list of Items.
 */
@AndroidEntryPoint
class UserListFragment @Inject constructor() : Fragment() {

    private var columnCount = 1
    private lateinit var binding: FragmentUserlistListBinding

    @Inject lateinit var repository: NewsRepository

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
                val users = repository.readUsers()
                withContext(Dispatchers.Main) { adapter = UserListRecyclerViewAdapter(users) }
            }
        }
        return binding.root
    }
}