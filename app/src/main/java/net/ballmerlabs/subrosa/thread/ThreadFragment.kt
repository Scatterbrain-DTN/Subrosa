package net.ballmerlabs.subrosa.thread

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import net.ballmerlabs.scatterbrainsdk.Identity
import net.ballmerlabs.subrosa.R
import net.ballmerlabs.subrosa.databinding.ThreadFragmentBinding

class ThreadFragment : Fragment() {
    private val args: ThreadFragmentArgs by navArgs()
    private var _binding: ThreadFragmentBinding? = null
    private val binding get() = _binding!!
    private val postList = ArrayList<Int>()

    companion object {
        fun newInstance() = ThreadFragment()
    }

    private lateinit var viewModel: ThreadViewModel

    private fun addPost(body: String, name: String, fingerprint: String) {
        val p = Post(requireContext())
        p.body = body
        p.fingerprint = fingerprint
        p.name = name
        p.id = View.generateViewId()
        postList.add(p.id)
        binding.threadLayout.addView(p)
        binding.postFlow.referencedIds = postList.toIntArray()
    }
    
    private fun addPost(body: String, sender: Identity) {
        addPost(body, sender.givenname, sender.fingerprint)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ThreadFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addPost("testbody", "testname", "testfingerprint")
    }
    
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ThreadViewModel::class.java)
        // TODO: Use the ViewModel
    }

}