package com.telenav.osv.tasks.adapter

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.telenav.osv.R
import com.telenav.osv.databinding.ItemOperationLogBinding
import com.telenav.osv.report.model.ClosedRoadType
import com.telenav.osv.report.utils.getClosedRoadContent
import com.telenav.osv.report.utils.getClosedRoadIcon
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.model.OperationLog
import com.telenav.osv.tasks.model.OperationLogAction
import com.telenav.osv.tasks.utils.CurrencyUtil
import com.telenav.osv.tasks.utils.getTaskStatusColor
import com.telenav.osv.tasks.utils.getTaskStatusContent
import com.telenav.osv.tasks.utils.getTaskStatusIcon
import com.telenav.osv.utils.LogUtils
import com.telenav.osv.utils.UiUtils.setData
import com.telenav.osv.utils.Utils
import java.lang.NumberFormatException
import java.util.Date

private const val THOUSAND_MULTIPLIER = 1000

/**
 * This class provides operation list to recycler view
 * It helps in updating data dynamically
 */
class OperationLogsAdapter(private val currencyUtil: CurrencyUtil) :
        RecyclerView.Adapter<OperationLogsAdapter.ItemOperationLogViewHolder>() {

    private val operationLogs = mutableListOf<OperationLog>()
    private val TAG = OperationLogsAdapter::class.java.simpleName
    private var currency: String? = null

    fun setCurrency(currency: String) {
        this.currency = currency
    }

    /**
     * This method helps in updating data for operation logs
     * @param operationLogs operation logs list to be updated
     */
    fun updateOperationLogs(operationLogs: List<OperationLog>) {
        this.operationLogs.clear()
        this.operationLogs.addAll(operationLogs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemOperationLogViewHolder {
        return ItemOperationLogViewHolder(DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_operation_log,
                parent,
                false))
    }

    override fun getItemCount(): Int {
        return operationLogs.size
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ItemOperationLogViewHolder, position: Int) {
        holder.bind(operationLogs[position])
    }

    inner class ItemOperationLogViewHolder(private val itemOperationLogBinding: ItemOperationLogBinding) :
            RecyclerView.ViewHolder(itemOperationLogBinding.root) {
        fun bind(operationLog: OperationLog) {
            setData(operationLog.note, itemOperationLogBinding.tvNote)
            setData(operationLog.createdByName, itemOperationLogBinding.tvName)
            val action = OperationLogAction.getByAction(operationLog.action)
            var actionIcon: Drawable? = null
            var actionText: SpannableString? = null
            val context = itemOperationLogBinding.root.context
            if (action != null) {
                when(action) {
                    OperationLogAction.CHANGE_STATUS -> {
                        val data = setDataForChangeStatus(context, operationLog)
                        actionIcon = data.first
                        actionText = data.second
                    }
                    OperationLogAction.ADD_NOTE -> {
                        actionIcon = context.getDrawable(R.drawable.vector_add_note)
                        actionText = SpannableString(context.getString(R.string.grid_log_add_note))
                    }
                    OperationLogAction.PICK_UP_GRID -> {
                        actionIcon = context.getDrawable(R.drawable.vector_pick_up_task)
                        actionText = SpannableString(context.getString(R.string.grid_log_pick_up))
                    }
                    OperationLogAction.GIVE_UP_GRID -> {
                        actionIcon = context.getDrawable(R.drawable.vector_give_up_task)
                        actionText = SpannableString(context.getString(R.string.grid_log_give_up))
                    }
                    OperationLogAction.UPDATE_AMOUNT -> {
                        val data = setDataForUpdateAmount(context, operationLog)
                        actionIcon = data.first
                        actionText = data.second
                    }
                    OperationLogAction.GRID_CREATION -> {
                        actionIcon = context.getDrawable(R.drawable.vector_grid_status_to_do)
                        actionText = SpannableString(context.getString(R.string.grid_log_create))
                    }
                    OperationLogAction.REPORT_CLOSED_ROAD -> {
                        val data = setDataForClosedRoad(context, operationLog)
                        actionIcon = data.first
                        actionText = data.second
                    }
                }
            }
            itemOperationLogBinding.ivStatus.setImageDrawable(actionIcon)
            setData(actionText, itemOperationLogBinding.tvAction)
            try {
                setData(Utils.operationLogDateFormat.format(
                        Date(operationLog.updatedAt*THOUSAND_MULTIPLIER)), itemOperationLogBinding.tvUpdatedAt)
            } catch (e: IllegalArgumentException) {
                LogUtils.logDebug(TAG, "Exception while converting updated at to date format")
            }
        }

        private fun setDataForChangeStatus(context: Context, operationLog: OperationLog): Pair<Drawable?, SpannableString?> {
            var actionIcon: Drawable? = null
            var actionText: SpannableString? = null
            val actionValue = operationLog.actionValue
            if (!actionValue.isNullOrEmpty()) {
                try {
                    val gridStatus = GridStatus.getByStatus(actionValue.toInt())
                    if (gridStatus != null) {
                        actionIcon = context.getDrawable(getTaskStatusIcon(gridStatus))
                        val statusText = context.getString(getTaskStatusContent(gridStatus))
                        actionText = SpannableString(String.format(context.getString(R.string.grid_log_changed_status), statusText))
                        val startIndex = actionText.length - statusText.length
                        val endIndex = actionText.length
                        actionText.setSpan(ForegroundColorSpan(ContextCompat.getColor(
                                context, getTaskStatusColor(gridStatus))), startIndex, endIndex, 0)
                        actionText.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, 0)
                    }
                } catch (e: NumberFormatException) {
                    LogUtils.logDebug(TAG, "Operation log action value for change status not a number: $actionValue")
                }
            }

            return Pair(actionIcon, actionText)
        }

        private fun setDataForUpdateAmount(context: Context, operationLog: OperationLog): Pair<Drawable?, SpannableString?> {
            val actionIcon = context.getDrawable(R.drawable.vector_amount)
            var actionText: SpannableString? = null
            val actionValue = operationLog.actionValue
            if (!actionValue.isNullOrEmpty()) {
                try {
                    val taskAmount = actionValue.toDouble()
                    val currentCurrency = currency
                    val amountText = if (currentCurrency.isNullOrEmpty()) {
                        taskAmount.toString()
                    } else {
                        currencyUtil.getAmountWithCurrencySymbol(currentCurrency, taskAmount)
                    }
                    actionText = SpannableString(String.format(context.getString(R.string.grid_log_update_amount), amountText))
                    val startIndex = actionText.length - amountText.length
                    val endIndex = actionText.length
                    actionText.setSpan(ForegroundColorSpan(ContextCompat.getColor(
                            context, R.color.color_15ccc1)), startIndex, endIndex, 0)
                    actionText.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, 0)
                } catch (e: NumberFormatException) {
                    LogUtils.logDebug(TAG, "Operation log action value for update amount not a number: $actionValue")
                }
            }
            return Pair(actionIcon, actionText)
        }
    }

    private fun setDataForClosedRoad(context: Context, operationLog: OperationLog): Pair<Drawable?, SpannableString?> {
        var actionIcon: Drawable? = null
        var actionText: SpannableString? = null
        val actionValue = operationLog.actionValue
        if (!actionValue.isNullOrEmpty()) {
            try {
                val closedRoadType = ClosedRoadType.getByType(actionValue.toInt())
                if (closedRoadType != null) {
                    actionIcon = context.getDrawable(getClosedRoadIcon(closedRoadType))
                    actionText = SpannableString(context.getString(getClosedRoadContent(closedRoadType)))
                }
            } catch (e: NumberFormatException) {
                LogUtils.logDebug(TAG, "Operation log action value for closed road not a number: $actionValue")
            }
        }

        return Pair(actionIcon, actionText)
    }
}