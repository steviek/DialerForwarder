package com.sixbynine.dialerforwarder.lifecycle

interface LifecycleActivity {
    fun registerLifecycleObserver(observer: LifecycleObserver)

    fun unregisterLifecycleObserver(observer: LifecycleObserver)
}