package com.sixbynine.dialerforwarder.intenthandler

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface CleanUpStoredCallsJobServiceModule {

    @ContributesAndroidInjector
    fun contributeDialIntentHandlerActivityInjector(): CleanUpStoredCallsJobService
}