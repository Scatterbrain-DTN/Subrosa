package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentGroupCreateDialogBinding
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import java.util.*
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [GroupCreateDialog.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class GroupCreateDialog @Inject constructor() : DialogFragment() {
    private lateinit var binding: FragmentGroupCreateDialogBinding

    private val args: GroupCreateDialogArgs by navArgs()

    @Inject lateinit var repository: NewsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    private fun validate(): Boolean {
        val text = binding.gcNameEdittext.text
        return !text.isNullOrBlank()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupCreateDialogBinding.inflate(inflater)
        binding.groupcreateToolbar.setNavigationOnClickListener { dismiss() }
        binding.groupcreateToolbar.title = "creating child group of ${args.parent?.groupName}"
        binding.gcConfirmButton.setOnClickListener {
            if (validate()) {
                repository.coroutineScope.launch(Dispatchers.Default) {
                    val text = binding.gcNameEdittext.text
                    val desc = binding.gcDescription.text
                    val group = NewsGroup(
                        uuid = UUID.randomUUID(),
                        parentCol = args.parent?.uuid,
                        groupName = text.toString(),
                        parentHash = args.parent?.hash,
                        description = desc.toString()
                    )
                    repository.insertGroup(group)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "created group $text", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                findNavController().popBackStack()
            } else {
                Snackbar.make(binding.root, "Invalid group name", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
        // Inflate the layout for this fragment
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