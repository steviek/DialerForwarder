package com.sixbynine.dialerforwarder.dialerappinfo

import com.google.common.base.Optional
import com.sixbynine.dialerforwarder.DialerAppInfoProtoOuterClass.DialerAppInfoProto
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DialerAppInfoFactory @Inject internal constructor(
    private val packageManagerDialerAppInfos: List<PackageManagerDialerAppInfo>,
    @WhatsApp private val whatsAppDialerAppInfo: Optional<DialerAppInfo>
) {

    fun fromProto(proto: DialerAppInfoProto): DialerAppInfo? {
        if (proto.whatsApp) {
            return whatsAppDialerAppInfo.orNull()
        }

        return packageManagerDialerAppInfos.find {
                dialerAppInfo ->
            dialerAppInfo.dialClassName == proto.dialClassName &&
                    dialerAppInfo.dialPackageName == proto.dialPackageName }
    }
}