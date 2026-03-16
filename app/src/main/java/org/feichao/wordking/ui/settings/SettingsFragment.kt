package org.feichao.wordking.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.feichao.wordking.R

data class SettingItem(val title: String, val subtitle: String)

class SettingsFragment : Fragment() {

    private val items = listOf(
        SettingItem("Git 同步配置", "配置Git仓库和SSH密钥"),
        SettingItem("AI 配置", "配置AI生成题库"),
        SettingItem("通用设置", "每日上限、振动反馈等"),
        SettingItem("关于", "应用版本信息")
    )

    private val targets = listOf(
        GitConfigActivity::class.java,
        AiConfigActivity::class.java,
        GeneralConfigActivity::class.java,
        AboutActivity::class.java
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SettingsAdapter()
        }
    }

    inner class SettingsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return object : RecyclerView.ViewHolder(v) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            holder.itemView.findViewById<TextView>(android.R.id.text1).text = item.title
            holder.itemView.findViewById<TextView>(android.R.id.text2).text = item.subtitle
            holder.itemView.setOnClickListener {
                targets.getOrNull(position)?.let { cls ->
                    startActivity(Intent(requireContext(), cls))
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
