package com.sixbynine.dialerforwarder

import android.app.Activity
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface LaunchActivityModule {

    @ActivityScoped
    @ContributesAndroidInjector(modules = [ActivityModule::class])
    fun contributeHostActivityInjector(): LaunchActivity

    @Module
    interface ActivityModule {

        @Binds
        fun bindActivity(activity: LaunchActivity): Activity
    }
}