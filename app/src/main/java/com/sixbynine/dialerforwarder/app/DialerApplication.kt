package com.sixbynine.dialerforwarder.app

import android.app.Activity
import android.app.Application
import android.app.Service
import com.github.tamir7.contacts.Contacts
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import javax.inject.Inject

class DialerApplication : Application(), HasActivityInjector, HasServiceInjector {

    @Inject
    lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>

    @dagger.Module
    class Module(private val application: DialerApplication) {
        @Provides
        fun provideApplication() = application
    }

    override fun onCreate() {
        super.onCreate()
        DaggerApplicationComponent.builder().module(Module(this)).build().inject(this)
        Contacts.initialize(this)
    }

    override fun activityInjector() = dispatchingActivityInjector

    override fun serviceInjector() = dispatchingServiceInjector
}