package com.sixbynine.dialerforwarder.dialerappinfo

import android.content.Intent
import android.graphics.drawable.Drawable
import com.github.tamir7.contacts.Contact
import com.sixbynine.dialerforwarder.DialerAppInfoProtoOuterClass

interface DialerAppInfo {
    val icon: Drawable?

    val label: CharSequence

    fun toProto(): DialerAppInfoProtoOuterClass.DialerAppInfoProto

    fun call(
        action: String = Intent.ACTION_DIAL,
        phoneNumber: String,
        contact: Contact? = null,
        afterAction: () -> Unit = {}
    ): Boolean

    fun supportsCallingContact(contact: Contact): Boolean

    fun supportsCallingWithJustNumber(number: String): Boolean

    fun isWhatsApp(): Boolean
}