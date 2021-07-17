package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.*
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.subrosa.databinding.ActivityMainBinding
import net.ballmerlabs.subrosa.listing.GroupListFragmentArgs
import net.ballmerlabs.subrosa.listing.GroupListViewModel
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.user.UserCreationFragment
import net.ballmerlabs.subrosa.user.UserViewFragmentArgs
import net.ballmerlabs.subrosa.util.uuidSha256
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var titleSet = false
    
    @Inject lateinit var broadcastReceiver: ScatterbrainBroadcastReceiver

    @Inject lateinit var repository: NewsRepository

    private val mainViewModel by viewModels<MainViewModel>()
    private val groupListViewModel by viewModels<GroupListViewModel>()

    enum class State {
        IDLE,
        COLLAPSED,
        EXPANDED,
        EXPANDING
    }

    private var barState = State.IDLE
    
    private fun onExpanded() {
        mainViewModel.collapsed.value = false
    }

    private fun onCollapsed() {
        if (titleSet) {
            val list = mainViewModel.strPath.toTypedArray()
            binding.flowlayout.setPaths(list)
            binding.collapsingToolbar.title = ""
            titleSet = false
        }
        mainViewModel.collapsed.value = true
    }

    private fun onExpanding() {
        if (!titleSet) {
            val list = mainViewModel.strPath.toMutableList()
            val front = list.removeLast()
            binding.flowlayout.setPaths(list.toTypedArray())
            binding.collapsingToolbar.title = front
            titleSet = true
        }
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver.unregister()
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
        lifecycleScope.launch(Dispatchers.IO) {
            if (!repository.isConnected()) {
                withContext(Dispatchers.Main) { binding.contentMain.connectionLostBanner.show() }
            } else {
                withContext(Dispatchers.Main) { binding.contentMain.connectionLostBanner.dismiss() }
            }
        }
    }

    private fun defaultFab() {
        binding.fab.setOnClickListener { view ->
            if (view.findNavController().currentDestination!!.id == R.id.GroupListFragment) {
                view.findNavController().navigate(R.id.action_GroupListFragment_to_postCreationDialog)
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.bottomNavigation.background = null

        mainViewModel.path.observe(this) { v ->
            val p = v.map { group ->
                Log.v("debug", "setting path")
                if(group.empty) "root" else group.name
            }.toTypedArray()
            Log.e("debug", "received livedata ${p.size}")
            binding.flowlayout.setPaths(p)
        }
        binding.appbarlayout.setExpanded(false)
        binding.appbarlayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener {
                appBarLayout, verticalOffset ->
            when (abs(verticalOffset)) {
                appBarLayout.totalScrollRange -> {
                    if (barState != State.COLLAPSED) {
                        onCollapsed()
                    }
                    barState = State.COLLAPSED
                }
                0 ->  {
                    if (barState != State.EXPANDED) {
                        onExpanded()
                    }
                    barState = State.EXPANDED
                }
                in 1..appBarLayout.totalScrollRange -> {
                    if (barState != State.EXPANDING) {
                        onExpanding()
                    }
                    barState = State.EXPANDING
                }
                else -> {
                    barState = State.IDLE
                }
            }
        })
        binding.pathscroll.addOnLayoutChangeListener { _: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            binding.pathscroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }

        val navController = findNavController(R.id.nav_host_fragment)

        navController.addOnDestinationChangedListener { nav, destination, arguments ->
            Log.v("debug", "navigating to ${destination.id}")
            when(destination.id) {
                R.id.UserViewFragment -> {
                    val args = UserViewFragmentArgs.fromBundle(arguments!!)
                    binding.pathscroll.visibility = View.GONE
                    defaultFab()
                }
                R.id.GroupListFragment -> {
                    val args = GroupListFragmentArgs.fromBundle(arguments!!)
                    Log.e("debug", "groupListFragment navigation: ${args.path.size}")
                    mainViewModel.path.value = args.path.toList()
                    binding.pathscroll.visibility = View.VISIBLE
                    binding.fab.show()
                    defaultFab()

                }
                R.id.UserCreationFragment -> {
                    binding.pathscroll.visibility = View.GONE
                    binding.contentMain.scrollView.scrollTo(0, binding.contentMain.scrollView.bottom)
                    binding.fab.hide()
                }
                else -> {
                    binding.pathscroll.visibility = View.GONE
                    binding.fab.hide()
                    defaultFab()
                }
            }
        }

        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph)
        lifecycleScope.launch(Dispatchers.Default) {
            val groups = resources.getStringArray(R.array.default_newsgroups)
                .map { s ->
                    val uuid = uuidSha256(s.encodeToByteArray())
                    NewsGroup(
                        uuid = uuid,
                        name = s,
                        parentCol = null,
                        parentHash = null
                ) }

            withContext(Dispatchers.IO) { repository.insertGroup(groups)}
            Log.e("debug", "groups inserted")
            val arg = NavArgument.Builder().setDefaultValue(groups.toTypedArray()).build()
            val top = NavArgument.Builder().setDefaultValue(NewsGroup.empty()).build()
            val path = NavArgument.Builder().setDefaultValue(arrayOf(NewsGroup.empty())).build()
            val immutable = NavArgument.Builder().setDefaultValue(true).build()
            graph[R.id.GroupListFragment].addArgument("grouplist", arg)
            graph[R.id.GroupListFragment].addArgument("immutable", immutable)
            graph[R.id.GroupListFragment].addArgument("parent", top)
            graph[R.id.GroupListFragment].addArgument("path", path)
            withContext(Dispatchers.Main) { navController.graph = graph }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            repository.observeConnections().collect { c ->
                if (c) {
                    binding.contentMain.connectionLostBanner.dismiss()
                } else {
                    binding.contentMain.connectionLostBanner.show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_create_user -> {
                findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.UserCreationFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}