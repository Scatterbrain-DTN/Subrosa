package net.ballmerlabs.subrosa.listing

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.FragmentSearchDialogBinding

@AndroidEntryPoint
class SearchDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentSearchDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View  {
        binding = FragmentSearchDialogBinding.inflate(inflater)
        val window = dialog?.window
        if (window != null) {
            window.setGravity(Gravity.TOP)
            window.attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }
        return binding.root
    }

    companion object {
        const val TAG = "SearchDialogFragment"
    }
}