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
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentUserCreationDialogBinding
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [UserCreationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class UserCreationFragment @Inject constructor(): DialogFragment() {
    private lateinit var binding: FragmentUserCreationDialogBinding
    private val req = registerForActivityResult(ActivityResultContracts.OpenDocument()) { res ->
        if (res != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, res)
                val bitmap = ImageDecoder.decodeBitmap(source)
                withContext(Dispatchers.Main) { binding.profilepic.setImageBitmap(bitmap) }
            }
        }
    }

    @Inject lateinit var repository: NewsRepository

    private fun DialogFragment.setWidth(percent: Int) {
        val p = percent.toFloat() / 100
        val dm = Resources.getSystem().displayMetrics
        val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
        val percentWidth = rect.width() * p
        dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun commit() {
        repository.coroutineScope.launch {
            try {
                val user = repository.createUser(
                    binding.nameedit.editText!!.text.toString(),
                    binding.bioEdit.editText!!.text.toString(),
                    imageBitmap = binding.profilepic.drawable.toBitmap()
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "created user ${user.name}",
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment UserCreationFragment.
         */
        @JvmStatic
        fun newInstance() = UserCreationFragment().apply {}
    }
}