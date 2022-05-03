package com.mintanable.notethepad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mintanable.notethepad.features.domain.util.Screen
import com.mintanable.notethepad.features.presentation.modify.AddEditNoteScreen
import com.mintanable.notethepad.features.presentation.modify.components.NotesScreen
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent{
            NoteThePadTheme{
                Surface(
                    color= MaterialTheme.colors.background
                ){
                    val navController= rememberNavController()
                    NavHost(
                        navController=navController,
                        startDestination = Screen.NotesScreen.route
                    ){
                        composable(route= Screen.NotesScreen.route){
                            NotesScreen(navController = navController)
                        }
                        composable(
                            route= Screen.AddEditNoteScreen.route + "?noteId={noteId)&noteColor={noteColor}",
                            arguments = listOf(
                                navArgument(
                                    name="noteId"
                                ){
                                    type = NavType.IntType
                                    defaultValue = -1
                                } ,
                                navArgument(
                                        name="noteColor"
                                        ){
                                    type = NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ){

                            val color = it.arguments?.getInt("noteColor")?:-1
                            AddEditNoteScreen(
                                navController = navController,
                                noteColor = color)
                        }
                    }
                }

            }
        }
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setSupportActionBar(binding.toolbar)
//
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//
//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}