package com.sixbynine.dialerforwarder.intenthandler

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.SharedPreferences
import dagger.android.AndroidInjection
import javax.inject.Inject

/** Job to remove any stored calls. */
class CleanUpStoredCallsJobService : JobService() {

    @Inject
    internal lateinit var sharedPreferences: SharedPreferences

    override fun onStartJob(params: JobParameters?): Boolean {
        AndroidInjection.inject(this)

        val editor = sharedPreferences.edit()
        sharedPreferences.all.entries
            .filter { it.key.startsWith("handledcall") }
            .forEach { editor.remove(it.key) }
        editor.apply()

        return true
    }

    override fun onStopJob(params: JobParameters?) = true
}