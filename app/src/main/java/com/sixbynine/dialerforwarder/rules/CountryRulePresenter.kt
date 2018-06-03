package com.sixbynine.dialerforwarder.rules

import android.content.Context
import com.github.tamir7.contacts.Contact
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.google.common.base.Joiner
import com.google.common.base.Preconditions.checkArgument
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.contacts.ContactCache
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfo
import com.sixbynine.dialerforwarder.getDistinctPhoneNumbers
import com.sixbynine.dialerforwarder.getRegionCode
import com.sixbynine.dialerforwarder.inject.ApplicationContext
import com.sixbynine.dialerforwarder.rules.CountryRuleOuterClass.CountryRule
import com.sixbynine.dialerforwarder.rules.RuleOuterClass.Rule
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppManager
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.*

internal class CountryRulePresenter @AutoFactory constructor(
    @Provided @ApplicationContext private val context: Context,
    @Provided private val phoneNumberUtil: PhoneNumberUtil,
    @Provided private val contactCache: ContactCache,
    @Provided private val whatsAppManager: WhatsAppManager,
    private val negated: Boolean,
    private val operator: Int,
    private val countries: List<String>,
    private val dialerAppInfo: DialerAppInfo
) : RulePresenter {

    init {
        checkArgument(operator == RulePresenter.IN)
    }

    override fun matches(contact: Contact): Boolean {
        val phoneNumbers = contact.getDistinctPhoneNumbers()
        if (phoneNumbers.isEmpty()) {
            return false
        }
        return phoneNumbers.all { phoneNumber -> matches(phoneNumber, contact) }
    }

    override fun matches(phoneNumber: String, contact: Contact?): Boolean {
        var matchedContact = contact
        if (matchedContact == null) {
            matchedContact = contactCache.getContactWithPhoneNumber(phoneNumber)
        }

        if ((matchedContact != null && !dialerAppInfo.supportsCallingContact(matchedContact)) ||
            (matchedContact == null && !dialerAppInfo.supportsCallingWithJustNumber(phoneNumber))) {
            return false
        }

        return negated != countries.contains(phoneNumberUtil.getRegionCode(phoneNumber))
    }

    override fun getDescription(): String {
        val countryString = Joiner.on(", ").join(countries.map { country ->
            Locale(
                "", country).getDisplayCountry(
                Locale.getDefault())
        })
        return if (negated) {
            context.getString(
                R.string.country_rule_description_negated, countryString, dialerAppInfo.label)
        } else {
            context.getString(
                R.string.country_rule_description, countryString, dialerAppInfo.label)
        }
    }

    override fun getDialerAppInfo() = dialerAppInfo

    override fun getRule(): RuleOuterClass.Rule {
        return Rule.newBuilder()
            .setType(RulePresenter.TYPE_COUNTRY)
            .setCountryRuleProto(
                CountryRule.newBuilder()
                    .setNegated(negated)
                    .setOperator(operator)
                    .addAllCountries(countries)
                    .setDialerAppInfo(dialerAppInfo.toProto())
                    .build())
            .build()
    }

    override fun isEnabled(): Boolean {
        return !dialerAppInfo.isWhatsApp() || whatsAppManager.isWhatsAppInstalledAndUsesContacts()
    }
}