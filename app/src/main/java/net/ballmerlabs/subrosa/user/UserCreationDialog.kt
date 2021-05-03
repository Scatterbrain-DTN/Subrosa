package net.ballmerlabs.subrosa.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentUserCreationDialogBinding

/**
 * A simple [Fragment] subclass.
 * Use the [UserCreationDialog.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserCreationDialog : DialogFragment() {
    private var _binding: FragmentUserCreationDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserCreationDialogBinding.inflate(inflater)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment UserCreationDialog.
         */
        @JvmStatic
        fun newInstance() = UserCreationDialog().apply {}
    }
}