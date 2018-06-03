package com.sixbynine.dialerforwarder

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.sixbynine.dialerforwarder.inject.BaseActivity
import javax.inject.Inject

class LaunchActivity : BaseActivity() {

    companion object {
        private const val HAVE_REQUESTED_INITIAL_PERMISSIONS = "have_requested_initial_permissions"
    }

    @Inject
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.getBoolean(HAVE_REQUESTED_INITIAL_PERMISSIONS, false)) {
            goToMainActivityAndFinish()
            return
        }

        prefs.edit().putBoolean(HAVE_REQUESTED_INITIAL_PERMISSIONS, true).apply()
        checkPermissionsAndShowContacts()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RequestCode.INITIAL_CONTACT_CHECK -> goToMainActivityAndFinish()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkPermissionsAndShowContacts() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE),
            RequestCode.INITIAL_CONTACT_CHECK
        )
    }

    private fun goToMainActivityAndFinish() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
