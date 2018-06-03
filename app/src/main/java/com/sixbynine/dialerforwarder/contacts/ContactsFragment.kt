package com.sixbynine.dialerforwarder.contacts

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.tamir7.contacts.Contact
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.sixbynine.dialerforwarder.AppChooserDialogShower
import com.sixbynine.dialerforwarder.ContactCaller
import com.sixbynine.dialerforwarder.R
import com.sixbynine.dialerforwarder.event.ContactActionModeEndedEvent
import com.sixbynine.dialerforwarder.event.ContactActionModeStartedEvent
import com.sixbynine.dialerforwarder.event.ShouldUseContactSettingChangedEvent
import com.sixbynine.dialerforwarder.inject.BaseFragment
import com.sixbynine.dialerforwarder.rules.ContactRulePresenterFactory
import com.sixbynine.dialerforwarder.rules.RuleManager
import com.sixbynine.dialerforwarder.rules.RulesChangedEvent
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppSettingChangedEvent
import java.text.Normalizer
import javax.inject.Inject

class ContactsFragment : BaseFragment(), ContactsAdapter.OnContactClickListener {

    @Inject
    lateinit var eventBus: EventBus
    @Inject
    lateinit var contactsAdapterFactory: ContactsAdapterFactory
    @Inject
    lateinit var contactCaller: ContactCaller
    @Inject
    lateinit var contactManager: ContactManager
    @Inject
    lateinit var contactRulePresenterFactory: ContactRulePresenterFactory
    @Inject
    lateinit var ruleManager: RuleManager
    @Inject
    lateinit var appChooserDialogShower: AppChooserDialogShower
    @Inject
    lateinit var contactCache: ContactCache

    private val contactComparator = Comparator<Contact> { contact1, contact2 ->
        if (contactManager.isFavourite(contact2) && !contactManager.isFavourite(
                contact1
            )
        ) {
            1
        } else if (contactManager.isFavourite(contact1) && !contactManager.isFavourite(contact2)) {
            -1
        } else {
            contact1.displayName.compareTo(contact2.displayName)
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_contacts, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val searchActionView = menu.findItem(R.id.action_search).actionView as SearchView
        searchActionView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // do nothing
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                adapter.contacts = contactCache.getAllContactsWithPhoneNumbers().filter { contact ->
                    contact.displayName.contains(
                        newText,
                        ignoreCase = true
                    ) || Normalizer.normalize(
                        contact.displayName,
                        Normalizer.Form.NFD
                    ).contains(newText, ignoreCase = true)
                }.sortedWith(contactComparator)
                adapter.onContactsChanged()
                return true
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contacts = contactCache.getAllContactsWithPhoneNumbers().sortedWith(contactComparator)
        recyclerView.post {
            adapter = contactsAdapterFactory.create(contacts, recyclerView)
            recyclerView.adapter = adapter
        }
    }

    override fun onContactClick(contact: Contact) {
        val phoneNumbers =
            contact.phoneNumbers.distinctBy { phoneNumber -> phoneNumber.normalizedNumber }
                .map { phoneNumber -> phoneNumber.number }
        if (phoneNumbers.size == 1) {
            contactCaller.forwardIntentForContact(
                phoneNumber = phoneNumbers.first(), contact = contact
            )
        } else {
            AlertDialog.Builder(activity!!).setTitle(R.string.choose_number)
                .setItems(phoneNumbers.toTypedArray(), { _, which ->
                    contactCaller.forwardIntentForContact(
                        phoneNumber = phoneNumbers[which], contact = contact
                    )
                }).show()
        }
    }

    override fun onContactLongClick(contact: Contact) {
        showAppOverrideDialog(contact)
    }

    override fun onDialerAppIconClick(contact: Contact) {
        showAppOverrideDialog(contact)
    }

    @Subscribe
    internal fun onRulesChanged(rulesChangedEvent: RulesChangedEvent) {
        adapter.onContactsChanged()
    }

    @Subscribe
    internal fun onWhatsAppSettingChanged(whatsAppSettingChangedEvent: WhatsAppSettingChangedEvent) {
        adapter.onContactsChanged()
    }

    @Subscribe
    internal fun onShouldUseContactSettingChangedEvent(event: ShouldUseContactSettingChangedEvent) {
        if (event.value) {
            val contacts =
                contactCache.getAllContactsWithPhoneNumbers().sortedWith(contactComparator)
            adapter = contactsAdapterFactory.create(contacts, recyclerView)
            recyclerView.adapter = adapter
        } else {
            recyclerView.adapter = null
        }
    }

    private fun showAppOverrideDialog(contact: Contact, callback: () -> Unit = {}) {
        appChooserDialogShower.showAppChooserDialog(
            { dialerAppInfo ->
                run {
                    if (dialerAppInfo != null) {
                        ruleManager.addRuleForContact(
                            contactRulePresenterFactory.create(
                                contact,
                                dialerAppInfo
                            ), contact
                        )
                        callback()
                    }
                }
            },
            activity!!.getString(R.string.add_override, contact.displayName),
            { appInfo -> appInfo.supportsCallingContact(contact) })
    }
}