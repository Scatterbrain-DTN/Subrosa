package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.HorizontalScrollView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.subrosa.databinding.ActivityMainBinding
import net.ballmerlabs.subrosa.listing.GroupItem
import net.ballmerlabs.subrosa.listing.GroupListFragmentArgs
import net.ballmerlabs.subrosa.listing.GroupListViewModel
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.user.UserViewFragmentArgs
import net.ballmerlabs.subrosa.util.uuidSha256
import java.security.MessageDigest
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
            binding.flowlayout.addPath(binding.collapsingToolbar.title.toString())
            binding.collapsingToolbar.title = ""
            titleSet = false
        }
        mainViewModel.collapsed.value = true
    }

    private fun onExpanding() {
        if (!titleSet) {
            val s = binding.flowlayout.removePath()
            if (s != null) {
                binding.collapsingToolbar.title = s
                titleSet = true
            } else {
                binding.collapsingToolbar.title = "Empty"
            }
        }
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver.unregister()
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

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
            when(destination.id) {
                R.id.UserViewFragment -> {
                    val args = UserViewFragmentArgs.fromBundle(arguments!!)
                    binding.pathscroll.visibility = View.GONE
                }
                R.id.GroupListFragment -> {
                    val args = GroupListFragmentArgs.fromBundle(arguments!!)
                    Log.e("debug", "groupListFragment navigation: ${args.path.size}")
                    mainViewModel.path.value = args.path.toList()

                }
                else -> binding.pathscroll.visibility = View.VISIBLE
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
                    .navigate(R.id.UserCreationDialog)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}