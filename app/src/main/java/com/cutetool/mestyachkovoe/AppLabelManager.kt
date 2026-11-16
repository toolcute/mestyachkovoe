package com.cutetool.mestyachkovoe

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.cutetool.mestyachkovoe.data.AppLabel
import com.cutetool.mestyachkovoe.data.LauncherIcon

object AppLabelManager {
    fun apply(context: Context, label: AppLabel, icon: LauncherIcon) {
        val pm = context.packageManager
        val pkg = context.packageName
        val selectedAlias = aliasName(label, icon)

        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg.$selectedAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        AppLabel.entries.forEach { otherLabel ->
            LauncherIcon.entries.forEach { otherIcon ->
                val alias = aliasName(otherLabel, otherIcon)
                if (alias != selectedAlias) {
                    runCatching {
                        pm.setComponentEnabledSetting(
                            ComponentName(pkg, "$pkg.$alias"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        }
    }

    private fun aliasName(label: AppLabel, icon: LauncherIcon): String =
        if (icon == LauncherIcon.NEW) "${label.baseAlias}Alias" else "${label.baseAlias}OldAlias"
}
