package com.sixbynine.dialerforwarder.app

import com.sixbynine.dialerforwarder.LaunchActivityModule
import com.sixbynine.dialerforwarder.MainActivityModule
import com.sixbynine.dialerforwarder.intenthandler.CleanUpStoredCallsJobService
import com.sixbynine.dialerforwarder.intenthandler.CleanUpStoredCallsJobServiceModule
import com.sixbynine.dialerforwarder.intenthandler.DialIntentHandlerActivityModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        CleanUpStoredCallsJobServiceModule::class,
        DialerApplication.Module::class,
        DialIntentHandlerActivityModule::class,
        LaunchActivityModule::class,
        MainActivityModule::class
    ]
)
internal interface ApplicationComponent {
    fun inject(application: DialerApplication)
}