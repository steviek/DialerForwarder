package com.sixbynine.dialerforwarder.rules

import android.content.Context
import com.github.tamir7.contacts.Contact
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.contacts.ContactCache
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfo
import com.sixbynine.dialerforwarder.inject.ApplicationContext
import com.sixbynine.dialerforwarder.rules.ContactRuleOuterClass.ContactRule
import com.sixbynine.dialerforwarder.rules.RuleOuterClass.Rule
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.Locale

internal class ContactRulePresenter @AutoFactory constructor(
    @Provided @ApplicationContext private val context: Context,
    @Provided private val phoneNumberUtil: PhoneNumberUtil,
    @Provided private val contactCache: ContactCache,
    @Provided private val permissionChecker: PermissionChecker,
    private val contact: Contact,
    private val dialerAppInfo: DialerAppInfo
) : RulePresenter {

    override fun matches(contact: Contact): Boolean {
        return contact.id == this.contact.id
    }

    override fun matches(phoneNumber: String, contact: Contact?): Boolean {
        var matchedContact = contact
        if (matchedContact != null) {
            matchedContact = contactCache.getAllContactsWithPhoneNumbers()
                .firstOrNull { potentialMatch -> potentialMatch.id == this.contact.id }
        }

        if ((matchedContact != null && !dialerAppInfo.supportsCallingContact(matchedContact)) ||
            (matchedContact == null && !dialerAppInfo.supportsCallingWithJustNumber(phoneNumber))
        ) {
            return false
        }

        return matchedContact
            ?.phoneNumbers
            ?.any { contactPhoneNumber ->
                phoneNumberUtil.parse(
                    contactPhoneNumber.normalizedNumber,
                    Locale.getDefault().country
                )
                    ?.exactlySameAs(
                        phoneNumberUtil.parse(
                            phoneNumber, Locale.getDefault().country
                        )
                    ) ?: false
            } ?: false
    }

    override fun getDescription(): String {
        return context.getString(
            R.string.contact_rule_description, contact.displayName, dialerAppInfo.label
        )
    }

    override fun getDialerAppInfo() = dialerAppInfo

    override fun getRule(): RuleOuterClass.Rule {
        return Rule.newBuilder()
            .setType(RulePresenter.TYPE_CONTACT)
            .setContactRuleProto(
                ContactRule.newBuilder()
                    .setContactId(contact.id)
                    .setDialerAppInfo(dialerAppInfo.toProto())
                    .build()
            )
            .build()
    }

    override fun isEnabled(): Boolean {
        return permissionChecker.shouldUseContacts() && contactCache.getAllContactsWithPhoneNumbers().contains(
            contact
        ) && dialerAppInfo.supportsCallingContact(contact)
    }
}