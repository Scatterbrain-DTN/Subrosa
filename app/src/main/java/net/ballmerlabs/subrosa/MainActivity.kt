package net.ballmerlabs.subrosa

import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.widget.HorizontalScrollView
import androidx.core.view.size
import androidx.navigation.NavArgument
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.navigation.get
import androidx.navigation.navArgs
import com.google.android.material.appbar.AppBarLayout
import net.ballmerlabs.subrosa.databinding.ActivityMainBinding
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var titleSet = false
    private val args: GroupListFragmentArgs by navArgs()



    enum class State {
        IDLE,
        COLLAPSED,
        EXPANDED,
        EXPANDING
    }

    private var barState = State.IDLE
    
    private fun onExpanded() {

    }

    private fun onCollapsed() {
        if (titleSet) {
            binding.flowlayout.addPath(binding.collapsingToolbar.title.toString())
            binding.collapsingToolbar.title = ""
            titleSet = false
        }
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
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
        binding.flowlayout.setPaths(arrayOf("fmef1", "fmef2", "fmef3", "fmefverylong4"))
        binding.pathscroll.addOnLayoutChangeListener { _: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            binding.pathscroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }

        val navController = findNavController(R.id.nav_host_fragment)
        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph)
        val manyFmefs = sequence {
            for (x in 0..30) {
                yield("fmef $x")
            }
        }.toList().toTypedArray()
        val arg = NavArgument.Builder().setDefaultValue(manyFmefs).build()
        graph[R.id.FirstFragment].arguments.keys.forEach {
            graph.addArgument(it, arg)
        }
        navController.graph = graph
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}