package com.sixbynine.dialerforwarder.rules

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.event.ModifyRuleActionModeEndedEvent
import com.sixbynine.dialerforwarder.event.ModifyRuleActionModeStartedEvent
import com.sixbynine.dialerforwarder.event.ShouldUseContactSettingChangedEvent
import com.sixbynine.dialerforwarder.inject.BaseFragment
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppManager
import javax.inject.Inject

class RulesFragment : BaseFragment() {

    @Inject
    internal lateinit var ruleManager: RuleManager

    @Inject
    internal lateinit var eventBus: EventBus

    @Inject
    internal lateinit var whatsAppManager: WhatsAppManager

    @Inject
    internal lateinit var rulesAdapterFactory: RulesAdapterFactory

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RulesAdapter

    private val adapterListener = object : RulesAdapter.Listener {
        override fun onCreateNewRule() {
            AddCountryRuleDialogFragment().show(activity!!.supportFragmentManager, "Add Rule")
        }

        override fun onRuleClicked(rule: RulePresenter) {

            val callback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.menuInflater.inflate(R.menu.menu_rule, menu)
                    adapter.selectedRule = rule
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
                    val index = ruleManager.rules.indexOf(rule)
                    menu.findItem(R.id.action_move_rule_down).isVisible =
                        index < ruleManager.rules.size - 1
                    menu.findItem(R.id.action_move_rule_up).isVisible =
                        index > 0
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    when (item.itemId) {
                        R.id.action_delete -> {
                            ruleManager.removeRule(rule)
                            mode.finish()
                            return true
                        }
                        R.id.action_move_rule_down -> {
                            ruleManager.moveRule(rule, up = false)
                            mode.invalidate()
                            return true
                        }
                        R.id.action_move_rule_up -> {
                            ruleManager.moveRule(rule, up = true)
                            mode.invalidate()
                            return true
                        }
                    }
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                    actionMode = null
                    eventBus.post(ModifyRuleActionModeEndedEvent())
                    adapter.selectedRule = null
                    syncViews()
                }
            }

            actionMode = activity!!.startActionMode(callback)
            eventBus.post(ModifyRuleActionModeStartedEvent())
            syncViews()
        }
    }

    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rules, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        adapter = rulesAdapterFactory.create(ruleManager.rules, adapterListener)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(inflater.context)

        return view
    }

    @Subscribe
    internal fun onRulesChanged(event: RulesChangedEvent) = syncViews()

    @Subscribe
    internal fun onShouldUseContactSettingChanged(event: ShouldUseContactSettingChangedEvent) {
        actionMode?.let(ActionMode::finish)
    }

    private fun syncViews() {
        adapter.rules = ruleManager.rules
        adapter.notifyDataSetChanged()
    }
}