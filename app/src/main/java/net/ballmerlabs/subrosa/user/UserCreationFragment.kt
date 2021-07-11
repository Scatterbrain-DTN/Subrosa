package net.ballmerlabs.subrosa.user

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
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