package com.sixbynine.dialerforwarder.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.common.eventbus.EventBus
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.sixbynine.dialerforwarder.inject.ApplicationContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
abstract class ApplicationModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @Reusable
        fun providePhoneNumberUtil(
            @ApplicationContext context: Context
        ): PhoneNumberUtil = PhoneNumberUtil.createInstance(
            context
        )

        @Provides
        @JvmStatic
        @Reusable
        fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("dialerforwarder", Context.MODE_PRIVATE)

        @Provides
        @JvmStatic
        @Singleton
        fun provideBus() = EventBus()

        @Provides
        @JvmStatic
        @Singleton
        fun provideScheduledExecutorService(): ListeningScheduledExecutorService =
            MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(5))
    }

    @Binds
    abstract fun bindApplication(application: DialerApplication): Application

    @Binds
    @ApplicationContext
    abstract fun bindContext(application: Application): Context

    @Binds
    abstract fun bindExecutor(executorService: ListeningScheduledExecutorService): Executor
}