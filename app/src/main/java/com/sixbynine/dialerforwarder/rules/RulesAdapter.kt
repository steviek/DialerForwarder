package com.sixbynine.dialerforwarder.rules

import android.app.Activity
import android.graphics.Color
import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SwitchCompat
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.inflateChild
import com.sixbynine.dialerforwarder.inject.UiThread
import com.sixbynine.dialerforwarder.toInt
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppManager
import com.sixbynine.dialerforwarder.whenDoneAnimating
import java.util.concurrent.Executor

@AutoFactory
internal class RulesAdapter(
    @Provided activity: Activity,
    @Provided private val whatsAppManager: WhatsAppManager,
    @Provided private val permissionChecker: PermissionChecker,
    @Provided @UiThread private val uiThreadExecutor: Executor,
    var rules: MutableList<RulePresenter>,
    private val listener: Listener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal interface Listener {
        fun onCreateNewRule()

        fun onRuleClicked(rule: RulePresenter)
    }

    @DrawableRes
    private val selectedItemBackgroundResID: Int

    init {
        val outValue = TypedValue()
        activity.theme.resolveAttribute(
            android.R.attr.selectableItemBackground, outValue, true
        )
        selectedItemBackgroundResID = outValue.resourceId
    }

    var selectedRule: RulePresenter? = null

    companion object {
        private const val VIEW_TYPE_RULE = 0
        private const val VIEW_TYPE_CREATE = 1
        private const val VIEW_TYPE_SETTING = 2

        private const val TAG = "RulesAdapter"
    }

    internal class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val description: TextView = itemView.findViewById(R.id.description)
        val appImage: ImageView = itemView.findViewById(R.id.app_image)
    }

    internal class CreateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal class SettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val switch = itemView as SwitchCompat
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < rules.size -> VIEW_TYPE_RULE
            position == rules.size -> VIEW_TYPE_CREATE
            else -> VIEW_TYPE_SETTING
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_RULE -> RuleViewHolder(parent.inflateChild(R.layout.rule_list_item))
            VIEW_TYPE_CREATE -> CreateViewHolder(parent.inflateChild(R.layout.add_rule_list_item))
            VIEW_TYPE_SETTING -> SettingViewHolder(parent.inflateChild(R.layout.switch_list_item))
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is RuleViewHolder) {
            if (holder is CreateViewHolder) {
                holder.itemView.setOnClickListener { _ -> listener.onCreateNewRule() }
            } else if (holder is SettingViewHolder) {
                val setting = settings.getPresentSettingAtIndex(position)
                holder.switch.isChecked = setting.isChecked()
                holder.switch.isEnabled = setting.isEnabled()
                holder.switch.setText(setting.textResId)
                holder.switch.setOnCheckedChangeListener { view, isChecked ->
                    view.whenDoneAnimating { setting.onCheckedChanged(isChecked) }
                }
            }
            return
        }

        val rule = rules[position]
        holder.description.text = rule.getDescription()
        holder.appImage.setImageDrawable(rule.getDialerAppInfo().icon)
        holder.itemView.setOnClickListener { _ -> listener.onRuleClicked(rule) }

        if (rule == selectedRule) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.itemView.setBackgroundResource(selectedItemBackgroundResID)
        }
    }

    override fun getItemCount() =
        rules.size + 1 + settings.getPresentSettings().size

    private val settings = object {

        fun getPresentSettings(): List<Setting> {
            return arrayListOf(makeCallsSetting, showContactsSetting, whatsAppSetting).filter(
                Setting::isPresent
            )
        }

        fun getPresentSettingAtIndex(index: Int): Setting {
            return getPresentSettings()[index - rules.size - 1]
        }
    }

    private fun getIndexForSetting(setting: Setting): Int {
        var index = rules.size + 1
        if (setting == makeCallsSetting) {
            return index
        } else {
            index += makeCallsSetting.isPresent().toInt()
        }

        if (setting == showContactsSetting) {
            return index
        } else {
            index += showContactsSetting.isPresent().toInt()
        }

        if (setting == whatsAppSetting) {
            return index
        } else {
            index += whatsAppSetting.isPresent().toInt()
        }

        throw IllegalArgumentException()
    }

    private val showContactsSetting = object : Setting {

        override val textResId = R.string.use_contacts

        override fun isChecked(): Boolean {
            return permissionChecker.shouldUseContacts()
        }

        override fun onCheckedChanged(isChecked: Boolean) {
            Futures.addCallback(
                permissionChecker.setShouldUseContacts(isChecked),
                object : FutureCallback<Boolean> {
                    override fun onSuccess(result: Boolean?) {
                        if (result != isChecked) {
                            notifyDataSetChanged()
                        } else if (whatsAppSetting.isPresent()) {
                            notifyItemChanged(getIndexForSetting(whatsAppSetting))
                        }
                    }

                    override fun onFailure(t: Throwable?) {
                        Log.w(TAG, "Failed to update contacts: ", t)
                    }
                },
                uiThreadExecutor
            )
        }
    }

    private val makeCallsSetting = object : Setting {

        override val textResId = R.string.make_calls

        override fun isChecked(): Boolean {
            return permissionChecker.shouldMakeCalls()
        }

        override fun onCheckedChanged(isChecked: Boolean) {
            Futures.addCallback(
                permissionChecker.setShouldMakeCalls(isChecked),
                object : FutureCallback<Boolean> {
                    override fun onSuccess(result: Boolean?) {
                        if (result != isChecked) {
                            notifyDataSetChanged()
                        }
                    }

                    override fun onFailure(t: Throwable?) {
                        Log.w(TAG, "Failed to update make calls: ", t)
                    }
                },
                uiThreadExecutor
            )
        }
    }

    private val whatsAppSetting = object : Setting {

        override val textResId = R.string.always_use_whatsapp

        override fun isPresent(): Boolean {
            return whatsAppManager.isWhatsAppInstalled()
        }

        override fun isEnabled(): Boolean {
            return whatsAppManager.isWhatsAppInstalled() && permissionChecker.shouldUseContacts()
        }

        override fun isChecked(): Boolean {
            return permissionChecker.shouldUseContacts() && whatsAppManager.shouldAlwaysUseWhatsAppToCallWhatsAppContacts()
        }

        override fun onCheckedChanged(isChecked: Boolean) {
            whatsAppManager.setShouldAlwaysUseWhatsAppToCallWhatsAppContacts(isChecked)
        }
    }

    internal interface Setting {

        val textResId: Int

        fun isPresent(): Boolean = true

        fun isEnabled(): Boolean = true

        fun isChecked(): Boolean

        fun onCheckedChanged(isChecked: Boolean)
    }
}