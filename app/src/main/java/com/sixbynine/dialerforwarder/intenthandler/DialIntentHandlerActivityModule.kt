package com.sixbynine.dialerforwarder.intenthandler

import android.app.Activity
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfoModule
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.lifecycle.LifecycleActivity
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface DialIntentHandlerActivityModule {

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            ActivityModule::class, DialerAppInfoModule::class]
    )
    fun contributeDialIntentHandlerActivityInjector(): DialIntentHandlerActivity

    @Module
    interface ActivityModule {

        @Binds
        fun bindActivity(activity: DialIntentHandlerActivity): Activity

        @Binds
        fun bindLifecycleActivity(activity: DialIntentHandlerActivity): LifecycleActivity
    }
}