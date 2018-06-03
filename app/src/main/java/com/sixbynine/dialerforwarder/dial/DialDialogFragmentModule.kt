package com.sixbynine.dialerforwarder.dial

import com.sixbynine.dialerforwarder.inject.FragmentScoped
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface DialDialogFragmentModule {

    @FragmentScoped
    @ContributesAndroidInjector
    fun contributeHostActivityInjector(): DialDialogFragment
}