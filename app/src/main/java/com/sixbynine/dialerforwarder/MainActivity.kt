package com.sixbynine.dialerforwarder

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.common.eventbus.Subscribe
import com.sixbynine.dialerforwarder.contacts.ContactsFragment
import com.sixbynine.dialerforwarder.dial.DialDialogFragment
import com.sixbynine.dialerforwarder.event.ContactActionModeEndedEvent
import com.sixbynine.dialerforwarder.event.ContactActionModeStartedEvent
import com.sixbynine.dialerforwarder.event.ModifyRuleActionModeEndedEvent
import com.sixbynine.dialerforwarder.event.ModifyRuleActionModeStartedEvent
import com.sixbynine.dialerforwarder.event.ShouldUseContactSettingChangedEvent
import com.sixbynine.dialerforwarder.inject.BaseFragmentActivity
import com.sixbynine.dialerforwarder.rules.RuleManager
import com.sixbynine.dialerforwarder.rules.RulesFragment
import de.psdev.licensesdialog.LicensesDialog
import javax.inject.Inject

class MainActivity : BaseFragmentActivity() {

    @Inject
    lateinit var ruleManager: RuleManager

    @Inject
    lateinit var permissionChecker: PermissionChecker

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        container.adapter = sectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        // This is part of a hack to make the tab indicator show on the top
        tabs.post {
            val tabList = tabs.getChildAt(0) as ViewGroup
            for (i in 0 until tabList.childCount) {
                tabList.getChildAt(i).scaleY = -1f
            }
            if (permissionChecker.shouldUseContacts()) {
                tabs.visibility = View.VISIBLE
            } else {
                tabs.visibility = View.GONE
            }
        }

        if (!ruleManager.rules.isEmpty() && savedInstanceState == null && permissionChecker.shouldUseContacts()) {
            container.setCurrentItem(1, false /* smoothScroll */)
        }

        fab.setOnClickListener { DialDialogFragment().show(supportFragmentManager, "dial") }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_open_source_licenses) {
            LicensesDialog.Builder(this)
                .setNotices(R.raw.notices)
                .setIncludeOwnLicense(false)
                .build()
                .showAppCompat()
            return true
        }

        if (id == R.id.action_about) {
            AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setMessage(R.string.about_text)
                .show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @Subscribe
    internal fun onModifyRuleActionModeStarted(event: ModifyRuleActionModeStartedEvent) {
        container.disableSwiping = true
        tabs.setViewGroupAndDescendentsEnabled(false)
    }

    @Subscribe
    internal fun onContactActionModeStarted(event: ContactActionModeStartedEvent) {
        container.disableSwiping = true
        tabs.setViewGroupAndDescendentsEnabled(false)
    }

    @Subscribe
    internal fun onModifyRuleActionModeEnded(event: ModifyRuleActionModeEndedEvent) {
        container.disableSwiping = false
        tabs.setViewGroupAndDescendentsEnabled(true)
    }

    @Subscribe
    internal fun onContactActionModeEnded(event: ContactActionModeEndedEvent) {
        container.disableSwiping = false
        tabs.setViewGroupAndDescendentsEnabled(true)
    }

    @Subscribe
    internal fun onShouldUseContactSettingChangedEvent(event: ShouldUseContactSettingChangedEvent) {
        sectionsPagerAdapter.onShouldUseContactsChanged(event.value)
        tabs.visibility = if (event.value) View.VISIBLE else View.GONE
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        var useContacts = permissionChecker.shouldUseContacts()

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return RulesFragment()
                1 -> return ContactsFragment()
            }
            throw UnsupportedOperationException()
        }

        override fun getCount(): Int {
            return 1 + useContacts.toInt()
        }

        fun onShouldUseContactsChanged(newValue: Boolean) {
            useContacts = newValue
            notifyDataSetChanged()
        }
    }
}
