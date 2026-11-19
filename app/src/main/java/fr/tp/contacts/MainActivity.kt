package fr.tp.contacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                openFragment(ParametresFragment(), "settings")
                true
            } else false
        }
        toolbar.setNavigationOnClickListener { supportFragmentManager.popBackStack() }
        supportFragmentManager.addOnBackStackChangedListener { updateToolbar() }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ListeContactsFragment())
                .commit()
        }
        updateToolbar()
    }

    private fun openFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(tag).commit()
    }

    private fun updateToolbar() {
        if (!::toolbar.isInitialized) return
        val canGoBack = supportFragmentManager.backStackEntryCount > 0
        if (canGoBack) toolbar.setNavigationIcon(android.R.drawable.ic_media_previous)
        else toolbar.navigationIcon = null
        toolbar.title = if (!canGoBack) "Contacts V4" else when (supportFragmentManager.findFragmentById(R.id.fragment_container)) {
            is ParametresFragment -> "Paramètres"
            is DetailContactFragment -> "Fiche contact"
            is CarteContactFragment -> "Carte du contact"
            else -> "Contacts V4"
        }
        toolbar.menu.findItem(R.id.action_settings)?.isVisible =
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { it is ListeContactsFragment } ?: true
    }
}
