package com.sixbynine.dialerforwarder.rules

import com.sixbynine.dialerforwarder.inject.FragmentScoped
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface AddCountryRuleDialogFragmentModule {

    @FragmentScoped
    @ContributesAndroidInjector
    fun contributeAddRuleDialogFragmentInjector(): AddCountryRuleDialogFragment
}