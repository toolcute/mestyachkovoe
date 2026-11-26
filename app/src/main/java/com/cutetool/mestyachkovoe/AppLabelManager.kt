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

    fun ensureApplied(context: Context, label: AppLabel, icon: LauncherIcon) {
        val pm = context.packageManager
        val active = ComponentName(context.packageName, "${context.packageName}.${aliasName(label, icon)}")
        val state = runCatching { pm.getComponentEnabledSetting(active) }.getOrDefault(
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        )
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return
        apply(context, label, icon)
    }

    private fun aliasName(label: AppLabel, icon: LauncherIcon): String = when (icon) {
        LauncherIcon.CLASSIC -> "${label.baseAlias}Alias"
        LauncherIcon.LOCAL -> "${label.baseAlias}LocalAlias"
    }
}
