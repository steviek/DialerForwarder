package com.sixbynine.dialerforwarder

import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.github.tamir7.contacts.Contact
import com.google.common.base.Optional
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import java.util.Locale

object RequestCode {
    const val INITIAL_CONTACT_CHECK = 0
    const val CONTACT_SETTING_CHECK = 1
    const val MAKE_CALLS_SETTING_CHECK = 2
}

fun PhoneNumberUtil.getRegionCode(number: Phonenumber.PhoneNumber): String {
    val regionCodes = getRegionCodesForCountryCode(number.countryCode)
    if (regionCodes.size == 1) {
        return regionCodes[0]
    } else {
        var regionWhereNumberIsValid = "ZZ"
        for (regionCode in regionCodes) {
            if (isValidNumberForRegion(number, regionCode)) {
                // If the number has already been found valid for one region, then we don't know which
                // region it belongs to so we return nothing.
                if (regionWhereNumberIsValid != "ZZ") {
                    return ""
                }
                regionWhereNumberIsValid = regionCode
            }
        }
        return regionWhereNumberIsValid
    }
}

fun PhoneNumberUtil.parse(number: String): Phonenumber.PhoneNumber {
    return parse(number, Locale.getDefault().country)
}

fun PhoneNumberUtil.getRegionCode(number: String): String {
    return getRegionCode(sanitizeAndParse(number))
}

fun PhoneNumberUtil.sanitizeAndParse(number: String): Phonenumber.PhoneNumber {
    var sanitizedNumber = number
    while (sanitizedNumber.contains(",")) {
        sanitizedNumber = sanitizedNumber.substring(0, number.indexOf(','))
    }
    try {
        return parse(sanitizedNumber)
    } catch (e: NumberParseException) {
        throw RuntimeException("Error parsing $number", e)
    }
}

/** Returns 1 for true booleans, 0 for false ones. */
fun Boolean.toInt() = if (this) 1 else 0

fun Contact.getDistinctPhoneNumbers(): List<String> {
    return phoneNumbers.map { phoneNumber -> phoneNumber.normalizedNumber }.distinct()
}

fun ViewGroup.inflateChild(layoutId: Int): View = LayoutInflater.from(context).inflate(
    layoutId, this, false
)

fun ViewGroup.setViewGroupAndDescendentsEnabled(enabled: Boolean) {
    isEnabled = enabled
    for (i in 0 until childCount) {
        if (getChildAt(i) is ViewGroup) {
            (getChildAt(i) as ViewGroup).setViewGroupAndDescendentsEnabled(enabled)
        } else {
            getChildAt(i).isEnabled = enabled
        }
    }
}

fun PackageManager.isPackageInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, 0)
        true
    } catch (e: NameNotFoundException) {
        false
    }
}

/**
 * Returns a list containing all elements of the original collection and then the given [element] if
 * it is present.
 */
fun <T> Collection<T>.plusOptional(element: Optional<out T>): List<T> {
    val result = ArrayList<T>(size + element.isPresent.toInt())
    result.addAll(this)
    if (element.isPresent) {
        result.add(element.get())
    }
    return result
}

fun showDialogInsteadOfLaunchingCallerForDebug(): Boolean {
    return false
}

fun View.whenDoneAnimating(callback: () -> Unit) {
    viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {

        private val doneRunnable = Runnable { onDone() }

        override fun onDraw() {
            handler.removeCallbacks(doneRunnable)
            handler.postDelayed(doneRunnable, 100)
        }

        private fun onDone() {
            viewTreeObserver.removeOnDrawListener(this)
            callback()
        }
    })
}
