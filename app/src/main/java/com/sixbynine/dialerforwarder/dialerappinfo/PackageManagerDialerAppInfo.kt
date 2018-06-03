package com.sixbynine.dialerforwarder.dialerappinfo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.app.AlertDialog
import com.github.tamir7.contacts.Contact
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.sixbynine.dialerforwarder.DialerAppInfoProtoOuterClass
import com.sixbynine.dialerforwarder.DialerAppInfoProtoOuterClass.DialerAppInfoProto
import com.sixbynine.dialerforwarder.PermissionChecker
import com.sixbynine.dialerforwarder.showDialogInsteadOfLaunchingCallerForDebug

class PackageManagerDialerAppInfo
@AutoFactory constructor(
    @Provided private val activity: Activity,
    @Provided private val permissionChecker: PermissionChecker,
    override val label: CharSequence,
    override val icon: Drawable?,
    val dialPackageName: String,
    val dialClassName: String,
    private val callPackageName: String,
    private val callClassName: String
) : DialerAppInfo {

    companion object {

        internal fun fromResolveInfos(
            factory: PackageManagerDialerAppInfoFactory,
            packageManager: PackageManager,
            dialResolveInfo: ResolveInfo,
            callResolveInfo: ResolveInfo?
        ): PackageManagerDialerAppInfo? {
            val activityInfo = dialResolveInfo.activityInfo ?: return null

            var label = activityInfo.loadLabel(packageManager)
            if (label == null) {
                label = dialResolveInfo.loadLabel(packageManager)
            }

            if (label == null) {
                return null
            }

            var icon = activityInfo.loadIcon(packageManager)
            if (icon == null) {
                icon = dialResolveInfo.loadIcon(packageManager)
            }

            return factory.create(
                label,
                icon,
                activityInfo.packageName,
                activityInfo.name,
                callResolveInfo?.activityInfo?.packageName ?: "",
                callResolveInfo?.activityInfo?.name ?: ""
            )
        }
    }

    private val dialComponentName = ComponentName(dialPackageName, dialClassName)

    private val callComponentName =
        if (!callPackageName.isBlank() && !callClassName.isBlank()) ComponentName(
            callPackageName, callClassName
        )
        else null

    override fun toProto(): DialerAppInfoProtoOuterClass.DialerAppInfoProto {
        return DialerAppInfoProto.newBuilder().setDialPackageName(dialPackageName)
            .setDialClassName(dialClassName).setCallPackageName(callPackageName)
            .setCallClassName(callClassName).build()
    }

    override fun call(
        action: String,
        phoneNumber: String,
        contact: Contact?,
        afterAction: () -> Unit
    ): Boolean {
        val intent = createIntent(action)
        intent.data = Uri.parse("tel:$phoneNumber")
        if (showDialogInsteadOfLaunchingCallerForDebug()) {
            AlertDialog.Builder(activity).setTitle("Debug Call").setMessage(
                "Starting activity with the following intent:\n" + "\nAction:\n" + intent.action + "\n\nData:\n" + intent.dataString + "\n\nComponent:\n" + intent.component
            )
                .setPositiveButton("Launch", { _, _ ->
                    activity.startActivity(intent)
                    afterAction()
                }).setNegativeButton("Cancel", { _, _ -> afterAction() }).show()
            return true
        }
        return try {
            activity.startActivity(intent)
            afterAction()
            true
        } catch (e: ActivityNotFoundException) {
            afterAction()
            false
        }
    }

    override fun supportsCallingContact(contact: Contact): Boolean {
        return contact.phoneNumbers.isNotEmpty()
    }

    override fun supportsCallingWithJustNumber(number: String) = true

    private fun createIntent(action: String): Intent {
        val intent = Intent()
        if (action == Intent.ACTION_CALL) {
            if (callComponentName != null && permissionChecker.shouldMakeCalls()) {
                intent.action = action
                intent.component = callComponentName
            } else {
                intent.action = Intent.ACTION_DIAL
                intent.component = dialComponentName
            }
        } else {
            intent.action = action
            intent.component = dialComponentName
        }
        return intent
    }

    override fun isWhatsApp(): Boolean {
        return false
    }
}