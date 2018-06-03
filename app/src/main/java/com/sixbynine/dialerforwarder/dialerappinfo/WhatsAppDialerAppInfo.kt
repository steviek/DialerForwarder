package com.sixbynine.dialerforwarder.dialerappinfo

import android.graphics.drawable.Drawable
import com.github.tamir7.contacts.Contact
import com.sixbynine.dialerforwarder.DialerAppInfoProtoOuterClass.DialerAppInfoProto
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppManager

internal class WhatsAppDialerAppInfo constructor(
    private val whatsAppManager: WhatsAppManager,
    override val label: CharSequence,
    override val icon: Drawable?
) : DialerAppInfo {

    override fun toProto(): DialerAppInfoProto {
        return DialerAppInfoProto.newBuilder().setWhatsApp(true).build()
    }

    override fun call(
        action: String,
        phoneNumber: String,
        contact: Contact?,
        afterAction: () -> Unit
    ): Boolean {
        return whatsAppManager.callWhatsApp(contact!!, afterAction)
    }

    override fun supportsCallingContact(contact: Contact): Boolean {
        return whatsAppManager.hasWhatsApp(contact)
    }

    override fun supportsCallingWithJustNumber(number: String): Boolean {
        return false
    }

    override fun isWhatsApp(): Boolean {
        return true
    }
}