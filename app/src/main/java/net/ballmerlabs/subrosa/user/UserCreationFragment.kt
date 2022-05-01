package net.ballmerlabs.subrosa.user

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentUserCreationDialogBinding
import net.ballmerlabs.subrosa.scatterbrain.User
import java.util.*
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [UserCreationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class UserCreationFragment @Inject constructor(): DialogFragment() {
    private lateinit var binding: FragmentUserCreationDialogBinding
    private var imageSet = false
    private val args by navArgs<UserCreationFragmentArgs>()
    private val req = registerForActivityResult(ActivityResultContracts.OpenDocument()) { res ->
        if (res != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, res)
                val bitmap = User.resizeBitmapCentered(ImageDecoder.decodeBitmap(source), IMAGE_WIDTH)
                withContext(Dispatchers.Main) { binding.profilepic.setImageBitmap(bitmap) }
                imageSet = true
            }
        }
    }

    @Inject lateinit var repository: NewsRepository

    private fun commit() {
        repository.coroutineScope.launch {
            try {
                val user = repository.createUser(
                    binding.nameedit.editText!!.text.toString(),
                    binding.bioEdit.editText!!.text.toString(),
                    imageBitmap = if (imageSet)
                        binding.profilepic.drawable.toBitmap()
                    else
                        null,
                    identity = if (args.uuid == null) null else UUID.fromString(args.uuid)
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "created user ${user.userName}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } catch (exc: Exception) {
                exc.printStackTrace()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "scatterbrain router not connected, unable to create user",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserCreationDialogBinding.inflate(inflater)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.profilepic.setOnClickListener { req.launch(arrayOf("image/*")) }
        binding.confirmButton.setOnClickListener {
            commit()
            findNavController().popBackStack()
        }
        (binding.profilepic.layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintMaxWidth = Resources.getSystem().displayMetrics.widthPixels
        }
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    companion object {
        const val IMAGE_WIDTH = 512
        @JvmStatic
        fun newInstance() = UserCreationFragment().apply {}
    }
}