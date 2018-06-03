package com.sixbynine.dialerforwarder.rules

import android.content.SharedPreferences
import android.util.Base64
import com.github.tamir7.contacts.Contact
import com.google.common.base.Preconditions
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.sixbynine.dialerforwarder.contacts.ContactCache
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfoFactory
import com.sixbynine.dialerforwarder.event.ShouldUseContactSettingChangedEvent
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.rules.ContactRuleOuterClass.ContactRule
import com.sixbynine.dialerforwarder.rules.CountryRuleOuterClass.CountryRule
import com.sixbynine.dialerforwarder.rules.RulesOuterClass.Rules
import javax.inject.Inject

@ActivityScoped
class RuleManager @Inject constructor(
    private val preferences: SharedPreferences,
    private val countryRuleFactory: CountryRulePresenterFactory,
    private val contactRulePresenterFactory: ContactRulePresenterFactory,
    private val dialerAppInfoFactory: DialerAppInfoFactory,
    private val eventBus: EventBus,
    private val executor: ListeningScheduledExecutorService,
    private val contactCache: ContactCache
) {

    internal val rules = getRules()

    init {
        eventBus.register(this)
    }

    private fun getRules(): MutableList<RulePresenter> {
        return preferences.getString("rules", null)
            ?.let { serializedRules ->
                Rules.parseFrom(Base64.decode(serializedRules, Base64.DEFAULT))
                    .rulesList
                    .mapNotNull(this::fromProto).toMutableList()
            }
            ?: ArrayList()
    }

    fun getDialerAppInfo(phoneNumber: String, contact: Contact?) =
        rules.firstOrNull { rule -> rule.matches(phoneNumber, contact) }?.getDialerAppInfo()

    private fun fromProto(ruleProto: RuleOuterClass.Rule): RulePresenter? {
        when (ruleProto.type) {
            RulePresenter.TYPE_COUNTRY -> {
                Preconditions.checkArgument(ruleProto.hasCountryRuleProto())
                val countryRuleProto: CountryRule = ruleProto.countryRuleProto
                val dialerAppInfo =
                    dialerAppInfoFactory.fromProto(countryRuleProto.dialerAppInfo) ?: return null

                return countryRuleFactory.create(
                    countryRuleProto.negated,
                    countryRuleProto.operator,
                    countryRuleProto.countriesList,
                    dialerAppInfo
                )
            }
            RulePresenter.TYPE_CONTACT -> {
                Preconditions.checkArgument(ruleProto.hasContactRuleProto())
                val contactRuleProto: ContactRule = ruleProto.contactRuleProto
                val dialerAppInfo =
                    dialerAppInfoFactory.fromProto(contactRuleProto.dialerAppInfo) ?: return null

                val contact = contactCache.getAllContactsWithPhoneNumbers()
                    .firstOrNull { contact -> contact.id == contactRuleProto.contactId }
                    ?: return null

                return contactRulePresenterFactory.create(contact, dialerAppInfo)
            }
            else -> throw IllegalArgumentException()
        }
    }

    internal fun addRule(newRule: RulePresenter) {
        rules.add(0, newRule)
        onRulesChanged()
    }

    internal fun removeRule(rule: RulePresenter) {
        rules.remove(rule)
        onRulesChanged()
    }

    internal fun addRuleForContact(newRule: RulePresenter, contact: Contact) {
        rules.removeAll { rulePresenter ->
            rulePresenter is ContactRulePresenter && rulePresenter.matches(
                contact
            )
        }
        rules.add(0, newRule)
        onRulesChanged()
    }

    internal fun moveRule(rule: RulePresenter, up: Boolean): Boolean {
        val index = rules.indexOf(rule)
        Preconditions.checkArgument(index != -1)
        if (up && index > 0) {
            rules.removeAt(index)
            rules.add(index - 1, rule)
            onRulesChanged()
            return true
        } else if (!up && index < rules.size - 1) {
            rules.removeAt(index)
            rules.add(index + 1, rule)
            onRulesChanged()
            return true
        }
        return false
    }

    private fun onRulesChanged() {
        executor.submit {
            val rulesProto = Rules.newBuilder()
                .addAllRules(rules.map(RulePresenter::getRule))
                .build()
            preferences.edit()
                .putString("rules", Base64.encodeToString(rulesProto.toByteArray(), Base64.DEFAULT))
                .apply()
        }

        eventBus.post(RulesChangedEvent())
    }

    @Subscribe
    internal fun onShouldUseContactSettingChanged(event: ShouldUseContactSettingChangedEvent) {
        val newRules = getRules().filter(RulePresenter::isEnabled)
        if (newRules != rules) {
            rules.clear()
            rules.addAll(newRules)
        }
        eventBus.post(RulesChangedEvent())
    }
}