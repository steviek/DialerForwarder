package com.sixbynine.dialerforwarder

import android.content.Intent
import android.util.Log
import com.github.tamir7.contacts.Contact
import com.google.common.base.Optional
import com.sixbynine.dialerforwarder.contacts.ContactCache
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfo
import com.sixbynine.dialerforwarder.dialerappinfo.WhatsApp
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.rules.RuleManager
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppManager
import javax.inject.Inject

@ActivityScoped
class ContactCaller @Inject constructor(
    private val dialerAppInfos: List<@JvmSuppressWildcards DialerAppInfo>,
    @WhatsApp private val whatsAppDialerAppInfo: Optional<DialerAppInfo>,
    private val contactCache: ContactCache,
    private val whatsAppManager: WhatsAppManager,
    private val ruleManager: RuleManager,
    private val appChooserDialogShower: AppChooserDialogShower
) {

    fun forwardIntentForContact(
        phoneNumber: String,
        contact: Contact? = null,
        action: String = Intent.ACTION_DIAL,
        showPicker: Boolean = false,
        afterAction: () -> Unit = {}
    ) {
        val dialerAppInfoAndContact = getAppInfoAndContact(phoneNumber, contact)

        val bestGuessContact = dialerAppInfoAndContact.first
        var dialerAppInfo = dialerAppInfoAndContact.second
        val filter = { appInfo: DialerAppInfo ->
            if (bestGuessContact != null) {
                appInfo.supportsCallingContact(bestGuessContact)
            } else {
                appInfo.supportsCallingWithJustNumber(phoneNumber)
            }
        }

        if (dialerAppInfo == null || showPicker) {
            when {
                dialerAppInfos.isEmpty() -> return
                dialerAppInfos.size == 1 -> dialerAppInfo = dialerAppInfos.first()
                else -> {
                    appChooserDialogShower.showAppChooserDialog({ dialer ->
                        if (dialer != null) {
                            dialer.call(
                                action, phoneNumber, bestGuessContact, afterAction)
                        } else {
                            afterAction()
                        }
                    }, filter = filter)
                    return
                }
            }
        }

        dialerAppInfo.call(action, phoneNumber, bestGuessContact, afterAction)
    }

    fun getAppInfoForContact(phoneNumber: String?, contact: Contact? = null): DialerAppInfo? =
        getAppInfoAndContact(phoneNumber, contact).second

    private fun getAppInfoAndContact(phoneNumber: String?, contact: Contact? = null): Pair<Contact?, DialerAppInfo?> {
        if (phoneNumber == null && contact == null) {
            return Pair(null, null)
        }

        var bestGuessContact = contact
        if (bestGuessContact == null) {
            bestGuessContact = contactCache.getContactWithPhoneNumber(phoneNumber!!)
        }

        if (bestGuessContact != null && whatsAppManager.shouldAlwaysUseWhatsAppToCallWhatsAppContacts() && whatsAppManager.hasWhatsApp(bestGuessContact)) {
            return Pair(bestGuessContact, whatsAppDialerAppInfo.get())
        }
        return if (phoneNumber != null) Pair(bestGuessContact, ruleManager.getDialerAppInfo(phoneNumber, bestGuessContact)) else Pair(null, null)
    }
}