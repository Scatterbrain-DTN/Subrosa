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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.subrosa.NewsRepository
import net.ballmerlabs.subrosa.databinding.FragmentUserCreationDialogBinding
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [UserCreationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class UserCreationFragment @Inject constructor(): Fragment() {
    private var _binding: FragmentUserCreationDialogBinding? = null
    private val binding get() = _binding!!
    private val req = registerForActivityResult(ActivityResultContracts.OpenDocument()) { res ->
        lifecycleScope.launch(Dispatchers.IO) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, res)
            val bitmap = ImageDecoder.decodeBitmap(source)
            withContext(Dispatchers.Main) { binding.profilepic.setImageBitmap(bitmap) }
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserCreationDialogBinding.inflate(inflater)
        binding.dismissbutton.setOnClickListener { v ->
            v.findNavController().popBackStack()
        }

        binding.profilepic.setOnClickListener { req.launch(arrayOf("image/*")) }

        binding.confirmButton.setOnClickListener { v ->
            lifecycleScope.launch {
                try {
                    val user = repository.createUser(
                        binding.nameedit.editText!!.text.toString(),
                        binding.bioEdit.editText!!.text.toString(),
                        imageBitmap = binding.profilepic.drawable.toBitmap()
                    )
                    Toast.makeText(
                        requireContext(),
                        "created user ${user.name}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    v.findNavController().popBackStack()
                } catch (exc: Exception) {
                    exc.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "scatterbrain router not connected, unable to create user",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }
        return binding.root
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