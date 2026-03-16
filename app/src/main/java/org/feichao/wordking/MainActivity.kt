package org.feichao.wordking

import android.os.Bundle
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.feichao.wordking.data.entity.SyncMeta
import org.feichao.wordking.data.entity.UserConfig
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.databinding.ActivityMainBinding
import org.feichao.wordking.util.Constants
import org.feichao.wordking.util.EncryptUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userConfigRepository: UserConfigRepository

    // 国旗映射
    private val languageFlags = mapOf(
        "en" to "🇺🇸",
        "id" to "🇮🇩",
        "th" to "🇹🇭",
        "ko" to "🇰🇷",
        "ja" to "🇯🇵",
        "es" to "🇪🇸",
        "pt" to "🇵🇹",
        "fr" to "🇫🇷"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化加密工具
        EncryptUtils.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        initDatabase()
        setupLanguageSwitch()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.wordListFragment -> {
                    navController.navigate(R.id.wordListFragment)
                    true
                }
                R.id.settingsFragment -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun initDatabase() {
        lifecycleScope.launch {
            val database = WordKingApplication.instance.database
            val configDao = database.userConfigDao()
            val syncMetaDao = database.syncMetaDao()

            if (configDao.getUserConfigSync() == null) {
                configDao.insertUserConfig(UserConfig())
            }
            if (syncMetaDao.getSyncMetaSync() == null) {
                syncMetaDao.insertSyncMeta(SyncMeta())
            }

            // 初始化语言切换器
            userConfigRepository = UserConfigRepository(configDao)
            updateLanguageFlag()
            observeLanguageChange()
        }
    }

    private fun setupLanguageSwitch() {
        binding.tvLanguageFlag.setOnClickListener { view ->
            showLanguagePopupMenu(view)
        }
    }

    private fun showLanguagePopupMenu(anchor: android.view.View) {
        val popup = PopupMenu(this, anchor)
        Constants.SUPPORT_LANGUAGES.forEach { code ->
            val name = Constants.LANGUAGE_NAMES[code] ?: code
            val flag = languageFlags[code] ?: ""
            popup.menu.add("$flag $name")
        }

        popup.setOnMenuItemClickListener { item ->
            val selectedText = item.title.toString()
            val languageName = selectedText.substring(2).trim()
            val selectedCode = Constants.LANGUAGE_NAMES.entries
                .find { it.value == languageName }?.key ?: return@setOnMenuItemClickListener false

            lifecycleScope.launch {
                userConfigRepository.updateCurrentLanguage(selectedCode)
            }
            true
        }

        popup.show()
    }

    private fun updateLanguageFlag() {
        lifecycleScope.launch {
            val config = userConfigRepository.getUserConfig().first()
            val language = config?.currentLanguage ?: "en"
            binding.tvLanguageFlag.text = languageFlags[language] ?: "🇺🇸"
        }
    }

    private fun observeLanguageChange() {
        lifecycleScope.launch {
            userConfigRepository.getUserConfig().collect { config ->
                config?.let {
                    binding.tvLanguageFlag.text = languageFlags[it.currentLanguage] ?: "🇺🇸"
                }
            }
        }
    }
}
