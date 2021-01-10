package com.amarchaud.estats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // nav host
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.my_first_host_fragment) as NavHostFragment
        navController = navHostFragment.navController


        // top Fragment (no arrow displayed, and display hamburger if there is a drawerlayout)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment,
                R.id.requestPositionFragment,
                R.id.mainFragment
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