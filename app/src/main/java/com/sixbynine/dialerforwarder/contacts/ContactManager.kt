package com.sixbynine.dialerforwarder.contacts

import android.content.Context
import android.provider.ContactsContract
import com.github.tamir7.contacts.Contact
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.event.ShouldUseContactSettingChangedEvent
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.inject.ApplicationContext
import javax.inject.Inject

@ActivityScoped
class ContactManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
    eventBus: EventBus
) {

    private val favourites = ArrayList<Long>()

    init {
        eventBus.register(this)
        favourites.addAll(getStarredContacts())
    }

    fun isFavourite(contact: Contact) = favourites.contains(contact.id)

    @Subscribe
    internal fun onShouldUseContactSettingChanged(event: ShouldUseContactSettingChangedEvent) {
        favourites.clear()
        favourites.addAll(getStarredContacts())
    }

    private fun getStarredContacts(): List<Long> {
        if (!permissionChecker.shouldUseContacts()) {
            return ArrayList()
        }

        val queryUri = ContactsContract.Contacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
            .build()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.STARRED
        )

        val selection = ContactsContract.Contacts.STARRED + "='1'"

        val cursor = context.contentResolver.query(
            queryUri,
            projection, selection, null, null
        )

        val list = ArrayList<Long>()
        while (cursor.moveToNext()) {
            val contactID = cursor.getLong(
                cursor
                    .getColumnIndex(ContactsContract.Contacts._ID)
            )
            list.add(contactID)
        }

        cursor.close()
        return list
    }
}