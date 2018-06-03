package com.sixbynine.dialerforwarder.contacts

import android.util.Log
import com.github.tamir7.contacts.Contact
import com.github.tamir7.contacts.Contacts
import com.github.tamir7.contacts.Query
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.sanitizeAndParse
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber
import java.util.concurrent.Executor
import javax.inject.Inject

@ActivityScoped
class ContactCache @Inject constructor(
    private val phoneNumberUtil: PhoneNumberUtil,
    private val permissionChecker: PermissionChecker,
    executor: Executor
) {

    init {
        // Load the map on the background when we're created
        executor.execute {
            getContactWithPhoneNumber("3475555555")
        }
    }

    private val callCache: MutableMap<ContactQueryWrapper, List<Contact>> = HashMap()
    private val phoneNumberMap: Lazy<MutableMap<PhoneNumber, Contact>> = lazy {
        val map = HashMap<PhoneNumber, Contact>()
        get(GetAllQuery()).forEach {
            val contact = it
            it.phoneNumbers.forEach {
                try {
                    map[phoneNumberUtil.sanitizeAndParse(it.number)] = contact
                } catch (e: Exception) {
                }
            }
        }
        map
    }

    // TODO: listen for contact changes

    interface ContactQueryWrapper {
        val query: Query
    }

    class GetAllQuery : ContactQueryWrapper {
        override val query: Query = Contacts.getQuery().hasPhoneNumber()

        override fun hashCode() = 0

        override fun equals(other: Any?) = other is GetAllQuery
    }

    fun getAllContactsWithPhoneNumbers(): List<Contact> {
        return get(GetAllQuery())
    }

    fun get(query: ContactQueryWrapper): List<Contact> {
        if (!permissionChecker.shouldUseContacts()) {
            return ArrayList()
        }
        return callCache.getOrPut(query, { query.query.find() })
    }

    fun getContactWithPhoneNumber(phoneNumber: String): Contact? {
        return try {
            getContactWithPhoneNumber(phoneNumberUtil.sanitizeAndParse(phoneNumber))
        } catch (e: Exception) {
            null
        }
    }

    fun getContactWithPhoneNumber(phoneNumber: PhoneNumber): Contact? {
        return phoneNumberMap.value[phoneNumber]
    }
}