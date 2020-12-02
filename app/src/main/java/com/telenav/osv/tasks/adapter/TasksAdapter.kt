package com.telenav.osv.tasks.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.telenav.osv.R
import com.telenav.osv.databinding.ItemTaskBinding
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.utils.CurrencyUtil
import com.telenav.osv.tasks.utils.getTaskStatusBackground
import com.telenav.osv.tasks.utils.getTaskStatusColor
import com.telenav.osv.tasks.utils.getTaskStatusContent

class TasksAdapter(private val currencyUtil: CurrencyUtil) :
        RecyclerView.Adapter<TasksAdapter.ItemTaskViewHolder>() {

    private val tasks = mutableListOf<Task>()
    private var itemClick: ((String) -> Unit)? = null
    private var isClickable: Boolean = true

    fun updateData(tasks: List<Task>) {
        this.tasks.clear()
        this.tasks.addAll(tasks)
        notifyDataSetChanged()
    }

    fun setIsClickable(isClickable: Boolean) {
        this.isClickable = isClickable
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTaskViewHolder {
        return ItemTaskViewHolder(DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_task,
                parent,
                false))
                .apply {
                    itemView.setOnClickListener {
                        val adapterPosition = this.adapterPosition
                        if (adapterPosition != RecyclerView.NO_POSITION && isClickable) {
                            itemClick?.invoke(tasks[adapterPosition].id)
                        }
                    }
                }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ItemTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    fun setOnTaskItemClick(click: (String) -> Unit) {
        itemClick = click
    }

    inner class ItemTaskViewHolder(private val itemTaskBinding: ItemTaskBinding) :
            RecyclerView.ViewHolder(itemTaskBinding.root) {
        fun bind(task: Task) {
            val gridStatus = GridStatus.getByStatus(task.status)
            if (gridStatus != null) {
                val context = itemTaskBinding.root.context
                val taskColor = ContextCompat.getColor(context, getTaskStatusColor(gridStatus))
                itemTaskBinding.llGrid.background = context.getDrawable(getTaskStatusBackground(gridStatus))
                itemTaskBinding.tvTitle.setTextColor(taskColor)
                itemTaskBinding.tvStatus.text = context.getString(getTaskStatusContent(gridStatus))
                itemTaskBinding.tvStatus.setTextColor(taskColor)
            }
            itemTaskBinding.tvTitle.text = task.title
            itemTaskBinding.tvAmount.text = currencyUtil.getAmountWithCurrencySymbol(task.currency, task.amount)
        }
    }
}