package org.feichao.wordking.ui.wordlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.feichao.wordking.R
import org.feichao.wordking.WordKingApplication
import org.feichao.wordking.data.entity.Word
import org.feichao.wordking.data.repository.UserConfigRepository
import org.feichao.wordking.data.repository.WordRepository
import org.feichao.wordking.service.AiGenerateService
import org.feichao.wordking.util.Constants

class WordListFragment : Fragment() {

    private lateinit var wordRepository: WordRepository
    private lateinit var userConfigRepository: UserConfigRepository
    private lateinit var aiGenerateService: AiGenerateService
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnAiGenerate: MaterialButton
    private lateinit var progressAi: ProgressBar

    private var currentTab = 0
    private var currentLanguage = "en"
    private val adapter = WordAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = WordKingApplication.instance.database
        wordRepository = WordRepository(database.wordDao())
        userConfigRepository = UserConfigRepository(database.userConfigDao())
        aiGenerateService = AiGenerateService(requireContext())

        recyclerView = view.findViewById(R.id.recycler_view)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tabLayout = view.findViewById(R.id.tab_layout)
        btnAiGenerate = view.findViewById(R.id.btn_ai_generate)
        progressAi = view.findViewById(R.id.progress_ai)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Tab切换监听
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadWords()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // AI生成按钮点击
        btnAiGenerate.setOnClickListener {
            generateWords()
        }

        observeConfig()
    }

    private fun observeConfig() {
        viewLifecycleOwner.lifecycleScope.launch {
            userConfigRepository.getUserConfig().collectLatest { config ->
                config?.let {
                    currentLanguage = it.currentLanguage
                    loadWords()
                }
            }
        }
    }

    private fun loadWords() {
        viewLifecycleOwner.lifecycleScope.launch {
            val words = when (currentTab) {
                0 -> wordRepository.getAllWordsSync().filter { it.languageCode == currentLanguage }
                1 -> wordRepository.getAllWordsSync().filter { it.languageCode == currentLanguage && it.stage == 0 }
                2 -> wordRepository.getAllWordsSync().filter { it.languageCode == currentLanguage && it.stage in 1..11 }
                3 -> wordRepository.getAllWordsSync().filter { it.languageCode == currentLanguage && it.stage == 12 }
                else -> emptyList()
            }

            adapter.submitList(words)
            tvEmpty.visibility = if (words.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun generateWords() {
        btnAiGenerate.isEnabled = false
        progressAi.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val targetCount = Constants.AiConfig.MANUAL_GENERATE_MAX  // 目标：50个新词
                val allNewWords = mutableListOf<Word>()
                val maxAttempts = 5  // 最多尝试5次

                // 循环调用AI直到获取足够的新词
                for (attempt in 1..maxAttempts) {
                    if (!isAdded) break

                    val result = aiGenerateService.generateWords(
                        currentLanguage,
                        Constants.AiConfig.MANUAL_GENERATE_MAX,
                        "MANUAL"
                    )

                    val words = result.getOrNull()
                    if (words != null) {
                        val existingWords = wordRepository.getAllOriginalWords(currentLanguage)
                        val newWords = words.filter { it.originalWord !in existingWords }

                        if (newWords.isNotEmpty()) {
                            allNewWords.addAll(newWords)
                        }
                    }

                    if (allNewWords.size >= targetCount) {
                        break
                    }
                }

                if (!isAdded) return@launch

                progressAi.visibility = View.GONE
                btnAiGenerate.isEnabled = true

                // 保存到数据库（去重）
                if (allNewWords.isNotEmpty()) {
                    val uniqueNewWords = allNewWords.distinctBy { it.originalWord }
                    wordRepository.insertWords(uniqueNewWords)
                    loadWords()
                    Toast.makeText(
                        requireContext(),
                        "生成成功！新增${uniqueNewWords.size}个单词（${Constants.LANGUAGE_NAMES[currentLanguage]}）",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "没有新单词生成", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    progressAi.visibility = View.GONE
                    btnAiGenerate.isEnabled = true
                    Toast.makeText(requireContext(), "生成失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadWords()
    }

    /**
     * 根据正确次数计算星级
     * 0：未学
     * 1：见过但没正确过
     * 2：正确过1次
     * 3：正确过3次
     * 4：正确过10次
     * 5：正确超过20次
     */
    private fun calculateStarLevel(word: Word): Int {
        return when {
            word.stage == 0 -> 0  // 未学
            word.correctStreak == 0 -> 1  // 见过但没正确过
            word.correctStreak >= 20 -> 5  // 正确超过20次
            word.correctStreak >= 10 -> 4  // 正确过10次
            word.correctStreak >= 3 -> 3  // 正确过3次
            word.correctStreak >= 1 -> 2  // 正确过1次
            else -> 1
        }
    }

    inner class WordAdapter : RecyclerView.Adapter<WordAdapter.ViewHolder>() {
        // ViewHolder is inner class now
        private var words = listOf<Word>()

        fun submitList(list: List<Word>) {
            words = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_word, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val word = words[position]

            // 设置单词文本
            holder.tvOriginalWord.text = word.originalWord
            holder.tvChinese.text = word.chineseTranslation

            // 计算星级
            val starLevel = calculateStarLevel(word)

            // 设置星星显示
            val stars = listOf(
                holder.star1, holder.star2, holder.star3, holder.star4, holder.star5
            )
            stars.forEachIndexed { index, textView ->
                textView.alpha = if (index < starLevel) 1.0f else 0.3f
            }
        }

        override fun getItemCount() = words.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvOriginalWord: TextView
            val tvChinese: TextView
            val star1: TextView
            val star2: TextView
            val star3: TextView
            val star4: TextView
            val star5: TextView

            init {
                tvOriginalWord = view.findViewById(R.id.tv_original_word)
                tvChinese = view.findViewById(R.id.tv_chinese)
                star1 = view.findViewById(R.id.star_1)
                star2 = view.findViewById(R.id.star_2)
                star3 = view.findViewById(R.id.star_3)
                star4 = view.findViewById(R.id.star_4)
                star5 = view.findViewById(R.id.star_5)
            }
        }
    }
}
