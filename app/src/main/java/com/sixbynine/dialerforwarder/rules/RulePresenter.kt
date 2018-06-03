package com.sixbynine.dialerforwarder.rules

import com.github.tamir7.contacts.Contact
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfo
import com.sixbynine.dialerforwarder.rules.RuleOuterClass.Rule

internal interface RulePresenter {

    companion object {
        const val IN = 0

        const val TYPE_COUNTRY = 0
        const val TYPE_CONTACT = 1
    }

    fun matches(contact: Contact): Boolean

    fun matches(phoneNumber: String, contact: Contact? = null): Boolean

    fun getDescription(): String

    fun getDialerAppInfo(): DialerAppInfo

    fun getRule(): Rule

    fun isEnabled(): Boolean
}