package net.ballmerlabs.subrosa.thread

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.database.User
import net.ballmerlabs.subrosa.databinding.FragmentPostCreationDialogBinding
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PostCreationDialog.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class PostCreationDialog @Inject constructor(): DialogFragment() {
    private lateinit var binding: FragmentPostCreationDialogBinding
    private lateinit var arrayAdapter: ArrayAdapter<User>

    @Inject lateinit var repository: NewsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPostCreationDialogBinding.inflate(inflater)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        arrayAdapter = ArrayAdapter<User>(requireContext(), android.R.layout.simple_dropdown_item_1line)
        lifecycleScope.launch(Dispatchers.IO) {
            val users = repository.readUsers()
            withContext(Dispatchers.Main) {
                arrayAdapter.addAll(users)
                arrayAdapter.notifyDataSetChanged()
            }
        }
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }


}