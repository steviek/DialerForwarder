package com.sixbynine.dialerforwarder.inject

import android.content.Context
import android.support.v4.app.DialogFragment
import com.google.common.eventbus.EventBus
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

abstract class BaseDialogFragment : DialogFragment() {

    @Inject
    internal lateinit var bus: EventBus

    private var registered = false

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
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