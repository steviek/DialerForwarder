package com.sixbynine.dialerforwarder.inject

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.common.eventbus.EventBus
import com.sixbynine.dialerforwarder.lifecycle.LifecycleActivity
import com.sixbynine.dialerforwarder.lifecycle.LifecycleObserver
import dagger.android.AndroidInjection
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), LifecycleActivity {

    @Inject
    internal lateinit var bus: EventBus

    private var registered = false
    private val lifecycleObservers = HashSet<LifecycleObserver>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (!registered) {
            bus.register(this)
            registered = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (!registered) {
            bus.register(this)
            registered = true
        }
    }

    override fun onStop() {
        super.onStop()
        bus.unregister(this)
        registered = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val callHandled = lifecycleObservers.any { observer ->
            observer.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
        }
        if (!callHandled) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun registerLifecycleObserver(observer: LifecycleObserver) {
        lifecycleObservers.add(observer)
    }

    override fun unregisterLifecycleObserver(observer: LifecycleObserver) {
        lifecycleObservers.remove(observer)
    }
}