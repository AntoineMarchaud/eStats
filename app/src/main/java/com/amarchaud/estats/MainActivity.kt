package com.amarchaud.estats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.amarchaud.estats.databinding.ActivityMainBinding
import com.amarchaud.estats.view.MapFragment.Companion.MODE_NORMAL
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // nav host
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.my_first_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.splashFragment -> {
                    supportActionBar?.hide()
                    binding.bottomNav.visibility = View.GONE
                }
                R.id.requestPositionFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                R.id.mapFragment -> {
                    val argument = NavArgument.Builder().setDefaultValue(MODE_NORMAL).build()
                    destination.addArgument("Mode", argument)
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }

        // bottom nav
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)

        // top Fragment (no arrow displayed, and display hamburger if there is a drawerlayout)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment,
                R.id.requestPositionFragment,
                R.id.mainFragment,
                R.id.mapFragment
            )
        )

        // actionBar config
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.my_first_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}