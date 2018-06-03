package com.sixbynine.dialerforwarder.dialerappinfo

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import com.google.common.base.Optional
import com.sixbynine.dialerforwarder.inject.ApplicationContext
import com.sixbynine.dialerforwarder.plusOptional
import com.sixbynine.dialerforwarder.whatsapp.WhatsAppManager
import dagger.Module
import dagger.Provides
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

@Module
abstract class DialerAppInfoModule {

    @Module
    companion object {

        private val SYSTEM_DIAL_PACKAGE_NAMES =
            arrayOf("com.google.android.dialer", "com.samsung.android.incallui", "com.android.phone")

        private const val SYSTEM_CALL_PACKAGE_NAME = "com.android.server.telecom"

        @Provides
        @JvmStatic
        fun providePackageManagerDialerAppInfos(
            @ApplicationContext context: Context,
            factory: PackageManagerDialerAppInfoFactory
        ): List<PackageManagerDialerAppInfo> {
            val packageManager = context.packageManager

            val dialResolveInfos = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:3475555555")), 0
            )

            val callResolveInfos = packageManager.queryIntentActivities(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:3475555555")), 0
            )

            return dialResolveInfos.map { dialResolveInfo ->
                Pair<ResolveInfo, ResolveInfo?>(dialResolveInfo,
                    callResolveInfos.find { callResolveInfo ->
                        callResolveInfo.activityInfo?.packageName ==
                            dialResolveInfo.activityInfo?.packageName
                            || (dialResolveInfo.activityInfo?.packageName in SYSTEM_DIAL_PACKAGE_NAMES
                            && callResolveInfo.activityInfo?.packageName == SYSTEM_CALL_PACKAGE_NAME)
                    })
            }.distinct().mapNotNull { pair ->
                PackageManagerDialerAppInfo.fromResolveInfos(
                    factory, packageManager, pair.first, pair.second
                )
            }.filter { dialerAppInfo ->
                dialerAppInfo.dialPackageName != context.packageName
            }.sortedWith(Comparator { dialer1, dialer2 ->
                dialer1.label.toString().compareTo(dialer2.label.toString())
            })
        }

        @Provides
        @JvmStatic
        fun provideDialerAppInfos(
            packageManagerDialerAppInfos: List<PackageManagerDialerAppInfo>,
            @WhatsApp whatsAppDialerAppInfo: Optional<DialerAppInfo>
        ): List<DialerAppInfo> {
            return packageManagerDialerAppInfos.plusOptional(whatsAppDialerAppInfo)
        }

        @Provides
        @WhatsApp
        @JvmStatic
        fun provideWhatsAppDialerAppInfo(
            whatsAppManager: WhatsAppManager,
            phoneNumberUtil: PhoneNumberUtil
        ): Optional<DialerAppInfo> {

            if (!whatsAppManager.isWhatsAppInstalledAndUsesContacts()) {
                return Optional.absent()
            }

            val icon = whatsAppManager.getWhatsAppIcon()
            val label = whatsAppManager.getWhatsAppLabel() ?: return Optional.absent()

            return Optional.of(WhatsAppDialerAppInfo(whatsAppManager, label, icon))
        }
    }
}
