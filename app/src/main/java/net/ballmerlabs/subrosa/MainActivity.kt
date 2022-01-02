package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.HorizontalScrollView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.navigation.*
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.subrosa.databinding.ActivityMainBinding
import net.ballmerlabs.subrosa.listing.GroupListFragmentArgs
import net.ballmerlabs.subrosa.listing.GroupListFragmentDirections
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.user.UserListFragmentDirections
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

    private var fabExpanded = false

    private val mainViewModel by viewModels<MainViewModel>()

    enum class State {
        IDLE,
        COLLAPSED,
        EXPANDED,
        EXPANDING
    }

    enum class FabType {
        UPPER,
        LOWER
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


    private fun expandFab() {
        if (!fabExpanded) {
            binding.fabAlt.animate().translationY(-resources.getDimension(R.dimen.fab_expand_lower))
            binding.fabAlt2.animate().translationY(-resources.getDimension(R.dimen.fab_expand_upper))
            binding.fab.setOnClickListener { contractFab() }
            fabExpanded = true
        }
    }

    private fun contractFab() {
        if (fabExpanded) {
            binding.fabAlt.animate().translationY(0.toFloat())
            binding.fabAlt2.animate().translationY(0.toFloat())
            binding.fab.setOnClickListener { expandFab() }
            fabExpanded = false
        }
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

    private fun setFab(action: NavDirections? = null, icon: Int? = null) {
        contractFab()
        binding.fab.setImageResource(icon ?: 0)
        if (action != null) {
            binding.fab.setOnClickListener {
                navController.navigate(action)
            }
            binding.fab.show()
            binding.fabAlt.show()
            binding.fabAlt2.show()
        } else {
            binding.fabAlt.hide()
            binding.fabAlt2.hide()
            binding.fab.hide()
            binding.fab.setOnClickListener {  }
        }
    }


    private fun setFabExpand(
        mainIcon: Int = R.drawable.ic_baseline_add_24,
        lowerIcon: Int? = null,
        upperIcon: Int? = null,
        action: (type: FabType, fab: FloatingActionButton) -> Unit
        ) {
        binding.fabAlt.setOnClickListener {
            contractFab()
            action(FabType.LOWER ,binding.fabAlt)
        }
        binding.fabAlt2.setOnClickListener {
            contractFab()
            action(FabType.UPPER, binding.fabAlt2)
        }
        binding.fab.setImageResource(mainIcon)
        binding.fabAlt.setImageResource(lowerIcon ?: 0)
        binding.fabAlt2.setImageResource(upperIcon ?: 0)
        binding.fab.setOnClickListener { expandFab() }
        binding.fab.show()
        binding.fabAlt.show()
        binding.fabAlt2.show()
    }


    private fun setupBottomNavigation() {
        binding.bottomNavigation.background = null
        binding.bottomNavigation.menu[2].apply {
            isEnabled = false
            isVisible = false
        }
    }

    private fun setAppBar(expand: Boolean = false, text: String? = null) {
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
        binding.collapsingToolbar.setCollapsedTitleTextAppearance(
            if (text == null) R.style.Transparent else R.style.TextAppearance_AppCompat_Large
        )
        binding.collapsingToolbar.title = text?: ""
    }

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

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            Log.v("debug", "navigating to $destination")
            when(destination.id) {
                R.id.GroupListFragment -> {
                    val args = GroupListFragmentArgs.fromBundle(arguments!!)
                    Log.v("debug", "on newsgroup ${args.parent}")
                    mainViewModel.path.value = args.path.toList()
                    binding.toolbar.title = args.parent.name
                    setFabExpand(
                        lowerIcon = R.drawable.ic_baseline_create_new_folder_24,
                        upperIcon = R.drawable.ic_baseline_email_24
                    ) { type, _ ->
                        when(type) {
                            FabType.UPPER -> {
                                val action = GroupListFragmentDirections.actionGroupListFragmentToPostCreationDialog(args.parent)
                                navController.navigate(action)
                            }
                            FabType.LOWER -> {
                                binding.appbarlayout.setExpanded(true)
                                val action = GroupListFragmentDirections.actionGroupListFragmentToGroupCreateDialog(args.parent)
                                navController.navigate(action)
                            }
                        }
                    }
                    setAppBar(expand = true)

                }
                R.id.userListFragment -> {
                    setFab(
                        action = UserListFragmentDirections.actionUserListFragmentToUserCreationFragment(),
                        icon = R.drawable.ic_baseline_person_add_alt_1_24
                    )
                    setAppBar(false, text = "Users")
                }
                else -> {
                    setFab()
                    setAppBar(false)
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
}