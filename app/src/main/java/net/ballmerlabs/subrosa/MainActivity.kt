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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.navigation.*
import androidx.navigation.ui.NavigationUI
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

    private lateinit var navController: NavController

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
                withContext(Dispatchers.Main) { binding.connectionLostBanner.show() }
            } else {
                withContext(Dispatchers.Main) { binding.connectionLostBanner.dismiss() }
            }
        }
    }

    private fun setFab(action: Int? = null, icon: Int? = null) {
        binding.fab.setImageResource(icon ?: 0)
        if (action != null) {
            binding.fab.setOnClickListener {
                navController.navigate(action)
            }
            binding.fab.show()
        } else {
            binding.fab.hide()
            binding.fab.setOnClickListener {  }
        }
    }


    private fun setupBottomNavigation() {
        binding.bottomNavigation.background = null
        binding.bottomNavigation.menu[2].apply {
            isEnabled = false
            isVisible = false
        }
    }

    private fun setExpand(expand: Boolean) {
        binding.contentMain.scrollView.isNestedScrollingEnabled = expand
        binding.pathscroll.visibility = if (expand) View.VISIBLE else View.GONE
        val layoutParams = binding.appbarlayout.layoutParams as CoordinatorLayout.LayoutParams
        if (layoutParams.behavior == null) {
            layoutParams.behavior = AppBarLayout.Behavior()
        }
        val behavior = layoutParams.behavior as AppBarLayout.Behavior
        behavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return expand
            }

        })
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupBottomNavigation()

        mainViewModel.path.observe(this) { v ->
            val p = v.map { group ->
                Log.v("debug", "setting path")
                if(group.empty) "root" else group.name
            }.toTypedArray()
            Log.e("debug", "received livedata ${p.size}")
            binding.flowlayout.setPaths(p)
        }
        binding.connectionLostBanner.setLeftButtonListener { b -> b.dismiss() }
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

        navController  = findNavController(R.id.nav_host_fragment)

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        navController.addOnDestinationChangedListener { nav, destination, arguments ->
            Log.v("debug", "navigating to ${destination.id}")
            when(destination.id) {
                R.id.GroupListFragment -> {
                    val args = GroupListFragmentArgs.fromBundle(arguments!!)
                    mainViewModel.path.value = args.path.toList()
                    setFab(
                        action = R.id.action_GroupListFragment_to_postCreationDialog,
                        icon = R.drawable.ic_baseline_email_24
                    )
                    setExpand(true)

                }
                R.id.userListFragment -> {
                    setFab(
                        action = R.id.action_userListFragment_to_UserCreationFragment,
                        icon = R.drawable.ic_baseline_person_add_alt_1_24
                    )
                    setExpand(false)
                }
                else -> {
                    setFab()
                    setExpand(false)
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
                    binding.connectionLostBanner.dismiss()
                } else {
                    binding.connectionLostBanner.show()
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