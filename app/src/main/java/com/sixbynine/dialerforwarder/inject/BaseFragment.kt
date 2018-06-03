package com.sixbynine.dialerforwarder.inject

import android.content.Context
import com.google.common.eventbus.EventBus
import dagger.android.support.DaggerFragment
import javax.inject.Inject

abstract class BaseFragment : DaggerFragment() {

    @Inject
    internal lateinit var bus: EventBus

    private var registered = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
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
}