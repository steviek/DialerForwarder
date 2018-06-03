package com.sixbynine.dialerforwarder

import android.app.Activity
import com.sixbynine.dialerforwarder.app.CommonActivityModule
import com.sixbynine.dialerforwarder.contacts.ContactsFragmentModule
import com.sixbynine.dialerforwarder.dial.DialDialogFragmentModule
import com.sixbynine.dialerforwarder.dialerappinfo.DialerAppInfoModule
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.lifecycle.LifecycleActivity
import com.sixbynine.dialerforwarder.rules.AddCountryRuleDialogFragmentModule
import com.sixbynine.dialerforwarder.rules.RulesFragmentModule
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface MainActivityModule {

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            AddCountryRuleDialogFragmentModule::class,
            ActivityModule::class,
            CommonActivityModule::class,
            ContactsFragmentModule::class,
            DialDialogFragmentModule::class,
            DialerAppInfoModule::class,
            RulesFragmentModule::class])
    fun contributeHostActivityInjector(): MainActivity

    @Module
    interface ActivityModule {

        @Binds
        fun bindActivity(activity: MainActivity): Activity

        @Binds
        fun bindLifecylceActivity(activity: MainActivity): LifecycleActivity
    }
}