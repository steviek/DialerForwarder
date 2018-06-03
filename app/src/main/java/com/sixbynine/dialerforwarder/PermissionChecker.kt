package com.sixbynine.dialerforwarder

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.common.eventbus.EventBus
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.sixbynine.dialerforwarder.event.ShouldUseContactSettingChangedEvent
import com.sixbynine.dialerforwarder.inject.ActivityScoped
import com.sixbynine.dialerforwarder.lifecycle.LifecycleActivity
import com.sixbynine.dialerforwarder.lifecycle.LifecycleObserver
import javax.inject.Inject

@ActivityScoped
class PermissionChecker @Inject constructor(
    private val activity: Activity,
    private val sharedPreferences: SharedPreferences,
    private val eventBus: EventBus,
    lifecycleActivity: LifecycleActivity
) : LifecycleObserver {

    init {
        lifecycleActivity.registerLifecycleObserver(this)
    }

    companion object {
        private const val KEY_USE_CONTACTS = "use_contacts"
        private const val KEY_MAKE_CALLS = "make_calls"
    }

    private var pendingContactRequest: SettableFuture<Boolean>? = null
    private var pendingCallRequest: SettableFuture<Boolean>? = null

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldUseContacts(): Boolean {
        return hasContactsPermission() && sharedPreferences.getBoolean(KEY_USE_CONTACTS, true)
    }

    fun setShouldUseContacts(value: Boolean): ListenableFuture<Boolean> {
        if (!value || hasContactsPermission()) {
            sharedPreferences.edit().putBoolean(KEY_USE_CONTACTS, value).apply()
            eventBus.post(ShouldUseContactSettingChangedEvent(value))
            return Futures.immediateFuture(value)
        }

        val requestFuture = SettableFuture.create<Boolean>()
        pendingContactRequest = requestFuture
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            RequestCode.CONTACT_SETTING_CHECK
        )
        return requestFuture
    }

    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldMakeCalls(): Boolean {
        return hasCallPermission() && sharedPreferences.getBoolean(KEY_MAKE_CALLS, true)
    }

    fun setShouldMakeCalls(value: Boolean): ListenableFuture<Boolean> {
        if (!value || hasCallPermission()) {
            sharedPreferences.edit().putBoolean(KEY_MAKE_CALLS, value).apply()
            return Futures.immediateFuture(value)
        }

        val requestFuture = SettableFuture.create<Boolean>()
        pendingCallRequest = requestFuture
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CALL_PHONE),
            RequestCode.MAKE_CALLS_SETTING_CHECK
        )
        return requestFuture
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return when (requestCode) {
            RequestCode.CONTACT_SETTING_CHECK -> {
                val result = grantResults[0] == PackageManager.PERMISSION_GRANTED
                pendingContactRequest?.set(result)
                sharedPreferences.edit().putBoolean(KEY_USE_CONTACTS, result).apply()
                eventBus.post(ShouldUseContactSettingChangedEvent(result))
                true
            }
            RequestCode.MAKE_CALLS_SETTING_CHECK -> {
                val result = grantResults[0] == PackageManager.PERMISSION_GRANTED
                pendingCallRequest?.set(result)
                sharedPreferences.edit().putBoolean(KEY_MAKE_CALLS, result).apply()
                true
            }
            else -> false
        }
    }
}