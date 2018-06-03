package com.sixbynine.dialerforwarder.app

import android.app.Activity
import com.sixbynine.dialerforwarder.inject.UiThread
import dagger.Module
import dagger.Provides
import dagger.Reusable
import java.util.concurrent.Executor

@Module
abstract class CommonActivityModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @Reusable
        @UiThread
        fun provideUiThreadExecutor(activity: Activity): Executor {
            return Executor { command -> activity.runOnUiThread(command) }
        }
    }
}