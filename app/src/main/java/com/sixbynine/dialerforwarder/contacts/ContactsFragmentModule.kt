package com.sixbynine.dialerforwarder.contacts

import com.sixbynine.dialerforwarder.inject.FragmentScoped
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ContactsFragmentModule {

    @FragmentScoped
    @ContributesAndroidInjector(modules = arrayOf(FragmentModule::class))
    abstract fun contributeContactsFragmentInjector(): ContactsFragment

    @Module
    internal abstract class FragmentModule {

        @Binds
        abstract fun bindOnContactClickListener(fragment: ContactsFragment): ContactsAdapter.OnContactClickListener
    }
}