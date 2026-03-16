package org.feichao.wordking.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.databinding.ActivityGeneralConfigBinding
import org.feichao.wordking.util.Constants

class GeneralConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeneralConfigBinding
    private lateinit var userConfigRepository: UserConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneralConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = WordKingApplication.instance.database
        userConfigRepository = UserConfigRepository(database.userConfigDao())

        loadConfig()
        setupButtons()
    }

    private fun loadConfig() {
        lifecycleScope.launch {
            val config = userConfigRepository.getUserConfigSync()
            config?.let {
                binding.etDailyLimit.setText(it.dailyNewWordLimit.toString())
                binding.switchVibrate.isChecked = it.vibrateEnabled
                binding.switchAutoGenerate.isChecked = it.autoGenerateWord
                binding.switchCheckUpdate.isChecked = it.checkUpdateOnStart
            }
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveConfig()
        }
    }

    private fun saveConfig() {
        val dailyLimit = binding.etDailyLimit.text.toString().toIntOrNull()
            ?: Constants.Defaults.DAILY_NEW_WORD_LIMIT
        val vibrateEnabled = binding.switchVibrate.isChecked
        val autoGenerate = binding.switchAutoGenerate.isChecked
        val checkUpdate = binding.switchCheckUpdate.isChecked

        lifecycleScope.launch {
            userConfigRepository.updateDailyLimit(dailyLimit)
            userConfigRepository.updateVibrateEnabled(vibrateEnabled)
            userConfigRepository.updateAutoGenerate(autoGenerate)
            userConfigRepository.updateCheckUpdate(checkUpdate)

            Toast.makeText(this@GeneralConfigActivity, "设置已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
