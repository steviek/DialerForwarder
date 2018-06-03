package com.sixbynine.dialerforwarder.whatsapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import android.support.v7.app.AlertDialog
import android.util.Log
import com.github.tamir7.contacts.Contact
import com.google.common.eventbus.EventBus
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.isPackageInstalled
import com.sixbynine.dialerforwarder.showDialogInsteadOfLaunchingCallerForDebug
import javax.inject.Inject

@ActivityScoped
class WhatsAppManager @Inject constructor(
    private val activity: Activity,
    private val sharedPreferences: SharedPreferences,
    private val permissionChecker: PermissionChecker,
    private val eventBus: EventBus
) {

    // Contact Id -> WhatsApp Id
    private val contactsWithWhatsApp: Map<Long, Map<String, Long>>

    companion object {
        const val WHATS_APP_PACKAGE_NAME = "com.whatsapp"
        private const val VOIP_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
        private const val KEY_ALWAYS_USE_WHATSAPP = "always_use_whatsapp"
    }

    init {
        contactsWithWhatsApp = if (isWhatsAppInstalledAndUsesContacts()) {
            getWhatsAppContacts()
        } else {
            emptyMap()
        }
    }

    fun hasWhatsApp(contact: Contact) = contactsWithWhatsApp.contains(contact.id)

    fun callWhatsApp(contact: Contact, afterAction: () -> Unit = {}): Boolean {
        if (!hasWhatsApp(contact)) {
            return false
        }

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW

        val whatsAppContactCallId = contactsWithWhatsApp[contact.id]?.get(VOIP_CALL) ?: return false

        // the _ids you save goes here at the end of /data/12562
        intent.setDataAndType(
            Uri.parse("content://com.android.contacts/data/$whatsAppContactCallId"), VOIP_CALL)
        intent.`package` = "com.whatsapp"

        if (showDialogInsteadOfLaunchingCallerForDebug()) {
            AlertDialog.Builder(activity).setTitle("Debug Call").setMessage(
                "Starting activity with the following intent:\n" + "\n\nAction:\n" + intent.action + "\n\nData:\n" + intent.dataString + "\n\nType:\n" + intent.type + "\n\nPackage:\n" + intent.`package`)
                .setPositiveButton("Launch", { _, _ ->
                    activity.startActivity(intent)
                    afterAction()
                }).setNegativeButton("Cancel", { _, _ -> afterAction() }).show()
            return true
        }

        activity.startActivity(intent)
        afterAction()
        return true
    }

    fun isWhatsAppInstalled() = activity.packageManager.isPackageInstalled(WHATS_APP_PACKAGE_NAME)

    fun isWhatsAppInstalledAndUsesContacts() = activity.packageManager.isPackageInstalled(WHATS_APP_PACKAGE_NAME) && permissionChecker.shouldUseContacts()

    fun getWhatsAppIcon(): Drawable? {
        if (!isWhatsAppInstalled()) {
            return null
        }

        return activity.packageManager.getApplicationIcon(WHATS_APP_PACKAGE_NAME)
    }

    fun getWhatsAppLabel(): CharSequence? {
        if (!isWhatsAppInstalled()) {
            return null
        }

        return activity.packageManager.getApplicationLabel(
            activity.packageManager.getApplicationInfo(
                WHATS_APP_PACKAGE_NAME, 0))
    }

    fun shouldAlwaysUseWhatsAppToCallWhatsAppContacts(): Boolean {
        return sharedPreferences.getBoolean(KEY_ALWAYS_USE_WHATSAPP, false)
    }

    fun setShouldAlwaysUseWhatsAppToCallWhatsAppContacts(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ALWAYS_USE_WHATSAPP, value).apply()
        eventBus.post(WhatsAppSettingChangedEvent())
    }

    private fun getWhatsAppContacts(): Map<Long, Map<String, Long>> {
        val resolver = activity.contentResolver
        val cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME)

        val contacts = HashMap<Long, HashMap<String, Long>>()
        while (cursor.moveToNext()) {
            val whatsAppId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID))
            val contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID))
            val displayName =
                cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME))
            val mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))

            if (!mimeType.contains(WHATS_APP_PACKAGE_NAME)) {
                continue
            }

            val map = contacts[contactId] ?: HashMap()
            map[mimeType] = whatsAppId

            // TODO handle multiple phone numbers
            contacts[contactId] = map
        }
        cursor.close()
        return contacts
    }
}