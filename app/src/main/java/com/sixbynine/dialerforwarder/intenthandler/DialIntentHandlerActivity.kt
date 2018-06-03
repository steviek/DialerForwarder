package com.sixbynine.dialerforwarder.intenthandler

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.sixbynine.dialerforwarder.ContactCaller
import com.sixbynine.dialerforwarder.inject.BaseActivity
import javax.inject.Inject

class DialIntentHandlerActivity : BaseActivity() {

    @Inject
    lateinit var contactCaller: ContactCaller

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.dataString == null
            || intent.action == null
            || !intent.dataString.startsWith("tel:")
        ) {
            finish()
            return
        }

        val phoneNumber = intent.dataString.substring(4)

        val prefKey = "handledcall$phoneNumber"
        val lastTimeHandled = sharedPreferences.getLong(prefKey, 0)

        // Store the current time that we handled this number to prevent the possibility of an
        // infinite loop if the app we redirect to redirects back to us.
        sharedPreferences.edit().putLong(prefKey, System.currentTimeMillis()).apply()

        // Schedule a job to delete this log once we're done with it so that we don't store a
        // log of the calls the user has made.
        val serviceComponent = ComponentName(this, CleanUpStoredCallsJobService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setMinimumLatency(3000) // wait at least 3 seconds
        builder.setOverrideDeadline(12 * 60 * 60 * 1000) // wait at most 12 hours
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(builder.build())

        // If we just handled this phone number, show the picker to prevent automatic redirecting
        // leading to an infinite loop.
        if (System.currentTimeMillis() - lastTimeHandled < 2000) {
            contactCaller.forwardIntentForContact(
                phoneNumber = phoneNumber,
                action = intent.action,
                afterAction = this::finish,
                showPicker = true
            )
            return
        }

        contactCaller.forwardIntentForContact(
            phoneNumber = phoneNumber,
            action = intent.action,
            afterAction = this::finish
        )
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing) {
            finish()
        }
    }
}