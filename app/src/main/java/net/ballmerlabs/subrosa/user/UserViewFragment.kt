package net.ballmerlabs.subrosa.user

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ballmerlabs.subrosa.R

class UserViewFragment : Fragment() {

    companion object {
        fun newInstance() = UserViewFragment()
    }

    private lateinit var viewModel: UserViewViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.user_view_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(UserViewViewModel::class.java)
        // TODO: Use the ViewModel
    }

}