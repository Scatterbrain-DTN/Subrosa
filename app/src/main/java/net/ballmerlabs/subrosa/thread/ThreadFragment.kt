package net.ballmerlabs.subrosa.thread

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ballmerlabs.subrosa.R

class ThreadFragment : Fragment() {

    companion object {
        fun newInstance() = ThreadFragment()
    }

    private lateinit var viewModel: ThreadViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.thread_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ThreadViewModel::class.java)
        // TODO: Use the ViewModel
    }

}