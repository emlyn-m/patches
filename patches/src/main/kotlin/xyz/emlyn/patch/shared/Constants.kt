package xyz.emlyn.patch.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    const val LOG_CLASS = "Lxyz/emlyn/patchextension/logging/Logging;"

    val TWITTER_COMPAT = Compatibility(
        name = "X",
        packageName = "com.twitter.android",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x1da1f2,
        targets = listOf(
            AppTarget(
                version = "12.35.0-prod.01"
            )
        )
    )
}
