package net.ballmerlabs.subrosa

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ballmerlabs.scatterbrainsdk.BinderWrapper
import net.ballmerlabs.scatterbrainsdk.IdentityImportContract
import net.ballmerlabs.scatterbrainsdk.ScatterbrainApi
import net.ballmerlabs.scatterbrainsdk.ScatterbrainBroadcastReceiver
import net.ballmerlabs.subrosa.databinding.ActivityMainBinding
import net.ballmerlabs.subrosa.listing.PostListFragmentArgs
import net.ballmerlabs.subrosa.listing.PostListFragmentDirections
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.user.UserListFragmentDirections
import net.ballmerlabs.subrosa.util.uuidSha256
import java.lang.Exception
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject lateinit var broadcastReceiver: ScatterbrainBroadcastReceiver

    @Inject lateinit var repository: NewsRepository

    private lateinit var navController: NavController

    private val fabExpanded: Boolean
    get() = binding.fabAlt2.translationY != 0.toFloat()

    private val mainViewModel by viewModels<MainViewModel>()

    private val importIdentity = registerForActivityResult(IdentityImportContract()) { idList ->
        if (idList != null) {
            if (idList.size == 1) {
                Log.v("debug", "registered ${idList.size} identities")
                val action =
                    UserListFragmentDirections.actionUserListFragmentToUserCreationFragment(idList.first().fingerprint.toString())
                navController.navigate(action)
            }
        }
    }

    private val adminPermissionListener = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val action =
                UserListFragmentDirections.actionUserListFragmentToUserCreationFragment()
            navController.navigate(action)
        } else {
            val snackbar = Snackbar.make(binding.root,R.string.permissions_fail, Snackbar.LENGTH_LONG)
            snackbar.show()
        }
    }

    private lateinit var navGraph: NavGraph

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
        popTitle()
    }

    private fun onCollapsed() {
        popTitle()
        mainViewModel.collapsed.value = true
    }


    private fun expandFab() {
        Log.v("debug", "fab expanding ${binding.fabAlt2.y}")
        if (!fabExpanded) {
            binding.fabAlt.animate().translationY(-resources.getDimension(R.dimen.fab_expand_lower))
            binding.fabAlt2.animate().translationY(-resources.getDimension(R.dimen.fab_expand_upper))
            binding.fab.setOnClickListener { contractFab() }
        }
    }

    private fun contractFab() {
        Log.v("debug", "fab contracting ${binding.fabAlt2.y}")
        if (fabExpanded) {
            binding.fabAlt.animate().translationY(0.toFloat())
            binding.fabAlt2.animate().translationY(0.toFloat())
            binding.fab.setOnClickListener { expandFab() }
        }
    }

    private fun popTitle() {
        Log.v("debug", "popTitle $fabExpanded")
        if (mainViewModel.strPath.isNotEmpty()) {
            val list = mainViewModel.strPath.toMutableList()
            val front = list.removeLast()
            setTitle(front)
            binding.flowlayout.setPaths(list.toTypedArray())
        }
    }

    private fun onExpanding() {
        popTitle()
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiver.unregister()
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiver.register()
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
    }

    private fun setAppBar(expand: Boolean = false, text: String? = null, isTitleEnabled: Boolean = true) {
        binding.pathscroll.visibility = if (expand) View.VISIBLE else View.GONE
        if(!expand) {
            binding.currentIdenticon.visibility = View.GONE
        } else {
            binding.currentIdenticon.visibility = View.VISIBLE
        }
        (binding.collapsingToolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = if (expand)
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            else
                AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
        }
        setTitle(text, isTitleEnabled = isTitleEnabled)
        ViewCompat.setNestedScrollingEnabled(binding.collapsingToolbar, expand)
        binding.appbarlayout.setExpanded(expand, true)
        binding.appbarlayout.requestFocus()
    }

    private suspend fun checkRouterConnected(): Boolean {
        val res = tryBind()
        if (!res) {
            val toast = Toast(baseContext)
            toast.setText(R.string.router_not_connected)
            binding.connectionLostBanner.show()
            toast.show()
        } else {
            Log.v("debug", "router connected")
            binding.connectionLostBanner.dismiss()
        }
        return res
    }

    private fun setTitle(text: String?, isTitleEnabled: Boolean = true) {
        Log.v("debug", "isTitleEnabled ${binding.collapsingToolbar.title}")
        binding.collapsingToolbar.isTitleEnabled = isTitleEnabled
        binding.toolbar.title = text
        binding.collapsingToolbar.title = text
    }

    private fun setupAppBarLayout() {
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
    }

    private fun setupBanners() {
        binding.connectionLostBanner.setLeftButtonListener { b -> b.dismiss() }
        binding.connectionLostBanner.setRightButtonListener {
            lifecycleScope.launch {
                repository.sdkComponent.binderWrapper.bindService()
                checkRouterConnected()
            }
        }

    }

    private fun setupPathsView() {
        mainViewModel.path.observe(this) { popTitle() }
        setupBanners()
        binding.pathscroll.addOnLayoutChangeListener { _: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            binding.pathscroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
    }

    private fun changeDestinationPostListFragment(arguments: Bundle) {
        val args = PostListFragmentArgs.fromBundle(arguments)
        Log.v("debug", "on newsgroup ${args.parent}")
        mainViewModel.path.value = args.path.toList()
        binding.currentIdenticon.hash = args.parent.hash.contentHashCode()
        mainViewModel.postListType.observe(this) { type ->
            when(type) {
                PostListType.TYPE_POST -> setFab(
                    action = PostListFragmentDirections.actionPostListFragmentToPostCreationDialog(args.parent),
                    icon = R.drawable.ic_baseline_email_24
                )
                PostListType.TYPE_GROUP -> setFab(
                    action = PostListFragmentDirections.actionPostListFragmentToGroupCreateDialog(args.parent),
                    icon = R.drawable.ic_baseline_create_new_folder_24
                )
                null -> Log.e("debug", "type null")
            }

        }
        setAppBar(expand = true, text = args.parent.groupName)

    }

    private fun changeDestinationUserListFragment() {
        mainViewModel.postListType.removeObservers(this)
        setFabExpand(
            lowerIcon = R.drawable.ic_baseline_person_add_alt_1_24,
            upperIcon = R.drawable.ic_baseline_import
        ) { type, _ ->
            val permission = ScatterbrainApi.PERMISSION_ADMIN
            if (ContextCompat.checkSelfPermission(applicationContext, permission)
                == PackageManager.PERMISSION_GRANTED) {
                when (type) {
                    FabType.LOWER -> {
                        val action =
                            UserListFragmentDirections.actionUserListFragmentToUserCreationFragment()
                        navController.navigate(action)
                    }
                    FabType.UPPER -> {
                        importIdentity.launch(1)
                    }

                }
            } else {
                adminPermissionListener.launch(permission)
            }
        }
        setAppBar(false, text = getString(R.string.user_list_title), isTitleEnabled = false)
    }


    private fun changeDestinationGroupListFragment() {
        mainViewModel.postListType.removeObservers(this)
        setFab(
            action = GroupListFragmentDirections.actionGroupListFragmentToGroupCreateDialog(null),
            icon = R.drawable.ic_baseline_create_new_folder_24
        )
        setAppBar(expand = false, text = getString(R.string.grouplist_title), isTitleEnabled = false)
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.v("debug", "nav up")
        if(binding.searchBox.visibility == View.VISIBLE) {
            hideSearch()
        } else {
            navController.popBackStack()
        }
        return super.onSupportNavigateUp()
    }

    private fun handleMenuVisibility(destination: Int) {
        val search = findViewById<View>(R.id.action_search)
        if (destination != R.id.groupListFragment) {
            hideSearch(animate = false)
            search.visibility = View.GONE
        } else {
            search.visibility = View.VISIBLE
        }

        val nested = findViewById<View>(R.id.action_subgroups)
        if (destination != R.id.PostListFragment) {
            nested.visibility = View.GONE
        } else {
            nested.visibility = View.VISIBLE
        }
    }

    private fun setupNavController() {
        navController  = findNavController(R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            Log.v("debug", "navigating to $destination")
            handleMenuVisibility(destination.id)
            when(destination.id) {
                R.id.PostListFragment -> { changeDestinationPostListFragment(arguments!!) }
                R.id.userListFragment -> { changeDestinationUserListFragment() }
                R.id.groupListFragment -> { changeDestinationGroupListFragment() }
                else -> {
                    setFab()
                    mainViewModel.postListType.removeObservers(this)
                    setAppBar(expand = false)
                }
            }
        }
    }

    private fun showSearch() {
        val transtion = Slide(Gravity.TOP)
        transtion.duration = 600
        transtion.addTarget(R.id.search_box)
        TransitionManager.beginDelayedTransition(binding.searchFramelayout, transtion)
        binding.searchBox.visibility = View.VISIBLE
        binding.searchInput.requestFocus()
        val input = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        input.showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT)

    }

    private fun hideSearch(animate: Boolean = true) {
        val input = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        input.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        if (animate) {
            val transtion = Slide(Gravity.TOP)
            transtion.duration = 600
            transtion.addTarget(R.id.search_box)
            TransitionManager.beginDelayedTransition(binding.searchFramelayout, transtion)
        }
        binding.searchBox.visibility = View.INVISIBLE
    }

    private fun setupNavGraph() {
        val inflater = navController.navInflater
        navGraph = inflater.inflate(R.navigation.nav_graph)
        lifecycleScope.launch(Dispatchers.Default) {
            val groups = resources.getStringArray(R.array.default_newsgroups)
                .map { s ->
                    val uuid = uuidSha256(s.encodeToByteArray())
                    NewsGroup(
                        uuid = uuid,
                        groupName = s,
                        parentCol = null,
                        parentHash = null,
                        description = ""
                    ) }

            withContext(Dispatchers.IO) { repository.insertGroup(groups)}
            Log.e("debug", "groups inserted")

            withContext(Dispatchers.Main) { navController.graph = navGraph }
        }
    }

    private fun setupSearch() {
        binding.searchInput.setOnEditorActionListener { view, id, _ ->
            mainViewModel.search.value = view.text.toString().ifBlank { null }
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                hideSearch()
            }
            true
        }
    }

    private suspend fun tryBind(): Boolean {
        return try {
            repository.sdkComponent.binderWrapper.bindService()
            true
        } catch (exc: Exception) {
            Log.e("debug", "failed to bind service")
            exc.printStackTrace()
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupNavController()
        setupNavGraph()
        setupBottomNavigation()
        setupPathsView()
        setupAppBarLayout()
        setupNavController()
        setupSearch()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        lifecycleScope.launch { repository.sdkComponent.binderWrapper.bindService() }
        repository.observeConnectionState()
                .observe(this) { state ->
                    when (state) {
                        BinderWrapper.Companion.BinderState.STATE_DISCONNECTED -> binding.connectionLostBanner.show()
                        BinderWrapper.Companion.BinderState.STATE_CONNECTED -> binding.connectionLostBanner.dismiss()
                        null -> binding.connectionLostBanner.show()
                    }
                }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search -> showSearch()
            android.R.id.home -> onSupportNavigateUp()
        }
        return true
    }
}