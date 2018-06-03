package com.sixbynine.dialerforwarder.lifecycle

/**
 * Interface to be implemented by classes that listen to lifecycle events. Events should be added
 * here as necessary with appropriate default implementations.
 */
interface LifecycleObserver {

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean = false
}